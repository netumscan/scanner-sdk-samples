# Android Samples

Android samples for Netum Scanner SDK `1.0.0`.

## Requirements

- Android Studio with Android API 35 installed.
- JDK 17.
- Android 8.0 (API 26) or later.
- A BLE scanner for real-device discovery and scan validation.
- Internet access while Gradle resolves Maven Central dependencies.

Both applications consume the released AAR:

```kotlin
implementation("com.netumscan:scanner-sdk-android:1.0.0")
```

They do not use `mavenLocal()` or a local `:sdk` project.

## Bluetooth permissions

- Android 12 and later: grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
- Android 8 through 11: grant location permission and keep system location
  services available when the OS requires them for BLE discovery.
- If permission is denied, grant it in system settings and retry discovery
  manually.

## Applications

- [Quick Start](quick-start): BLE GATT discovery, connection, scan display, and
  three safe commands.
- [Full Demo](full-demo): broader command coverage, diagnostics, localization,
  recovery flows, and Compatibility Record export.

## Validation builds

```bash
cd android/quick-start
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug

cd ../full-demo
./gradlew :demo:testDebugUnitTest :demo:assembleDebug :demo:assembleRelease
```

Release APKs are unsigned validation artifacts. This repository does not
publish APK or AAB files.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
