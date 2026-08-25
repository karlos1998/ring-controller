# Ring Controller protocol

Status: **planned; not yet implemented in firmware or Android**.

## Transport

Bluetooth Low Energy will be the initial transport. The phone is a client; the ESP32 advertises as `D4WID-Ring`. Configuration and the last durable state are stored on the ESP32.

The protocol must be explicitly versioned before the first BLE implementation. Do not silently change message fields once an APK has been released.

## Required capabilities

- Discover, connect, and report controller/firmware version.
- Read complete controller state.
- Set global enabled/disabled state.
- Set color and brightness independently for rings 1–4.
- Apply effects and presets.
- Read, reorder, add, and remove the physical button's favorite-color cycle.
- Configure momentary-button short, multi-click, and long-press actions.
- Enable/disable the 12 V input automation.
- Configure the action on 12 V activation and deactivation.
- Report physical input state and current override source.
- Persist configuration and acknowledge successful writes.

## Vehicle-signal model

Activation actions:

```text
IGNORE
FORCE_WHITE
TURN_ON_LAST
APPLY_PRESET
TURN_OFF
```

Deactivation actions:

```text
RESTORE_PREVIOUS
TURN_OFF
KEEP_CURRENT
APPLY_PRESET
```

Associated settings include enabled, white brightness, preset ID, activation debounce, and deactivation debounce.

## Compatibility rules

- Include a protocol major/minor version in controller information.
- Reject unsupported commands with a machine-readable error.
- Ignore unknown optional fields when safe.
- Keep safety/off commands available across compatible protocol versions.
- The ESP32 remains authoritative; the app refreshes state after every reconnect.

## Default physical-button behavior

- Short press while off: turn the rings on using the current favorite.
- Short press while on: advance to the next favorite color.
- Long press (850 ms): turn user lighting off.
- The cabin RGB indicator mirrors the shared solid color. When rings differ or an effect is active, it follows Ring 1 because one RGB indicator cannot display four colors simultaneously.

The favorites and button rules are durable controller configuration. The Android app edits them, but the ESP32 executes them without a connected phone.
