package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.Ntc06hSettingDefinition

internal data class MasterConsoleSectionState(
    val isExecuting: Boolean,
    val canExecuteMasterCommands: Boolean,
    val quickActionsExpanded: Boolean,
    val commandsExpanded: Boolean,
    val quickActionCount: Int,
    val quickActionRows: List<CommandGroup<QuickActionCommand>>,
    val selectedMasterTab: Int,
    val masterTabItems: List<CommandGroup<ConsoleCommand>>,
    val selectedMasterTabItem: CommandGroup<ConsoleCommand>?,
    val selectedTabSections: List<CommandGroup<ConsoleCommand>>,
    val selectedMasterCommandCount: Int,
)

internal data class MasterConsoleSectionActions(
    val onQuickActionsExpandedChange: (Boolean) -> Unit,
    val onPendingQuickActionChange: (QuickActionCommand?) -> Unit,
    val onCommandsExpandedChange: (Boolean) -> Unit,
    val onSelectedMasterTabChange: (Int) -> Unit,
    val onPendingCommandChange: (ConsoleCommand?) -> Unit,
)

internal data class ModuleConsoleSectionState(
    val activeModuleFamily: ModuleFamily,
    val moduleSettingsExpanded: Boolean,
    val isNtc06hFamily: Boolean,
    val isExecuting: Boolean,
    val supportsModuleCommands: Boolean,
    val canExecuteModuleCommands: Boolean,
    val moduleSummary: String,
    val ntc06hSettings: List<Ntc06hSettingDefinition>,
    val ntc06hDomainGroups: List<Ntc06hUiDomainGroup>,
    val modulePresets: List<ModuleSettingPreset>,
    val moduleDomainGroups: List<ModuleDomainGroup>,
    val modulePresetIndexById: Map<Int, Int>,
    val ntc06hSettingIndexByKey: Map<String, Int>,
    val selectedModulePreset: ModuleSettingPreset?,
    val selectedNtc06hSetting: Ntc06hSettingDefinition?,
    val selectedModulePresetIndex: Int,
    val selectedNtc06hSettingIndex: Int,
    val moduleSelectedTab: Int,
    val expandedModuleFamilyKey: String?,
    val inlineCustomPresetId: Int?,
    val inlineNtc06hSettingKey: String?,
    val moduleParameterIdText: String,
    val modulePayloadHexText: String,
    val modulePersistWrite: Boolean,
    val ntc06hCustomCode: String,
    val ntc06hTemplateHexValue: String,
    val ntc06hSaveAfterWrite: Boolean,
)

internal data class ModuleConsoleSectionActions(
    val onModuleSettingsExpandedChange: (Boolean) -> Unit,
    val onModuleSelectedTabChange: (Int) -> Unit,
    val onExpandedModuleFamilyKeyChange: (String?) -> Unit,
    val onInlineCustomPresetIdChange: (Int?) -> Unit,
    val onInlineNtc06hSettingKeyChange: (String?) -> Unit,
    val onSelectedModulePresetIndexChange: (Int) -> Unit,
    val onSelectedNtc06hSettingIndexChange: (Int) -> Unit,
    val onModuleParameterIdTextChange: (String) -> Unit,
    val onModulePayloadHexTextChange: (String) -> Unit,
    val onModulePersistWriteToggle: () -> Unit,
    val onNtc06hCustomCodeChange: (String) -> Unit,
    val onNtc06hTemplateHexValueChange: (String) -> Unit,
    val onNtc06hSaveAfterWriteToggle: () -> Unit,
)

internal data class DataRuleConsoleSectionState(
    val isExecuting: Boolean,
    val canExecuteMasterCommands: Boolean,
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
