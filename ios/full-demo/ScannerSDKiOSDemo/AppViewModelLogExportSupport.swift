import Foundation

@MainActor
extension AppViewModel {
    func makeLogExport(title: String, scopeSummaryLines: [String], events: [ConsoleEvent]) -> String {
        let deviceSummaryLines = [
            "\(DemoStrings.tr("status")): \(statusSummary)",
            "\(DemoStrings.tr("device")): \(deviceSummary)",
            selectedModelSummary,
            "\(DemoStrings.tr("info")): \(infoSummary)",
            sdkResolvedModelSummary,
            "\(DemoStrings.tr("capability_summary")): \(capabilitySummary)",
            "\(DemoStrings.tr("module_capability")): \(moduleSummary)",
            "\(DemoStrings.tr("module_commands_ready")): \(moduleCommandAvailabilitySummary)",
            "\(DemoStrings.tr("battery")): \(batterySummary)",
            "\(DemoStrings.tr("device_charset")): \(deviceCharsetSummary)",
            "\(DemoStrings.tr("device_terminal")): \(deviceTerminalSummary)",
            "\(DemoStrings.tr("local_scan_charset")): \(localCharsetSummary)",
            "\(DemoStrings.tr("local_scan_terminator")): \(localTerminatorSummary)",
        ]
        let body = events.map(formatLogLine).joined(separator: "\n")
        return ([
            title,
            "\(DemoStrings.tr("exported_at")): \(logDateFormatter.string(from: Date()))",
        ] + scopeSummaryLines + deviceSummaryLines + ["", body])
            .map(redactDemoLogMessage)
            .joined(separator: "\n")
    }

    func makeCompatibilityRecordExport() -> String {
        [
            DemoStrings.tr("compatibility_record"),
            "\(DemoStrings.tr("exported_at")): \(logDateFormatter.string(from: Date()))",
            "",
            DemoCompatibilityRecord.format(state: diagnosticsStore.snapshot()),
        ].joined(separator: "\n")
    }
}
