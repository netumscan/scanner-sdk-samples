package com.netumscan.scannersdk.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class DataRuleFormModeTest {
    @Test
    fun `data rule mode label uses sdk runtime labels`() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
            assertEquals("前缀", dataRuleFormModeLabel(DataRuleFormMode.PREFIX))
            assertEquals("替换", dataRuleFormModeLabel(DataRuleFormMode.REPLACE))

            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
            assertEquals("Prefix", dataRuleFormModeLabel(DataRuleFormMode.PREFIX))
            assertEquals("Replace", dataRuleFormModeLabel(DataRuleFormMode.REPLACE))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }

    @Test
    fun `system language option label follows selected display language`() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
            assertEquals("跟随系统", DemoLocaleController.languageLabel(DemoLanguage.SYSTEM))

            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
            assertEquals("Follow System", DemoLocaleController.languageLabel(DemoLanguage.SYSTEM))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }
}
