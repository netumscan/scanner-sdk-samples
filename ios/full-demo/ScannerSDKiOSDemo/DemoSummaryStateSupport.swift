import Foundation
import ScannerSDK

enum DemoStatusSummarySource: Equatable {
    case notConnected
    case sdkInitialized
    case session(SessionState)
    case sdkLabel(localizationKey: String, fallback: String)

    var text: String {
        switch self {
        case .notConnected:
            return DemoStrings.tr("not_connected")
        case .sdkInitialized:
            return DemoStrings.tr("sdk_initialized")
        case .session(let state):
            return statusText(state)
        case .sdkLabel(let localizationKey, let fallback):
            return DemoStrings.sdk(localizationKey, fallback: fallback)
        }
    }
}

enum DemoDeviceSummarySource: Equatable {
    case noneSelected
    case device(name: String, deviceId: String)

    var text: String {
        switch self {
        case .noneSelected:
            return DemoStrings.tr("no_selected_device")
        case .device(let name, let deviceId):
            return "\(name.ifBlank(DemoStrings.unknownDeviceName)) / \(deviceId)"
        }
    }
}

enum DemoInfoSummarySource: Equatable {
    case notLoaded
    case info(
        firmwareVersion: String,
        hardwareVersion: String,
        versionSeriesCode: String,
        bluetoothName: String,
        bluetoothFirmwareVersion: String
    )
    case value(String)

    var text: String {
        switch self {
        case .notLoaded:
            return DemoStrings.tr("device_info_not_loaded")
        case .info(let firmwareVersion, let hardwareVersion, let versionSeriesCode, let bluetoothName, let bluetoothFirmwareVersion):
            return "\(DemoStrings.tr("firmware"))=\(blankFallback(firmwareVersion))  " +
                "\(DemoStrings.tr("hardware"))=\(blankFallback(hardwareVersion))  " +
                "\(DemoStrings.tr("series_code"))=\(blankFallback(versionSeriesCode))  " +
                "\(DemoStrings.tr("bluetooth_name"))=\(blankFallback(bluetoothName))  " +
                "\(DemoStrings.tr("bluetooth_firmware_version"))=\(blankFallback(bluetoothFirmwareVersion))"
        case .value(let value):
            return value
        }
    }

    private func blankFallback(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "-" : value
    }
}

enum DemoSdkResolvedModelSummarySource: Equatable {
    case notLoaded
    case resolved(selectedModelKey: String, resolvedModel: String)
    case value(String)

    var text: String {
        switch self {
        case .notLoaded:
            return DemoStrings.tr("sdk_resolved_model_not_loaded")
        case .resolved(let selectedModelKey, let resolvedModel):
            return formatSdkResolvedModelSummary(
                selectedModelKey: selectedModelKey,
                resolvedModel: resolvedModel
            )
        case .value(let value):
            return value
        }
    }
}

enum DemoBatterySummarySource: Equatable {
    case notLoaded
    case value(voltageText: String, percent: Int)

    var text: String {
        switch self {
        case .notLoaded:
            return DemoStrings.tr("battery_not_loaded")
        case .value(let voltageText, let percent):
            return "\(voltageText) / \(percent)%"
        }
    }
}

enum DemoDeviceConfigSummarySource: Equatable {
    case notRead
    case notReadCharsetQuery
    case notSetSessionCacheEmpty
    case value(String)

    var text: String {
        switch self {
        case .notRead:
            return DemoStrings.tr("not_read")
        case .notReadCharsetQuery:
            return DemoStrings.tr("not_read_charset_query")
        case .notSetSessionCacheEmpty:
            return DemoStrings.tr("not_set_session_cache_empty")
        case .value(let value):
            return value
        }
    }
}
