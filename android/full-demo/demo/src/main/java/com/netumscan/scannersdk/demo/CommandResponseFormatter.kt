package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.CommandResponse
import java.nio.charset.Charset

internal object CommandResponseFormatter {
    fun format(
        prefix: String,
        response: CommandResponse,
        commandTextCharset: Charset,
        recordCharset: Charset,
    ): String {
        val emptyText = DemoStrings.text(R.string.command_response_empty)
        val text = response.decodeText(commandTextCharset).ifBlank { emptyText }
        val raw = response.rawHex.ifBlank { emptyText }
        val recordTextView = response.decodeRecords(recordCharset)
        if (!response.acknowledged && isFailureText(text)) {
            return "$prefix ${DemoStrings.text(R.string.command_response_failed)}: $text raw=$raw"
        }
        if (response.recordCount <= 0) {
            return "$prefix ${DemoStrings.text(R.string.command_response_ack)}=${response.acknowledged} text=$text raw=$raw"
        }
        val summary = "$prefix ${DemoStrings.text(R.string.command_response_ack)}=${response.acknowledged} " +
            "${DemoStrings.text(R.string.command_response_records)}=${response.recordCount} " +
            "${DemoStrings.text(R.string.command_response_complete)}=${response.recordsComplete} " +
            "${DemoStrings.text(R.string.command_response_first)}=${recordTextView.firstOrNull().orEmpty().ifBlank { emptyText }} " +
            "text=$text raw=$raw"
        val details = recordTextView.joinToString(separator = "\n")
        return if (details.isBlank()) summary else "$summary\n$details"
    }

    private fun isFailureText(text: String): Boolean {
        val basicPrefix = "${DemoStrings.text(R.string.basic_command_failed)}:"
        val masterPrefix = "${DemoStrings.text(R.string.master_command_failed)}:"
        return text.startsWith(basicPrefix) || text.startsWith(masterPrefix)
    }
}
