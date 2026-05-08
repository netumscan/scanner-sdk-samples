# Android Samples

## Quick start

Open `android/quick-start` in Android Studio, or run:

```bash
cd android/quick-start
./gradlew :app:assembleDebug
```

The app demonstrates the minimum BLE GATT flow:

- initialize `ScannerSdk`
- request Bluetooth permissions
- start discovery
- connect to a selected scanner
- display scan text from `ScannerSession.scanEvents`
- disconnect

## Full demo

Open `android/full-demo` in Android Studio, or run:

```bash
cd android/full-demo
./gradlew :demo:assembleDebug
```

The full demo is the maintained reference app. It includes discovery, BLE/SPP mode selection, session state, command console, module commands, app logs, diagnostics, localization, fake mode, and smoke-check documentation.

