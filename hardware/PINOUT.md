# ESP32 pin map

Canonical map for the 30-pin ESP32-WROOM-32 DevKit.

## PWM outputs

| Output index | Load | Color | GPIO | HW-153 input | HW-153 switched terminal |
|---:|---|---|---:|---|---|
| 0 | Ring 1 | R | 25 | Board 1 CH1 `S` | M2- |
| 1 | Ring 1 | G | 26 | Board 1 CH2 `S` | M3- |
| 2 | Ring 1 | B | 27 | Board 1 CH3 `S` | M4- |
| 3 | Ring 2 | R | 32 | Board 2 CH1 `S` | M2- |
| 4 | Ring 2 | G | 33 | Board 2 CH2 `S` | M3- |
| 5 | Ring 2 | B | 4 | Board 2 CH3 `S` | M4- |
| 6 | Ring 3 | R | 13 | Board 3 CH1 `S` | M2- |
| 7 | Ring 3 | G | 14 | Board 3 CH2 `S` | M3- |
| 8 | Ring 3 | B | 16 | Board 3 CH3 `S` | M4- |
| 9 | Ring 4 | R | 17 | Board 4 CH1 `S` | M2- |
| 10 | Ring 4 | G | 18 | Board 4 CH2 `S` | M3- |
| 11 | Ring 4 | B | 19 | Board 4 CH3 `S` | M4- |
| 12 | Cabin indicator | R | 21 | Driver pending | Pending |
| 13 | Cabin indicator | G | 22 | Driver pending | Pending |
| 14 | Cabin indicator | B | 23 | Driver pending | Pending |

All HW-153 `+` control pins connect to ESP32 3V3. All HW-153 `-` control pins connect to ESP32 GND. The ESP32 pin connects only to `S`.

## Inputs

| Function | GPIO | Mode | Required external circuit |
|---|---:|---|---|
| Momentary button | 34 | active-low input | 10 kΩ pull-up to 3.3 V; button to GND |
| Vehicle-signal optoisolator | 35 | active-low input | PC817 output powered at 3.3 V; module output pull-up or external 10 kΩ |
| Daytime-light optoisolator | 36 (`VP`) | active-low input | Second PC817 output powered at 3.3 V; module output pull-up or external 10 kΩ |

GPIO34, GPIO35, and GPIO36 are input-only and do not provide software-selectable internal pull-ups.

## Pins intentionally avoided

- GPIO0, GPIO2, GPIO5, GPIO12, GPIO15: boot-strapping behavior.
- GPIO1 and GPIO3: USB serial TX/RX.
- GPIO6–GPIO11: connected to module flash; never use.
- GPIO39 (`VN`) remains available as an input-only expansion pin; it also lacks an internal pull-up.

Any change to this file must be mirrored in `firmware/include/PinMap.h` and the root `README.md`.
