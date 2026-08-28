# Ring Controller BLE protocol

Status: **protocol 1.2 implemented in ESP32 firmware and Android**.

## Transport and discovery

The ESP32 advertises as `D4WID-Ring`. The Android phone is the only expected GATT client. It scans for the service UUID, requests an MTU of 185 bytes, enables state notifications, and sends `GET` after connecting. The ESP32 is authoritative and sends a complete state snapshot after every accepted command and every physical-input change.

| GATT item | UUID | Properties |
|---|---|---|
| Ring Controller service | `7d2f0001-9c5a-4f28-b4d7-4b3a6d9a0001` | primary service |
| Command | `7d2f0002-9c5a-4f28-b4d7-4b3a6d9a0001` | write, write without response |
| State | `7d2f0003-9c5a-4f28-b4d7-4b3a6d9a0001` | read, notify |
| Controller info | `7d2f0004-9c5a-4f28-b4d7-4b3a6d9a0001` | read |

Messages are UTF-8 text. Fields use `|`; RGB colors are six uppercase hexadecimal digits without `#`. State snapshots remain small enough for the negotiated 185-byte MTU.

## Commands

```text
GET
POWER|0|1
BRIGHTNESS|0..255
COLOR|target|RRGGBB
SCENE|-1|0..19
FAVORITES|RRGGBB,RRGGBB,...
VEHICLE|0|1
DAYLIGHT|enabled|brightnessPercent
CUSTOM_BEGIN|slot|momentCount
CUSTOM_STEP|slot|index|durationMs|transition|ring0,ring1,ring2,ring3
CUSTOM_COMMIT|slot
CUSTOM_PLAY|slot
CUSTOM_DELETE|slot
```

The `POWER` syntax above means one final field containing either `0` or `1`, not two fields. `COLOR` target `0..3` selects one ring; target `255` selects all rings. `SCENE|-1` stops an effect; IDs `0..19` select one of the controller-rendered scenes below. Favorites contain 1–12 colors and define both the physical-button cycle and Scene 19.

Protocol 1.1 adds eight custom-scene slots (`0..7`). A custom scene contains `2..12` moments. Each moment has four colors, a duration of `150..5000` ms, and transition `0` (hold, then jump) or `1` (smooth interpolation to the next moment). Uploads are transactional: `CUSTOM_BEGIN` opens a staging buffer, all indexed `CUSTOM_STEP` messages populate it, and `CUSTOM_COMMIT` persists it only when every declared moment arrived. `CUSTOM_PLAY` starts the saved slot and `CUSTOM_DELETE` removes it. `SCENE|-1` also stops a custom scene.

Protocol 1.2 adds `DAYLIGHT|enabled|brightnessPercent`. `enabled` is `0` or `1`; `brightnessPercent` is `0..100`. If enabled, the ESP32 applies that global brightness once whenever the debounced GPIO36 / `VP` signal changes from inactive to active, and once at boot if the signal is already active. It does not turn the rings on, restore brightness when the signal turns off, or continuously lock brightness. Sending this command while the input is active applies the new level immediately.

Example two-moment upload:

```text
CUSTOM_BEGIN|0|2
CUSTOM_STEP|0|0|450|0|FF6A00,FF6A00,FF6A00,FF6A00
CUSTOM_STEP|0|1|550|1|000000,000000,000000,000000
CUSTOM_COMMIT|0
CUSTOM_PLAY|0
```

| ID | Scene | Behavior |
|---:|---|---|
| 0 | Amber Chase | One amber highlight runs around the four rings. |
| 1 | Demon Pulse | All rings breathe in deep red. |
| 2 | Spectrum Wave | A phase-shifted rainbow rolls across the rings. |
| 3 | Hazard Flash | All four rings blink amber together. |
| 4 | Double Hazard | Two quick all-amber flashes followed by a pause. |
| 5 | Inner / Outer | Outer and inner amber pairs alternate. |
| 6 | Left Amber | The two left rings blink amber together. |
| 7 | Right Amber | The two right rings blink amber together. |
| 8 | Inward Sweep | Repeated amber pair movement from the outside toward the grille. |
| 9 | Outward Sweep | Repeated amber pair movement from the grille toward the outside. |
| 10 | Bright White | Steady neutral white. |
| 11 | Ice White | Steady cool white. |
| 12 | Challenger Amber | Steady Dodge-inspired amber. |
| 13 | Courtesy Fade | Slow soft-white welcome breathing. |
| 14 | Amber Breathing | Slow warm amber breathing. |
| 15 | Redline Chase | Deep-red ping-pong scanner. |
| 16 | Cyan Scanner | Faster cyan ping-pong scanner with a tail. |
| 17 | Split Horizon | Cyan left and violet right breathe in opposition. |
| 18 | Mirror Rainbow | Outer and inner pairs rotate through offset hues. |
| 19 | Favorite Carousel | Smoothly crossfades through the saved favorites. |

Examples:

```text
POWER|1
BRIGHTNESS|224
COLOR|255|00E5E5
COLOR|2|FF304E
SCENE|2
FAVORITES|F2F6FF,FF6A00,FF304E,A855F7,00E5E5,43E07B
VEHICLE|1
DAYLIGHT|1|50
```

## State notification

```text
STATE|protocol|firmware|enabled|brightness|scene|override|vehicleSignal|vehicleAutomation|ring0,ring1,ring2,ring3|favorites|customScene|daylightSignal|daylightAutomation|daylightBrightnessPercent
```

Example:

```text
STATE|1.2|0.5.0|1|128|-1|0|0|1|00E5E5,00E5E5,00E5E5,00E5E5|F2F6FF,FF6A00,FF304E|0|1|1|50
```

- `enabled`: saved user power state.
- `brightness`: global brightness, `0..255`.
- `scene`: `-1` for solid colors or `0..19` for a running scene.
- `override`: `1` while the configured vehicle input is overriding normal output.
- `vehicleSignal`: debounced physical GPIO35 input state.
- `vehicleAutomation`: whether the vehicle-input action is enabled.
- `ring0..ring3`: saved solid colors; while `override=1`, actual output is forced white.
- `favorites`: durable physical-button color cycle.
- `customScene`: `-1` when no custom scene is active, otherwise controller slot `0..7`.
- `daylightSignal`: debounced physical GPIO36 / `VP` input state.
- `daylightAutomation`: whether the one-shot brightness action is enabled.
- `daylightBrightnessPercent`: configured retrigger level, `0..100`.

The info characteristic contains `INFO|protocol|firmware|deviceName`.

## Ownership and persistence

The ESP32 stores power, brightness, four solid colors, active built-in/custom scene, eight custom-scene definitions, favorites, favorite index, and both vehicle-automation configurations in Preferences/NVS. Custom names and descriptions remain phone-local UI metadata; timing, transitions, and ring colors are stored on the controller. The button, both vehicle inputs, and an already uploaded custom scene work without a phone. On reconnect, Android replaces its local preview with the controller snapshot, including colors changed with the physical button.

## Compatibility

- Protocol major/minor and firmware versions are present in every snapshot.
- Scene IDs `0..2` retain their original protocol-1.0 meanings; the appended IDs `3..19` are backward-compatible additions.
- Protocol 1.1 appends the optional `customScene` state field and new `CUSTOM_*` commands; a protocol-1.0 client can continue parsing the first 11 state fields and controlling built-in scenes.
- Protocol 1.2 appends three optional daylight fields and the `DAYLIGHT` command; older clients can ignore the appended fields, while the protocol-1.2 app supplies safe defaults when connected to older firmware.
- Unknown commands are ignored and logged over USB serial.
- Safety/off control remains available through `POWER|0`.
- Future optional fields must be appended; incompatible field changes require protocol 2.x.

## Default physical-button behavior

- Short press while off with a uniform saved solid color: restore that color.
- Short press while a built-in/custom scene is active or solid ring colors differ: stop the effect and force fallback ice white on all four rings.
- Short press while a uniform solid color is on: advance to the next durable favorite. After the fallback white, advance to the matching next favorite or the first favorite when white is absent.
- Long press (850 ms): turn user lighting off.
- The cabin indicator mirrors color and global brightness for a uniform solid state. Mixed colors or an active scene produce a repeating full-brightness amber double flash and long pause; user-off suppresses it and the vehicle override shows forced white.
