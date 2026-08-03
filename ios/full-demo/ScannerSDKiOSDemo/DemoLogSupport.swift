import Foundation
import SwiftUI

enum ConsoleEventSource: String, CaseIterable, Identifiable {
    case ui = "UI"
    case sdk = "SDK"
    case core = "CORE"
    case ble = "BLE"
    case session = "SESSION"
    case scan = "SCAN"
    case command = "COMMAND"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .ui: return "UI"
        case .sdk: return DemoStrings.tr("log_source_sdk")
        case .core: return "CORE"
        case .ble: return "BLE"
        case .session: return DemoStrings.tr("log_source_session")
        case .scan: return DemoStrings.tr("log_source_scan")
        case .command: return DemoStrings.tr("log_source_command")
        }
    }
}

enum ConsoleEventLevel: String, CaseIterable, Identifiable {
    case debug = "DEBUG"
    case info = "INFO"
    case warn = "WARN"
    case error = "ERROR"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .debug: return DemoStrings.tr("log_level_debug")
        case .info: return DemoStrings.tr("log_level_info")
        case .warn: return DemoStrings.tr("log_level_warn")
        case .error: return DemoStrings.tr("log_level_error")
        }
    }
}

enum AppLogLevelFilter: String, CaseIterable, Identifiable {
    case all
    case debug
    case info
    case warn
    case error

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all: return DemoStrings.tr("all")
        case .debug: return ConsoleEventLevel.debug.label
        case .info: return ConsoleEventLevel.info.label
        case .warn: return ConsoleEventLevel.warn.label
        case .error: return ConsoleEventLevel.error.label
        }
    }
}

struct ConsoleEvent: Identifiable, Hashable {
    let id = UUID()
    let timestamp = Date()
    let source: ConsoleEventSource
    let level: ConsoleEventLevel
    let message: String
}

func redactDemoLogMessage(_ message: String) -> String {
    let replacements = [
        (
            #"(?i)\b(serial(?:number)?|device(?:id|_id))\s*[=:]\s*[^,\s/]+"#,
            "$1=<redacted>"
        ),
        (
            #"(?i)\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\b"#,
            "<redacted-device-id>"
        ),
        (
            #"(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b"#,
            "<redacted-device-id>"
        ),
    ]
    return replacements.reduce(message) { value, replacement in
        value.replacingOccurrences(
            of: replacement.0,
            with: replacement.1,
            options: .regularExpression
        )
    }
}

func makeSdkConsoleEvent(_ rawMessage: String) -> ConsoleEvent {
    let trimmed = rawMessage.trimmingCharacters(in: .whitespacesAndNewlines)
    let source = sdkDebugSource(for: trimmed)
    let level = sdkDebugLevel(for: trimmed)
    let message = formatSdkDebugMessage(trimmed)
    return ConsoleEvent(source: source, level: level, message: message)
}

func sdkDiagnosticEvents(in events: [ConsoleEvent]) -> [ConsoleEvent] {
    events.filter { $0.source == .sdk && ($0.level == .warn || $0.level == .error) }
}

func sdkDiagnosticsSummaryLines(for events: [ConsoleEvent]) -> [String] {
    let diagnostics = sdkDiagnosticEvents(in: events)
    let warnings = diagnostics.filter { $0.level == .warn }.count
    let errors = diagnostics.filter { $0.level == .error }.count
    let latest = diagnostics.last?.message ?? DemoStrings.tr("none")
    return [
        "\(DemoStrings.tr("sdk_warnings")): \(warnings)",
        "\(DemoStrings.tr("sdk_errors")): \(errors)",
        "\(DemoStrings.tr("latest_sdk_diagnostic")): \(latest)",
    ]
}

func escapeControlText(_ text: String) -> String {
    var output = ""
    for scalar in text.unicodeScalars {
        switch scalar.value {
        case 0x00...0x1F, 0x7F:
            output += String(format: "\\x%02X", scalar.value)
        default:
            output.unicodeScalars.append(scalar)
        }
    }
    return output
}

func hexSummary(_ data: Data) -> String {
    data.map { String(format: "%02X", $0) }.joined(separator: " ")
}

let logDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    formatter.locale = Locale(identifier: "en_US_POSIX")
    return formatter
}()

func formatLogLine(_ event: ConsoleEvent) -> String {
    "[\(logDateFormatter.string(from: event.timestamp))] [\(event.source.label)] [\(event.level.label)] \(redactDemoLogMessage(event.message))"
}

func matchesSource(_ event: ConsoleEvent, filters: Set<ConsoleEventSource>) -> Bool {
    filters.isEmpty || filters.contains(event.source)
}

func matchesLevel(_ event: ConsoleEvent, filter: AppLogLevelFilter) -> Bool {
    switch filter {
    case .all:
        return true
    case .debug:
        return event.level == .debug
    case .info:
        return event.level == .info
    case .warn:
        return event.level == .warn
    case .error:
        return event.level == .error
    }
}

func sourceFilterSummary(_ filters: Set<ConsoleEventSource>) -> String {
    if filters.isEmpty {
        return DemoStrings.tr("all")
    }

    let orderedLabels = ConsoleEventSource.allCases
        .filter { filters.contains($0) }
        .map(\.label)
    return orderedLabels.joined(separator: ", ")
}

private func sdkDebugLevel(for rawMessage: String) -> ConsoleEventLevel {
    if rawMessage.hasPrefix("core-error:") {
        return .error
    }
    if rawMessage.hasPrefix("BLE") &&
        (
            rawMessage.localizedCaseInsensitiveContains("failed") ||
                rawMessage.localizedCaseInsensitiveContains("error")
        ) {
        return .error
    }
    if rawMessage.contains("capability-warning") {
        return .warn
    }
    if rawMessage.contains("defaultModuleProbe"), rawMessage.contains("supported=false") {
        return .warn
    }
    if rawMessage.contains(" failure ") || rawMessage.hasSuffix(" failure") || rawMessage.hasPrefix("failure ") {
        return .error
    }
    if rawMessage.hasPrefix("reject ") {
        return .warn
    }
    if rawMessage.hasPrefix("initializeSession ") || rawMessage.hasPrefix("capability ") {
        return .info
    }
    if rawMessage.hasPrefix("resolvedModel=") {
        return .info
    }
    if rawMessage.hasPrefix("state=") {
        return .info
    }
    return .debug
}

private func sdkDebugSource(for rawMessage: String) -> ConsoleEventSource {
    if rawMessage.hasPrefix("core:") || rawMessage.hasPrefix("core-error:") {
        return .core
    }
    if rawMessage.hasPrefix("BLE") ||
        rawMessage.hasPrefix("[NSDKAppleBLE]") ||
        rawMessage.contains("onConnectionStateChange") ||
        rawMessage.contains("onServicesDiscovered") {
        return .ble
    }
    return .sdk
}

private func formatSdkDebugMessage(_ rawMessage: String) -> String {
    let (context, body) = splitSdkDebugContext(rawMessage)
    let prefix = context.map { "\($0) " } ?? ""

    if let message = formatSdkCapabilityWarning(body) {
        return prefix + message
    }
    if let message = formatSdkProbeMessage(body) {
        return prefix + message
    }

    switch body {
    case let value where value.hasPrefix("core-error:"):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_core_error",
            fallback: "Core error",
            value: value.replacingOccurrences(of: "core-error:", with: "").trimmingCharacters(in: .whitespaces)
        )
    case let value where value.hasPrefix("core:"):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_core_log",
            fallback: "Core log",
            value: value.replacingOccurrences(of: "core:", with: "").trimmingCharacters(in: .whitespaces)
        )
    case let value where value.hasPrefix("initializeSession "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_session_initialized",
            fallback: "Session initialized",
            value: value.replacingOccurrences(of: "initializeSession ", with: "")
        )
    case let value where value.hasPrefix("capability "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_capability_summary",
            fallback: "Capability summary",
            value: value.replacingOccurrences(of: "capability ", with: "")
        )
    case let value where value.hasPrefix("resolvedModel="):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_resolved_model",
            fallback: "SDK resolved model",
            value: value.replacingOccurrences(of: "resolvedModel=", with: "")
        )
    case let value where value.hasPrefix("state="):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_session_state_mirror",
            fallback: "Session state mirror",
            value: value.replacingOccurrences(of: "state=", with: "")
        )
    case let value where value.hasPrefix("failure "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_session_failure_mirror",
            fallback: "Session failure mirror",
            value: value.replacingOccurrences(of: "failure ", with: "")
        )
    case let value where value.hasPrefix("reject "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_call_rejected",
            fallback: "Call rejected",
            value: value.replacingOccurrences(of: "reject ", with: "")
        )
    case let value where value.hasPrefix("BLE scan started "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_ble_scan_started",
            fallback: "BLE scan started",
            value: value.replacingOccurrences(of: "BLE scan started ", with: "")
        )
    case "BLE scan stopped":
        return prefix + DemoStrings.tr("ble_scan_stopped")
    case let value where value.hasPrefix("BLE discovery preflight failed: "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_ble_discovery_preflight_failed",
            fallback: "BLE discovery preflight failed",
            value: value.replacingOccurrences(of: "BLE discovery preflight failed: ", with: "")
        )
    case let value where value.hasPrefix("Connect: "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_connect_request",
            fallback: "Connect request",
            value: value.replacingOccurrences(of: "Connect: ", with: "")
        )
    case let value where value.hasPrefix("Drop unnamed BLE discovery "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_drop_unnamed_ble_discovery",
            fallback: "Dropped unnamed BLE discovery",
            value: value.replacingOccurrences(of: "Drop unnamed BLE discovery ", with: "")
        )
    case let value where value.hasPrefix("Drop BLE discovery "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_drop_ble_discovery",
            fallback: "Dropped BLE discovery by profile filter",
            value: value.replacingOccurrences(of: "Drop BLE discovery ", with: "")
        )
    case let value where value.hasPrefix("cachedBattery "):
        return prefix + DemoStrings.withLocalizedValue(
            "sdk_log_cached_battery",
            fallback: "Cached battery",
            value: value.replacingOccurrences(of: "cachedBattery ", with: "")
        )
    default:
        return rawMessage
    }
}

private func splitSdkDebugContext(_ rawMessage: String) -> (String?, String) {
    guard rawMessage.hasPrefix("session["),
          let separator = rawMessage.firstIndex(of: " ") else {
        return (nil, rawMessage)
    }
    return (String(rawMessage[..<separator]), String(rawMessage[rawMessage.index(after: separator)...]))
}

private func formatSdkCapabilityWarning(_ body: String) -> String? {
    guard body.hasPrefix("capability-warning ") else { return nil }

    if let supportStatus = sdkValue(after: "supportStatus=", in: body) {
        return DemoStrings.withLocalizedValue(
            "sdk_log_capability_downgrade",
            fallback: "Capability downgrade",
            value: "supportStatus=\(supportStatus)"
        )
    }
    if let family = sdkValue(after: "defaultModuleProbeUnavailable family=", in: body) {
        return DemoStrings.withLocalizedValue(
            "sdk_log_default_module_probe_unavailable",
            fallback: "Default module probe unavailable",
            value: "family=\(family)"
        )
    }
    if let family = sdkValue(after: "moduleCommandsWithoutNativeOrBridge family=", in: body) {
        return DemoStrings.withLocalizedValue(
            "sdk_log_module_capability_downgraded",
            fallback: "Module capability downgraded",
            value: "family=\(family), native=false, bridge=false"
        )
    }
    if let family = sdkValue(after: "moduleFamily=", in: body),
       body.contains("but moduleCommands=false") {
        return DemoStrings.withLocalizedValue(
            "sdk_log_module_commands_unavailable",
            fallback: "Module commands unavailable",
            value: "family=\(family), moduleCommands=false"
        )
    }
    if let family = sdkValue(after: "passthroughOnly family=", in: body) {
        return DemoStrings.withLocalizedValue(
            "sdk_log_passthrough_only_module_capability",
            fallback: "Passthrough-only module capability",
            value: "family=\(family)"
        )
    }
    return DemoStrings.withLocalizedValue(
        "sdk_log_sdk_warning",
        fallback: "SDK warning",
        value: body.replacingOccurrences(of: "capability-warning ", with: "")
    )
}

private func formatSdkProbeMessage(_ body: String) -> String? {
    if let family = sdkValue(after: "defaultModuleProbe family=", in: body) {
        let supported = sdkValue(after: "supported=", in: body) ?? "unknown"
        if supported == "false" {
            return DemoStrings.withLocalizedValue(
                "sdk_log_default_module_probe_failed",
                fallback: "Default module probe failed",
                value: "family=\(family)"
            )
        }
        return DemoStrings.withLocalizedValue(
            "sdk_log_default_module_probe",
            fallback: "Default module probe",
            value: "family=\(family), supported=\(supported)"
        )
    }

    if let command = sdkValue(after: "masterCommandProbe command=", in: body) {
        let supported = sdkValue(after: "supported=", in: body) ?? "unknown"
        return DemoStrings.withLocalizedValue(
            "sdk_log_master_command_probe",
            fallback: "Master command probe",
            value: "command=\(command), supported=\(supported)"
        )
    }

    if let family = sdkValue(after: "moduleCommandProbe family=", in: body) {
        let kind = sdkValue(after: "kind=", in: body) ?? "unknown"
        let supported = sdkValue(after: "supported=", in: body) ?? "unknown"
        return DemoStrings.withLocalizedValue(
            "sdk_log_module_command_probe",
            fallback: "Module command probe",
            value: "family=\(family), kind=\(kind), supported=\(supported)"
        )
    }

    return nil
}

private func sdkValue(after key: String, in text: String) -> String? {
    guard let range = text.range(of: key) else { return nil }
    let tail = text[range.upperBound...]
    let end = tail.firstIndex(of: " ") ?? tail.endIndex
    return String(tail[..<end])
}
