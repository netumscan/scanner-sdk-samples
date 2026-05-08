package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoErrorFormatterTest {
    @Test
    fun scannerExceptionDetailPreservesOperationAndCode() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.EN)

            val detail = DemoErrorFormatter.detail(
                ScannerException("executeModuleRawFrame", 1)
            )

            assertTrue(detail.contains("Invalid argument"))
            assertTrue(detail.contains("executeModuleRawFrame"))
            assertTrue(detail.contains("code=1"))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }

    @Test
    fun nativeErrorReasonUsesLocalizedStrings() {
        val originalLanguage = DemoLocaleController.currentLanguage
        try {
            DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)

            assertEquals("设备响应超时", DemoErrorFormatter.nativeErrorReason(5))
            assertEquals("SDK 内部错误", DemoErrorFormatter.nativeErrorReason(-1))
        } finally {
            DemoLocaleController.setLanguageForTest(originalLanguage)
        }
    }
}
