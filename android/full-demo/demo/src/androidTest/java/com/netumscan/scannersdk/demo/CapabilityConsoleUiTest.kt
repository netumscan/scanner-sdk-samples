package com.netumscan.scannersdk.demo

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme
import com.netumscan.scannersdk.model.CapabilityDomain
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryAvailability
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityEntrySource
import com.netumscan.scannersdk.model.CapabilityRiskLevel
import com.netumscan.scannersdk.model.CapabilityValueKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CapabilityConsoleUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.EN)
        AppLogStore.clear()
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SESSION,
                level = DebugEventLevel.Info,
                message = "session-ready-before-navigation",
                timestampMs = 1,
            )
        )
    }

    @After
    fun tearDown() {
        AppLogStore.clear()
        DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.ZH)
    }

    @Test
    fun capabilityDomain_opensDetail_keepsLogsAndLogsEntry() {
        val vm = CommandConsoleViewModel()
        val domain = capabilityDomain()
        composeRule.setContent {
            val context = LocalContext.current
            DemoTheme {
                CommandConsolePageContent(
                    vm = vm,
                    uiState = CommandConsoleUiState(
                        capabilityDomains = listOf(domain.definition),
                        capabilityEntries = domain.items.map { it.definition },
                        capabilityCatalog = listOf(domain),
                    ),
                    context = context,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(DemoTestTags.CONSOLE_SCOPE_SETTINGS).performClick()
        composeRule.onNodeWithTag(DemoTestTags.capabilityDomain("feedback")).performClick()
        composeRule.onNodeWithTag(DemoTestTags.CAPABILITY_DOMAIN_DETAIL_TITLE)
            .assertTextEquals("Feedback")
        composeRule.onNodeWithTag(DemoTestTags.capabilitySettingGroup("vibration", "prompt"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(DemoTestTags.capabilitySetting("setting.AckBeep"))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(DemoTestTags.capabilitySettingDialog("setting.AckBeep"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Feedback / Vibration / Prompt").assertIsDisplayed()
        composeRule.onNodeWithText("Value overview").assertIsDisplayed()
        composeRule.onNodeWithText("Current").assertIsDisplayed()
        composeRule.onNodeWithText("Default").assertIsDisplayed()
        composeRule.onNodeWithText("New value").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithTag(DemoTestTags.TOP_BAR_MENU).performClick()
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_MENU_ITEM).assertIsDisplayed()

        val logs = AppLogStore.events.value
        assertEquals("session-ready-before-navigation", logs.first().message)
        assertEquals(DebugEventSource.UI, logs.last().source)
        assertEquals(true, logs.last().message.contains("Feedback"))
    }

    private fun capabilityDomain(): CapabilityCatalogDomain {
        val definition = CapabilityDomain(
            key = "feedback",
            displayName = "Feedback",
            localizationKey = "test.feedback",
            sortOrder = 0,
            visibleByDefault = true,
        )
        val setting = CapabilityEntry(
            entryKey = "setting.AckBeep",
            kind = CapabilityEntryKind.SETTING,
            domainKey = "feedback",
            groupKey = "vibration",
            familyKey = "vibration",
            sectionKey = "prompt",
            semanticKey = "AckBeep",
            defaultValue = "true",
            notes = "",
            source = CapabilityEntrySource.MASTER,
            transportScopes = 1,
            availability = CapabilityEntryAvailability.AVAILABLE,
            routePriority = 0,
            visibleByDefault = true,
            supportsRead = true,
            supportsWrite = true,
            supportsExecute = false,
            requiresValue = true,
            valueKind = CapabilityValueKind.BOOLEAN,
            riskLevel = CapabilityRiskLevel.NORMAL,
            valueHint = "",
        )
        val action = CapabilityEntry(
            entryKey = "action.Vibrate",
            kind = CapabilityEntryKind.ACTION,
            domainKey = "feedback",
            groupKey = "vibration",
            familyKey = "vibration",
            sectionKey = "prompt",
            semanticKey = "Vibrate",
            defaultValue = "",
            notes = "",
            source = CapabilityEntrySource.MASTER,
            transportScopes = 1,
            availability = CapabilityEntryAvailability.AVAILABLE,
            routePriority = 0,
            visibleByDefault = true,
            supportsRead = false,
            supportsWrite = false,
            supportsExecute = true,
            requiresValue = false,
            valueKind = CapabilityValueKind.ACTION,
            riskLevel = CapabilityRiskLevel.NORMAL,
            valueHint = "",
        )
        return CapabilityCatalogDomain(
            definition = definition,
            title = "Feedback",
            sections = listOf(
                CapabilityCatalogSection(
                    familyKey = "vibration",
                    familyTitle = "Vibration",
                    sectionKey = "prompt",
                    sectionTitle = "Prompt",
                    items = listOf(
                        CapabilityCatalogItem(
                            definition = setting,
                            title = "Ack Beep",
                            familyTitle = "Vibration",
                            sectionTitle = "Prompt",
                        ),
                        CapabilityCatalogItem(
                            definition = action,
                            title = "Vibrate",
                            familyTitle = "Vibration",
                            sectionTitle = "Prompt",
                        )
                    ),
                )
            ),
        )
    }
}
