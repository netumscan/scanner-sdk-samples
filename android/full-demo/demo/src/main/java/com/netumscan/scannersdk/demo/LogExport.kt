package com.netumscan.scannersdk.demo

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val exportTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

fun shareDebugEvents(
    context: Context,
    title: String,
    summaryLines: List<String>,
    events: List<DebugEvent>,
) {
    if (events.isEmpty()) {
        Toast.makeText(context, DemoStrings.fromContext(context, R.string.no_logs_to_export), Toast.LENGTH_SHORT).show()
        return
    }

    val payload = buildString {
        appendLine(title)
        appendLine("${DemoStrings.fromContext(context, R.string.exported_at)}: ${exportTimestampFormat.format(Date())}")
        summaryLines.filter { it.isNotBlank() }.forEach { appendLine(it) }
        appendLine()
        events.forEach { event ->
            appendLine(formatDebugEvent(event))
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, payload)
    }
    context.startActivity(Intent.createChooser(intent, DemoStrings.fromContext(context, R.string.export_logs)))
}

fun shareTextPayload(
    context: Context,
    title: String,
    payload: String,
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, payload)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun formatDebugEvent(event: DebugEvent): String {
    val timestamp = exportTimestampFormat.format(Date(event.timestampMs))
    val header = "[$timestamp] [${sourceLabel(event.source)}] [${levelLabel(event.level)}]"
    return "$header ${redactDemoLogMessage(event.message)}"
}
