package com.netumscan.scannersdk.demo

internal enum class DemoScenarioPresetKind {
    QUICK_SCAN,
    READ_INFO,
    READ_BATTERY,
    MASTER_COMMANDS,
    MODULE_PARAMETERS,
    DATA_RULE_BUILDER,
}

internal data class DemoScenarioPreset(
    val kind: DemoScenarioPresetKind,
    val titleProvider: () -> String,
    val summaryProvider: () -> String,
)

internal object DemoScenarioPresets {
    fun all(): List<DemoScenarioPreset> = listOf(
        DemoScenarioPreset(
            DemoScenarioPresetKind.QUICK_SCAN,
            { DemoStrings.text(R.string.scenario_quick_scan) },
            { DemoStrings.text(R.string.scenario_quick_scan_hint) },
        ),
        DemoScenarioPreset(
            DemoScenarioPresetKind.READ_INFO,
            { DemoStrings.text(R.string.scenario_read_info) },
            { DemoStrings.text(R.string.scenario_read_info_hint) },
        ),
        DemoScenarioPreset(
            DemoScenarioPresetKind.READ_BATTERY,
            { DemoStrings.text(R.string.scenario_read_battery) },
            { DemoStrings.text(R.string.scenario_read_battery_hint) },
        ),
        DemoScenarioPreset(
            DemoScenarioPresetKind.MASTER_COMMANDS,
            { DemoStrings.text(R.string.scenario_master_commands) },
            { DemoStrings.text(R.string.scenario_master_commands_hint) },
        ),
        DemoScenarioPreset(
            DemoScenarioPresetKind.MODULE_PARAMETERS,
            { DemoStrings.text(R.string.scenario_module_parameters) },
            { DemoStrings.text(R.string.scenario_module_parameters_hint) },
        ),
        DemoScenarioPreset(
            DemoScenarioPresetKind.DATA_RULE_BUILDER,
            { DemoStrings.text(R.string.scenario_data_rule_builder) },
            { DemoStrings.text(R.string.scenario_data_rule_builder_hint) },
        ),
    )
}
