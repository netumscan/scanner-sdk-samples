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
