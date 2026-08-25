# Changelog

All notable changes will be documented here.

## Unreleased

## 0.1.0 - 2026-08-25

- Replaced the custom bottom tab strip with an inset-aware Material 3 navigation bar, keeping app destinations clear of both three-button and gesture navigation while simplifying the selected-state styling.
- Improved live-preview halo luminance with a perceptual brightness curve, a brighter emissive core, and localized bloom so low settings remain legible while 100% visibly glows.
- Added a localized moment-based Scene Studio with live playback, custom names/descriptions, four-ring color editing, per-moment timing, smooth/jump transitions, reordering, duplication, and deletion. Eight executable custom scenes are transactionally uploaded over a serialized BLE write queue and retained by ESP32 for phone-free playback.
- Branded Challenger/Bluetooth Android launcher icon with cyan/orange RGB halos, adaptive and legacy density variants, and Android 13 themed-icon support.
- Persistent car/simplified live-preview selection with four large interactive halo controls on both Drive and Scenes screens.
- Initial monorepo structure.
- ESP32 four-ring pin map, favorite-color button cycle, long-press off action, and vehicle override firmware.
- Interactive Android dashboard with a four-halo front view, global/per-ring color controls, favorites, brightness, and animated show-mode previews.
- Separate Drive, Scenes, and Config navigation with localized English and Polish text.
- Supplied white 2013 Challenger front cutout with interactive halo overlays aligned to the original headlamps and retained yellow outer lamp centers.
- Pixel-accurate per-ring tint masks and a clean app-rendered vehicle shadow replace the oversized circular overlays and rough source-image ground edge.
- Stronger headlamp-clipped ambient glows and a subtly desaturated vehicle layer make live ring colors easier to read without enlarging the ring artwork.
- Full-spectrum touch color picker and editable 12-slot favorite-color collection, without manual HEX entry.
- A unified staged color-and-brightness studio with Cancel/Save actions, a fixed upper vehicle preview, and a compact brightness slider directly above the actions; gesture-heavy controls no longer interfere with Drive-screen scrolling.
- Expanded the shared Android/ESP32 scene catalog from 3 to 20 deliberate effects, grouped into amber signal patterns, everyday lighting, and parked-only show modes. Existing scene IDs 0–2 remain stable, while the favorite carousel uses the controller's durable palette.
- Frame-synchronized, elapsed-time scene previews replace the previous 260 ms stepped animation loop.
- Protocol-1.0 BLE service and Android GATT client with automatic discovery, connection status, command writes, state notifications, and authoritative reconnect synchronization.
- ESP32 stores and drives four independent colors, global brightness, editable favorites, and durable local scene selection while preserving physical-button and vehicle-input operation without a phone.
- Complete linked bill of materials, corrected 5 V regulator selection, BLE commissioning checks, and hardware wiring/safety documentation.
- Continuous integration and tagged APK release workflow.
