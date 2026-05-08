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
                "信息/电量",
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
                "刷新信息",
                "读取电量",
                "开启确认蜂鸣",
                "关闭确认蜂鸣",
                "开启振动",
                "关闭振动",
            ),
            labels
        )
    }

    @Test
    fun quickActionRows_doNotContainModuleSettings() {
        val actions = CommandCatalog.quickActionRows.flatMap { row -> row.commands.map { it.second } }.toSet()

        assertFalse(actions.any { it.name.startsWith("NTC06H_") })
        assertFalse(actions.any { it.name.startsWith("NT212X_") })
        assertFalse(actions.any { it.name.startsWith("NT280H_") })
        assertFalse(actions.any { it.name.startsWith("SE4750_") })
    }

    @Test
    fun dangerousQuickActions_matchRiskFlags() {
        val dangerous = CommandCatalog.dangerousQuickActions
        assertFalse(QuickActionCommand.REFRESH_INFO.isDangerous)
        assertFalse(QuickActionCommand.GET_BATTERY.isDangerous)
        assertTrue(QuickActionCommand.ACK_BEEP_ON.isDangerous)
        assertTrue(QuickActionCommand.ACK_BEEP_OFF.isDangerous)
        assertTrue(QuickActionCommand.VIBRATE_ON.isDangerous)
        assertTrue(QuickActionCommand.VIBRATE_OFF.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_SAVE.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_ACK_ON.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_CODE39_ON.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_CODE39_OFF.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_EAN13_ON.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_EAN13_OFF.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_CODE128_ON.isDangerous)
        assertTrue(QuickActionCommand.NTC06H_CODE128_OFF.isDangerous)
        assertFalse(QuickActionCommand.NT212X_QR_READ.isDangerous)
        assertTrue(QuickActionCommand.NT212X_QR_ON.isDangerous)
        assertTrue(QuickActionCommand.NT212X_QR_OFF.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SCAN_KEY_MODE.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SCAN_AUTO_MODE.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SCAN_CONTINUOUS_MODE.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SLEEP_NEVER.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SLEEP_10S.isDangerous)
        assertTrue(QuickActionCommand.NT280H_DUPLICATE_500MS.isDangerous)
        assertTrue(QuickActionCommand.NT280H_LIGHT_HIGH.isDangerous)
        assertTrue(QuickActionCommand.NT280H_SENSITIVITY_HIGH.isDangerous)
        assertFalse(QuickActionCommand.SE4750_CAPABILITIES_READ.isDangerous)
        assertTrue(QuickActionCommand.SE4750_AIM_ON.isDangerous)
        assertTrue(QuickActionCommand.SE4750_AIM_OFF.isDangerous)
        assertTrue(QuickActionCommand.SE4750_ILLUMINATION_ON.isDangerous)
        assertTrue(QuickActionCommand.SE4750_ILLUMINATION_OFF.isDangerous)
        assertTrue(QuickActionCommand.SE4750_ALL_SYMBOLOGY_ON.isDangerous)
        assertTrue(QuickActionCommand.SE4750_ALL_SYMBOLOGY_OFF.isDangerous)
        assertTrue(QuickActionCommand.SE4750_BEEP.isDangerous)
        assertTrue(QuickActionCommand.SE4750_PAGER.isDangerous)
        assertEquals(
            setOf(
                QuickActionCommand.ACK_BEEP_ON,
                QuickActionCommand.ACK_BEEP_OFF,
                QuickActionCommand.VIBRATE_ON,
                QuickActionCommand.VIBRATE_OFF,
                QuickActionCommand.NTC06H_SAVE,
                QuickActionCommand.NTC06H_ACK_ON,
                QuickActionCommand.NTC06H_CODE39_ON,
                QuickActionCommand.NTC06H_CODE39_OFF,
                QuickActionCommand.NTC06H_EAN13_ON,
                QuickActionCommand.NTC06H_EAN13_OFF,
                QuickActionCommand.NTC06H_CODE128_ON,
                QuickActionCommand.NTC06H_CODE128_OFF,
                QuickActionCommand.NT212X_QR_ON,
                QuickActionCommand.NT212X_QR_OFF,
                QuickActionCommand.NT280H_SCAN_KEY_MODE,
                QuickActionCommand.NT280H_SCAN_AUTO_MODE,
                QuickActionCommand.NT280H_SCAN_CONTINUOUS_MODE,
                QuickActionCommand.NT280H_SLEEP_NEVER,
                QuickActionCommand.NT280H_SLEEP_10S,
                QuickActionCommand.NT280H_DUPLICATE_500MS,
                QuickActionCommand.NT280H_LIGHT_HIGH,
                QuickActionCommand.NT280H_SENSITIVITY_HIGH,
                QuickActionCommand.SE4750_AIM_ON,
                QuickActionCommand.SE4750_AIM_OFF,
                QuickActionCommand.SE4750_ILLUMINATION_ON,
                QuickActionCommand.SE4750_ILLUMINATION_OFF,
                QuickActionCommand.SE4750_ALL_SYMBOLOGY_ON,
                QuickActionCommand.SE4750_ALL_SYMBOLOGY_OFF,
                QuickActionCommand.SE4750_BEEP,
                QuickActionCommand.SE4750_PAGER,
            ),
            dangerous
        )
    }

    @Test
    fun dangerousQuickActions_haveSpecificRiskText() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
        assertEquals(
            "这会开启设备确认蜂鸣，并立即改变反馈行为。",
            QuickActionCommand.ACK_BEEP_ON.riskText
        )
        assertEquals(
            "这会关闭设备确认蜂鸣，并立即改变反馈行为。",
            QuickActionCommand.ACK_BEEP_OFF.riskText
        )
        assertEquals(
            "这会开启振动反馈，并立即改变反馈行为。",
            QuickActionCommand.VIBRATE_ON.riskText
        )
        assertEquals(
            "这会关闭振动反馈，并立即改变反馈行为。",
            QuickActionCommand.VIBRATE_OFF.riskText
        )
        assertEquals(
            "这会把当前 NTC06H 设置持久保存到模组，后续重启仍会生效。",
            QuickActionCommand.NTC06H_SAVE.riskText
        )
        assertEquals(
            "这会开启 NTC06H 设置应答，后续发送设置码时更容易收到 ACK。建议确认回 ACK 稳定后再点保存设置。",
            QuickActionCommand.NTC06H_ACK_ON.riskText
        )
        assertEquals(
            "这会关闭 NTC06H 的 EAN-13 识读，可能导致 EAN-13 无法识读。",
            QuickActionCommand.NTC06H_EAN13_OFF.riskText
        )
        assertEquals(
            "这会写入 NT212X QR Code 码制开关参数 0xF025，QR 识读行为会立即变化。",
            QuickActionCommand.NT212X_QR_ON.riskText
        )
        assertEquals(
            "这会关闭 NT212X QR Code 码制参数 0xF025，可能导致 QR Code 无法识读。",
            QuickActionCommand.NT212X_QR_OFF.riskText
        )
        assertEquals(
            "这会把 NT280H 扫描模式切回按键扫描，模组触发行为会立即变化。",
            QuickActionCommand.NT280H_SCAN_KEY_MODE.riskText
        )
        assertEquals(
            "这会把 NT280H 休眠超时设为 10 秒，模组功耗行为会立即变化。",
            QuickActionCommand.NT280H_SLEEP_10S.riskText
        )
        assertEquals(
            "这会开启 SE4750 瞄准图案，模组光学行为会立即变化。",
            QuickActionCommand.SE4750_AIM_ON.riskText
        )
        assertEquals(
            "这会触发一次 SE4750 模组蜂鸣反馈，设备会立即发声。",
            QuickActionCommand.SE4750_BEEP.riskText
        )
        assertEquals(
            "这会按 demo 约定写入 SE4750 全部码制关闭 payload=00，可能导致大多数码制立即无法识读。",
            QuickActionCommand.SE4750_ALL_SYMBOLOGY_OFF.riskText
        )
    }

    @Test
    fun quickActionCatalog_switchesWithLanguage() {
        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
        assertEquals(
            listOf("Info/Battery", "Ack Beep", "Vibrate"),
            CommandCatalog.quickActionRows.map { it.title }
        )
        assertEquals("Ack Beep On", QuickActionCommand.ACK_BEEP_ON.title)
        assertEquals("Save Settings", QuickActionCommand.NTC06H_SAVE.title)
        assertEquals("Enable Setting ACK", QuickActionCommand.NTC06H_ACK_ON.title)
        assertEquals("QR On", QuickActionCommand.NT212X_QR_ON.title)
        assertEquals("Trigger Scan", QuickActionCommand.NT280H_SCAN_KEY_MODE.title)
        assertEquals("Read Capabilities", QuickActionCommand.SE4750_CAPABILITIES_READ.title)
        assertEquals("All Symbologies On", QuickActionCommand.SE4750_ALL_SYMBOLOGY_ON.title)
        assertEquals(
            "This enables the device acknowledgment beep and changes feedback behavior immediately.",
            QuickActionCommand.ACK_BEEP_ON.riskText
        )
        assertEquals(
            "This persists current NTC06H settings to the module and keeps them after reboot.",
            QuickActionCommand.NTC06H_SAVE.riskText
        )
        assertEquals(
            "This enables NTC06H setting ACK so later setting-code writes are more likely to return ACK. Save settings after ACK becomes stable.",
            QuickActionCommand.NTC06H_ACK_ON.riskText
        )
        assertEquals(
            "This writes NT212X QR Code symbology parameter 0xF025 and changes QR decode behavior immediately.",
            QuickActionCommand.NT212X_QR_ON.riskText
        )
        assertEquals(
            "This sets NT280H sleep timeout to 10 seconds and changes module power behavior immediately.",
            QuickActionCommand.NT280H_SLEEP_10S.riskText
        )
        assertEquals(
            "This triggers one SE4750 module beep and the device will emit feedback immediately.",
            QuickActionCommand.SE4750_BEEP.riskText
        )
        assertEquals(
            "This writes the demo-assumed SE4750 all-symbologies disable payload=00 and may immediately disable most symbologies.",
            QuickActionCommand.SE4750_ALL_SYMBOLOGY_OFF.riskText
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

        assertTrue(zhTabs.isNotEmpty())
        assertEquals(zhTabs.map { it.key }, enTabs.map { it.key })
        assertEquals(zhSectionKeysByTab, enSectionKeysByTab)
    }
}
