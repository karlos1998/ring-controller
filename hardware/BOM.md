# Bill of materials

## Confirmed core components

| Quantity | Component | Notes |
|---:|---|---|
| 1 | ESP32-WROOM-32 30-pin DevKit, CH340, USB-C | Main controller |
| 4 | HW-153 V1.1 four-channel IRF540N MOSFET board | One per ring; low-side switching |
| 4 | 12 V common-anode analog RGB ring | Four wires: common +, R-, G-, B- |
| 1 | PC817/EL817 optoisolator module, 12 V input | Output side must support 3.3 V |
| 1 | Momentary push button | Active-low input |
| 1 | 10 kΩ resistor | GPIO34 external pull-up; keep spares |
| 1 | Cabin RGB indicator | Exact part/type pending |
| 1 | Automotive-protected 12 V → regulated 5 V converter | Sized for ESP32 peak current |

## Installation components

- Main battery fuse and holder located close to the battery tap.
- Individually fused ESP32/converter and lighting branches.
- 0.5–1 A fuse for the sensed 12 V signal tap.
- Automotive wire sized from measured current and run length.
- Crimped, insulated, vibration-resistant connectors.
- Heat-shrink tubing, strain relief, and an electrically insulated enclosure.
- Optional transient/reverse-polarity protection if not integrated into the DC/DC converter.

## Must be measured before final installation

- Current of one ring at full white and at the maximum planned brightness.
- Combined current of all four rings.
- Whether each ring is definitely common-anode.
- Whether the selected vehicle signal is steady 12–14 V, PWM, low-side switched, or diagnostic-pulsed.
- Electrical type and current requirement of the cabin RGB indicator/button.

