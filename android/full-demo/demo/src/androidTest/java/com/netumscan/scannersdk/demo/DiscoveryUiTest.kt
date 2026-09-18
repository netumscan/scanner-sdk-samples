package com.netumscan.scannersdk.demo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectingBle_loadsAndDisplaysSupportedModelsFromSdk() {
        composeRule.onNodeWithTag(DemoTestTags.DISCOVERY_TRANSPORT_BLE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(DemoTestTags.DISCOVERY_MODEL_BUTTON).assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithTag(DemoTestTags.DISCOVERY_MODEL_STATUS)
            .assertTextContains("SDK", substring = true)
        composeRule.onNodeWithTag(DemoTestTags.DISCOVERY_MODEL_BUTTON).performClick()
        composeRule.onNodeWithTag(DemoTestTags.discoverySupportedModel("NT-1228BC"))
            .assertExists()
        composeRule.onNodeWithTag(DemoTestTags.discoverySupportedModel("RW-185"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(DemoTestTags.DISCOVERY_MODEL_BUTTON)
            .assertTextContains("RW-185", substring = true)
    }
}
