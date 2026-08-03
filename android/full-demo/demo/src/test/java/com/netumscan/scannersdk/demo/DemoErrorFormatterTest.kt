package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerException
import com.netumscan.scannersdk.TransportType
import com.netumscan.scannersdk.model.BleScanIssue
import com.netumscan.scannersdk.model.DiscoveryFailure
import com.netumscan.scannersdk.model.DiscoveryFailureCode
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
                ScannerException("readCapabilityValue", 1)
            )

            assertTrue(detail.contains("Invalid argument"))
            assertTrue(detail.contains("readCapabilityValue"))
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

    @Test
    fun scannerExceptionDetailIncludesStructuredDiscoveryCause() {
        val error = ScannerException(
            operation = "nativeStartDiscovery",
            errorCode = 8,
            detail = null,
            discoveryFailure = DiscoveryFailure(
                transportType = TransportType.BLE_GATT,
                code = DiscoveryFailureCode.BLE_UNFILTERED_SCAN_FAILED,
                message = "BLE discovery failed in unfiltered mode: platform internal error",
                bleScanIssue = BleScanIssue.INTERNAL_ERROR,
                platformErrorCode = 3,
            ),
        )

        val detail = DemoErrorFormatter.detail(error)

        assertTrue(detail.contains("nativeStartDiscovery"))
        assertTrue(detail.contains("code=8"))
        assertTrue(detail.contains("discovery=BLE_UNFILTERED_SCAN_FAILED"))
        assertTrue(detail.contains("issue=INTERNAL_ERROR"))
        assertTrue(detail.contains("raw=3"))
    }
}
