package com.netumscan.scannersdk.demo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.netumscan.scannersdk.demo.ui.theme.DemoTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppLogUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.ZH)
        AppLogStore.clear()
    }

    @After
    fun tearDown() {
        AppLogStore.clear()
        DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.ZH)
    }

    @Test
    fun page_filters_logs_by_source_chip() {
        seedLogs()
        composeRule.setAppLogContent()

        composeRule.onNodeWithTag(DemoTestTags.appLogSourceChip(AppLogSourceFilter.SDK)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_LIST)
            .performScrollToNode(hasText("sdk startup complete"))

        composeRule.onNodeWithText("显示 1 / 3 条，来源=SDK，等级=全部").assertIsDisplayed()
        composeRule.onNodeWithText("sdk startup complete").assertIsDisplayed()
        assertNoVisibleText("ui tapped connect")
    }

    @Test
    fun page_filters_logs_by_search_query() {
        seedLogs()
        composeRule.setAppLogContent()

        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_SEARCH_FIELD).performTextInput("beta")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_LIST)
            .performScrollToNode(hasText("scan beta payload"))

        composeRule.onNodeWithText("显示 1 / 3 条，来源=全部，等级=全部").assertIsDisplayed()
        composeRule.onNodeWithText("scan beta payload").assertIsDisplayed()
        assertNoVisibleText("ui tapped connect")
        assertNoVisibleText("sdk startup complete")
        composeRule.onNodeWithText("搜索关键词: beta").assertIsDisplayed()
    }

    @Test
    fun page_clear_logs_shows_empty_state() {
        seedLogs()
        composeRule.setAppLogContent()

        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_CLEAR_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_LIST)
            .performScrollToNode(hasTestTag(DemoTestTags.APP_LOG_EMPTY_STATE))

        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun page_updates_visible_labels_after_language_switch() {
        seedLogs()
        composeRule.setAppLogContent()

        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_TOP_BAR_TITLE).assertTextEquals("应用日志")
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_FILTER_TITLE).assertTextEquals("全局日志")

        composeRule.runOnIdle {
            DemoLocaleController.setLanguageForTest(composeRule.activity, DemoLanguage.EN)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_TOP_BAR_TITLE).assertTextEquals("App Logs")
        composeRule.onNodeWithTag(DemoTestTags.APP_LOG_FILTER_TITLE).assertTextEquals("Global Logs")
    }

    private fun seedLogs() {
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.UI,
                level = DebugEventLevel.Info,
                message = "ui tapped connect",
                timestampMs = 1,
            )
        )
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SDK,
                level = DebugEventLevel.Warn,
                message = "sdk startup complete",
                timestampMs = 2,
            )
        )
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SCAN,
                level = DebugEventLevel.Error,
                message = "scan beta payload",
                timestampMs = 3,
            )
        )
    }

    private fun assertNoVisibleText(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.setAppLogContent() {
        setContent {
            DemoTheme {
                AppLogRoute(onBack = {}, autoScrollToLatest = false)
            }
        }
    }
}
