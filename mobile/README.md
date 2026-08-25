# Android application

Native Android application for D4WID Ring.

- Package/application ID: `it.letscode.ringcontroller`
- Kotlin + Jetpack Compose
- Minimum Android version: Android 8.0 / API 26
- Distribution: signed APK attached to GitHub Releases

## Build

```bash
./gradlew :app:assembleDebug
```

The current interactive dashboard includes:

- A tabbed Drive / Scenes / Config layout that keeps everyday controls separate from show modes and hardware behavior.
- A four-halo front view matching the physical left/right layout.
- A transparent raster cutout of the supplied white 2013 Challenger front reference. A pixel-derived halo mask preserves the source rings' exact thin, partially occluded shape while allowing each lamp to be tinted independently. A headlamp-clipped color glow and subtly desaturated vehicle layer emphasize the live output without changing the ring geometry, while the original rough ground edge is replaced by an app-rendered soft shadow. Confirm redistribution rights for the supplied source image before publishing an APK or repository release.
- Global and per-ring selection.
- A full HSV color field and hue strip for choosing any RGB color without typing numeric values.
- An editable Favorites tab with up to 12 colors that can be saved from the picker, applied, or removed.
- One full-screen color studio keeps drag gestures away from the Drive screen and combines color, favorites, and brightness in a single staged edit. Its upper live preview reflects all changes—including visible glow intensity—and Cancel discards them without sending BLE commands; Save commits them to the controller.
- Scene previews are calculated from elapsed time and synchronized to display frames for smooth 30+ FPS motion instead of coarse timer steps.
- Automatic BLE discovery for `D4WID-Ring`, persistent connection status in the header, and a detailed status card with firmware version.
- Bidirectional protocol-1.0 synchronization: app changes are sent to the controller, while reconnects and physical input changes refresh power, colors, brightness, scene, favorites, and vehicle state from ESP32.
- A compact Drive card opens the combined color-and-brightness studio instead of embedding gesture-heavy controls in the scrolling page.
- A categorized library of 20 signal, everyday, and show scenes, with frame-synchronized previews and stable BLE IDs shared with firmware. The Favorite Carousel follows the editable saved palette.
- Summaries of the physical-button and vehicle-signal rules.
- English and Polish UI resources, selected automatically from the Android system language.
- A branded Challenger/Bluetooth launcher icon with cyan/orange halos, legacy density variants, adaptive-icon masks, Android 13 monochrome theming, and a matching dark system splash screen.

When no ESP32 is available, controls remain usable as a local visual preview. With `D4WID-Ring` in range, the ESP32 becomes the authoritative state source through the versioned contract in `../protocol/`.

## Launcher icon

The approved source, generated density variants, and regeneration instructions are kept in [`branding/`](branding/). Edit the master and rerun the generator instead of manually changing individual mipmap files.

## Release signing

Release builds use environment variables rather than checked-in keys:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
RELEASE_VERSION
VERSION_CODE
```

Never commit the keystore. Keep a secure offline backup because Android requires future updates to be signed with the same key.
