package com.netumscan.scannersdk.demo

data class DebugEvent(
    val source: DebugEventSource,
    val level: DebugEventLevel,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

enum class DebugEventSource {
    UI,
    SDK,
    CORE,
    BLE,
    SESSION,
    SCAN,
    COMMAND,
}

enum class DebugEventLevel {
    Debug,
    Info,
    Warn,
    Error,
}

private val demoMacAddressPattern = Regex("""(?i)\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\b""")
private val demoUuidPattern = Regex(
    """(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b"""
)
private val demoLabeledIdentifierPattern = Regex(
    """(?i)\b(serial(?:number)?|device(?:id|_id))\s*[=:]\s*[^,\s/]+"""
)

internal fun redactDemoLogMessage(message: String): String {
    return message
        .replace(demoLabeledIdentifierPattern) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
        .replace(demoMacAddressPattern, "<redacted-device-id>")
        .replace(demoUuidPattern, "<redacted-device-id>")
}

internal fun DebugEvent.redactedForStorage(): DebugEvent =
    copy(message = redactDemoLogMessage(message))
