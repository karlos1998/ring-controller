# Ring Controller Agent Guide

Work in this repository as the D4WID Ring project specialist.

## Read first

1. Read `README.md` for electrical safety, wiring, and project status.
2. Read `hardware/PINOUT.md` before changing GPIOs or output mappings.
3. Read `protocol/README.md` before changing app/firmware communication.
4. Inspect `git status -sb`; preserve unrelated user changes.

## Project invariants

- The visible app name is **D4WID Ring**; the neutral technical name is **Ring Controller**.
- Android application ID: `it.letscode.ringcontroller`.
- Android-only app: Kotlin + Jetpack Compose.
- Firmware: classic 30-pin ESP32-WROOM-32, PlatformIO, Arduino framework.
- Four common-anode 12 V analog RGB rings are switched on their negative R/G/B returns by four HW-153 boards.
- The direct pin plan uses 15 PWM outputs and must stay within the classic ESP32's 16 LEDC channels.
- GPIO34 is the momentary-button input; GPIO35 is the active-low PC817 vehicle-signal input. Both are input-only and lack internal pull-ups.
- Do not use boot-strapping GPIO0/2/5/12/15 or USB serial GPIO1/3 without an explicit, documented redesign.
- Never connect vehicle 12 V directly to an ESP32 GPIO, 3V3, or 5V pin.
- Vehicle automation must run locally on the ESP32 without requiring the Android app.

## Change discipline

- Keep firmware constants, `hardware/PINOUT.md`, and the root README synchronized.
- Version protocol changes and preserve backward compatibility where practical.
- Build firmware with `pio run -d firmware`.
- Build Android with `cd mobile && ./gradlew :app:assembleDebug`.
- Tag releases as `vMAJOR.MINOR.PATCH`; the release workflow publishes the signed APK.
- Never commit signing keys, passwords, tokens, or generated APKs.

