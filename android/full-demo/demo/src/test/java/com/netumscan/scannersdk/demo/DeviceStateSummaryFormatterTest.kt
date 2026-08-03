package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ScannerInfo
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceStateSummaryFormatterTest {
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
    fun format_returns_info_summary_and_hides_unqueried_config_fields() {
        val info = ScannerInfo(
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
        val summary = DeviceStateSummaryFormatter.format(info)

        assertEquals(
            "固件=FW1.0  硬件=HW2.0  系列码=customer  蓝牙名称=scanner-bt  蓝牙固件版本=bt-fw",
            summary.infoSummary.asStringForCurrentLanguage()
        )
        assertEquals(
            "当前页面未自动查询设备配置项",
            summary.deviceCharsetSummary.asStringForCurrentLanguage()
        )
        assertEquals("当前页面未自动查询设备终端符", summary.deviceTerminalSummary.asStringForCurrentLanguage())
    }

    @Test
    fun format_falls_back_to_placeholder_text_when_info_is_empty() {
        val info = ScannerInfo(
            deviceId = "device-1",
            name = "",
            serialNumber = "",
            firmwareVersion = "",
            hardwareVersion = "",
            manufacturer = "",
            versionFormatFamily = "",
            versionBootCode = "",
            versionSeriesCode = "",
            versionTransportCode = "",
            versionTransportSuffix = "",
            versionWirelessCode = "",
            versionBluetoothCode = "",
            versionChipsetCode = "",
            versionChipsetSuffix = "",
            versionReleaseCode = "",
            versionExtensionCode = "",
            bluetoothName = "",
            bluetoothFirmwareVersion = "",
        )
        val summary = DeviceStateSummaryFormatter.format(info)

        assertEquals("固件=-  硬件=-  系列码=-  蓝牙名称=-  蓝牙固件版本=-", summary.infoSummary.asStringForCurrentLanguage())
        assertEquals("当前页面未自动查询设备配置项", summary.deviceCharsetSummary.asStringForCurrentLanguage())
        assertEquals("当前页面未自动查询设备终端符", summary.deviceTerminalSummary.asStringForCurrentLanguage())
    }

    @Test
    fun format_keeps_labels_dynamic_when_language_changes() {
        val info = ScannerInfo(
            deviceId = "device-1",
            name = "",
            serialNumber = "",
            firmwareVersion = "FW1.0",
            hardwareVersion = "HW2.0",
            manufacturer = "",
            versionFormatFamily = "",
            versionBootCode = "",
            versionSeriesCode = "customer",
            versionTransportCode = "",
            versionTransportSuffix = "",
            versionWirelessCode = "",
            versionBluetoothCode = "",
            versionChipsetCode = "",
            versionChipsetSuffix = "",
            versionReleaseCode = "",
            versionExtensionCode = "",
            bluetoothName = "",
            bluetoothFirmwareVersion = "",
        )
        val summary = DeviceStateSummaryFormatter.format(info)

        assertEquals("固件=FW1.0  硬件=HW2.0  系列码=customer  蓝牙名称=-  蓝牙固件版本=-", summary.infoSummary.asStringForCurrentLanguage())

        DemoLocaleController.setLanguageForTest(DemoLanguage.EN)

        assertEquals("Firmware=FW1.0  Hardware=HW2.0  Series Code=customer  Bluetooth name=-  Bluetooth firmware version=-", summary.infoSummary.asStringForCurrentLanguage())
    }
}
