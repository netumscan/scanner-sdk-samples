import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    var localCharsetSummary: String { localCharset.displayName }

    var localTerminatorSummary: String { hexSummary(localTerminator) }

    var moduleCommandAvailabilitySummary: String {
        formatModuleCommandAvailabilitySummary(
            canExecute: canExecuteModuleCommands,
            supportsModuleCommands: supportsModuleCommands
        )
    }

    var eventCountSummary: String {
        DemoStrings.format("recent_event_count", events.count)
    }

    var protocolChannelKindSummary: String {
        let label = selectedChannelKind.localizedLabel
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    var displayDevices: [DiscoveredDevice] {
        let filtered = devices.filter { isPreferredScannerDevice($0) }
        let base = filtered.isEmpty ? devices : filtered
        return base.sorted(by: compareDiscoveredDevices)
    }

    func isPreferredScannerDevice(_ device: DiscoveredDevice) -> Bool {
        let name = device.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !name.isEmpty else { return false }
        return name.contains("scanner") || name.hasPrefix("cs ")
    }

    private func compareDiscoveredDevices(_ lhs: DiscoveredDevice, _ rhs: DiscoveredDevice) -> Bool {
        let lhsKnownModel = lhs.modelId != .unknown
        let rhsKnownModel = rhs.modelId != .unknown
        if lhsKnownModel != rhsKnownModel {
            return lhsKnownModel && !rhsKnownModel
        }

        let lhsSignal = lhs.rssi ?? Int.min
        let rhsSignal = rhs.rssi ?? Int.min
        if lhsSignal != rhsSignal {
            return lhsSignal > rhsSignal
        }

        let lhsName = lhs.name.ifBlank(lhs.deviceId).localizedLowercase
        let rhsName = rhs.name.ifBlank(rhs.deviceId).localizedLowercase
        if lhsName != rhsName {
            return lhsName < rhsName
        }

        return lhs.deviceId < rhs.deviceId
    }
}
