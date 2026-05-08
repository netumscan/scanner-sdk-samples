import Foundation
import ScannerSDK

enum DemoConsoleCommand: Hashable {
    case basic(BasicDeviceCommand)
    case master(MasterCommand)
    case text(String)
}

struct DemoCommandEntry: Identifiable, Hashable {
    let title: String
    let command: DemoConsoleCommand
    let isDangerous: Bool

    var id: String {
        switch command {
        case .basic(let basic):
            return "basic:\(basic.rawValue)"
        case .master(let master):
            return "master:\(master.rawValue)"
        case .text(let text):
            return "text:\(text)"
        }
    }
}

extension DemoCommandEntry {
    var riskText: String {
        switch command {
        case .basic(let basic):
            let label = basic.localizedRiskLabel
            return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
        case .master(let master):
            let label = master.localizedRiskLabel
            return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
        case .text(let text):
            return Self.textRiskText(text)
        }
    }

    private static func textRiskText(_ command: String) -> String {
        switch command {
        case "$POWER#OFF":
            return DemoStrings.tr("risk_power_off")
        case "%#IFSNO$1", "%#IFSNO$4", "AT+MODE=2", "AT+MODE=1", "AT+MODE=3", "%#IFSNO$2", "%#IFSNO$3":
            return DemoStrings.tr("risk_transport_change")
        case "%000604":
            return DemoStrings.tr("risk_usb_auto_on")
        case "%000605":
            return DemoStrings.tr("risk_usb_auto_off")
        case "$RF#CH0%", "$RF#CH02", "$RF#CH0$", "%%ALL-CH":
            return DemoStrings.tr("risk_pairing")
        case "%%0H8":
            return DemoStrings.tr("risk_rf_bt_swap")
        default:
            return DemoStrings.tr("risk_high")
        }
    }
}

struct DemoCommandGroup: Identifiable, Hashable {
    let title: String
    let commands: [DemoCommandEntry]

    var id: String { commands.map(\.id).joined(separator: "|") }
}

enum DemoQuickAction: String, CaseIterable, Identifiable {
    case refreshInfo
    case getBattery
    case ackBeepOn
    case ackBeepOff
    case vibrateOn
    case vibrateOff

    var id: String { rawValue }
}

extension DemoQuickAction {
    private var commandCode: CommandCode {
        switch self {
        case .refreshInfo: return .getInfo
        case .getBattery: return .getBatteryInfo
        case .ackBeepOn: return .beep
        case .ackBeepOff: return .disableAckBeep
        case .vibrateOn: return .vibrateOn
        case .vibrateOff: return .vibrateOff
        }
    }

    var title: String {
        let command = commandCode
        guard let label = try? ScannerSDK.shared.getCommandCodeLabel(command) else {
            return "\(command)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    var isDangerous: Bool {
        switch self {
        case .refreshInfo, .getBattery:
            return false
        case .ackBeepOn, .ackBeepOff, .vibrateOn, .vibrateOff:
            return true
        }
    }

    var riskText: String {
        let label = commandCode.localizedRiskLabel
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }
}

struct DemoQuickActionRow: Identifiable, Hashable {
    let title: String
    let actions: [DemoQuickAction]

    var id: String { actions.map(\.id).joined(separator: "|") }
}

struct DemoModuleAction: Identifiable, Hashable {
    let id: String
    let title: String
    let family: ModuleFamily
    let kind: ModuleCommandKind
    let parameterID: UInt32
    let payload: Data
    let persist: Bool
    let isDangerous: Bool
    let riskText: String

    var executionLabel: String {
        let payloadSummary = payload.isEmpty ? "-" : hexSummary(payload)
        return "\(title): family=\(family) kind=\(kind) parameterID=0x\(String(parameterID, radix: 16).uppercased()) payload=\(payloadSummary) persist=\(persist)"
    }
}

struct DemoModuleActionRow: Identifiable, Hashable {
    let title: String
    let actions: [DemoModuleAction]

    var id: String { actions.map(\.id).joined(separator: "|") }
}
