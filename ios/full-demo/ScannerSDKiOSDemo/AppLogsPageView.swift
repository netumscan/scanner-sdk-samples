import SwiftUI

struct AppLogsPageView: View {
    @ObservedObject var viewModel: AppViewModel
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void

    @State private var selectedSources = Set<ConsoleEventSource>()
    @State private var levelFilter: AppLogLevelFilter = .all
    @State private var query = ""
    @State private var exportSnapshot = ""
    @State private var isSharePresented = false
    @State private var autoScrollToLatest = true

    private var filteredEvents: [ConsoleEvent] {
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return viewModel.events.filter { event in
            matchesSource(event, filters: selectedSources) &&
                matchesLevel(event, filter: levelFilter) &&
                (
                    trimmedQuery.isEmpty ||
                        event.message.localizedCaseInsensitiveContains(trimmedQuery) ||
                        event.source.rawValue.localizedCaseInsensitiveContains(trimmedQuery) ||
                        event.level.rawValue.localizedCaseInsensitiveContains(trimmedQuery) ||
                        event.source.label.localizedCaseInsensitiveContains(trimmedQuery) ||
                        event.level.label.localizedCaseInsensitiveContains(trimmedQuery)
                )
        }
    }

    private var sdkDiagnostics: [ConsoleEvent] {
        sdkDiagnosticEvents(in: filteredEvents)
    }

    private var sdkWarningCount: Int {
        sdkDiagnostics.filter { $0.level == .warn }.count
    }

    private var sdkErrorCount: Int {
        sdkDiagnostics.filter { $0.level == .error }.count
    }

    var body: some View {
        ScrollViewReader { proxy in
            List {
                Section(DemoStrings.tr("filters")) {
                    Color.clear
                        .frame(height: 0)
                        .id("logs-top")

                    LabeledContent(
                        DemoStrings.tr("source_filter"),
                        value: sourceFilterSummary(selectedSources)
                    )
                    Menu(DemoStrings.tr("toggle_sources")) {
                        ForEach(ConsoleEventSource.allCases) { source in
                            Button {
                                if selectedSources.contains(source) {
                                    selectedSources.remove(source)
                                } else {
                                    selectedSources.insert(source)
                                }
                            } label: {
                                Label(
                                    source.label,
                                    systemImage: selectedSources.contains(source) ? "checkmark.circle.fill" : "circle"
                                )
                            }
                        }
                    }

                    Picker(DemoStrings.tr("level_filter"), selection: $levelFilter) {
                        ForEach(AppLogLevelFilter.allCases) { filter in
                            Text(filter.label).tag(filter)
                        }
                    }
                    .pickerStyle(.menu)

                    Toggle(isOn: $autoScrollToLatest) {
                        Text(DemoStrings.tr("auto_scroll_latest"))
                    }

                    if !query.isEmpty {
                        LabeledContent(
                            DemoStrings.tr("search"),
                            value: query
                        )
                    }
                }

                Section(DemoStrings.tr("sdk_diagnostics")) {
                    LabeledContent(
                        DemoStrings.tr("warnings_errors"),
                        value: "\(sdkWarningCount) / \(sdkErrorCount)"
                    )

                    if sdkDiagnostics.isEmpty {
                        Text(DemoStrings.tr("none"))
                        .foregroundStyle(.secondary)
                    } else {
                        Button(DemoStrings.tr("focus_sdk_logs")) {
                            selectedSources = [.sdk]
                            levelFilter = .all
                            query = ""
                        }
                        .accessibilityIdentifier(DemoAccessibility.logsFocusSdkButton)

                        ForEach(Array(sdkDiagnostics.suffix(3).reversed())) { event in
                            Text(formatLogLine(event))
                                .font(.system(.footnote, design: .monospaced))
                                .textSelection(.enabled)
                        }
                    }
                }

                Section(DemoStrings.tr("export_cleanup")) {
                    LabeledContent(
                        DemoStrings.tr("results"),
                        value: "\(filteredEvents.count) / \(viewModel.events.count)"
                    )

                    Button(DemoStrings.tr("export_filtered")) {
                        exportSnapshot = viewModel.makeLogExport(
                            title: DemoStrings.tr("app_logs"),
                            scopeSummaryLines: [
                                "\(DemoStrings.tr("source_filter")): \(sourceFilterSummary(selectedSources))",
                                "\(DemoStrings.tr("level_filter")): \(levelFilter.label)",
                                "\(DemoStrings.tr("search")): \(query.isEmpty ? DemoStrings.tr("none") : query)",
                                "\(DemoStrings.tr("event_count")): \(filteredEvents.count)",
                            ] + sdkDiagnosticsSummaryLines(for: filteredEvents),
                            events: filteredEvents
                        )
                        isSharePresented = true
                    }
                    .disabled(filteredEvents.isEmpty)
                    .accessibilityIdentifier(DemoAccessibility.logsExportFilteredButton)

                    Button(DemoStrings.tr("export_all_logs")) {
                        exportSnapshot = viewModel.makeLogExport(
                            title: DemoStrings.tr("app_logs"),
                            scopeSummaryLines: [
                                "\(DemoStrings.tr("export_scope")): \(DemoStrings.tr("all_logs"))",
                                "\(DemoStrings.tr("event_count")): \(viewModel.events.count)",
                            ] + sdkDiagnosticsSummaryLines(for: viewModel.events),
                            events: viewModel.events
                        )
                        isSharePresented = true
                    }
                    .disabled(viewModel.events.isEmpty)
                    .accessibilityIdentifier(DemoAccessibility.logsExportAllButton)

                    Button(DemoStrings.tr("export_compatibility_record")) {
                        exportSnapshot = viewModel.makeCompatibilityRecordExport()
                        isSharePresented = true
                    }

                    Button(DemoStrings.tr("clear_global_logs"), role: .destructive) {
                        viewModel.clearLogs()
                    }
                    .disabled(viewModel.events.isEmpty)
                    .accessibilityIdentifier(DemoAccessibility.logsClearButton)
                }

                Section(DemoStrings.tr("events")) {
                    if filteredEvents.isEmpty {
                        Text(DemoStrings.tr("no_logs"))
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(filteredEvents) { event in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(event.source.label)
                                        .font(.caption2)
                                        .fontWeight(.bold)
                                    Text(event.level.label)
                                        .font(.caption2)
                                        .foregroundStyle(color(for: event.level))
                                }
                                Text(formatLogLine(event))
                                    .font(.system(.footnote, design: .monospaced))
                                    .textSelection(.enabled)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    Color.clear
                        .frame(height: 1)
                        .id("logs-bottom")
                }
            }
            .accessibilityIdentifier(DemoAccessibility.logsList)
            .onChange(of: filteredEvents.count) { _ in
                guard autoScrollToLatest, !filteredEvents.isEmpty else { return }
                withAnimation {
                    proxy.scrollTo("logs-bottom", anchor: .bottom)
                }
            }
            .toolbar {
                DemoLanguageToolbarMenu(
                    selectedLanguage: selectedLanguage,
                    onLanguageChange: onLanguageChange
                )
                ToolbarItemGroup(placement: .bottomBar) {
                    Button(DemoStrings.tr("top")) {
                        withAnimation {
                            autoScrollToLatest = false
                            proxy.scrollTo("logs-top", anchor: .top)
                        }
                    }
                    .disabled(filteredEvents.isEmpty && viewModel.events.isEmpty)

                    Spacer()

                    Button(DemoStrings.tr("latest")) {
                        withAnimation {
                            autoScrollToLatest = true
                            proxy.scrollTo("logs-bottom", anchor: .bottom)
                        }
                    }
                    .disabled(filteredEvents.isEmpty)
                }
            }
        }
        .navigationTitle(DemoStrings.tr("app_logs"))
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: DemoStrings.tr("search_message_source")
        )
        .sheet(isPresented: $isSharePresented) {
            ActivityView(activityItems: [exportSnapshot])
        }
    }

    private func color(for level: ConsoleEventLevel) -> Color {
        switch level {
        case .debug: return .secondary
        case .info: return .primary
        case .warn: return .orange
        case .error: return .red
        }
    }
}
