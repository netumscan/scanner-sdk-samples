# iOS Samples

iOS samples for Netum Scanner SDK `2.0.1`.

## Requirements

- Xcode with an iOS 16 or later SDK.
- XcodeGen 2.38 or later.
- An iPhone running iOS 16 or later for BLE validation.
- A BLE scanner.
- Internet access while SwiftPM resolves the public package and binary
  XCFramework.

Both applications consume:

```yaml
packages:
  ScannerSDK:
    url: https://github.com/netumscan/scanner-sdk-ios.git
    from: "2.0.1"
```

They do not use a local SwiftPM path or a revision pin. The public Swift package
downloads the versioned binary XCFramework.

## Bluetooth permission

iOS displays the Bluetooth permission prompt when discovery first needs it. If
permission is denied, grant Bluetooth access in Settings and retry discovery
manually.

## Applications

- [Quick Start](quick-start): BLE GATT discovery, connection, scan display, and
  three safe commands.
- [Full Demo](full-demo): broader command coverage, diagnostics, localization,
  recovery flows, and Compatibility Record export.

## Project generation and validation

Run `xcodegen generate` inside either sample directory before opening or
building the generated Xcode project.

```bash
cd ios/quick-start
xcodegen generate
xcodebuild build \
  -project ScannerSDKQuickStart.xcodeproj \
  -scheme ScannerSDKQuickStart \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO

cd ../full-demo
xcodegen generate
xcodebuild build \
  -project ScannerSDKiOSDemo.xcodeproj \
  -scheme ScannerSDKiOSDemo \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO
```

Select your development team before running on an iPhone. The unsigned builds
are validation outputs; this repository does not publish IPA or TestFlight
builds.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
