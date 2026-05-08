# iOS Samples

## Quick start

The quick-start sample uses SwiftUI and Swift Package Manager.

```bash
cd ios/quick-start
xcodegen generate
open ScannerSDKQuickStart.xcodeproj
```

The app demonstrates the minimum BLE GATT flow:

- initialize `ScannerSDK`
- start discovery
- connect to a selected scanner
- display scan text from `ScannerSession.scanEvents`
- disconnect

## Full demo

The full demo is the maintained reference app.

```bash
cd ios/full-demo
xcodegen generate
open ScannerSDKiOSDemo.xcodeproj
```

It includes discovery, session state, command console, module commands, app logs, diagnostics, localization, fake mode, and smoke-check documentation.

