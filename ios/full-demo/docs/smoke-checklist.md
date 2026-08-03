# iOS Full Demo Smoke Checklist

Validate this source sample against Scanner SDK `1.0.0` and record the
iPhone, iOS version, scanner model, and firmware used for the run.

## Build and launch

- [ ] Simulator unit tests and the generic-device unsigned build succeed.
- [ ] The application launches on an iPhone running iOS 16 or later.
- [ ] Bluetooth permission denial produces an actionable message.
- [ ] Granting permission in Settings and retrying discovery works without
      reinstalling.

## Discovery and session

- [ ] The intended BLE model can be selected.
- [ ] Start and stop discovery work and duplicate advertisements do not create
      duplicate device rows.
- [ ] The device name, model, and RSSI are useful without displaying a complete
      device identifier.
- [ ] Connect reaches Ready and manual disconnect returns to discovery.
- [ ] Unexpected disconnect leaves the UI recoverable without automatic
      reconnection.

## Scan and commands

- [ ] Continuous scanning preserves event order and the session remains
      responsive.
- [ ] UTF-8 and GBK scans show the expected text, charset, text byte length, and
      raw byte length.
- [ ] Trigger Scan, Refresh Device Info, and Read Battery report success,
      failure, or unsupported state accurately.
- [ ] Any reversible write test restores the original value.

## Recovery

- [ ] Bluetooth off/on recovery succeeds after manual retry.
- [ ] Disconnect and reconnect succeed without stale device or command state.
- [ ] A failed command does not block the next supported command.

## Privacy and evidence

- [ ] The application and SDK privacy manifests declare no tracking.
- [ ] Logs contain no scan text, raw hex, complete device identifier, or serial
      number.
- [ ] The Compatibility Record contains SDK version, SDK commit, Demo version,
      Demo build, platform, transport, selected/resolved model, and session
      state.
- [ ] No IPA or TestFlight build is produced or published.
