package it.letscode.ringcontroller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var bleManager: RingBleManager

    private val appPreferences by lazy {
        getSharedPreferences("d4wid_ring_ui", MODE_PRIVATE)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.all { it }) bleManager.start() else bleManager.markPermissionRequired()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = RingBleManager(applicationContext)
        enableEdgeToEdge()
        setContent {
            RingControllerApp(
                bleManager = bleManager,
                initialSimplifiedPreview = appPreferences.getBoolean("simplified_preview", false),
                onSimplifiedPreviewChanged = { enabled ->
                    appPreferences.edit().putBoolean("simplified_preview", enabled).apply()
                },
            )
        }
        if (bleManager.hasRequiredPermissions()) {
            bleManager.start()
        } else {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permissionLauncher.launch(permissions)
        }
    }

    override fun onDestroy() {
        bleManager.close()
        super.onDestroy()
    }
}

enum class VehicleActivationAction(val displayName: String) {
    Ignore("Do nothing"),
    ForceWhite("Force bright white"),
    TurnOnLast("Turn on last color"),
    ApplyPreset("Apply preset"),
    TurnOff("Turn rings off"),
}
