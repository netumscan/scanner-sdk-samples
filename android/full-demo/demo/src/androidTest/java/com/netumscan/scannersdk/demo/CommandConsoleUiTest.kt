package com.netumscan.scannersdk.demo

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.formatModuleParameterId
import com.netumscan.scannersdk.model.ntc06hTemplateValueOrEmpty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CommandConsoleUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.ZH)
        DemoSessionCoordinator.clear()
    }

    @Test
    fun page_switches_to_module_scope() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageContent(
                    vm = vm,
                    uiState = CommandConsoleUiState(
                        currentModuleFamily = ModuleFamily.NT212X,
                        supportsModuleCommands = true,
                        canExecuteModuleCommands = true,
                        hasReadySession = true,
                        capabilitySummary = rawDisplayText("supports module commands"),
                        moduleSummary = rawDisplayText("NT212X"),
                    ),
                    context = composeRule.activity,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SCOPE_MODULE).performClick()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_MODULE_COMMANDS).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("主控快捷操作").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun generic_module_catalog_opens_custom_editor() {
        val firstFamily = ModuleSettingsCatalog.domainGroupsFor(ModuleFamily.NT212X)
            .first()
            .families
            .first { family -> family.sections.any { it.presets.isNotEmpty() } }
        val firstPresetParameterId = firstFamily.sections.first { it.presets.isNotEmpty() }.presets.first().parameterId
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                GenericModuleCatalogTestHarness(vm = vm, family = ModuleFamily.NT212X)
            }
        }

        waitUntilTagExists(DemoTestTags.consoleModuleCustomEdit(firstPresetParameterId))
        composeRule.onNodeWithTag(DemoTestTags.consoleModuleCustomEdit(firstPresetParameterId)).performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE)
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE).assertIsDisplayed()
    }

    @Test
    fun ntc06h_module_catalog_opens_custom_editor_with_save_controls() {
        val firstFamily = buildNtc06hDomainGroups()
            .first()
            .families
            .first { family -> family.sections.any { it.settings.isNotEmpty() } }
        val firstSettingKey = firstFamily.sections.first { it.settings.isNotEmpty() }.settings.first().key
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                Ntc06hModuleCatalogTestHarness(vm = vm)
            }
        }

        waitUntilTagExists(DemoTestTags.consoleNtc06hCustomEdit(firstSettingKey))
        composeRule.onNodeWithTag(DemoTestTags.consoleNtc06hCustomEdit(firstSettingKey)).performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE)
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE).assertIsDisplayed()
        composeRule.onNodeWithText("单独保存").assertIsDisplayed()
    }

    @Test
    fun data_rule_section_renders_parser_and_builder_when_expanded() {
        val vm = CommandConsoleViewModel()
        val parseExpanded = mutableStateOf(true)
        val dataRuleExpanded = mutableStateOf(true)
        composeRule.setContent {
            DemoTheme {
                DataRuleSectionTestHarness(
                    vm = vm,
                    parseExpandedState = parseExpanded,
                    dataRuleExpandedState = dataRuleExpanded,
                )
            }
        }

        composeRule.runOnIdle {
            assertTrue(parseExpanded.value)
            assertTrue(dataRuleExpanded.value)
        }
        composeRule.onNodeWithText("单独读取字符集").assertExists()
        composeRule.onNodeWithText("构建并发送").assertExists()
    }

    @Test
    fun page_shows_danger_dialog_for_dangerous_quick_action() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageContent(
                    vm = vm,
                    uiState = CommandConsoleUiState(
                        currentModuleFamily = ModuleFamily.NT212X,
                        supportsModuleCommands = true,
                        canExecuteModuleCommands = true,
                        hasReadySession = true,
                        capabilitySummary = rawDisplayText("supports module commands"),
                        moduleSummary = rawDisplayText("NT212X"),
                    ),
                    context = composeRule.activity,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
        composeRule.waitForIdle()
        scrollConsolePageToText("开启确认蜂鸣")
        composeRule.onNodeWithText("开启确认蜂鸣").performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun page_disables_master_actions_until_session_ready() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageContent(
                    vm = vm,
                    uiState = CommandConsoleUiState(
                        currentModuleFamily = ModuleFamily.NT212X,
                        supportsModuleCommands = true,
                        canExecuteModuleCommands = true,
                        hasReadySession = false,
                        capabilitySummary = rawDisplayText("supports module commands"),
                        moduleSummary = rawDisplayText("NT212X"),
                    ),
                    context = composeRule.activity,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
        composeRule.waitForIdle()
        scrollConsolePageToText("开启确认蜂鸣")
        composeRule.onNodeWithText("开启确认蜂鸣").assertIsNotEnabled()
        assertTrue(composeRule.onAllNodesWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun page_updates_visible_labels_after_language_switch() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageHarness(
                    vm = vm,
                    familyState = remember { mutableStateOf(ModuleFamily.NT212X) },
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_STATUS_TITLE).assertTextEquals("当前状态")
        composeRule.runOnIdle {
            DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.EN)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_STATUS_TITLE).assertTextEquals("Status")
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_TOP_BAR_TITLE).assertTextEquals("Scanner Control Console")
    }

    @Test
    fun danger_dialog_updates_labels_after_language_switch() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageHarness(
                    vm = vm,
                    familyState = remember { mutableStateOf(ModuleFamily.NT212X) },
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
        composeRule.waitForIdle()
        scrollConsolePageToText("开启确认蜂鸣")
        composeRule.onNodeWithText("开启确认蜂鸣").performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)

        composeRule.runOnIdle {
            DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.EN)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE).assertTextEquals("Confirm Execution")
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CONFIRM).assertTextEquals("Continue")
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CANCEL).assertTextEquals("Cancel")
        composeRule.onNodeWithText("Ack Beep On", substring = true).assertIsDisplayed()
    }

    @Test
    fun page_danger_dialog_cancel_and_continue_clear_dialog() {
        val vm = CommandConsoleViewModel()
        composeRule.setContent {
            DemoTheme {
                CommandConsolePageHarness(
                    vm = vm,
                    familyState = remember { mutableStateOf(ModuleFamily.NT212X) },
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SECTION_QUICK_ACTIONS).performClick()
        composeRule.waitForIdle()
        scrollConsolePageToText("开启确认蜂鸣")
        composeRule.onNodeWithText("开启确认蜂鸣").performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CANCEL).performClick()
        waitUntilTagGone(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)

        scrollConsolePageToText("开启确认蜂鸣")
        composeRule.onNodeWithText("开启确认蜂鸣").performClick()
        waitUntilTagExists(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CONFIRM).performClick()
        waitUntilTagGone(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE)
    }

    @Test
    fun page_switching_generic_module_tab_collapses_inline_editor() {
        val domainGroups = ModuleSettingsCatalog.domainGroupsFor(ModuleFamily.NT212X)
        check(domainGroups.size > 1) { "Expected multiple NT212X domain tabs" }
        val firstFamily = domainGroups.first().families.first { family -> family.sections.any { it.presets.isNotEmpty() } }
        val firstPresetParameterId = firstFamily.sections.first { it.presets.isNotEmpty() }.presets.first().parameterId
        val firstPresetIndex = ModuleSettingsCatalog.presetsFor(ModuleFamily.NT212X)
            .indexOfFirst { it.parameterId == firstPresetParameterId }
        check(firstPresetIndex >= 0) { "Expected preset index for parameter 0x${firstPresetParameterId.toString(16)}" }
        val vm = CommandConsoleViewModel()

        composeRule.setContent {
            DemoTheme {
                GenericModuleTabResetHarness(
                    vm = vm,
                    family = ModuleFamily.NT212X,
                    expandedFamilyKey = firstFamily.key,
                    initialPresetParameterId = firstPresetParameterId,
                    initialPresetIndex = firstPresetIndex,
                )
            }
        }

        waitUntilTagExists(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE)
        assertTrue(composeRule.onAllNodesWithTag(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE).fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithTag(DemoTestTags.consoleModuleDomainTab(1)).performClick()
        waitUntilTagGone(DemoTestTags.CONSOLE_GENERIC_CUSTOM_CLOSE)
    }

    @Test
    fun page_switching_family_collapses_ntc06h_inline_editor() {
        val firstFamily = buildNtc06hDomainGroups()
            .first()
            .families
            .first { family -> family.sections.any { it.settings.isNotEmpty() } }
        val firstSettingKey = firstFamily.sections.first { it.settings.isNotEmpty() }.settings.first().key
        val firstSettingIndex = Ntc06hSettingCatalog.all.indexOfFirst { it.key == firstSettingKey }
        check(firstSettingIndex >= 0) { "Expected setting index for $firstSettingKey" }
        val vm = CommandConsoleViewModel()
        val familyState = mutableStateOf(ModuleFamily.NTC06H)

        composeRule.setContent {
            DemoTheme {
                CommandConsolePageHarness(
                    vm = vm,
                    familyState = familyState,
                    initialState = CommandConsolePageInitialState(
                        startInModuleScope = true,
                        moduleSettingsExpanded = true,
                        moduleSelectedTab = 0,
                        expandedModuleFamilyKey = firstFamily.key,
                        inlineNtc06hSettingKey = firstSettingKey,
                        selectedNtc06hSettingIndex = firstSettingIndex,
                        preserveInlineEditorOnFirstComposition = true,
                    ),
                )
            }
        }

        waitUntilTagExists(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE)
        assertTrue(composeRule.onAllNodesWithTag(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE).fetchSemanticsNodes().isNotEmpty())

        composeRule.runOnIdle {
            familyState.value = ModuleFamily.NT212X
        }
        waitUntilTagGone(DemoTestTags.CONSOLE_NTC06H_CUSTOM_CLOSE)
        composeRule.runOnIdle {
            assertEquals(ModuleFamily.NT212X, familyState.value)
        }
    }

    private fun assertNoVisibleText(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }

    private fun waitUntilTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilTagGone(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitUntilTextExists(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollConsolePageToTag(tag: String) {
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_PAGE_LIST)
            .performScrollToNode(hasTestTag(tag))
    }

    private fun scrollConsolePageToText(text: String) {
        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_PAGE_LIST)
            .performScrollToNode(hasText(text))
    }
}

@Composable
private fun GenericModuleCatalogTestHarness(
    vm: CommandConsoleViewModel,
    family: ModuleFamily,
) {
    val modulePresets = remember(family) { ModuleSettingsCatalog.presetsFor(family) }
    val moduleDomainGroups = remember(family) { ModuleSettingsCatalog.domainGroupsFor(family) }
    val modulePresetIndexById = remember(modulePresets) {
        modulePresets.mapIndexed { index, preset -> preset.parameterId to index }.toMap()
    }
    check(modulePresets.isNotEmpty()) { "Expected presets for $family" }
    var selectedModulePresetIndex by remember { mutableStateOf(0) }
    var moduleSelectedTab by remember { mutableStateOf(0) }
    var expandedModuleFamilyKey by remember {
        mutableStateOf(
            moduleDomainGroups
                .firstOrNull()
                ?.families
                ?.firstOrNull { family -> family.sections.any { it.presets.isNotEmpty() } }
                ?.key
        )
    }
    var inlineCustomPresetId by remember { mutableStateOf<Int?>(null) }
    var moduleParameterIdText by remember {
        mutableStateOf(formatModuleParameterId(modulePresets.first().parameterId))
    }
    var modulePayloadHexText by remember {
        mutableStateOf(modulePresets.first().writeOnHex)
    }
    var modulePersistWrite by remember { mutableStateOf(true) }

    val state = ModuleConsoleSectionState(
        activeModuleFamily = family,
        moduleSettingsExpanded = true,
        isNtc06hFamily = false,
        isExecuting = false,
        supportsModuleCommands = true,
        canExecuteModuleCommands = true,
        moduleSummary = family.name,
        ntc06hSettings = emptyList(),
        ntc06hDomainGroups = emptyList(),
        modulePresets = modulePresets,
        moduleDomainGroups = moduleDomainGroups,
        modulePresetIndexById = modulePresetIndexById,
        ntc06hSettingIndexByKey = emptyMap(),
        selectedModulePreset = modulePresets.getOrNull(selectedModulePresetIndex),
        selectedNtc06hSetting = null,
        selectedModulePresetIndex = selectedModulePresetIndex,
        selectedNtc06hSettingIndex = 0,
        moduleSelectedTab = moduleSelectedTab,
        expandedModuleFamilyKey = expandedModuleFamilyKey,
        inlineCustomPresetId = inlineCustomPresetId,
        inlineNtc06hSettingKey = null,
        moduleParameterIdText = moduleParameterIdText,
        modulePayloadHexText = modulePayloadHexText,
        modulePersistWrite = modulePersistWrite,
        ntc06hCustomCode = "",
        ntc06hTemplateHexValue = "",
        ntc06hSaveAfterWrite = true,
    )
    val actions = ModuleConsoleSectionActions(
        onModuleSettingsExpandedChange = {},
        onModuleSelectedTabChange = { moduleSelectedTab = it },
        onExpandedModuleFamilyKeyChange = { expandedModuleFamilyKey = it },
        onInlineCustomPresetIdChange = { inlineCustomPresetId = it },
        onInlineNtc06hSettingKeyChange = {},
        onSelectedModulePresetIndexChange = { selectedModulePresetIndex = it },
        onSelectedNtc06hSettingIndexChange = {},
        onModuleParameterIdTextChange = { moduleParameterIdText = it },
        onModulePayloadHexTextChange = { modulePayloadHexText = it },
        onModulePersistWriteToggle = { modulePersistWrite = !modulePersistWrite },
        onNtc06hCustomCodeChange = {},
        onNtc06hTemplateHexValueChange = {},
        onNtc06hSaveAfterWriteToggle = {},
    )
    GenericModuleCatalog(
        vm = vm,
        state = state,
        actions = actions,
    )
}

@Composable
private fun GenericModuleTabResetHarness(
    vm: CommandConsoleViewModel,
    family: ModuleFamily,
    expandedFamilyKey: String,
    initialPresetParameterId: Int,
    initialPresetIndex: Int,
) {
    val modulePresets = remember(family) { ModuleSettingsCatalog.presetsFor(family) }
    val moduleDomainGroups = remember(family) { ModuleSettingsCatalog.domainGroupsFor(family) }
    val modulePresetIndexById = remember(modulePresets) {
        modulePresets.mapIndexed { index, preset -> preset.parameterId to index }.toMap()
    }
    check(modulePresets.isNotEmpty()) { "Expected presets for $family" }
    var selectedModulePresetIndex by remember { mutableStateOf(initialPresetIndex) }
    var moduleSelectedTab by remember { mutableStateOf(0) }
    var expandedModuleFamilyKey by remember { mutableStateOf<String?>(expandedFamilyKey) }
    var inlineCustomPresetId by remember { mutableStateOf<Int?>(initialPresetParameterId) }
    var skipInitialReset by remember { mutableStateOf(true) }
    var moduleParameterIdText by remember {
        mutableStateOf(formatModuleParameterId(modulePresets[initialPresetIndex].parameterId))
    }
    var modulePayloadHexText by remember {
        mutableStateOf(modulePresets[initialPresetIndex].writeOnHex)
    }
    var modulePersistWrite by remember { mutableStateOf(true) }

    LaunchedEffect(moduleSelectedTab) {
        if (skipInitialReset) {
            skipInitialReset = false
            return@LaunchedEffect
        }
        inlineCustomPresetId = null
    }

    val state = ModuleConsoleSectionState(
        activeModuleFamily = family,
        moduleSettingsExpanded = true,
        isNtc06hFamily = false,
        isExecuting = false,
        supportsModuleCommands = true,
        canExecuteModuleCommands = true,
        moduleSummary = family.name,
        ntc06hSettings = emptyList(),
        ntc06hDomainGroups = emptyList(),
        modulePresets = modulePresets,
        moduleDomainGroups = moduleDomainGroups,
        modulePresetIndexById = modulePresetIndexById,
        ntc06hSettingIndexByKey = emptyMap(),
        selectedModulePreset = modulePresets.getOrNull(selectedModulePresetIndex),
        selectedNtc06hSetting = null,
        selectedModulePresetIndex = selectedModulePresetIndex,
        selectedNtc06hSettingIndex = 0,
        moduleSelectedTab = moduleSelectedTab,
        expandedModuleFamilyKey = expandedModuleFamilyKey,
        inlineCustomPresetId = inlineCustomPresetId,
        inlineNtc06hSettingKey = null,
        moduleParameterIdText = moduleParameterIdText,
        modulePayloadHexText = modulePayloadHexText,
        modulePersistWrite = modulePersistWrite,
        ntc06hCustomCode = "",
        ntc06hTemplateHexValue = "",
        ntc06hSaveAfterWrite = true,
    )
    val actions = ModuleConsoleSectionActions(
        onModuleSettingsExpandedChange = {},
        onModuleSelectedTabChange = { moduleSelectedTab = it },
        onExpandedModuleFamilyKeyChange = { expandedModuleFamilyKey = it },
        onInlineCustomPresetIdChange = { inlineCustomPresetId = it },
        onInlineNtc06hSettingKeyChange = {},
        onSelectedModulePresetIndexChange = { selectedModulePresetIndex = it },
        onSelectedNtc06hSettingIndexChange = {},
        onModuleParameterIdTextChange = { moduleParameterIdText = it },
        onModulePayloadHexTextChange = { modulePayloadHexText = it },
        onModulePersistWriteToggle = { modulePersistWrite = !modulePersistWrite },
        onNtc06hCustomCodeChange = {},
        onNtc06hTemplateHexValueChange = {},
        onNtc06hSaveAfterWriteToggle = {},
    )
    GenericModuleCatalog(
        vm = vm,
        state = state,
        actions = actions,
    )
}

@Composable
private fun Ntc06hModuleCatalogTestHarness(vm: CommandConsoleViewModel) {
    val ntc06hSettings = remember { Ntc06hSettingCatalog.all }
    val ntc06hDomainGroups = buildNtc06hDomainGroups()
    val ntc06hSettingIndexByKey = remember(ntc06hSettings) {
        ntc06hSettings.mapIndexed { index, setting -> setting.key to index }.toMap()
    }
    check(ntc06hSettings.isNotEmpty()) { "Expected NTC06H settings" }
    val firstSetting = ntc06hSettings.first()
    var selectedNtc06hSettingIndex by remember { mutableStateOf(0) }
    var moduleSelectedTab by remember { mutableStateOf(0) }
    var expandedModuleFamilyKey by remember {
        mutableStateOf(
            ntc06hDomainGroups
                .firstOrNull()
                ?.families
                ?.firstOrNull { family -> family.sections.any { it.settings.isNotEmpty() } }
                ?.key
        )
    }
    var inlineNtc06hSettingKey by remember { mutableStateOf<String?>(null) }
    var ntc06hCustomCode by remember {
        mutableStateOf(firstSetting.templateExampleCode.ifBlank { firstSetting.displayCode })
    }
    var ntc06hTemplateHexValue by remember {
        mutableStateOf(ntc06hTemplateValueOrEmpty(firstSetting))
    }
    var ntc06hSaveAfterWrite by remember { mutableStateOf(firstSetting.requiresSave) }

    val state = ModuleConsoleSectionState(
        activeModuleFamily = ModuleFamily.NTC06H,
        moduleSettingsExpanded = true,
        isNtc06hFamily = true,
        isExecuting = false,
        supportsModuleCommands = true,
        canExecuteModuleCommands = true,
        moduleSummary = "NTC06H",
        ntc06hSettings = ntc06hSettings,
        ntc06hDomainGroups = ntc06hDomainGroups,
        modulePresets = emptyList(),
        moduleDomainGroups = emptyList(),
        modulePresetIndexById = emptyMap(),
        ntc06hSettingIndexByKey = ntc06hSettingIndexByKey,
        selectedModulePreset = null,
        selectedNtc06hSetting = ntc06hSettings.getOrNull(selectedNtc06hSettingIndex),
        selectedModulePresetIndex = 0,
        selectedNtc06hSettingIndex = selectedNtc06hSettingIndex,
        moduleSelectedTab = moduleSelectedTab,
        expandedModuleFamilyKey = expandedModuleFamilyKey,
        inlineCustomPresetId = null,
        inlineNtc06hSettingKey = inlineNtc06hSettingKey,
        moduleParameterIdText = "",
        modulePayloadHexText = "",
        modulePersistWrite = true,
        ntc06hCustomCode = ntc06hCustomCode,
        ntc06hTemplateHexValue = ntc06hTemplateHexValue,
        ntc06hSaveAfterWrite = ntc06hSaveAfterWrite,
    )
    val actions = ModuleConsoleSectionActions(
        onModuleSettingsExpandedChange = {},
        onModuleSelectedTabChange = { moduleSelectedTab = it },
        onExpandedModuleFamilyKeyChange = { expandedModuleFamilyKey = it },
        onInlineCustomPresetIdChange = {},
        onInlineNtc06hSettingKeyChange = { inlineNtc06hSettingKey = it },
        onSelectedModulePresetIndexChange = {},
        onSelectedNtc06hSettingIndexChange = { selectedNtc06hSettingIndex = it },
        onModuleParameterIdTextChange = {},
        onModulePayloadHexTextChange = {},
        onModulePersistWriteToggle = {},
        onNtc06hCustomCodeChange = { ntc06hCustomCode = it },
        onNtc06hTemplateHexValueChange = { ntc06hTemplateHexValue = it },
        onNtc06hSaveAfterWriteToggle = { ntc06hSaveAfterWrite = !ntc06hSaveAfterWrite },
    )
    Ntc06hModuleCatalog(
        vm = vm,
        state = state,
        actions = actions,
    )
}

@Composable
private fun DataRuleSectionTestHarness(
    vm: CommandConsoleViewModel,
    parseExpandedState: MutableState<Boolean> = remember { mutableStateOf(false) },
    dataRuleExpandedState: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    var builderMode by remember { mutableStateOf(DataRuleFormMode.PREFIX) }
    var builderValueA by remember { mutableStateOf("") }
    var builderValueB by remember { mutableStateOf("") }

    val state = DataRuleConsoleSectionState(
        isExecuting = false,
        canExecuteMasterCommands = true,
        scanCharsetSummary = "UTF_8",
        scanTerminatorSummary = "0D",
        parseStateExpanded = parseExpandedState.value,
        dataRuleExpanded = dataRuleExpandedState.value,
        builderMode = builderMode,
        builderValueA = builderValueA,
        builderValueB = builderValueB,
    )
    val actions = DataRuleConsoleSectionActions(
        onParseStateExpandedChange = { parseExpandedState.value = it },
        onDataRuleExpandedChange = { dataRuleExpandedState.value = it },
        onBuilderModeChange = { builderMode = it },
        onBuilderValueAChange = { builderValueA = it },
        onBuilderValueBChange = { builderValueB = it },
    )
    DataRuleConsoleSection(
        vm = vm,
        state = state,
        actions = actions,
    )
}

@Composable
private fun CommandConsolePageHarness(
    vm: CommandConsoleViewModel,
    familyState: MutableState<ModuleFamily>,
    initialState: CommandConsolePageInitialState = CommandConsolePageInitialState(),
) {
    val context = LocalContext.current
    CommandConsolePageContent(
        vm = vm,
        uiState = CommandConsoleUiState(
            currentModuleFamily = familyState.value,
            supportsModuleCommands = true,
            canExecuteModuleCommands = true,
            hasReadySession = true,
            capabilitySummary = rawDisplayText("supports module commands"),
            moduleSummary = rawDisplayText(familyState.value.name),
        ),
        context = context,
        onBack = {},
        initialState = initialState,
    )
}
