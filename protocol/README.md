# Ring Controller BLE protocol

Status: **protocol 1.0 implemented in ESP32 firmware and Android**.

## Transport and discovery

The ESP32 advertises as `D4WID-Ring`. The Android phone is the only expected GATT client. It scans for the service UUID, requests an MTU of 185 bytes, enables state notifications, and sends `GET` after connecting. The ESP32 is authoritative and sends a complete state snapshot after every accepted command and every physical-input change.

| GATT item | UUID | Properties |
|---|---|---|
| Ring Controller service | `7d2f0001-9c5a-4f28-b4d7-4b3a6d9a0001` | primary service |
| Command | `7d2f0002-9c5a-4f28-b4d7-4b3a6d9a0001` | write, write without response |
| State | `7d2f0003-9c5a-4f28-b4d7-4b3a6d9a0001` | read, notify |
| Controller info | `7d2f0004-9c5a-4f28-b4d7-4b3a6d9a0001` | read |

Messages are UTF-8 text. Fields use `|`; RGB colors are six uppercase hexadecimal digits without `#`. Protocol 1.0 is intentionally small enough for the negotiated 185-byte MTU.

## Commands

```text
GET
POWER|0|1
BRIGHTNESS|0..255
COLOR|target|RRGGBB
SCENE|-1|0|1|2
FAVORITES|RRGGBB,RRGGBB,...
VEHICLE|0|1
```

The `POWER` syntax above means one final field containing either `0` or `1`, not two fields. `COLOR` target `0..3` selects one ring; target `255` selects all rings. `SCENE|-1` stops an effect; IDs `0`, `1`, and `2` are Amber Chase, Demon Pulse, and Spectrum Wave. Favorites contain 1–12 colors and define the physical-button cycle.

Examples:

```text
POWER|1
BRIGHTNESS|224
COLOR|255|00E5E5
COLOR|2|FF304E
SCENE|2
FAVORITES|F2F6FF,FF6A00,FF304E,A855F7,00E5E5,43E07B
VEHICLE|1
```

## State notification

```text
STATE|protocol|firmware|enabled|brightness|scene|override|vehicleSignal|vehicleAutomation|ring0,ring1,ring2,ring3|favorites
```

Example:

```text
STATE|1.0|0.2.0|1|224|-1|0|0|1|00E5E5,00E5E5,00E5E5,00E5E5|F2F6FF,FF6A00,FF304E
```

- `enabled`: saved user power state.
- `brightness`: global brightness, `0..255`.
- `scene`: `-1` for solid colors or `0..2` for a running scene.
- `override`: `1` while the configured vehicle input is overriding normal output.
- `vehicleSignal`: debounced physical GPIO35 input state.
- `vehicleAutomation`: whether the vehicle-input action is enabled.
- `ring0..ring3`: saved solid colors; while `override=1`, actual output is forced white.
- `favorites`: durable physical-button color cycle.

The info characteristic contains `INFO|protocol|firmware|deviceName`.

## Ownership and persistence

The ESP32 stores power, brightness, four solid colors, active scene, favorites, favorite index, and vehicle-automation enablement in Preferences/NVS. The button and vehicle input work without a phone. On reconnect, Android replaces its local preview with the controller snapshot, including colors changed with the physical button.

## Compatibility

- Protocol major/minor and firmware versions are present in every snapshot.
- Unknown commands are ignored and logged over USB serial.
- Safety/off control remains available through `POWER|0`.
- Future optional fields must be appended; incompatible field changes require protocol 2.x.

## Default physical-button behavior

- Short press while off: turn on with the current saved color.
- Short press while on: advance to the next durable favorite.
- Long press (850 ms): turn user lighting off.
- The cabin indicator mirrors Ring 1 when outputs differ or an effect is active.
