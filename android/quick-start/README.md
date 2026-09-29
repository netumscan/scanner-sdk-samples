# Scanner SDK Android Quick Start

English-only BLE GATT Quick Start for Scanner SDK `2.0.1`.

## SDK dependency

The application resolves the released AAR from Maven Central:

```kotlin
implementation("com.netumscan:scanner-sdk-android:2.0.1")
```

No local SDK project or `mavenLocal()` repository is used.

## What the sample covers

- Runtime Bluetooth permission handling.
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

## Build

```bash
./gradlew \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleRelease \
  :app:lintDebug
```

Use the Debug build for real-device validation. The Release APK is unsigned and
is only a build gate; it is not published.

## Privacy

Scan text is visible only in the in-memory recent-results list. It is not
logged, persisted, or exported. Device identifiers are used internally for
connection and list identity but are not displayed.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
