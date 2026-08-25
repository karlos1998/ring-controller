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

The current UI is an intentionally small project foundation. BLE connection, real ring controls, presets, and configuration persistence will be implemented against the versioned contract in `../protocol/`.

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

