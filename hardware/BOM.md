# Bill of materials

This list describes the current prototype. Product links are references to the exact discussed listings; listings can expire, so match the electrical specification rather than only the title or product photo.

## Core electronics

| Quantity | Component | Reference and requirement |
|---:|---|---|
| 1 | ESP32-WROOM-32 30-pin DevKit, CH340, USB-C | [Discussed Allegro board](https://allegro.pl/oferta/esp32-30pin-wifi-bluetooth-usb-c-ch340-esp-wroom-32-devkitc-v1-16920206271); main controller with BLE |
| 4 | HW-153 V1.1 four-channel IRF540N MOSFET board | [Discussed Allegro module](https://allegro.pl/oferta/258-modul-4x-mosfet-irf540-irf540n-arduino-stm32-11148276256); one board per ring, three channels used |
| 4 | Existing 12 V common-anode analog RGB ring | Four wires: common positive plus separate R, G, and B negative returns |
| 2 | PC817/EL817 optoisolator module with 12 V input | [Discussed Allegro module](https://allegro.pl/produkt/modul-optoizolatora-1-kanalowego-pc817-12v-e5123d84-65a7-4efe-84a9-f1782654e68e?offerId=18445276315); one for the GPIO35 forced-white input and one for the GPIO36 daytime-light brightness input |
| 1 | Protected 12 V to regulated 5 V converter | [Pololu S18V20F5 item 2574](https://www.pololu.com/product/2574), or an automotive-rated equivalent with enough ESP32 peak-current margin |
| 1 | Normally-open momentary push button with dry contacts | One contact to GPIO34 and one to ESP32 ground |
| 3 | 10 kΩ resistor, plus spares | One external pull-up for GPIO34; one for each GPIO35/GPIO36 PC817 output if the verified modules do not already provide suitable 3.3 V pull-ups |
| 1 | Cabin RGB indicator and suitable driver/resistors | Exact electrical type is still to be selected and verified before wiring |

The similarly named [Pololu item 2577](https://www.pololu.com/product/2577) regulates to **12 V**, not 5 V. It cannot be used as the ESP32 supply. Neither regulator alone should be treated as a complete automotive load-dump protection system.

## Vehicle power and installation parts

- One main fuse holder and fuse, installed close to the battery take-off point.
- Separate fused branches for the ESP32/converter and the RGB-ring power distribution.
- A 0.5–1 A inline fuse close to each tapped vehicle signal feeding a PC817 input.
- Automotive transient suppression and reverse-polarity protection upstream of the 5 V converter, selected for the installation and expected load-dump environment.
- Fused 12 V distribution blocks or sealed junctions for the four rings and four HW-153 boards.
- Automotive wire sized from measured current and cable length; do not select ring-power wire size before measuring full-white current.
- Chassis-ground and positive-supply terminals appropriate for the chosen take-off points.
- Crimped, insulated, vibration-resistant connectors; heat-shrink tubing and strain relief.
- An electrically insulated, splash-resistant enclosure with suitable ventilation and secured PCB mounting.
- A USB-C data cable for the initial firmware upload and diagnostics.

## Cabin-indicator warning

Do not buy a self-cycling or automatic-color RGB push button as a substitute for the controlled indicator. The ESP32 needs independent electrical control of the red, green, and blue channels. Before purchase, obtain the button/indicator pinout, common-anode or common-cathode type, rated LED voltage, internal resistor information, and channel current. Its three LED channels may require resistors and/or a small transistor driver instead of direct GPIO connection.

## Measurements required before final installation

- Confirm that every ring is common-anode and identify its R, G, and B return wires.
- Measure one ring's current at full white and at the maximum intended brightness.
- Measure the combined current of all four rings before selecting final fuses, connectors, and wire gauge.
- Confirm the HW-153 input polarity and that a 3.3 V ESP32 signal switches every channel fully.
- Measure the chosen vehicle-light signal with the vehicle off, ignition on, engine running, and lights switched; determine whether it is steady, PWM-driven, low-side-switched, or diagnostic-pulsed.
- Confirm the cabin indicator's electrical type and current requirement.
- Measure parked current after the complete controller is installed and decide whether a hard disconnect or sleep/wake circuit is required.

## Not suitable as direct substitutes

- Pololu S18V20F12 item 2577: stable 12 V output, not an ESP32 5 V supply.
- A bare generic LM2596 board with no verified automotive transient or reverse-polarity protection.
- A self-animating RGB LED/button with only power and switch wires: it cannot mirror arbitrary ring colors.
- Direct battery voltage on any ESP32 GPIO or its 5 V/3.3 V rails.
