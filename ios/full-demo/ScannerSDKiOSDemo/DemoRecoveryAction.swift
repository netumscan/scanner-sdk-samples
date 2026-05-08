import ScannerSDK

enum DemoRecoveryAction: Equatable {
    case enableBluetooth
    case allowBluetoothAccess
    case recoverBleScanner
    case recoverBleConnection

    var title: String {
        switch self {
        case .enableBluetooth:
            return DemoStrings.tr("open_app_settings")
        case .allowBluetoothAccess:
            return DemoStrings.tr("open_app_settings")
        case .recoverBleScanner:
            return DemoStrings.tr("open_app_settings")
        case .recoverBleConnection:
            return DemoStrings.tr("open_app_settings")
        }
    }

    var hint: String {
        switch self {
        case .enableBluetooth:
            return DemoStrings.tr("ios_enable_bluetooth_hint")
        case .allowBluetoothAccess:
            return DemoStrings.tr("open_app_settings_hint")
        case .recoverBleScanner:
            return DemoStrings.tr("ios_ble_scanner_unavailable_hint")
        case .recoverBleConnection:
            return DemoStrings.tr("ios_ble_connection_recovery_hint")
        }
    }
}

func demoRecoveryAction(for failure: DiscoveryFailure) -> DemoRecoveryAction? {
    guard !failure.recoverable else { return nil }
    if failure.code == .bleAdapterDisabled {
        return .enableBluetooth
    }
    if failure.code == .bleScannerUnavailable {
        return .recoverBleScanner
    }

    switch failure.bleScanIssue {
    case .registrationFailed, .internalError, .featureUnsupported, .outOfHardwareResources, .unknown:
        return .allowBluetoothAccess
    case .alreadyStarted, .throttled, .none:
        return nil
    }
}

func demoRecoveryAction(for failure: SessionFailure) -> DemoRecoveryAction? {
    guard failure.transportType == .bleGatt else { return nil }
    switch failure.code {
    case .platformStatusError, .gattFailure:
        return .recoverBleConnection
    case .unknown:
        return failure.bleTransportIssue == .platformStatusError ? .recoverBleConnection : nil
    case .connectionTimeout,
         .serviceDiscoveryStartFailed,
         .serviceDiscoveryFailed,
         .notifyServiceMissing,
         .notifyCharacteristicMissing,
         .setNotificationFailed,
         .notifyDescriptorMissing,
         .notifyDescriptorWriteFailed:
        return nil
    }
}
