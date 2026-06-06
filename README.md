# scanner-sdk-samples

Public sample apps for Netum Scanner SDK.

This repository is intentionally split into two layers:

- `quick-start`: minimal apps that show the shortest working path: initialize SDK, discover a scanner, connect, receive scan text, and disconnect.
- `full-demo`: long-lived demo apps copied from the private development repository and adapted to consume the public SDK distribution.

## Samples

| Platform | Quick start | Full demo |
| --- | --- | --- |
| Android | [android/quick-start](android/quick-start) | [android/full-demo](android/full-demo) |
| iOS | [ios/quick-start](ios/quick-start) | [ios/full-demo](ios/full-demo) |

## SDK packages

- Android: `com.netumscan:scanner-sdk-android:0.1.3`
- iOS SwiftPM: `https://github.com/netumscan/scanner-sdk-ios.git`, from `0.1.3`

Release readiness notes:

- Android samples consume `com.netumscan:scanner-sdk-android:0.1.3` from public repositories. They do not use `mavenLocal()` in release validation.
- iOS samples consume the public SwiftPM repository from version `0.1.3`.

Use `quick-start` first when validating a fresh integration. Use `full-demo` when you need a more complete reference for permissions, discovery, connection lifecycle, command console behavior, logs, diagnostics, and model-specific capabilities.
