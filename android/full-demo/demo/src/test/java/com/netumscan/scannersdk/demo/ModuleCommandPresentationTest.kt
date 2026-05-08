package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleCommandPresentationTest {
    private lateinit var originalLanguage: DemoLanguage

    @BeforeTest
    fun setUp() {
        originalLanguage = DemoLocaleController.currentLanguage
        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)
    }

    @AfterTest
    fun tearDown() {
        DemoLocaleController.setLanguageForTest(originalLanguage)
    }

    @Test
    fun isNtc06hSilentAckTimeout_acceptsSdkTimeoutError() {
        assertTrue(
            ModuleCommandPresentation.isNtc06hSilentAckTimeout(
                ScannerException("writeNtc06hSetting", 5)
            )
        )
    }

    @Test
    fun isNtc06hSilentAckTimeout_acceptsTimeoutMessage() {
        assertTrue(
            ModuleCommandPresentation.isNtc06hSilentAckTimeout(
                IllegalStateException("command timeout code=5")
            )
        )
    }

    @Test
    fun isNtc06hSilentAckTimeout_rejectsUnrelatedErrors() {
        assertFalse(
            ModuleCommandPresentation.isNtc06hSilentAckTimeout(
                IllegalStateException("device rejected command")
            )
        )
    }

    @Test
    fun ntc06hWriteTimeoutMessage_describesWriteOnlyAndSaveAfterWriteModes() {
        assertEquals(
            "Restore sent setting code 000B0, but the device returned no confirmation frame. " +
                "NTC06H may apply settings silently when setting ACK is disabled. Enable 02421 first, then retest.",
            ModuleCommandPresentation.ntc06hWriteTimeoutMessage(
                label = "Restore",
                settingCode = "000B0",
                saveAfterWrite = false,
            )
        )
        assertEquals(
            "Restore sent setting code 000B0, but the device returned no confirmation frame. " +
                "NTC06H may apply settings silently when setting ACK is disabled. Save was not sent; " +
                "enable 02421 first, then retry save.",
            ModuleCommandPresentation.ntc06hWriteTimeoutMessage(
                label = "Restore",
                settingCode = "000B0",
                saveAfterWrite = true,
            )
        )
    }
}
