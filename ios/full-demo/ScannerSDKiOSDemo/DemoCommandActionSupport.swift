import Foundation
import ScannerSDK

extension DemoCommandCatalog {
    static var quickActionRows: [DemoQuickActionRow] {
        [
            DemoQuickActionRow(title: DemoStrings.tr("quick_group_info_battery"), actions: [.refreshInfo, .getBattery]),
            DemoQuickActionRow(title: DemoStrings.tr("quick_group_ack_beep"), actions: [.ackBeepOn, .ackBeepOff]),
            DemoQuickActionRow(title: DemoStrings.tr("quick_group_vibrate"), actions: [.vibrateOn, .vibrateOff]),
        ]
    }

    static var moduleActionRows: [DemoModuleActionRow] {
        [
            DemoModuleActionRow(
                title: "NTC06H",
                actions: [
                    ntc06hAction(
                        id: "ntc06h_save",
                        title: moduleCommandTitle(.saveSettings),
                        kind: .saveSettings,
                        settingCode: "",
                        riskText: DemoStrings.tr("risk_ntc06h_save")
                    ),
                    ntc06hAction(
                        id: "ntc06h_ack_on",
                        title: moduleActionPresetTitle("ntc06h_ack_on", fallback: "Enable Setting ACK"),
                        settingCode: "02421",
                        riskText: DemoStrings.tr("risk_ntc06h_ack_on")
                    ),
                    ntc06hAction(
                        id: "ntc06h_code39_on",
                        title: moduleActionPresetTitle("ntc06h_code39_on", fallback: "Code 39 On"),
                        settingCode: "00221",
                        riskText: DemoStrings.tr("risk_ntc06h_code39_on")
                    ),
                    ntc06hAction(
                        id: "ntc06h_code39_off",
                        title: moduleActionPresetTitle("ntc06h_code39_off", fallback: "Code 39 Off"),
                        settingCode: "00220",
                        riskText: DemoStrings.tr("risk_ntc06h_code39_off")
                    ),
                    ntc06hAction(
                        id: "ntc06h_ean13_on",
                        title: moduleActionPresetTitle("ntc06h_ean13_on", fallback: "EAN-13 On"),
                        settingCode: "00361",
                        riskText: DemoStrings.tr("risk_ntc06h_ean13_on")
                    ),
                    ntc06hAction(
                        id: "ntc06h_ean13_off",
                        title: moduleActionPresetTitle("ntc06h_ean13_off", fallback: "EAN-13 Off"),
                        settingCode: "00360",
                        riskText: DemoStrings.tr("risk_ntc06h_ean13_off")
                    ),
                    ntc06hAction(
                        id: "ntc06h_code128_on",
                        title: moduleActionPresetTitle("ntc06h_code128_on", fallback: "Code 128 On"),
                        settingCode: "00691",
                        riskText: DemoStrings.tr("risk_ntc06h_code128_on")
                    ),
                    ntc06hAction(
                        id: "ntc06h_code128_off",
                        title: moduleActionPresetTitle("ntc06h_code128_off", fallback: "Code 128 Off"),
                        settingCode: "00690",
                        riskText: DemoStrings.tr("risk_ntc06h_code128_off")
                    ),
                ]
            ),
            DemoModuleActionRow(
                title: "NT212X",
                actions: [
                    moduleAction(
                        id: "nt212x_qr_read",
                        title: moduleActionPresetTitle("nt212x_qr_read", fallback: "Read QR"),
                        family: .nt212x,
                        kind: .readParameter,
                        parameterID: UInt32(Nt212xParameterAliases.qrCodeEnable),
                        dangerous: false,
                        riskText: DemoStrings.tr("risk_nt212x_qr_read")
                    ),
                    moduleAction(
                        id: "nt212x_qr_on",
                        title: moduleActionPresetTitle("nt212x_qr_on", fallback: "QR On"),
                        family: .nt212x,
                        kind: .writeParameter,
                        parameterID: UInt32(Nt212xParameterAliases.qrCodeEnable),
                        payload: Data([0x01]),
                        persist: true,
                        riskText: DemoStrings.tr("risk_nt212x_qr_on")
                    ),
                    moduleAction(
                        id: "nt212x_qr_off",
                        title: moduleActionPresetTitle("nt212x_qr_off", fallback: "QR Off"),
                        family: .nt212x,
                        kind: .writeParameter,
                        parameterID: UInt32(Nt212xParameterAliases.qrCodeEnable),
                        payload: Data([0x00]),
                        persist: true,
                        riskText: DemoStrings.tr("risk_nt212x_qr_off")
                    ),
                ]
            ),
            DemoModuleActionRow(
                title: "NT280H",
                actions: [
                    nt280hAction(id: "nt280h_scan_key", title: moduleActionPresetTitle("nt280h_scan_key", fallback: "Trigger Scan"), parameterID: 0xA102, payload: 0x01, riskText: DemoStrings.tr("risk_nt280h_scan_key")),
                    nt280hAction(id: "nt280h_scan_auto", title: moduleActionPresetTitle("nt280h_scan_auto", fallback: "Auto Scan"), parameterID: 0xA102, payload: 0x02, riskText: DemoStrings.tr("risk_nt280h_scan_auto")),
                    nt280hAction(id: "nt280h_scan_continuous", title: moduleActionPresetTitle("nt280h_scan_continuous", fallback: "Continuous Scan"), parameterID: 0xA102, payload: 0x03, riskText: DemoStrings.tr("risk_nt280h_scan_continuous")),
                    nt280hAction(id: "nt280h_sleep_never", title: moduleActionPresetTitle("nt280h_sleep_never", fallback: "Never Sleep"), parameterID: 0xA107, payload: 0x01, riskText: DemoStrings.tr("risk_nt280h_sleep_never")),
                    nt280hAction(id: "nt280h_sleep_10s", title: moduleActionPresetTitle("nt280h_sleep_10s", fallback: "Sleep 10s"), parameterID: 0xA107, payload: 0x07, riskText: DemoStrings.tr("risk_nt280h_sleep_10s")),
                    nt280hAction(id: "nt280h_duplicate_500ms", title: moduleActionPresetTitle("nt280h_duplicate_500ms", fallback: "Duplicate 500ms"), parameterID: 0xA108, payload: 0x05, riskText: DemoStrings.tr("risk_nt280h_duplicate_500ms")),
                    nt280hAction(id: "nt280h_light_high", title: moduleActionPresetTitle("nt280h_light_high", fallback: "High Illumination"), parameterID: 0xA109, payload: 0x03, riskText: DemoStrings.tr("risk_nt280h_light_high")),
                    nt280hAction(id: "nt280h_sensitivity_high", title: moduleActionPresetTitle("nt280h_sensitivity_high", fallback: "High Sensitivity"), parameterID: 0xA10A, payload: 0x03, riskText: DemoStrings.tr("risk_nt280h_sensitivity_high")),
                ]
            ),
            DemoModuleActionRow(
                title: "SE4750",
                actions: [
                    moduleAction(id: "se4750_capabilities", title: moduleCommandTitle(.capabilitiesRequest), family: .se4750, kind: .capabilitiesRequest, dangerous: false, riskText: DemoStrings.tr("risk_se4750_capabilities_read")),
                    moduleAction(id: "se4750_aim_on", title: moduleCommandTitle(.aimOn), family: .se4750, kind: .aimOn, riskText: DemoStrings.tr("risk_se4750_aim_on")),
                    moduleAction(id: "se4750_aim_off", title: moduleCommandTitle(.aimOff), family: .se4750, kind: .aimOff, riskText: DemoStrings.tr("risk_se4750_aim_off")),
                    moduleAction(id: "se4750_light_on", title: moduleCommandTitle(.illuminationOn), family: .se4750, kind: .illuminationOn, riskText: DemoStrings.tr("risk_se4750_illumination_on")),
                    moduleAction(id: "se4750_light_off", title: moduleCommandTitle(.illuminationOff), family: .se4750, kind: .illuminationOff, riskText: DemoStrings.tr("risk_se4750_illumination_off")),
                    moduleAction(id: "se4750_all_on", title: moduleActionPresetTitle("se4750_all_on", fallback: "All Symbologies On"), family: .se4750, kind: .changeAllCodeTypes, payload: Data([0x01]), riskText: DemoStrings.tr("risk_se4750_all_symbologies_on")),
                    moduleAction(id: "se4750_all_off", title: moduleActionPresetTitle("se4750_all_off", fallback: "All Symbologies Off"), family: .se4750, kind: .changeAllCodeTypes, payload: Data([0x00]), riskText: DemoStrings.tr("risk_se4750_all_symbologies_off")),
                    moduleAction(id: "se4750_beep", title: moduleCommandTitle(.beep), family: .se4750, kind: .beep, riskText: DemoStrings.tr("risk_se4750_beep")),
                    moduleAction(id: "se4750_pager", title: moduleCommandTitle(.pagerMotorActivation), family: .se4750, kind: .pagerMotorActivation, riskText: DemoStrings.tr("risk_se4750_pager_motor")),
                ]
            ),
        ]
    }

    private static func moduleCommandTitle(_ kind: ModuleCommandKind) -> String {
        guard let label = try? ScannerSDK.shared.getModuleCommandKindLabel(kind) else {
            return "\(kind)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    private static func moduleActionPresetTitle(_ actionID: String, fallback: String) -> String {
        guard let label = try? ScannerSDK.shared.getModuleActionPresetLabel(actionID: actionID) else {
            return fallback
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }

    private static func ntc06hAction(
        id: String,
        title: String,
        kind: ModuleCommandKind = .writeParameter,
        settingCode: String,
        riskText: String
    ) -> DemoModuleAction {
        moduleAction(
            id: id,
            title: title,
            family: .ntc06h,
            kind: kind,
            payload: Data(settingCode.utf8),
            persist: false,
            riskText: riskText
        )
    }

    private static func nt280hAction(
        id: String,
        title: String,
        parameterID: UInt32,
        payload: UInt8,
        riskText: String
    ) -> DemoModuleAction {
        moduleAction(
            id: id,
            title: title,
            family: .nt280h,
            kind: .writeParameter,
            parameterID: parameterID,
            payload: Data([payload]),
            persist: true,
            riskText: riskText
        )
    }

    private static func moduleAction(
        id: String,
        title: String,
        family: ModuleFamily,
        kind: ModuleCommandKind,
        parameterID: UInt32 = 0,
        payload: Data = Data(),
        persist: Bool = false,
        dangerous: Bool = true,
        riskText: String
    ) -> DemoModuleAction {
        DemoModuleAction(
            id: id,
            title: title,
            family: family,
            kind: kind,
            parameterID: parameterID,
            payload: payload,
            persist: persist,
            isDangerous: dangerous,
            riskText: riskText
        )
    }
}
