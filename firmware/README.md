# Firmware

PlatformIO/Arduino firmware for the classic ESP32-WROOM-32 30-pin DevKit.

## Current behavior

- Configures all 15 planned PWM outputs at 1 kHz, 8-bit resolution.
- Displays the saved solid colors across four independently controlled rings.
- Mirrors a uniform solid color and its global brightness on the reserved cabin RGB indicator pins.
- Replaces the cabin preview with a repeating full-brightness amber double flash whenever solid ring colors differ or a built-in/custom scene is active.
- A short button press stops any scene or mixed-color state and forces fallback ice white across all four rings; subsequent presses advance through the durable favorites. When a uniform solid color was merely off, the first press restores it.
- Holding the button for 850 ms turns the user lighting off.
- Forces all rings to bright white while the debounced, active-low vehicle signal on GPIO35 is active.
- Stores the user enabled state, favorite index, and initial vehicle-automation settings in ESP32 Preferences/NVS.
- Advertises a protocol-1.1 BLE GATT service as `D4WID-Ring` and accepts power, per-ring color, brightness, built-in/custom scene, favorite, and vehicle-automation commands.
- Notifies Android of the authoritative power state, four colors, brightness, active scene, vehicle input/override state, favorites, and firmware version after commands or physical input changes.
- Runs all 20 signal, everyday, and show scenes locally at roughly 66 render updates per second, so a connected phone is not required for effects. Scene 19 crossfades through the durable favorite-color collection.
- Stores eight custom-scene slots in NVS. Each slot supports 2–12 four-ring moments with independent `150..5000` ms timing and either smooth interpolation or a held jump; committed scenes keep running without the phone.

The initial built-in favorites are Ice, Amber, Red, Violet, Cyan, and Green. Android can replace the durable 1–12 color list through BLE; the physical button then uses the updated cycle without a phone. After the warning-reset ice white, the next press selects the favorite following an exact matching white, or the first favorite if that white is no longer saved.

## Build and upload

```bash
python3 -m pip install platformio==6.1.19
pio run -d firmware
pio run -d firmware -t upload --upload-port /dev/cu.usbserial-10
pio device monitor -d firmware --port /dev/cu.usbserial-10
```

If the upload is unstable, keep the upload speed at 115200 as configured in `platformio.ini`.

## Electrical assumptions

- HW-153 `S` inputs are active-high. Change `kModuleInputActiveLow` only after measuring a board that behaves differently.
- GPIO34 has an external 10 kΩ pull-up and a button to GND.
- GPIO35 is pulled high by the 3.3 V output side of the PC817 module and goes low when the sensed 12 V signal is active.
- The cabin indicator must not be connected until its driver/current requirements are known.

The canonical pin map is `include/PinMap.h` and must remain synchronized with `../hardware/PINOUT.md`.
