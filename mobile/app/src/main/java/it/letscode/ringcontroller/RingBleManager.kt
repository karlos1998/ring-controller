package it.letscode.ringcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

internal enum class BleConnectionState {
    LocalDemo,
    PermissionRequired,
    BluetoothOff,
    Unsupported,
    Scanning,
    Connecting,
    Connected,
    Disconnected,
}

@SuppressLint("MissingPermission")
internal class RingBleManager(private val context: Context) {
    var connectionState by mutableStateOf(BleConnectionState.PermissionRequired)
        private set
    var snapshot by mutableStateOf<ControllerSnapshot?>(null)
        private set
    var deviceName by mutableStateOf(BleProtocol.DEVICE_NAME)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null
    private var scanning = false
    private var pendingCommand: String? = null
    private val writeQueue = ArrayDeque<String>()
    private var writeInFlight = false
    private val sendPendingCommand = Runnable {
        pendingCommand?.let(::writeCommandNow)
        pendingCommand = null
    }
    private val scanTimeout = Runnable {
        if (scanning) {
            stopScan()
            updateConnectionState(BleConnectionState.Disconnected)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun markPermissionRequired() {
        updateConnectionState(BleConnectionState.PermissionRequired)
    }

    fun start() {
        if (!hasRequiredPermissions()) {
            markPermissionRequired()
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            updateConnectionState(BleConnectionState.Unsupported)
            return
        }
        if (!adapter.isEnabled) {
            updateConnectionState(BleConnectionState.BluetoothOff)
            return
        }

        closeGatt()
        stopScan()
        val scanner = adapter.bluetoothLeScanner ?: run {
            updateConnectionState(BleConnectionState.BluetoothOff)
            return
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanning = true
        updateConnectionState(BleConnectionState.Scanning)
        scanner.startScan(listOf(filter), settings, scanCallback)
        mainHandler.removeCallbacks(scanTimeout)
        mainHandler.postDelayed(scanTimeout, 10_000)
    }

    fun close() {
        stopScan()
        closeGatt()
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun setPower(enabled: Boolean) = writeCommand(BleProtocol.power(enabled), immediate = true)

    fun setBrightness(value: Float) = writeCommand(BleProtocol.brightness(value))

    fun setColor(target: Int?, color: Color) = writeCommand(BleProtocol.color(target, color))

    fun setScene(index: Int?) = writeCommand(BleProtocol.scene(index), immediate = true)

    fun setFavorites(colors: List<Color>) = writeCommand(BleProtocol.favorites(colors), immediate = true)

    fun setVehicleAutomation(enabled: Boolean) =
        writeCommand(BleProtocol.vehicleAutomation(enabled), immediate = true)

    fun uploadCustomScene(scene: CustomScene, playAfterUpload: Boolean) =
        enqueueCommands(BleProtocol.customSceneUpload(scene, playAfterUpload))

    fun deleteCustomScene(slot: Int) =
        writeCommand(BleProtocol.customSceneDelete(slot), immediate = true)

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!scanning) return
            stopScan()
            val discoveredName = result.scanRecord?.deviceName ?: result.device.name ?: BleProtocol.DEVICE_NAME
            mainHandler.post { deviceName = discoveredName }
            updateConnectionState(BleConnectionState.Connecting)
            bluetoothGatt = result.device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
            )
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            updateConnectionState(BleConnectionState.Disconnected)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) {
                gatt.close()
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                updateConnectionState(BleConnectionState.Connecting)
                if (!gatt.requestMtu(185)) gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                commandCharacteristic = null
                stateCharacteristic = null
                bluetoothGatt = null
                updateConnectionState(BleConnectionState.Disconnected)
                gatt.close()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateConnectionState(BleConnectionState.Disconnected)
                return
            }
            val service = gatt.getService(BleProtocol.SERVICE_UUID) ?: run {
                updateConnectionState(BleConnectionState.Disconnected)
                return
            }
            commandCharacteristic = service.getCharacteristic(BleProtocol.COMMAND_UUID)
            stateCharacteristic = service.getCharacteristic(BleProtocol.STATE_UUID)
            val state = stateCharacteristic
            if (commandCharacteristic == null || state == null) {
                updateConnectionState(BleConnectionState.Disconnected)
                return
            }
            gatt.setCharacteristicNotification(state, true)
            val descriptor = state.getDescriptor(CLIENT_CONFIGURATION_UUID)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            } else {
                finishConnection()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            finishConnection()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            mainHandler.post {
                if (writeQueue.isNotEmpty()) writeQueue.removeFirst()
                writeInFlight = false
                drainWriteQueue()
            }
        }

        @Deprecated("Deprecated in Android 13")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            handleState(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleState(value)
        }
    }

    private fun finishConnection() {
        updateConnectionState(BleConnectionState.Connected)
        writeCommandNow("GET")
    }

    private fun handleState(bytes: ByteArray) {
        val decoded = bytes.toString(Charsets.UTF_8)
        val parsed = BleProtocol.parseState(decoded) ?: return
        mainHandler.post { snapshot = parsed }
    }

    private fun writeCommand(command: String, immediate: Boolean = false) {
        if (immediate) {
            mainHandler.removeCallbacks(sendPendingCommand)
            pendingCommand = null
            writeCommandNow(command)
        } else {
            pendingCommand = command
            mainHandler.removeCallbacks(sendPendingCommand)
            mainHandler.postDelayed(sendPendingCommand, 35)
        }
    }

    private fun writeCommandNow(command: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { writeCommandNow(command) }
            return
        }
        writeQueue.addLast(command)
        drainWriteQueue()
    }

    private fun enqueueCommands(commands: List<String>) {
        mainHandler.removeCallbacks(sendPendingCommand)
        pendingCommand = null
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { enqueueCommands(commands) }
            return
        }
        commands.forEach(writeQueue::addLast)
        drainWriteQueue()
    }

    private fun drainWriteQueue() {
        if (writeInFlight || writeQueue.isEmpty()) return
        val gatt = bluetoothGatt ?: return
        val characteristic = commandCharacteristic ?: return
        val bytes = writeQueue.first().toByteArray(Charsets.UTF_8)
        val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
        if (accepted) {
            writeInFlight = true
        } else {
            mainHandler.postDelayed(::drainWriteQueue, 80)
        }
    }

    private fun stopScan() {
        if (!scanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanning = false
        mainHandler.removeCallbacks(scanTimeout)
    }

    private fun closeGatt() {
        val gatt = bluetoothGatt
        bluetoothGatt = null
        gatt?.disconnect()
        gatt?.close()
        commandCharacteristic = null
        stateCharacteristic = null
        writeQueue.clear()
        writeInFlight = false
    }

    private fun updateConnectionState(state: BleConnectionState) {
        mainHandler.post { connectionState = state }
    }

    private companion object {
        val CLIENT_CONFIGURATION_UUID: java.util.UUID =
            java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
