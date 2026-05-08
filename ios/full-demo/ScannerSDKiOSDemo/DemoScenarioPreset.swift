import Foundation

enum DemoScenarioPresetKind: CaseIterable {
    case quickScan
    case readInfo
    case readBattery
    case masterCommands
    case moduleParameters
    case dataRuleBuilder
}

struct DemoScenarioPreset: Identifiable, Hashable {
    let kind: DemoScenarioPresetKind
    let titleKey: String
    let summaryKey: String

    var id: DemoScenarioPresetKind { kind }
    var title: String { DemoStrings.tr(titleKey) }
    var summary: String { DemoStrings.tr(summaryKey) }
}

enum DemoScenarioPresets {
    static let all: [DemoScenarioPreset] = [
        DemoScenarioPreset(kind: .quickScan, titleKey: "scenario_quick_scan", summaryKey: "scenario_quick_scan_hint"),
        DemoScenarioPreset(kind: .readInfo, titleKey: "scenario_read_info", summaryKey: "scenario_read_info_hint"),
        DemoScenarioPreset(kind: .readBattery, titleKey: "scenario_read_battery", summaryKey: "scenario_read_battery_hint"),
        DemoScenarioPreset(kind: .masterCommands, titleKey: "scenario_master_commands", summaryKey: "scenario_master_commands_hint"),
        DemoScenarioPreset(kind: .moduleParameters, titleKey: "scenario_module_parameters", summaryKey: "scenario_module_parameters_hint"),
        DemoScenarioPreset(kind: .dataRuleBuilder, titleKey: "scenario_data_rule_builder", summaryKey: "scenario_data_rule_builder_hint"),
    ]
}
