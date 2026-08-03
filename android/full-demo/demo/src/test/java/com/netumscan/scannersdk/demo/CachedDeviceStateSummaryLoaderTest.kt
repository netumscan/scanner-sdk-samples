package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ScannerInfo
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CachedDeviceStateSummaryLoaderTest {
    private lateinit var originalLanguage: DemoLanguage

    @Before
    fun setUp() {
        originalLanguage = DemoLocaleController.currentLanguage
        DemoLocaleController.setLanguageForTest(DemoLanguage.ZH)
    }

    @After
    fun tearDown() {
        DemoLocaleController.setLanguageForTest(originalLanguage)
    }

    @Test
    fun load_reads_cached_info_once_and_formats_summary() = runTest {
        var infoCalls = 0

        val summary = CachedDeviceStateSummaryLoader.load(
            infoProvider = {
                infoCalls += 1
                ScannerInfo(
                    deviceId = "device-1",
                    name = "scanner-name",
                    serialNumber = "SN123",
                    firmwareVersion = "FW1.0",
                    hardwareVersion = "HW2.0",
                    manufacturer = "NLS",
                    versionFormatFamily = "family",
                    versionBootCode = "boot",
                    versionSeriesCode = "customer",
                    versionTransportCode = "transport",
                    versionTransportSuffix = "suffix",
                    versionWirelessCode = "wireless",
                    versionBluetoothCode = "btcode",
                    versionChipsetCode = "chip",
                    versionChipsetSuffix = "chipsfx",
                    versionReleaseCode = "release",
                    versionExtensionCode = "ext",
                    bluetoothName = "scanner-bt",
                    bluetoothFirmwareVersion = "bt-fw",
                )
            },
        )

        assertEquals(1, infoCalls)
        assertEquals("固件=FW1.0  硬件=HW2.0  系列码=customer  蓝牙名称=scanner-bt  蓝牙固件版本=bt-fw", summary.infoSummary.asStringForCurrentLanguage())
        assertEquals("当前页面未自动查询设备配置项", summary.deviceCharsetSummary.asStringForCurrentLanguage())
        assertEquals("当前页面未自动查询设备终端符", summary.deviceTerminalSummary.asStringForCurrentLanguage())
    }
}
