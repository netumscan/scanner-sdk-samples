import SwiftUI

struct AppLogsPageView: View {
    @ObservedObject var viewModel: AppViewModel
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void

    @State private var selectedSources = Set<ConsoleEventSource>()
    @State private var levelFilter: AppLogLevelFilter = .all
    @State private var query = ""
    @State private var autoScrollToLatest = true
    @State private var showsLogTools = false

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
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Color.clear
                        .frame(height: 0)
                        .id("logs-top")

                    AppLogsSectionCard(title: DemoStrings.tr("export_cleanup")) {
                        LabeledContent(
                            DemoStrings.tr("results"),
                            value: "\(filteredEvents.count) / \(viewModel.events.count)"
                        )

                        AppLogFilterSummaryBlock(
                            filteredCount: filteredEvents.count,
                            totalCount: viewModel.events.count,
                            selectedSources: selectedSources,
                            levelFilter: levelFilter,
                            query: query,
                            sdkWarningCount: sdkWarningCount,
                            sdkErrorCount: sdkErrorCount
                        )

                        HStack(spacing: 10) {
                            ShareLink(
                                item: viewModel.makeLogExport(
                                    title: DemoStrings.tr("app_logs"),
                                    scopeSummaryLines: [
                                        "\(DemoStrings.tr("export_scope")): \(DemoStrings.tr("all_logs"))",
                                        "\(DemoStrings.tr("event_count")): \(viewModel.events.count)",
                                    ] + sdkDiagnosticsSummaryLines(for: viewModel.events),
                                    events: viewModel.events
                                )
                            ) {
                                Text(DemoStrings.tr("export_all_logs"))
                                    .frame(maxWidth: .infinity)
                            }
                            .disabled(viewModel.events.isEmpty)
                            .accessibilityIdentifier(DemoAccessibility.logsExportAllButton)

                            ShareLink(item: viewModel.makeCompatibilityRecordExport()) {
                                Text(DemoStrings.tr("export_compatibility_record"))
                                    .frame(maxWidth: .infinity)
                            }
                        }

                        DisclosureGroup(DemoStrings.tr("advanced_log_tools"), isExpanded: $showsLogTools) {
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

                            LabeledContent(
                                DemoStrings.tr("warnings_errors"),
                                value: "\(sdkWarningCount) / \(sdkErrorCount)"
                            )
                            if !sdkDiagnostics.isEmpty {
                                Button(DemoStrings.tr("focus_sdk_logs")) {
                                    selectedSources = [.sdk]
                                    levelFilter = .all
                                    query = ""
                                }
                                .accessibilityIdentifier(DemoAccessibility.logsFocusSdkButton)
                            }

                            ShareLink(
                                item: viewModel.makeLogExport(
                                    title: DemoStrings.tr("app_logs"),
                                    scopeSummaryLines: [
                                        "\(DemoStrings.tr("source_filter")): \(sourceFilterSummary(selectedSources))",
                                        "\(DemoStrings.tr("level_filter")): \(levelFilter.label)",
                                        "\(DemoStrings.tr("search")): \(query.isEmpty ? DemoStrings.tr("none") : query)",
                                        "\(DemoStrings.tr("event_count")): \(filteredEvents.count)",
                                    ] + sdkDiagnosticsSummaryLines(for: filteredEvents),
                                    events: filteredEvents
                                )
                            ) {
                                Text(DemoStrings.tr("export_filtered"))
                            }
                            .disabled(filteredEvents.isEmpty)
                            .accessibilityIdentifier(DemoAccessibility.logsExportFilteredButton)

                            Button(DemoStrings.tr("clear_global_logs"), role: .destructive) {
                                viewModel.clearLogs()
                            }
                            .disabled(viewModel.events.isEmpty)
                            .accessibilityIdentifier(DemoAccessibility.logsClearButton)
                        }
                    }

                    AppLogsSectionCard(title: DemoStrings.tr("recent_events")) {
                        if filteredEvents.isEmpty {
                            Text(DemoStrings.tr("no_logs"))
                                .foregroundStyle(.secondary)
                        } else {
                            VStack(alignment: .leading, spacing: 10) {
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
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                                }
                            }
                        }
                    }

                    Color.clear
                        .frame(height: 1)
                        .id("logs-bottom")
                }
                .padding(16)
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

private struct AppLogFilterSummaryBlock: View {
    let filteredCount: Int
    let totalCount: Int
    let selectedSources: Set<ConsoleEventSource>
    let levelFilter: AppLogLevelFilter
    let query: String
    let sdkWarningCount: Int
    let sdkErrorCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(DemoStrings.tr("filter_summary", fallback: "Filter Summary"))
                .font(.subheadline.weight(.semibold))
            Text(
                DemoStrings.format(
                    "filter_summary_format",
                    fallback: "%d / %d events, sources=%@, level=%@",
                    filteredCount,
                    totalCount,
                    sourceFilterSummary(selectedSources),
                    levelFilter.label
                )
            )
            .font(.footnote)
            .foregroundStyle(.secondary)
            if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(
                    DemoStrings.format(
                        "search_keyword_format",
                        fallback: "Search keyword: %@",
                        query
                    )
                )
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            Text("\(DemoStrings.tr("warnings_errors")): \(sdkWarningCount) / \(sdkErrorCount)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct AppLogsSectionCard<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
    }
}
