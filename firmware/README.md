# Firmware

PlatformIO/Arduino firmware for the classic ESP32-WROOM-32 30-pin DevKit.

## Current behavior

- Configures all 15 planned PWM outputs at 1 kHz, 8-bit resolution.
- Displays phase-shifted rainbow colors across four rings.
- Mirrors ring 1 on the reserved cabin RGB indicator pins.
- Toggles user lighting with an active-low momentary button on GPIO34.
- Forces all rings to bright white while the debounced, active-low vehicle signal on GPIO35 is active.
- Stores the user enabled state and initial vehicle-automation settings in ESP32 Preferences/NVS.

BLE configuration is not implemented yet. The current behavior is a hardware integration test and architecture foundation.

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

