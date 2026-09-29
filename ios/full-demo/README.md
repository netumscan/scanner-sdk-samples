# Scanner SDK iOS Full Demo

Maintained iOS reference application for Scanner SDK `2.0.1`.

## SDK dependency

`project.yml` resolves
`https://github.com/netumscan/scanner-sdk-ios.git` from `2.0.1`. The public
Swift package downloads the versioned binary XCFramework; no local SwiftPM path
or revision pin is used.

## What the sample covers

- Bluetooth permission handling, supported-model selection, BLE discovery, and
  connection lifecycle handling.
- Session state, scan control, device information, battery information, module
  commands, settings, and data-rule examples.
- English and Simplified Chinese UI resources.
- Recovery paths for permission denial, Bluetooth state changes, disconnects,
  and command failures.
- Redacted SDK diagnostics and a Compatibility Record containing SDK, commit,
  Demo version, build, platform, transport, model, and session state.
- An application privacy manifest plus the SDK privacy manifest delivered by
  SwiftPM.

The application is a reference integration and test console, not a production
application architecture template.

## Generate, test, and build

```bash
xcodegen generate

SIMULATOR_ID="<available-iphone-simulator-udid>"
xcodebuild test \
  -project ScannerSDKiOSDemo.xcodeproj \
  -scheme ScannerSDKiOSDemo \
  -destination "platform=iOS Simulator,id=${SIMULATOR_ID}" \
  CODE_SIGNING_ALLOWED=NO

xcodebuild build \
  -project ScannerSDKiOSDemo.xcodeproj \
  -scheme ScannerSDKiOSDemo \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO
```

Run the [public smoke checklist](docs/smoke-checklist.md) before a customer
demonstration or compatibility report.

## Privacy and distribution

Diagnostics record scan type, charset, and byte lengths instead of scan text or
raw hex. Exported logs and Compatibility Records must not contain scan payloads,
complete device identifiers, or serial numbers.

The unsigned build is only a validation gate. This repository does not publish
IPA, TestFlight, or app-store packages.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
