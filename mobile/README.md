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
- A full HSV color field, hue strip, and exact six-digit HEX input for choosing any RGB color.
- An editable Favorites tab with up to 12 colors that can be saved from the picker, applied, or removed.
- Favorite-color selection and brightness preview.
- Animated Amber Chase, Demon Pulse, and Spectrum Wave demonstrations.
- Summaries of the physical-button and vehicle-signal rules.
- English and Polish UI resources, selected automatically from the Android system language.

These controls currently update an in-app demo model. BLE connection, controller synchronization, editable favorites, durable presets, and configuration persistence will be implemented against the versioned contract in `../protocol/`.

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
