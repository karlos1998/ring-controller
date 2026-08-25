# Installation checklist

## Bench verification

- [ ] Ring common-positive wiring verified with a meter.
- [ ] HW-153 output-positive bus continuity verified while unpowered.
- [ ] One ring tested from a current-limited bench supply.
- [ ] R/G/B mapping confirmed and documented.
- [ ] Outputs remain off during ESP32 reset and firmware upload.
- [ ] PC817 input tested at 12 V and confirmed active-low on GPIO35.
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
- [ ] Vehicle-signal automation works with no phone connected.
- [ ] ESP32 parked-current strategy measured and accepted.
- [ ] Installation does not interfere with mandatory road lighting.

