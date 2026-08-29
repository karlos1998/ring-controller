# Installation checklist

## Bench verification

- [ ] Firmware uploaded and `D4WID-Ring` BLE advertisement visible on the Android phone.
- [ ] Android app connection status reaches Connected and initial ESP32 state appears in the UI.
- [ ] A color, brightness, power, favorite, and scene change from the app is reflected by the ESP32 and returned in a state notification.
- [ ] Physical-button changes appear in the connected app without reopening it.
- [ ] Disconnect/reconnect restores the authoritative ESP32 state instead of stale app values.
- [ ] Ring common-positive wiring verified with a meter.
- [ ] HW-153 output-positive bus continuity verified while unpowered.
- [ ] With all power disconnected, HW-153 channel `+` and `-` continuity to the other channels and to `M1+`/`M1-` documented.
- [ ] HW-153 channel `+` and `-` remain disconnected from ESP32; only each `S` input is connected to its assigned GPIO.
- [ ] One ring tested from a current-limited bench supply.
- [ ] R/G/B mapping confirmed and documented.
- [ ] Outputs remain off during ESP32 reset and firmware upload.
- [ ] First PC817 input tested at 12 V and confirmed active-low on GPIO35.
- [ ] Second PC817 input tested at 12 V and confirmed active-low on GPIO36 / `VP`.
- [ ] GPIO36 activation applies the configured brightness once; later app/scene changes remain until the signal is cycled off and on.
- [ ] Physical button tested with a 10 kΩ external pull-up.
- [ ] Full-white current measured for each ring and for the complete set.
- [ ] MOSFET and connector temperatures checked during an extended full-load test.

## Vehicle preparation

- [ ] Battery connection point and main fuse rating selected.
- [ ] Main fuse positioned close to the battery source.
- [ ] Converter branch and lighting branches individually fused.
- [ ] DC/DC converter accepts automotive voltage variation and transients.
- [ ] Signal tap measured relative to chassis ground with the vehicle off, ignition on, engine running, and lights switched.
- [ ] PWM or diagnostic pulses accounted for in firmware filtering.
- [ ] Wiring protected from sharp edges, exhaust heat, moisture, and moving parts.
- [ ] Electronics mounted in an insulated, ventilated enclosure.

## Final checks

- [ ] Correct behavior with the engine off.
- [ ] Correct behavior during engine cranking.
- [ ] Correct behavior with the alternator charging.
- [ ] Physical button works with no phone connected.
- [ ] Both vehicle-signal automations work with no phone connected.
- [ ] Android reconnects and resynchronizes after ESP32 power cycling and after leaving/re-entering BLE range.
- [ ] ESP32 parked-current strategy measured and accepted.
- [ ] Installation does not interfere with mandatory road lighting.
