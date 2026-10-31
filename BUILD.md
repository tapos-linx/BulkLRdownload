# BUILD.md — Reproducible Build & Installation Instructions for LRgetBD

## Prerequisites
- **JDK**: Java 17 or Java 21 (Eclipse Temurin 21 pre-configured in build container)
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
- **Build System**: Gradle (Kotlin DSL)

## Building the Debug APK
```bash
gradle assembleDebug
```
The compiled installable APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

## Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

## Installing on an Android Phone
1. Export or download the generated `app-debug.apk` from Google AI Studio's Build / Export menu (or build locally via `gradle assembleDebug`).
2. Transfer the APK to your Android phone or connect via USB with **USB Debugging** enabled (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
3. Open **LRgetBD**, grant Notification permission (Android 13+), and optionally tap **"ফোল্ডার পাল্টান" (Choose Folder)** to select any target folder in your phone memory via Android's Storage Access Framework.
