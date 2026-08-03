# Scanner SDK Android Full Demo

Maintained Android reference application for Scanner SDK `1.0.0`.

## SDK dependency

The application resolves
`com.netumscan:scanner-sdk-android:1.0.0` from Maven Central. It does not
use a local SDK project or `mavenLocal()`.

## What the sample covers

- Android Bluetooth permissions, supported-model selection, BLE discovery, and
  connection lifecycle handling.
- Session state, scan control, device information, battery information, module
  commands, settings, and data-rule examples.
- English and Simplified Chinese UI resources.
- Recovery paths for permission denial, Bluetooth state changes, disconnects,
  and command failures.
- Redacted SDK diagnostics and a Compatibility Record containing SDK, commit,
  Demo version, build, platform, transport, model, and session state.

The application is a reference integration and test console, not a production
application architecture template.

## Build

```bash
./gradlew \
  :demo:testDebugUnitTest \
  :demo:assembleDebug \
  :demo:assembleRelease \
  :demo:lintDebug
```

Run the [public smoke checklist](docs/smoke-checklist.md) before a customer
demonstration or compatibility report.

## Privacy and distribution

Diagnostics record scan type, charset, and byte lengths instead of scan text or
raw hex. Exported logs and Compatibility Records must not contain scan payloads,
complete device identifiers, or serial numbers.

The Release APK is unsigned and is only a build gate. This repository does not
publish APK or AAB files.

## Support

Report sample integration problems in
[scanner-sdk-samples Issues](https://github.com/netumscan/scanner-sdk-samples/issues).
