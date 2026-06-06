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

- Android: `com.netumscan:scanner-sdk-android:0.1.2`
- iOS SwiftPM: `https://github.com/netumscan/scanner-sdk-ios.git`, pinned to the `v0.1.2` release revision

Release readiness notes:

- Android samples include `mavenLocal()` as a local validation fallback, but the public sample dependency is pinned to `com.netumscan:scanner-sdk-android:0.1.2`.
- iOS samples are pinned to the public SwiftPM repository `v0.1.2` revision because the public repository currently uses a `v0.1.2` GitHub Release tag. If a plain SemVer `0.1.2` tag is added later, samples can switch back to `from: 0.1.2`.

Use `quick-start` first when validating a fresh integration. Use `full-demo` when you need a more complete reference for permissions, discovery, connection lifecycle, command console behavior, logs, diagnostics, and model-specific capabilities.
