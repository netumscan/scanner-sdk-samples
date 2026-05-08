package com.netumscan.scannersdk.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CachedCommandSuccessStateLoaderTest {

    @Test
    fun load_reads_summary_once_and_includes_success_message() = runTest {
        var calls = 0

        val state = CachedCommandSuccessStateLoader.load(
            actionResult = "主控指令 SET_DECODER_MODULE_3 执行完成",
            summaryProvider = {
                calls += 1
                DeviceStateSummary(
                    infoSummary = rawDisplayText("固件=FW1.0  硬件=HW2.0  系列码=customer"),
                    deviceCharsetSummary = rawDisplayText("UTF8 (Txt) / 接收设备=Windows / 布局=EN / 接口=RF HID+USB COM"),
                    deviceTerminalSummary = rawDisplayText("CRLF"),
                )
            },
        )

        assertEquals(1, calls)
        assertEquals("固件=FW1.0  硬件=HW2.0  系列码=customer", state.infoSummary.asStringForCurrentLanguage())
        assertEquals(
            "UTF8 (Txt) / 接收设备=Windows / 布局=EN / 接口=RF HID+USB COM",
            state.deviceCharsetSummary.asStringForCurrentLanguage(),
        )
        assertEquals("CRLF", state.deviceTerminalSummary.asStringForCurrentLanguage())
        assertEquals("主控指令 SET_DECODER_MODULE_3 执行完成", state.lastActionResult)
    }
}
