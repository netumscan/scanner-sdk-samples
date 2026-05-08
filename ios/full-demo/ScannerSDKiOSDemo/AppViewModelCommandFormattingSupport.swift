import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func commandCodeTitle(_ command: CommandCode) -> String {
        guard let label = try? ScannerSDK.shared.getCommandCodeLabel(command) else {
            return "\(command)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    func formatCommandResponse(prefix: String, response: CommandResponse) -> String {
        let emptyText = DemoStrings.tr("command_response_empty")
        let text = response.decodeText(localCharset).ifBlank(emptyText)
        let raw = response.rawHex.ifBlank(emptyText)
        let records = response.decodeRecords(localCharset)
        if !response.acknowledged && isFailureText(text) {
            return "\(prefix) \(DemoStrings.tr("command_response_failed")): \(text) raw=\(raw)"
        }
        if response.recordCount <= 0 {
            return "\(prefix) \(DemoStrings.tr("command_response_ack"))=\(response.acknowledged) text=\(text) raw=\(raw)"
        }
        let first = records.first?.ifBlank(emptyText) ?? emptyText
        let summary = "\(prefix) \(DemoStrings.tr("command_response_ack"))=\(response.acknowledged) \(DemoStrings.tr("command_response_records"))=\(response.recordCount) \(DemoStrings.tr("command_response_complete"))=\(response.recordsComplete) \(DemoStrings.tr("command_response_first"))=\(first) text=\(text) raw=\(raw)"
        let details = records.joined(separator: "\n")
        return details.isEmpty ? summary : "\(summary)\n\(details)"
    }

    func commandText(for command: MasterCommand) -> String {
        (try? ScannerSDK.shared.getMasterCommandDescriptor(command).text) ?? ""
    }

    func masterCommandLabel(_ command: MasterCommand) -> String {
        guard let label = try? ScannerSDK.shared.getMasterCommandLabel(command) else {
            return "\(command)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    func basicCommandLabel(_ command: BasicDeviceCommand) -> String {
        guard let label = try? ScannerSDK.shared.getBasicDeviceCommandLabel(command) else {
            return "\(command)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    func currentModuleActionTitle(id: String, fallback: String) -> String {
        DemoCommandCatalog.moduleActionRows
            .flatMap(\.actions)
            .first { $0.id == id }?
            .title ?? fallback
    }

    func moduleParameterCommandTitle(
        kind: ModuleCommandKind,
        family: ModuleFamily,
        parameterID: UInt32,
        fallbackLabel: String
    ) -> String {
        let parameterTitle = DemoModuleSettingsCatalog.presets(for: family)
            .first { $0.parameterID == parameterID }?
            .title ?? fallbackLabel
        let action: String
        switch kind {
        case .readParameter:
            action = DemoStrings.tr("read_module_parameter")
        case .writeParameter:
            action = DemoStrings.tr("write_module_parameter")
        default:
            return fallbackLabel
        }
        return "\(action) 0x\(formatModuleParameterID(parameterID)) \(parameterTitle)"
    }

    func commandText(for command: BasicDeviceCommand) -> String {
        switch command {
        case .getVersion: return "$SW#VER"
        case .factoryReset: return "%#IFSNO$B"
        case .writeCustomDefaults: return "%#IFSNO$CS"
        case .restoreCustomDefaults: return "%#IFSNO$CR"
        case .normalMode: return "%#NORMD"
        case .storeMode: return "%#INVMD"
        case .uploadMemoryData: return "%#TXMEM"
        case .getMemoryBarcodeCount: return "%#+TCNT"
        case .uploadMemoryDataAndClear: return "%#TXMEM#C"
        case .getMemoryUsage: return "%#+TCNT#"
        case .clearMemory: return "%#*NEW*"
        case .autoStoreModeOff: return "%AutoSav#Off"
        case .autoStoreModeOn: return "%AutoSav#On"
        }
    }

    func encodeSsiCommand(_ commandText: String) -> Data {
        let payload = Data(commandText.utf8)
        var frame = Data()
        frame.append(0x02)
        frame.append(UInt8(payload.count + 4))
        frame.append(0x0A)
        frame.append(payload)

        let crc = checksum(frame.dropFirst())
        frame.append(UInt8((crc >> 8) & 0xFF))
        frame.append(UInt8(crc & 0xFF))
        frame.append(0x03)
        return frame
    }

    private func isFailureText(_ text: String) -> Bool {
        let labels = [
            DemoStrings.tr("basic_command_failed"),
            DemoStrings.tr("master_command_failed")
        ]
        return labels.contains { label in
            text.hasPrefix("\(label):") || text.hasPrefix("\(label)：")
        }
    }

    private func checksum<S: Sequence>(_ bytes: S) -> Int where S.Element == UInt8 {
        let array = Array(bytes)
        var sum = 0
        var weight = array.count
        for byte in array {
            sum += Int(byte) * weight
            if weight > 0 {
                weight -= 1
            }
        }
        return (0x10000 - (sum & 0xFFFF)) & 0xFFFF
    }
}
