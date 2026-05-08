package com.netumscan.scannersdk.demo

internal enum class AppLogSourceFilter {
    UI,
    SDK,
    CORE,
    BLE,
    SESSION,
    SCAN,
    COMMAND,
}

internal enum class AppLogLevelFilter {
    ALL,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

internal fun matchesSource(event: DebugEvent, filters: Set<AppLogSourceFilter>): Boolean {
    if (filters.isEmpty()) return true
    return sourceFilterFor(event.source) in filters
}

internal fun matchesLevel(event: DebugEvent, filter: AppLogLevelFilter): Boolean {
    return when (filter) {
        AppLogLevelFilter.ALL -> true
        AppLogLevelFilter.DEBUG -> event.level == DebugEventLevel.Debug
        AppLogLevelFilter.INFO -> event.level == DebugEventLevel.Info
        AppLogLevelFilter.WARN -> event.level == DebugEventLevel.Warn
        AppLogLevelFilter.ERROR -> event.level == DebugEventLevel.Error
    }
}

internal fun sourceFilterLabel(filter: AppLogSourceFilter): String {
    return when (filter) {
        AppLogSourceFilter.UI -> "UI"
        AppLogSourceFilter.SDK -> DemoStrings.text(R.string.log_source_sdk)
        AppLogSourceFilter.CORE -> "CORE"
        AppLogSourceFilter.BLE -> "BLE"
        AppLogSourceFilter.SESSION -> DemoStrings.text(R.string.log_source_session)
        AppLogSourceFilter.SCAN -> DemoStrings.text(R.string.log_source_scan)
        AppLogSourceFilter.COMMAND -> DemoStrings.text(R.string.log_source_command)
    }
}

internal fun sourceFilterSummary(filters: Set<AppLogSourceFilter>): String {
    if (filters.isEmpty()) return DemoStrings.text(R.string.all)
    return filters.joinToString(" / ") { sourceFilterLabel(it) }
}

internal fun levelFilterLabel(filter: AppLogLevelFilter): String {
    return when (filter) {
        AppLogLevelFilter.ALL -> DemoStrings.text(R.string.all)
        AppLogLevelFilter.DEBUG -> DemoStrings.text(R.string.log_level_debug)
        AppLogLevelFilter.INFO -> DemoStrings.text(R.string.log_level_info)
        AppLogLevelFilter.WARN -> DemoStrings.text(R.string.log_level_warn)
        AppLogLevelFilter.ERROR -> DemoStrings.text(R.string.log_level_error)
    }
}

internal fun sourceLabel(source: DebugEventSource): String {
    return when (source) {
        DebugEventSource.UI -> "UI"
        DebugEventSource.SDK -> DemoStrings.text(R.string.log_source_sdk)
        DebugEventSource.CORE -> "CORE"
        DebugEventSource.BLE -> "BLE"
        DebugEventSource.SESSION -> DemoStrings.text(R.string.log_source_session)
        DebugEventSource.SCAN -> DemoStrings.text(R.string.log_source_scan)
        DebugEventSource.COMMAND -> DemoStrings.text(R.string.log_source_command)
    }
}

internal fun levelLabel(level: DebugEventLevel): String {
    return when (level) {
        DebugEventLevel.Debug -> DemoStrings.text(R.string.log_level_debug)
        DebugEventLevel.Info -> DemoStrings.text(R.string.log_level_info)
        DebugEventLevel.Warn -> DemoStrings.text(R.string.log_level_warn)
        DebugEventLevel.Error -> DemoStrings.text(R.string.log_level_error)
    }
}

internal fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun sourceFilterFor(source: DebugEventSource): AppLogSourceFilter {
    return when (source) {
        DebugEventSource.UI -> AppLogSourceFilter.UI
        DebugEventSource.SDK -> AppLogSourceFilter.SDK
        DebugEventSource.CORE -> AppLogSourceFilter.CORE
        DebugEventSource.BLE -> AppLogSourceFilter.BLE
        DebugEventSource.SESSION -> AppLogSourceFilter.SESSION
        DebugEventSource.SCAN -> AppLogSourceFilter.SCAN
        DebugEventSource.COMMAND -> AppLogSourceFilter.COMMAND
    }
}
