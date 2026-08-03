# scanner-sdk-samples

Public source samples for Netum Scanner SDK `1.0.0`.

The applications in this repository consume released SDK binaries. They do not
build the SDK from source or require a local SDK checkout.

## Samples

| Platform | Quick Start | Full Demo |
| --- | --- | --- |
| Android | [android/quick-start](android/quick-start) | [android/full-demo](android/full-demo) |
| iOS | [ios/quick-start](ios/quick-start) | [ios/full-demo](ios/full-demo) |

Start with a Quick Start for the smallest BLE GATT integration. Use a Full Demo
for command coverage, diagnostics, localization, recovery flows, and
Compatibility Record generation.

## Online SDK dependencies

- Android: `com.netumscan:scanner-sdk-android:1.0.0` from Maven Central.
- iOS: `https://github.com/netumscan/scanner-sdk-ios.git`, from `1.0.0`.
  The Swift package resolves the versioned binary XCFramework.

The samples do not use `mavenLocal()`, a local Android SDK project, a local
SwiftPM path, or a SwiftPM revision pin.

## Distribution boundary

This repository distributes source examples only. Debug and unsigned Release
builds are validation outputs. No APK, AAB, IPA, TestFlight build, or app-store
package is published here.

## Platform guides

- [Android samples](android/README.md)
- [iOS samples](ios/README.md)

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
