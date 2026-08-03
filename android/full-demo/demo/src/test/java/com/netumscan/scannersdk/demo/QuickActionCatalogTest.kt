package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickActionCatalogTest {

    @Test
    fun quickActionRows_keepExpectedOrder() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val titles = CommandCatalog.quickActionRows.map { it.title }
        assertEquals(
            listOf(
                "确认蜂鸣",
                "震动",
            ),
            titles
        )
    }

    @Test
    fun quickActionButtons_matchExpectedLabels() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val labels = CommandCatalog.quickActionRows.flatMap { row -> row.commands.map { it.first } }
        assertEquals(
            listOf(
                "开启确认蜂鸣",
                "关闭确认蜂鸣",
                "开启振动",
                "关闭振动",
            ),
            labels
        )
    }

    @Test
    fun dangerousQuickActions_matchPublicRiskFlags() {
        val dangerous = CommandCatalog.dangerousQuickActions
        assertFalse(QuickActionCommand.REFRESH_INFO.isDangerous)
        assertFalse(QuickActionCommand.GET_BATTERY.isDangerous)
        assertFalse(QuickActionCommand.READ_BT_FIRMWARE_VERSION.isDangerous)
        assertFalse(QuickActionCommand.GET_MEMORY_USAGE.isDangerous)
        assertTrue(QuickActionCommand.SET_ACK_BEEP_ENABLED.isDangerous)
        assertTrue(QuickActionCommand.SET_ACK_BEEP_DISABLED.isDangerous)
        assertTrue(QuickActionCommand.SET_VIBRATION_ENABLED.isDangerous)
        assertTrue(QuickActionCommand.SET_VIBRATION_DISABLED.isDangerous)
        assertEquals(
            setOf(
                QuickActionCommand.SET_ACK_BEEP_ENABLED,
                QuickActionCommand.SET_ACK_BEEP_DISABLED,
                QuickActionCommand.SET_VIBRATION_ENABLED,
                QuickActionCommand.SET_VIBRATION_DISABLED,
            ),
            dangerous
        )
    }

    @Test
    fun quickActionCatalog_switchesWithLanguage() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        assertEquals(
            listOf("Ack Beep", "Vibrate"),
            CommandCatalog.quickActionRows.map { it.title }
        )
        assertEquals("Read Bluetooth Firmware Version", QuickActionCommand.READ_BT_FIRMWARE_VERSION.title)
        assertEquals("Ack Beep On", QuickActionCommand.SET_ACK_BEEP_ENABLED.title)
        assertEquals(
            "This enables the device acknowledgment beep and changes feedback behavior immediately.",
            QuickActionCommand.SET_ACK_BEEP_ENABLED.riskText
        )
    }

    @Test
    fun masterConsoleBuckets_useStableKeysAcrossLanguages() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        val zhTabs = CommandCatalog.masterConsoleBuckets
        val zhSectionKeysByTab = zhTabs.associate { tab ->
            tab.key to CommandCatalog.masterBucketSectionsForKey(tab.key).map { section -> section.key }
        }

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        val enTabs = CommandCatalog.masterConsoleBuckets
        val enSectionKeysByTab = enTabs.associate { tab ->
            tab.key to CommandCatalog.masterBucketSectionsForKey(tab.key).map { section -> section.key }
        }

        assertTrue(zhTabs.isEmpty())
        assertEquals(zhTabs.map { it.key }, enTabs.map { it.key })
        assertEquals(zhSectionKeysByTab, enSectionKeysByTab)
    }

    @Test
    fun deviceActions_doNotDuplicateSettingsOnlyWorkModes() {
        val deviceActionLabels = CommandCatalog.masterBucketSectionsForKey("ACTIONS")
            .flatMap { section -> section.commands.map { it.first } }

        assertFalse(deviceActionLabels.contains("Normal Mode"))
        assertFalse(deviceActionLabels.contains("Store Mode"))
    }
}
