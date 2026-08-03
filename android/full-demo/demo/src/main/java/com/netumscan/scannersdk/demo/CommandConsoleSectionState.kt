package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CapabilityEntry

internal data class MasterConsoleSectionState(
    val isExecuting: Boolean,
    val canExecuteMasterCommands: Boolean,
    val quickActionsExpanded: Boolean,
    val capabilityActionsExpanded: Boolean,
    val quickActionCount: Int,
    val quickActionRows: List<CommandGroup<QuickActionCommand>>,
    val actionCapabilityEntries: List<CapabilityEntry>,
)

internal data class MasterConsoleSectionActions(
    val onQuickActionsExpandedChange: (Boolean) -> Unit,
    val onPendingQuickActionChange: (QuickActionCommand?) -> Unit,
    val onCapabilityActionsExpandedChange: (Boolean) -> Unit,
    val onPendingCapabilityActionChange: (CapabilityEntry?) -> Unit,
)

internal data class ProductSettingsSectionState(
    val isExecuting: Boolean,
    val isLoadingSettings: Boolean,
    val hasReadySession: Boolean,
    val settingsReadSupported: Boolean,
    val settingsWriteSupported: Boolean,
    val settingDefinitions: List<CapabilityEntry>,
    val settingOptionsByKey: Map<String, List<ProductSettingOption>>,
    val settingGroups: List<ProductSettingGroup>,
    val capabilityReadValues: Map<String, com.netumscan.scannersdk.model.CapabilityValue>,
    val settingDrafts: Map<String, ProductSettingDraft>,
    val settingOperations: Map<String, ProductSettingOperation>,
    val settingValidationErrors: Map<String, String>,
    val settingRecentlyUpdatedKeys: Set<String>,
)

internal enum class ProductSettingOperation {
    READING,
    WRITING,
}

internal data class DataRuleConsoleSectionState(
    val isExecuting: Boolean,
    val canExecuteMasterCommands: Boolean,
    val dataRulesSupported: Boolean,
    val deviceCharsetSummary: UiText,
    val scanCharsetSummary: String,
    val scanTerminatorSummary: String,
    val parseStateExpanded: Boolean,
    val dataRuleExpanded: Boolean,
    val builderMode: DataRuleFormMode,
    val builderValueA: String,
    val builderValueB: String,
)

internal data class DataRuleConsoleSectionActions(
    val onParseStateExpandedChange: (Boolean) -> Unit,
    val onDataRuleExpandedChange: (Boolean) -> Unit,
    val onBuilderModeChange: (DataRuleFormMode) -> Unit,
    val onBuilderValueAChange: (String) -> Unit,
    val onBuilderValueBChange: (String) -> Unit,
)
