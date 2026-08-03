package com.netumscan.scannersdk.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DataRuleFormModeTest {
    @Test
    fun `data rule mode label uses sdk fallback labels`() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
            assertEquals("Prefix", dataRuleFormModeLabel(DataRuleFormMode.PREFIX))
            assertEquals("Replace", dataRuleFormModeLabel(DataRuleFormMode.REPLACE))

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

    @Test
    fun `data rule builder previews semantic summary`() {
        assertEquals(
            "Set prefix (2 bytes)",
            dataRuleCommandPreviewText(buildDataRuleCommand(DataRuleFormMode.PREFIX, "AB", ""))
        )
        assertEquals(
            "Set suffix (1 bytes)",
            dataRuleCommandPreviewText(buildDataRuleCommand(DataRuleFormMode.SUFFIX, "\\x0D", ""))
        )
        assertEquals(
            "Hide first 3 byte(s)",
            dataRuleCommandPreviewText(buildDataRuleCommand(DataRuleFormMode.HIDE_START, "3", ""))
        )
        assertEquals(
            "Hide 4 byte(s) from position 2",
            dataRuleCommandPreviewText(buildDataRuleCommand(DataRuleFormMode.HIDE_MIDDLE, "2", "4"))
        )
        assertEquals(
            "Replace 1 byte(s) with 1 byte(s)",
            dataRuleCommandPreviewText(buildDataRuleCommand(DataRuleFormMode.REPLACE, "A", "B"))
        )
    }

    @Test
    fun `data rule builder rejects values core would reject`() {
        assertIllegalArgument {
            buildDataRuleCommand(DataRuleFormMode.PREFIX, "", "")
        }
        assertIllegalArgument {
            buildDataRuleCommand(DataRuleFormMode.SUFFIX, "12345678901", "")
        }
        assertIllegalArgument {
            buildDataRuleCommand(DataRuleFormMode.HIDE_END, "0", "")
        }
        assertIllegalArgument {
            buildDataRuleCommand(DataRuleFormMode.REPLACE, "abcdef", "g")
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
