# Scanner SDK iOS Quick Start

English-only BLE GATT Quick Start for Scanner SDK `2.0.1`.

## SDK dependency

`project.yml` resolves the public Swift package:

```yaml
packages:
  ScannerSDK:
    url: https://github.com/netumscan/scanner-sdk-ios.git
    from: "2.0.1"
```

The package downloads the versioned binary XCFramework. No local SwiftPM path
or revision pin is used.

## What the sample covers

- BLE-supported model loading with CS7501 selected when available.
- Start, stop, and retry discovery.
- A deduplicated device list showing name, resolved model, and RSSI without the
  complete device identifier.
- Connect, Ready, disconnect, unexpected-disconnect cleanup, and manual
  rediscovery. Automatic reconnection is not enabled.
- The latest 20 scan results, held in memory only.
- `Trigger Scan`, `Refresh Device Info`, and `Read Battery`, enabled from
  session operation support.

The sample does not expose capability catalogs, setting read/write controls,
data rules, serial numbers, raw bytes, or log export.

## Generate, test, and build

```bash
xcodegen generate

SIMULATOR_ID="<available-iphone-simulator-udid>"
xcodebuild test \
  -project ScannerSDKQuickStart.xcodeproj \
  -scheme ScannerSDKQuickStart \
  -destination "platform=iOS Simulator,id=${SIMULATOR_ID}" \
  CODE_SIGNING_ALLOWED=NO

xcodebuild build \
  -project ScannerSDKQuickStart.xcodeproj \
  -scheme ScannerSDKQuickStart \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO
```

Select a development team before running on an iPhone.

## Privacy

The privacy manifest declares no tracking and no data collection. Scan text is
visible only in the in-memory recent-results list; it is not logged, persisted,
or exported. Device identifiers are not displayed.

No IPA or TestFlight build is published from this repository.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
