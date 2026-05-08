package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CommandResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandResponseFormatterTest {
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
    fun format_includes_ack_text_and_raw_hex_when_response_has_no_records() {
        val response = commandResponse(
            text = "OK",
            rawHex = "4F 4B",
            acknowledged = true,
            recordBytes = emptyList(),
        )

        assertEquals(
            "Basic ack=true text=OK raw=4F 4B",
            CommandResponseFormatter.format(
                prefix = "Basic",
                response = response,
                commandTextCharset = Charsets.UTF_8,
                recordCharset = Charsets.UTF_8,
            )
        )
    }

    @Test
    fun format_marks_known_failure_response_as_failed() {
        val response = commandResponse(
            text = "Basic command failed: timeout",
            rawHex = "45 52",
            acknowledged = false,
            recordBytes = emptyList(),
        )

        assertEquals(
            "Basic failed: Basic command failed: timeout raw=45 52",
            CommandResponseFormatter.format(
                prefix = "Basic",
                response = response,
                commandTextCharset = Charsets.UTF_8,
                recordCharset = Charsets.UTF_8,
            )
        )
    }

    @Test
    fun format_appends_record_details_when_records_are_present() {
        val response = commandResponse(
            text = "",
            rawHex = "",
            acknowledged = true,
            recordBytes = listOf("A".toByteArray(), "B".toByteArray()),
        )

        assertEquals(
            "Upload ack=true records=2 complete=true first=A text=<empty> raw=<empty>\nA\nB",
            CommandResponseFormatter.format(
                prefix = "Upload",
                response = response,
                commandTextCharset = Charsets.UTF_8,
                recordCharset = Charsets.UTF_8,
            )
        )
    }

    private fun commandResponse(
        text: String,
        rawHex: String,
        acknowledged: Boolean,
        recordBytes: List<ByteArray>,
    ): CommandResponse {
        return CommandResponse(
            textBytes = text.toByteArray(),
            rawHex = rawHex,
            acknowledged = acknowledged,
            recordCount = recordBytes.size,
            recordBytes = recordBytes,
            recordsComplete = true,
        )
    }
}
