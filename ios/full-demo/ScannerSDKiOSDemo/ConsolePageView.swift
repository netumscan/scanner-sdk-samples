import ScannerSDK
import SwiftUI

private enum ConsoleTab: String, CaseIterable, Identifiable {
    case common
    case capabilities
    case dataRules
    case diagnostics

    var id: String { rawValue }

    var title: String {
        switch self {
        case .common: return DemoStrings.tr("common_functions", fallback: "Common")
        case .capabilities: return DemoStrings.tr("device_capabilities", fallback: "Capabilities")
        case .dataRules: return DemoStrings.tr("data_rules")
        case .diagnostics: return DemoStrings.tr("diagnostics")
        }
    }
}

struct ConsolePageView: View {
    @ObservedObject var viewModel: AppViewModel
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void
    let onOpenLogs: () -> Void

    @StateObject private var catalogStore = CapabilityCatalogStore()
    @State private var selectedTab: ConsoleTab = .common
    @State private var capabilityEntries: [CapabilityEntry] = []
    @State private var settingsLoadError: String?
    @State private var settingsQuery = ""
    @State private var masterSettingDomainKey: String?
    @State private var masterSettingFamilyKey: String?
    @State private var moduleSettingDomainKey: String?
    @State private var moduleSettingFamilyKey: String?
    @State private var settingDrafts: [String: SettingDraft] = [:]
    @State private var capabilityOptionsByKey: [String: [DemoCapabilityOption]] = [:]
    @State private var capabilityLabelsByIdentity: [String: CapabilityLabel] = [:]
    @State private var deviceActionInputs: [String: String] = [:]
    @State private var expandedCapabilityEntryKeys = Set<String>()
    @State private var dataRuleMode: DataRuleFormMode = .prefix
    @State private var dataRuleValueA = ""
    @State private var dataRuleValueB = ""

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                ConsoleSectionCard(title: DemoStrings.tr("console_tools")) {
                    ConsoleTabBar(selectedTab: $selectedTab)
                }

                switch selectedTab {
                case .common:
                    ConsoleSectionCard(title: DemoStrings.tr("scan_workspace")) {
                        scanWorkspaceContent
                    }
                    ConsoleSectionCard(title: DemoStrings.tr("common_functions", fallback: "Common")) {
                        quickActionsContent
                    }
                case .capabilities:
                    ConsoleSectionCard(title: DemoStrings.tr("device_capabilities", fallback: "Capabilities")) {
                        capabilitiesTab
                    }
                case .dataRules:
                    ConsoleSectionCard(title: DemoStrings.tr("data_rules")) {
                        consoleFeedback
                        dataRulesTab
                    }
                case .diagnostics:
                    ConsoleSectionCard(title: DemoStrings.tr("diagnostics")) {
                        consoleFeedback
                        diagnosticsTab
                    }
                }
            }
            .padding(16)
        }
        .accessibilityIdentifier(DemoAccessibility.consoleList)
        .navigationTitle(DemoStrings.tr("scanner_console"))
        .toolbar {
            DemoLanguageToolbarMenu(
                selectedLanguage: selectedLanguage,
                onLanguageChange: onLanguageChange
            )
            DemoAppLogsToolbarButton(onOpenLogs: onOpenLogs, isDisabled: !viewModel.canOpenLogs)
        }
        .onAppear {
            catalogStore.reload(using: viewModel)
        }
        .onChange(of: viewModel.selectedModelKey) { _ in
            catalogStore.reload(using: viewModel)
        }
        .onChange(of: viewModel.sessionState) { state in
            if state == .ready {
                catalogStore.reload(using: viewModel)
            }
        }
        .onChange(of: selectedLanguage) { _ in
            catalogStore.reload(using: viewModel)
        }
        .onChange(of: viewModel.latestCapabilityDraftValues) { values in
            catalogStore.applyReadDraftValues(values)
        }
    }

    private var scanWorkspaceContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            ScanSummaryPanel(
                status: viewModel.statusSummary,
                scanCount: viewModel.scanCount,
                lastScanText: viewModel.lastScanText,
                lastScanMeta: viewModel.lastScanMeta,
                lastScanRawHex: viewModel.lastScanRawHex
            )

            consoleFeedback

            CompactActionRow {
                Button(DemoStrings.tr("trigger_scan_action")) {
                    viewModel.triggerScan()
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
                .disabled(!viewModel.canRunSessionCommands || !viewModel.triggerScanSupported)

                Button(DemoStrings.tr("disconnect_connection")) {
                    viewModel.disconnect()
                }
                .buttonStyle(.bordered)
                .tint(.red)
                .frame(maxWidth: .infinity)
                .disabled(viewModel.isExecuting || viewModel.isConnecting || !viewModel.hasActiveSession)
                .accessibilityIdentifier(DemoAccessibility.consoleDisconnectButton)
            }

            DisclosureGroup(DemoStrings.tr("local_parse_controls")) {
                LabeledContent(DemoStrings.tr("selected_model"), value: displayModelLabel(viewModel.selectedModelKey))
                LabeledContent("SDK", value: ScannerSDK.shared.version)
                LabeledContent(DemoStrings.tr("info"), value: viewModel.infoSummary)
                LabeledContent(DemoStrings.tr("battery"), value: viewModel.batterySummary)
                LabeledContent(
                    DemoStrings.tr("operation_support", fallback: "Operation Support"),
                    value: viewModel.operationSupportSummary
                )
                LabeledContent(DemoStrings.tr("raw_hex"), value: viewModel.lastScanRawHex)
                LabeledContent(DemoStrings.tr("device_charset"), value: viewModel.deviceCharsetSummary)
                LabeledContent(DemoStrings.tr("local_charset"), value: viewModel.localCharsetSummary)

                Button(DemoStrings.tr("read_charset_only")) {
                    viewModel.readDeviceCharsetSetting()
                }
                .disabled(viewModel.isExecuting || !viewModel.hasActiveSession)

                Picker(DemoStrings.tr("local_charset"), selection: Binding(
                    get: { viewModel.localCharset },
                    set: { viewModel.setScanTextCharset($0) }
                )) {
                    ForEach(DemoCommandCatalog.charsetOptions, id: \.self) { charset in
                        Text(charset.displayName).tag(charset)
                    }
                }
                Picker(DemoStrings.tr("local_terminator"), selection: Binding(
                    get: { terminatorPreset(for: viewModel.localTerminator) },
                    set: { viewModel.setScanTextTerminator($0.bytes) }
                )) {
                    ForEach(DemoCommandCatalog.terminatorPresets) { preset in
                        Text(preset.label).tag(preset)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var capabilitiesTab: some View {
        Text(DemoStrings.tr(
            "capability_catalog_description",
            fallback: "Capabilities are grouped by the SDK domain catalog for the resolved model and transport."
        ))
        .font(.footnote)
        .foregroundStyle(.secondary)

        switch catalogStore.loadState {
        case .idle, .loading:
            HStack(spacing: 10) {
                ProgressView()
                Text(DemoStrings.tr(
                    "capability_catalog_loading",
                    fallback: "Loading capability catalog…"
                ))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, minHeight: 80, alignment: .center)
        case .failed(let message):
            ConsoleFeedbackBanner(
                text: message,
                background: Color.red.opacity(0.12),
                foreground: .red
            )
            Button(DemoStrings.tr("retry", fallback: "Retry")) {
                viewModel.appendEvent(
                    .ui,
                    .info,
                    DemoStrings.tr(
                        "capability_catalog_retry_logged",
                        fallback: "Capability catalog retry requested"
                    )
                )
                catalogStore.reload(using: viewModel)
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)
        case .loaded:
            if catalogStore.catalog.isEmpty {
                Text(DemoStrings.tr(
                    "capability_catalog_empty",
                    fallback: "No visible capabilities are available for the resolved model and transport."
                ))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
            } else {
                LazyVStack(alignment: .leading, spacing: 10) {
                    ForEach(catalogStore.catalog) { domain in
                        NavigationLink {
                            CapabilityDomainDetailView(
                                viewModel: viewModel,
                                catalogStore: catalogStore,
                                domain: domain,
                                selectedLanguage: selectedLanguage,
                                onLanguageChange: onLanguageChange,
                                onOpenLogs: onOpenLogs
                            )
                        } label: {
                            CapabilityDomainCard(domain: domain)
                        }
                        .buttonStyle(.plain)
                        .simultaneousGesture(TapGesture().onEnded {
                            viewModel.appendEvent(
                                .ui,
                                .info,
                                DemoStrings.format(
                                    "capability_domain_opened_logged",
                                    fallback: "Capability domain opened: %@ (%d items)",
                                    domain.title,
                                    domain.items.count
                                )
                            )
                        })
                    }
                }
            }
        }
    }

    private var actionsTab: some View {
        let visibleCapabilityActions = viewModel.capabilityEntries
            .filter { $0.kind == .action && $0.supportsExecute && $0.visibleByDefault }
            .sorted {
            capabilityActionTitle($0).localizedCaseInsensitiveCompare(capabilityActionTitle($1)) == .orderedAscending
        }

        return VStack(alignment: .leading, spacing: 12) {
            if !visibleCapabilityActions.isEmpty {
                Text(
                    DemoStrings.format(
                        "device_actions_count",
                        fallback: "%d actions",
                        visibleCapabilityActions.count
                    )
                )
                .font(.footnote)
                .foregroundStyle(.secondary)

                LazyVStack(alignment: .leading, spacing: 10) {
                    ForEach(visibleCapabilityActions, id: \.entryKey) { action in
                        if action.requiresValue {
                            CapabilityActionInputCard(
                                title: capabilityActionTitle(action),
                                risk: capabilityActionRiskText(action),
                                hint: localizedCapabilityActionValueHint(action).ifBlank(
                                    DemoStrings.tr(
                                        "device_action_value_hint",
                                        fallback: "Enter action value"
                                    )
                                ),
                                value: Binding(
                                    get: { deviceActionInputs[action.entryKey, default: ""] },
                                    set: { deviceActionInputs[action.entryKey] = $0 }
                                ),
                                isDangerous: action.riskLevel != .normal,
                                isExecuting: viewModel.isExecuting,
                                isEnabled: viewModel.canRunSessionCommands,
                                onSend: {
                                    let inputValue = deviceActionInputs[action.entryKey, default: ""]
                                    viewModel.appendEvent(.ui, .info, "UI tap: device_action=\(action.entryKey) value=\(inputValue)")
                                    viewModel.executeCapabilityAction(action, inputValue: inputValue)
                                }
                            )
                        } else {
                            CommandActionRow(
                                title: capabilityActionTitle(action),
                                risk: capabilityActionRiskText(action),
                                isDangerous: action.riskLevel != .normal,
                                isExecuting: viewModel.isExecuting,
                                isEnabled: viewModel.canRunSessionCommands,
                                onSend: {
                                    viewModel.appendEvent(.ui, .info, "UI tap: device_action=\(action.entryKey)")
                                    viewModel.executeCapabilityAction(action)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private var quickActionsContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            ConsoleGroup(
                title: DemoStrings.tr("action_quick_actions"),
                summary: ""
            ) {
                CompactActionRow {
                    CompactActionButton(
                        title: DemoStrings.tr("load_device_info_action"),
                        isDangerous: false,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.refreshInfoSupported,
                        onSend: viewModel.requestInfo
                    )
                    CompactActionButton(
                        title: DemoStrings.tr("load_battery_info_action"),
                        isDangerous: false,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.batteryInfoSupported,
                        onSend: viewModel.requestBatteryLevel
                    )
                }

                CompactActionRow {
                    CompactActionButton(
                        title: DemoStrings.tr("load_memory_usage_action", fallback: "Memory Usage"),
                        isDangerous: false,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands,
                        onSend: viewModel.requestStorageUsage
                    )
                }

                CompactActionRow {
                    CompactActionButton(
                        title: DemoStrings.tr("ack_beep_on_action", fallback: "Ack Beep On"),
                        isDangerous: true,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.ackBeepSupported,
                        accessibilityIdentifier: DemoAccessibility.consoleAckBeepOnButton,
                        onSend: {
                            viewModel.appendEvent(.ui, .info, "UI tap: ack_beep_on")
                            viewModel.setAckBeepEnabled(true)
                        }
                    )
                    CompactActionButton(
                        title: DemoStrings.tr("ack_beep_off_action", fallback: "Ack Beep Off"),
                        isDangerous: true,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.ackBeepSupported,
                        accessibilityIdentifier: DemoAccessibility.consoleAckBeepOffButton,
                        onSend: {
                            viewModel.appendEvent(.ui, .info, "UI tap: ack_beep_off")
                            viewModel.setAckBeepEnabled(false)
                        }
                    )
                }

                CompactActionRow {
                    CompactActionButton(
                        title: DemoStrings.tr("vibrate_on_action", fallback: "Vibrate On"),
                        isDangerous: true,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.vibrationSupported,
                        accessibilityIdentifier: DemoAccessibility.consoleVibrateOnButton,
                        onSend: {
                            viewModel.appendEvent(.ui, .info, "UI tap: vibrate_on")
                            viewModel.setVibrationEnabled(true)
                        }
                    )
                    CompactActionButton(
                        title: DemoStrings.tr("vibrate_off_action", fallback: "Vibrate Off"),
                        isDangerous: true,
                        isExecuting: viewModel.isExecuting,
                        isEnabled: viewModel.canRunSessionCommands && viewModel.vibrationSupported,
                        accessibilityIdentifier: DemoAccessibility.consoleVibrateOffButton,
                        onSend: {
                            viewModel.appendEvent(.ui, .info, "UI tap: vibrate_off")
                            viewModel.setVibrationEnabled(false)
                        }
                    )
                }
            }
        }
    }

    private func settingsTab(source: CapabilityEntrySource) -> some View {
        let displayedSettings = filteredSettings(source: source)
        let groupedSettings = groupedSettings(displayedSettings)
        let domainFilters = settingDomainFilters(source: source)
        let familyFilters = settingFamilyFilters(source: source)
        let selectedDomain = selectedSettingDomainKey(source: source)

        return VStack(alignment: .leading, spacing: 12) {
            TextField(DemoStrings.tr("search_settings", fallback: "Search settings"), text: $settingsQuery)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            Text(settingsSummary(displayedSettings))
                .font(.footnote)
                .foregroundStyle(.secondary)

            HStack(alignment: .top, spacing: 10) {
                Picker(DemoStrings.tr("setting_domain", fallback: "Domain"), selection: settingDomainBinding(source: source)) {
                    Text(DemoStrings.tr("all")).tag(String?.none)
                    ForEach(domainFilters) { filter in
                        Text(filter.title).tag(Optional(filter.key))
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: .infinity, alignment: .leading)

                Picker(DemoStrings.tr("setting_family", fallback: "Family"), selection: settingFamilyBinding(source: source)) {
                    Text(DemoStrings.tr("all")).tag(String?.none)
                    ForEach(familyFilters) { filter in
                        Text(filter.title).tag(Optional(filter.key))
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: .infinity, alignment: .leading)
                .disabled(selectedDomain == nil && familyFilters.isEmpty)
            }

            Text(
                DemoStrings.format(
                    "setting_list_count",
                    fallback: "Visible settings: %d",
                    displayedSettings.count
                )
            )
            .font(.footnote)
            .foregroundStyle(.secondary)

            if let settingsLoadError {
                Text(settingsLoadError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            } else if displayedSettings.isEmpty {
                Text(DemoStrings.tr("setting_search_empty", fallback: "No settings match the current filters."))
                    .foregroundStyle(.secondary)
            } else {
                if displayedSettings.contains(where: \.supportsRead) {
                    Button(
                        DemoStrings.format(
                            "read_visible_settings_count",
                            fallback: "Read Visible Settings (%d)",
                            displayedSettings.filter(\.supportsRead).count
                        )
                    ) {
                        viewModel.readCapabilities(displayedSettings)
                    }
                    .buttonStyle(.bordered)
                    .disabled(viewModel.isExecuting || !viewModel.hasReadySession)
                }

                LazyVStack(alignment: .leading, spacing: 12) {
                    ForEach(groupedSettings) { group in
                        SettingSectionCard(title: group.title, subtitle: group.subtitle) {
                            LazyVStack(alignment: .leading, spacing: 10) {
                                ForEach(group.definitions, id: \.entryKey) { definition in
                                    CapabilityEntryRow(
                                        definition: definition,
                                        title: settingDisplayTitle(definition),
                                        pathText: settingPath(definition),
                                        options: capabilityOptionsByKey[definition.entryKey] ?? [],
                                        latestValue: viewModel.latestCapabilityValues[definition.entryKey],
                                        draft: settingDraftBinding(for: definition),
                                        canRead: viewModel.hasReadySession && !viewModel.isExecuting && definition.supportsRead,
                                        canWrite: viewModel.hasReadySession && !viewModel.isExecuting && definition.supportsWrite,
                                        isDetailsExpanded: Binding(
                                            get: { expandedCapabilityEntryKeys.contains(definition.entryKey) },
                                            set: { isExpanded in
                                                if isExpanded {
                                                    expandedCapabilityEntryKeys.insert(definition.entryKey)
                                                } else {
                                                    expandedCapabilityEntryKeys.remove(definition.entryKey)
                                                }
                                            }
                                        ),
                                        onRead: { viewModel.readCapability(definition) },
                                        onResetDraft: { resetSettingDraft(definition) },
                                        onWrite: { draft in
                                            writeCapability(definition, draft: draft)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        .onChange(of: selectedDomain) { _ in
            clearInvalidSettingFamily(source: source)
        }
    }

    private var dataRulesTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            ConsoleGroup(title: DemoStrings.tr("data_rule_builder"), summary: DemoStrings.tr("data_rules")) {
                if !viewModel.dataRulesSupported {
                    ConsoleFeedbackBanner(
                        text: DemoStrings.tr("data_rules_not_supported"),
                        background: Color.red.opacity(0.12),
                        foreground: .red
                    )
                }
                Text(DemoStrings.tr("scenario_data_rule_builder_hint"))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                DataRuleModeSelector(selectedMode: dataRuleMode) { mode in
                    dataRuleMode = mode
                    dataRuleValueA = ""
                    dataRuleValueB = ""
                }
                dataRuleBuilderFields
                dataRulePreview
                Button(DemoStrings.tr("execute")) {
                    viewModel.executeDataRule(
                        mode: dataRuleMode,
                        valueA: dataRuleValueA,
                        valueB: dataRuleValueB
                    )
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.canRunSessionCommands || !viewModel.dataRulesSupported)
            }
        }
    }

    private var consoleFeedback: some View {
        let state = consoleFeedbackState
        return ConsoleFeedbackBanner(
            text: state.text,
            background: state.background,
            foreground: state.foreground
        )
        .frame(maxWidth: .infinity, minHeight: 56, alignment: .topLeading)
        .opacity(state.isVisible ? 1 : 0)
        .accessibilityHidden(!state.isVisible)
    }

    private var consoleFeedbackState: (
        text: String,
        background: Color,
        foreground: Color,
        isVisible: Bool
    ) {
        if viewModel.isExecuting {
            return (
                text: DemoStrings.tr(
                    "command_running_wait",
                    fallback: "Command is running. Wait for the result before sending another command."
                ),
                background: Color.orange.opacity(0.12),
                foreground: .orange,
                isVisible: true
            )
        }
        if let errorText = viewModel.errorText {
            return (
                text: errorText,
                background: Color.red.opacity(0.12),
                foreground: .red,
                isVisible: true
            )
        }
        if let lastAction = viewModel.lastActionResult {
            return (
                text: lastAction,
                background: Color.accentColor.opacity(0.12),
                foreground: .primary,
                isVisible: true
            )
        }
        return (
            text: " ",
            background: .clear,
            foreground: .clear,
            isVisible: false
        )
    }

    private var diagnosticsTab: some View {
        VStack(alignment: .leading, spacing: 10) {
            LabeledContent(DemoStrings.tr("status"), value: viewModel.statusSummary)
            LabeledContent(DemoStrings.tr("selected_model"), value: displayModelLabel(viewModel.selectedModelKey))
            LabeledContent(DemoStrings.tr("selected_device"), value: viewModel.deviceSummary)
            LabeledContent(
                DemoStrings.tr("operation_support", fallback: "Operation Support"),
                value: viewModel.operationSupportSummary
            )
            Text(viewModel.diagnosticsSummary)
                .font(.caption.monospaced())
                .textSelection(.enabled)
            if let lastAction = viewModel.lastActionResult {
                Text(lastAction)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            if let errorText = viewModel.errorText {
                Text(errorText)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }

    @ViewBuilder
    private var dataRuleBuilderFields: some View {
        switch dataRuleMode {
        case .prefix, .suffix:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_text_ascii_hint"),
                text: $dataRuleValueA
            )
        case .hideStart, .hideEnd:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_length_hint"),
                text: $dataRuleValueA,
                keyboard: .numberPad
            )
        case .hideMiddle:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_start_hint"),
                text: $dataRuleValueA,
                keyboard: .numberPad
            )
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_length_hint"),
                text: $dataRuleValueB,
                keyboard: .numberPad
            )
        case .replace:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_source_ascii_hint"),
                text: $dataRuleValueA
            )
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_target_ascii_hint"),
                text: $dataRuleValueB
            )
        }
    }

    private var dataRulePreview: some View {
        let preview = makeDataRulePreview()
        return Text(preview)
            .font(.caption.monospaced())
            .foregroundStyle(preview.hasPrefix(DemoStrings.tr("data_rule_valid_hint")) ? Color.secondary : Color.red)
            .textSelection(.enabled)
    }

    private func makeDataRulePreview() -> String {
        do {
            let command = try DemoCommandCatalog.buildDataRule(
                mode: dataRuleMode,
                valueA: dataRuleValueA,
                valueB: dataRuleValueB
            )
            return [
                DemoStrings.tr("data_rule_valid_hint"),
                "\(DemoStrings.tr("data_rule_command_preview")): \(dataRuleMode.title)",
                "\(command)"
            ].joined(separator: "\n")
        } catch {
            return DemoErrorFormatter.detail(error)
        }
    }

    private func filteredSettings(source: CapabilityEntrySource) -> [CapabilityEntry] {
        let query = settingsQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let domainKey = selectedSettingDomainKey(source: source)
        let familyKey = selectedSettingFamilyKey(source: source)
        return capabilityEntries.filter { definition in
            guard definition.kind == .setting else { return false }
            guard definition.source == source else { return false }
            if let domainKey, definition.domainKey != domainKey { return false }
            if let familyKey, definition.familyKey != familyKey { return false }
            guard !query.isEmpty else { return true }
            return definition.entryKey.localizedCaseInsensitiveContains(query)
                || definition.semanticKey.localizedCaseInsensitiveContains(query)
                || definition.defaultValue.localizedCaseInsensitiveContains(query)
                || definition.domainKey.localizedCaseInsensitiveContains(query)
                || definition.familyKey.localizedCaseInsensitiveContains(query)
                || definition.sectionKey.localizedCaseInsensitiveContains(query)
                || definition.notes.localizedCaseInsensitiveContains(query)
        }
        .sorted(by: sortSettings)
    }

    private func settingDomainFilters(source: CapabilityEntrySource) -> [SettingFilter] {
        makeSettingFilters(
            capabilityEntries
                .filter { $0.kind == .setting && $0.source == source }
                .map(\.domainKey)
        )
    }

    private func settingFamilyFilters(source: CapabilityEntrySource) -> [SettingFilter] {
        let domainKey = selectedSettingDomainKey(source: source)
        return makeSettingFilters(
            capabilityEntries
                .filter { definition in
                    definition.kind == .setting && definition.source == source && (domainKey == nil || definition.domainKey == domainKey)
                }
                .map(\.familyKey)
        )
    }

    private func makeSettingFilters(_ keys: [String]) -> [SettingFilter] {
        Array(Set(keys))
            .sorted { formatSettingGroupTitle($0) < formatSettingGroupTitle($1) }
            .map { SettingFilter(key: $0, title: formatSettingGroupTitle($0)) }
    }

    private func selectedSettingDomainKey(source: CapabilityEntrySource) -> String? {
        switch source {
        case .master: return masterSettingDomainKey
        case .module: return moduleSettingDomainKey
        case .session: return masterSettingDomainKey
        }
    }

    private func selectedSettingFamilyKey(source: CapabilityEntrySource) -> String? {
        switch source {
        case .master: return masterSettingFamilyKey
        case .module: return moduleSettingFamilyKey
        case .session: return masterSettingFamilyKey
        }
    }

    private func settingDomainBinding(source: CapabilityEntrySource) -> Binding<String?> {
        Binding(
            get: { selectedSettingDomainKey(source: source) },
            set: { newValue in
                switch source {
                case .master:
                    masterSettingDomainKey = newValue
                    masterSettingFamilyKey = nil
                case .module:
                    moduleSettingDomainKey = newValue
                    moduleSettingFamilyKey = nil
                case .session:
                    masterSettingDomainKey = newValue
                    masterSettingFamilyKey = nil
                }
            }
        )
    }

    private func settingFamilyBinding(source: CapabilityEntrySource) -> Binding<String?> {
        Binding(
            get: { selectedSettingFamilyKey(source: source) },
            set: { newValue in
                switch source {
                case .master: masterSettingFamilyKey = newValue
                case .module: moduleSettingFamilyKey = newValue
                case .session: masterSettingFamilyKey = newValue
                }
            }
        )
    }

    private func clearInvalidSettingFamily(source: CapabilityEntrySource) {
        let familyKeys = Set(settingFamilyFilters(source: source).map(\.key))
        guard let selectedFamily = selectedSettingFamilyKey(source: source),
              !familyKeys.contains(selectedFamily) else {
            return
        }
        switch source {
        case .master: masterSettingFamilyKey = nil
        case .module: moduleSettingFamilyKey = nil
        case .session: masterSettingFamilyKey = nil
        }
    }

    private func settingsSummary(_ definitions: [CapabilityEntry]) -> String {
        DemoStrings.format(
            "setting_items_count",
            definitions.count,
            definitions.filter(\.supportsRead).count,
            definitions.filter(\.supportsWrite).count
        )
    }

    private func loadSettings() {
        do {
            capabilityEntries = try ScannerSDK.shared.getCapabilityEntries(
                modelKey: viewModel.activeModelKey,
                transport: .bleGatt
            )
            capabilityOptionsByKey = Dictionary(uniqueKeysWithValues: capabilityEntries.map { definition in
                (definition.entryKey, definition.options.map(DemoCapabilityOption.init(sdkOption:)))
            })
            capabilityLabelsByIdentity = loadCapabilityLabelMap()
            syncSettingDrafts()
            expandedCapabilityEntryKeys.formIntersection(capabilityEntries.map { $0.entryKey })
            settingsLoadError = nil
        } catch {
            capabilityEntries = []
            capabilityOptionsByKey.removeAll()
            capabilityLabelsByIdentity.removeAll()
            settingDrafts.removeAll()
            expandedCapabilityEntryKeys.removeAll()
            settingsLoadError = "\(DemoStrings.tr("load_settings_failed")): \(DemoErrorFormatter.detail(error))"
        }
    }

    private func settingDraftBinding(for definition: CapabilityEntry) -> Binding<SettingDraft> {
        Binding(
            get: { settingDrafts[definition.entryKey] ?? defaultDraft(for: definition) },
            set: { settingDrafts[definition.entryKey] = $0 }
        )
    }

    private func resetSettingDraft(_ definition: CapabilityEntry) {
        settingDrafts.removeValue(forKey: definition.entryKey)
    }

    private func writeCapability(_ definition: CapabilityEntry, draft: SettingDraft) {
        do {
            let value = try encodeSettingDraft(definition: definition, draft: draft)
            let summary = settingDraftSummary(
                definition: definition,
                draft: draft,
                options: capabilityOptionsByKey[definition.entryKey] ?? []
            )
            viewModel.writeCapability(definition, value: value, persist: draft.persist, valueSummary: summary)
        } catch {
            viewModel.errorText = "\(DemoStrings.tr("write")) \(settingDisplayTitle(definition)): \(DemoErrorFormatter.detail(error))"
            viewModel.lastActionResult = DemoStrings.tr("command_failed", fallback: "Command failed")
        }
    }

    private func syncSettingDrafts() {
        let validKeys = Set(capabilityEntries.map { $0.entryKey })
        settingDrafts = settingDrafts.filter { validKeys.contains($0.key) }
    }

    private func defaultDraft(for definition: CapabilityEntry) -> SettingDraft {
        SettingDraft(valueText: definition.defaultValue, persist: false)
    }

    private func sortSettings(_ lhs: CapabilityEntry, _ rhs: CapabilityEntry) -> Bool {
        let leftName = settingDisplayTitle(lhs)
        let rightName = settingDisplayTitle(rhs)
        if lhs.domainKey != rhs.domainKey { return settingGroupTitle(kind: .domain, key: lhs.domainKey) < settingGroupTitle(kind: .domain, key: rhs.domainKey) }
        if lhs.familyKey != rhs.familyKey { return settingGroupTitle(kind: .family, key: lhs.familyKey) < settingGroupTitle(kind: .family, key: rhs.familyKey) }
        if lhs.sectionKey != rhs.sectionKey { return settingGroupTitle(kind: .section, key: lhs.sectionKey) < settingGroupTitle(kind: .section, key: rhs.sectionKey) }
        if leftName != rightName { return leftName.localizedCaseInsensitiveCompare(rightName) == .orderedAscending }
        return lhs.entryKey.localizedCaseInsensitiveCompare(rhs.entryKey) == .orderedAscending
    }

    private func groupedSettings(_ definitions: [CapabilityEntry]) -> [CapabilitySectionGroup] {
        Dictionary(grouping: definitions) { definition in
            [
                definition.domainKey,
                definition.familyKey,
                definition.sectionKey,
            ].joined(separator: "\u{1F}")
        }
        .map { _, definitions in
            let first = definitions[0]
            let title = settingGroupTitle(kind: .section, key: first.sectionKey)
            let subtitleParts = [
                settingGroupTitle(kind: .domain, key: first.domainKey),
                settingGroupTitle(kind: .family, key: first.familyKey),
            ]
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

            return CapabilitySectionGroup(
                key: [
                    first.domainKey,
                    first.familyKey,
                    first.sectionKey,
                ].joined(separator: "/"),
                title: title,
                subtitle: subtitleParts.joined(separator: " / "),
                definitions: definitions
            )
        }
        .sorted { lhs, rhs in
            if lhs.subtitle != rhs.subtitle {
                return lhs.subtitle.localizedCaseInsensitiveCompare(rhs.subtitle) == .orderedAscending
            }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }
    }

    private func terminatorPreset(for bytes: Data) -> ScanTerminatorPreset {
        DemoCommandCatalog.terminatorPresets.first { $0.bytes == bytes } ?? .cr
    }

    private func loadCapabilityLabelMap() -> [String: CapabilityLabel] {
        var labels: [String: CapabilityLabel] = [:]
        for kind in CapabilityLabelKind.allCases {
            guard let kindLabels = try? ScannerSDK.shared.getCapabilityLabels(kind: kind) else {
                continue
            }
            for label in kindLabels {
                labels[capabilityLabelIdentity(kind: kind, key: label.key, ownerKey: label.ownerKey)] = label
            }
        }
        return labels
    }

    private func capabilityActionTitle(_ action: CapabilityEntry) -> String {
        capabilityLabelDisplay(kind: .action, key: action.entryKey)
            .ifBlank(readableCapabilityKey(action.semanticKey.ifBlank(action.entryKey)))
    }

    private func settingDisplayTitle(_ definition: CapabilityEntry) -> String {
        capabilityLabelDisplay(kind: .setting, key: definition.entryKey)
            .ifBlank(readableCapabilityKey(definition.semanticKey.ifBlank(definition.entryKey)))
    }

    private func settingGroupTitle(kind: CapabilityLabelKind, key: String) -> String {
        capabilityLabelDisplay(kind: kind, key: key)
            .ifBlank(formatSettingGroupTitle(key))
    }

    private func capabilityLabelDisplay(kind: CapabilityLabelKind, key: String, ownerKey: String = "") -> String {
        guard let label = capabilityLabelsByIdentity[capabilityLabelIdentity(kind: kind, key: key, ownerKey: ownerKey)] else {
            return ""
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.displayName)
    }
}

private struct ConsoleTabBar: View {
    @Binding var selectedTab: ConsoleTab

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(ConsoleTab.allCases) { tab in
                    Button(tab.title) {
                        selectedTab = tab
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(selectedTab == tab ? .accentColor : Color.secondary.opacity(0.35))
                    .accessibilityIdentifier(DemoAccessibility.consoleTab(tab.rawValue))
                }
            }
        }
    }
}

private struct CapabilityDomainCard: View {
    let domain: CapabilityCatalogDomain

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "slider.horizontal.3")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Color.accentColor)
                .frame(width: 40, height: 40)
                .background(Color.accentColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 4) {
                Text(domain.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                Text(DemoStrings.format(
                    "capability_domain_summary",
                    fallback: "Read %d · Write %d · Actions %d",
                    domain.readableCount,
                    domain.writableCount,
                    domain.actionCount
                ))
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }

            Spacer()

            Text(DemoStrings.format(
                "capability_item_count",
                fallback: "%d items",
                domain.items.count
            ))
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Color.accentColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(Color.accentColor.opacity(0.12), in: Capsule())

            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 72, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.12), lineWidth: 1)
        )
        .accessibilityIdentifier(DemoAccessibility.capabilityDomain(domain.id))
    }
}

private struct CapabilityDomainDetailView: View {
    @ObservedObject var viewModel: AppViewModel
    @ObservedObject var catalogStore: CapabilityCatalogStore
    let domain: CapabilityCatalogDomain
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void
    let onOpenLogs: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                Text(DemoStrings.format(
                    "capability_domain_detail_summary",
                    fallback: "%d capabilities · %d readable · %d writable · %d actions",
                    domain.items.count,
                    domain.readableCount,
                    domain.writableCount,
                    domain.actionCount
                ))
                .font(.footnote)
                .foregroundStyle(.secondary)

                if !readableSettings.isEmpty {
                    Button(DemoStrings.tr(
                        "read_domain_settings",
                        fallback: "Read All in This Domain"
                    )) {
                        viewModel.readCapabilities(readableSettings)
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                    .disabled(!viewModel.canRunSessionCommands)
                }

                if !viewModel.hasReadySession {
                    ConsoleFeedbackBanner(
                        text: DemoStrings.tr(
                            "settings_requires_ready_session",
                            fallback: "Settings become available after the session is ready."
                        ),
                        background: Color.red.opacity(0.12),
                        foreground: .red
                    )
                }

                ForEach(domain.sections) { section in
                    CapabilityDomainSectionView(
                        viewModel: viewModel,
                        catalogStore: catalogStore,
                        domain: domain,
                        section: section
                    )
                }
            }
            .padding(16)
        }
        .navigationTitle(domain.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            DemoLanguageToolbarMenu(
                selectedLanguage: selectedLanguage,
                onLanguageChange: onLanguageChange
            )
            DemoAppLogsToolbarButton(onOpenLogs: onOpenLogs, isDisabled: !viewModel.canOpenLogs)
        }
        .accessibilityIdentifier(DemoAccessibility.capabilityDomainDetail(domain.id))
    }

    private var readableSettings: [CapabilityEntry] {
        domain.items.map(\.definition).filter {
            $0.kind == .setting && $0.supportsRead
        }
    }
}

private struct CapabilityDomainSectionView: View {
    @ObservedObject var viewModel: AppViewModel
    @ObservedObject var catalogStore: CapabilityCatalogStore
    let domain: CapabilityCatalogDomain
    let section: CapabilityCatalogSection

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(sectionTitle)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)

            if !settingItems.isEmpty {
                VStack(spacing: 0) {
                    ForEach(Array(settingItems.enumerated()), id: \.offset) { index, item in
                        CapabilitySettingCompactRow(
                            viewModel: viewModel,
                            catalogStore: catalogStore,
                            domain: domain,
                            item: item
                        )
                        if index < settingItems.count - 1 {
                            Divider().padding(.horizontal, 12)
                        }
                    }
                }
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.secondary.opacity(0.12), lineWidth: 1)
                )
                .accessibilityIdentifier(
                    DemoAccessibility.capabilitySettingGroup(section.familyKey, section.sectionKey)
                )
            }

            ForEach(actionItems) { item in
                CapabilityDomainActionCard(viewModel: viewModel, item: item)
            }
        }
    }

    private var settingItems: [CapabilityCatalogItem] {
        section.items.filter { $0.definition.kind == .setting }
    }

    private var actionItems: [CapabilityCatalogItem] {
        section.items.filter { $0.definition.kind == .action }
    }

    private var sectionTitle: String {
        [section.familyTitle, section.sectionTitle]
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .reduce(into: [String]()) { result, value in
                if result.last != value { result.append(value) }
            }
            .joined(separator: " / ")
    }
}

private struct CapabilitySettingCompactRow: View {
    @ObservedObject var viewModel: AppViewModel
    @ObservedObject var catalogStore: CapabilityCatalogStore
    let domain: CapabilityCatalogDomain
    let item: CapabilityCatalogItem

    @State private var editorPresented = false
    @State private var pendingRiskDraft: SettingDraft?

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(item.title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(compactSupportLabel)
                    .font(.caption2)
                    .foregroundStyle(definition.riskLevel == .normal ? Color.secondary : Color.red)
                    .lineLimit(1)
            }

            Spacer()
            trailingControl
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { editorPresented = true }
        .accessibilityIdentifier(DemoAccessibility.capabilitySetting(definition.entryKey))
        .sheet(isPresented: $editorPresented) {
            CapabilitySettingEditorSheet(
                viewModel: viewModel,
                catalogStore: catalogStore,
                domain: domain,
                item: item,
                onRequestWrite: requestWrite
            )
            .presentationDetents([.medium, .large])
        }
        .alert(
            item.title,
            isPresented: Binding(
                get: { pendingRiskDraft != nil },
                set: { if !$0 { pendingRiskDraft = nil } }
            )
        ) {
            Button(DemoStrings.tr("cancel"), role: .cancel) {
                viewModel.appendEvent(
                    .ui,
                    .info,
                    DemoStrings.format(
                        "risk_action_cancelled_logged",
                        fallback: "Risk action cancelled: %@",
                        item.title
                    )
                )
                pendingRiskDraft = nil
            }
            Button(DemoStrings.tr("confirm", fallback: "Confirm"), role: .destructive) {
                guard let pendingRiskDraft else { return }
                write(pendingRiskDraft)
                self.pendingRiskDraft = nil
            }
        } message: {
            Text(capabilityActionRiskText(definition))
        }
    }

    @ViewBuilder
    private var trailingControl: some View {
        let draft = catalogStore.draft(for: definition)
        if definition.valueKind == .boolean,
           viewModel.latestCapabilityValues[definition.entryKey] != nil || !definition.supportsRead {
            Toggle(
                "",
                isOn: Binding(
                    get: { parseBooleanDraft(draft.valueText) ?? false },
                    set: { enabled in
                        var nextDraft = draft
                        nextDraft.valueText = enabled ? "true" : "false"
                        catalogStore.updateDraft(nextDraft, for: definition)
                        requestWrite(nextDraft)
                    }
                )
            )
            .labelsHidden()
            .disabled(!canWrite)
        } else {
            HStack(spacing: 6) {
                Text(currentSummary)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .frame(maxWidth: 110, alignment: .trailing)
                if definition.supportsRead && !definition.supportsWrite {
                    Button(DemoStrings.tr("read_setting_action", fallback: "Read")) {
                        viewModel.readCapability(definition)
                    }
                    .buttonStyle(.borderless)
                    .disabled(!canRead)
                } else {
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var definition: CapabilityEntry { item.definition }
    private var canRead: Bool { viewModel.canRunSessionCommands && definition.supportsRead }
    private var canWrite: Bool { viewModel.canRunSessionCommands && definition.supportsWrite }
    private var currentSummary: String {
        viewModel.latestCapabilityValues[definition.entryKey]
            ?? (definition.supportsRead
                ? DemoStrings.tr("setting_value_not_loaded", fallback: "Not loaded")
                : DemoStrings.tr("setting_write_only", fallback: "Write only"))
    }
    private var compactSupportLabel: String {
        let source = capabilitySourceShortLabel(definition.source)
        let support: String
        if definition.supportsRead && definition.supportsWrite {
            support = "\(DemoStrings.tr("read")) · \(DemoStrings.tr("write"))"
        } else if definition.supportsRead {
            support = DemoStrings.tr("setting_support_read_only", fallback: "Read only")
        } else {
            support = DemoStrings.tr("setting_support_write_only", fallback: "Write only")
        }
        return "\(source) · \(support)"
    }

    private func requestWrite(_ draft: SettingDraft) {
        if definition.riskLevel == .normal {
            write(draft)
        } else {
            pendingRiskDraft = draft
        }
    }

    private func write(_ draft: SettingDraft) {
        do {
            let value = try encodeSettingDraft(definition: definition, draft: draft)
            viewModel.writeCapability(
                definition,
                value: value,
                persist: draft.persist,
                valueSummary: settingDraftSummary(
                    definition: definition,
                    draft: draft,
                    options: catalogStore.optionsByKey[definition.entryKey] ?? []
                )
            )
        } catch {
            viewModel.errorText = "\(DemoStrings.tr("write")) \(item.title): \(DemoErrorFormatter.detail(error))"
            viewModel.lastActionResult = DemoStrings.tr("command_failed", fallback: "Command failed")
        }
    }
}

private struct CapabilitySettingEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: AppViewModel
    @ObservedObject var catalogStore: CapabilityCatalogStore
    let domain: CapabilityCatalogDomain
    let item: CapabilityCatalogItem
    let onRequestWrite: (SettingDraft) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 8) {
                        SettingSupportCapsule(text: capabilitySourceShortLabel(definition.source))
                        if definition.supportsRead {
                            SettingSupportCapsule(text: DemoStrings.tr("setting_support_read", fallback: "Readable"))
                        }
                        if definition.supportsWrite {
                            SettingSupportCapsule(text: DemoStrings.tr("setting_support_write", fallback: "Writable"))
                        }
                    }

                    Text(DemoStrings.tr("setting_value_overview", fallback: "Value overview"))
                        .font(.subheadline.weight(.semibold))
                    SettingMetaBlock(
                        title: DemoStrings.tr("setting_current_value", fallback: "Current"),
                        value: currentSummary
                    )
                    SettingMetaBlock(
                        title: DemoStrings.tr("setting_default_value", fallback: "Default"),
                        value: defaultSummary
                    )

                    if !definition.notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(DemoStrings.tr("setting_description", fallback: "Description"))
                                .font(.subheadline.weight(.semibold))
                            Text(definition.notes)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
                    }

                    if definition.supportsWrite {
                        Text(DemoStrings.tr("setting_new_value", fallback: "New value"))
                            .font(.subheadline.weight(.semibold))
                        CapabilityEditor(
                            definition: definition,
                            draft: draftBinding,
                            options: options
                        )
                        if let validationError {
                            Text(validationError)
                                .font(.caption)
                                .foregroundStyle(.red)
                        }

                        Toggle(
                            DemoStrings.tr("persist_setting_write", fallback: "Persist after write"),
                            isOn: Binding(
                                get: { draftBinding.wrappedValue.persist },
                                set: { draftBinding.wrappedValue.persist = $0 }
                            )
                        )

                        Button(DemoStrings.tr("reset_setting_draft", fallback: "Reset Draft")) {
                            catalogStore.resetDraft(for: definition)
                        }
                        .buttonStyle(.bordered)
                    }

                    HStack(spacing: 10) {
                        if definition.supportsRead {
                            Button(DemoStrings.tr("read_setting_action", fallback: "Read")) {
                                viewModel.readCapability(definition)
                            }
                            .buttonStyle(.bordered)
                            .frame(maxWidth: .infinity)
                            .disabled(!viewModel.canRunSessionCommands)
                        }
                        if definition.supportsWrite {
                            Button(DemoStrings.tr("write_setting_action", fallback: "Write")) {
                                let draft = draftBinding.wrappedValue
                                dismiss()
                                onRequestWrite(draft)
                            }
                            .buttonStyle(.borderedProminent)
                            .frame(maxWidth: .infinity)
                            .disabled(!viewModel.canRunSessionCommands || validationError != nil)
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle(item.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(DemoStrings.tr("setting_dialog_close", fallback: "Done")) {
                        dismiss()
                    }
                }
            }
            .accessibilityIdentifier(DemoAccessibility.capabilitySettingEditor(definition.entryKey))
        }
    }

    private var definition: CapabilityEntry { item.definition }
    private var options: [DemoCapabilityOption] {
        catalogStore.optionsByKey[definition.entryKey] ?? []
    }
    private var draftBinding: Binding<SettingDraft> {
        Binding(
            get: { catalogStore.draft(for: definition) },
            set: { catalogStore.updateDraft($0, for: definition) }
        )
    }
    private var validationError: String? {
        validateSettingDraft(
            definition: definition,
            draft: draftBinding.wrappedValue,
            options: options
        )
    }
    private var currentSummary: String {
        viewModel.latestCapabilityValues[definition.entryKey]
            ?? (definition.supportsRead
                ? DemoStrings.tr("setting_value_not_loaded", fallback: "Not loaded")
                : DemoStrings.tr("setting_write_only", fallback: "Write only"))
    }
    private var defaultSummary: String {
        if definition.valueKind == .enumeration,
           let option = options.first(where: { $0.rawValue == definition.defaultValue }) {
            return option.label
        }
        return definition.defaultValue.ifBlank(
            DemoStrings.tr("setting_default_not_provided", fallback: "Not provided")
        )
    }
}

private struct CapabilityDomainActionCard: View {
    @ObservedObject var viewModel: AppViewModel
    let item: CapabilityCatalogItem

    @State private var inputValue = ""
    @State private var confirmationPresented = false

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(item.title)
                        .font(.subheadline.weight(.semibold))
                    Text(capabilityActionRiskLabel(definition.riskLevel))
                        .font(.caption)
                        .foregroundStyle(definition.riskLevel == .normal ? Color.secondary : Color.red)
                }
                Spacer()
                SettingSupportCapsule(text: capabilitySourceShortLabel(definition.source))
            }

            if definition.requiresValue {
                TextField(
                    localizedCapabilityActionValueHint(definition),
                    text: $inputValue
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)
            }

            if !definition.notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(definition.notes)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Button(DemoStrings.tr("execute_device_action", fallback: "Execute Device Action")) {
                if definition.riskLevel == .normal {
                    execute()
                } else {
                    confirmationPresented = true
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(definition.riskLevel == .normal ? .accentColor : .red)
            .frame(maxWidth: .infinity)
            .disabled(
                !viewModel.canRunSessionCommands ||
                    !definition.supportsExecute ||
                    (definition.requiresValue && inputValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            )
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.12), lineWidth: 1)
        )
        .alert(
            item.title,
            isPresented: $confirmationPresented
        ) {
            Button(DemoStrings.tr("cancel"), role: .cancel) {
                viewModel.appendEvent(
                    .ui,
                    .info,
                    DemoStrings.format(
                        "risk_action_cancelled_logged",
                        fallback: "Risk action cancelled: %@",
                        item.title
                    )
                )
            }
            Button(DemoStrings.tr("confirm", fallback: "Confirm"), role: .destructive) {
                execute()
            }
        } message: {
            Text(capabilityActionRiskText(definition))
        }
    }

    private var definition: CapabilityEntry { item.definition }

    private func execute() {
        if definition.requiresValue {
            viewModel.executeCapabilityAction(definition, inputValue: inputValue)
        } else {
            viewModel.executeCapabilityAction(definition)
        }
    }
}

private func capabilitySourceShortLabel(_ source: CapabilityEntrySource) -> String {
    switch source {
    case .master:
        return DemoStrings.tr("setting_source_master_short", fallback: "Master")
    case .module:
        return DemoStrings.tr("setting_source_module_short", fallback: "Module")
    case .session:
        return DemoStrings.tr("setting_source_session_short", fallback: "Session")
    }
}

private struct SettingFilter: Identifiable {
    let key: String
    let title: String

    var id: String { key }
}

private struct CapabilitySectionGroup: Identifiable {
    let key: String
    let title: String
    let subtitle: String
    let definitions: [CapabilityEntry]

    var id: String { key }
}

private func formatSettingGroupTitle(_ key: String) -> String {
    let normalized = key
        .replacingOccurrences(of: #"([a-z0-9])([A-Z])"#, with: "$1 $2", options: .regularExpression)
        .replacingOccurrences(of: "_", with: " ")
        .replacingOccurrences(of: "-", with: " ")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    guard !normalized.isEmpty else {
        return DemoStrings.tr("setting_group_general", fallback: "General")
    }
    return normalized
        .split(whereSeparator: \.isWhitespace)
        .map { word in
            word.prefix(1).uppercased() + word.dropFirst().lowercased()
        }
        .joined(separator: " ")
}

private func capabilityLabelIdentity(kind: CapabilityLabelKind, key: String, ownerKey: String = "") -> String {
    "\(kind.rawValue)\u{1F}\(ownerKey)\u{1F}\(key)"
}

private func readableCapabilityKey(_ key: String) -> String {
    let normalized = key
        .replacingOccurrences(of: "nsdk.", with: "")
        .replacingOccurrences(of: #"([a-z0-9])([A-Z])"#, with: "$1 $2", options: .regularExpression)
        .replacingOccurrences(of: ".", with: " ")
        .replacingOccurrences(of: "_", with: " ")
        .replacingOccurrences(of: "-", with: " ")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    guard !normalized.isEmpty else {
        return DemoStrings.emptyValue
    }
    let acronyms: Set<String> = ["ACK", "ASCII", "BLE", "BT", "GATT", "HID", "ID", "NTC06H", "RF", "SDK", "SPP", "USB"]
    return normalized
        .split(whereSeparator: \.isWhitespace)
        .map { rawWord in
            let word = String(rawWord)
            let uppercased = word.uppercased()
            if acronyms.contains(uppercased) {
                return uppercased
            }
            return word.prefix(1).uppercased() + word.dropFirst().lowercased()
        }
        .joined(separator: " ")
}

private struct CommandActionRow: View {
    let title: String
    let risk: String
    let isDangerous: Bool
    let isExecuting: Bool
    let isEnabled: Bool
    let accessibilityIdentifier: String?
    let onSend: () -> Void

    init(
        title: String,
        risk: String,
        isDangerous: Bool,
        isExecuting: Bool,
        isEnabled: Bool,
        accessibilityIdentifier: String? = nil,
        onSend: @escaping () -> Void
    ) {
        self.title = title
        self.risk = risk
        self.isDangerous = isDangerous
        self.isExecuting = isExecuting
        self.isEnabled = isEnabled
        self.accessibilityIdentifier = accessibilityIdentifier
        self.onSend = onSend
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(title) {
                onSend()
            }
            .buttonStyle(isDangerous ? .borderedProminent : .borderedProminent)
            .tint(isDangerous ? .red : .accentColor)
            .frame(maxWidth: .infinity)
            .disabled(!isEnabled || isExecuting)
            .applyOptionalAccessibilityIdentifier(accessibilityIdentifier)

            Text(risk)
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.08), lineWidth: 1)
        )
    }
}

private struct CompactActionRow<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        HStack(spacing: 10) {
            content
        }
    }
}

private struct CompactActionButton: View {
    let title: String
    let isDangerous: Bool
    let isExecuting: Bool
    let isEnabled: Bool
    let accessibilityIdentifier: String?
    let onSend: () -> Void

    init(
        title: String,
        isDangerous: Bool,
        isExecuting: Bool,
        isEnabled: Bool,
        accessibilityIdentifier: String? = nil,
        onSend: @escaping () -> Void
    ) {
        self.title = title
        self.isDangerous = isDangerous
        self.isExecuting = isExecuting
        self.isEnabled = isEnabled
        self.accessibilityIdentifier = accessibilityIdentifier
        self.onSend = onSend
    }

    var body: some View {
        Button(title) {
            onSend()
        }
        .buttonStyle(.borderedProminent)
        .tint(isDangerous ? .red : .accentColor)
        .frame(maxWidth: .infinity)
        .disabled(!isEnabled || isExecuting)
        .applyOptionalAccessibilityIdentifier(accessibilityIdentifier)
    }
}

private struct CapabilityActionInputCard: View {
    let title: String
    let risk: String
    let hint: String
    @Binding var value: String
    let isDangerous: Bool
    let isExecuting: Bool
    let isEnabled: Bool
    let onSend: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.medium))

            TextField(hint, text: $value)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            Text(risk)
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            Button(DemoStrings.tr("execute_device_action", fallback: "Execute Device Action")) {
                onSend()
            }
            .buttonStyle(.borderedProminent)
            .tint(isDangerous ? .red : .accentColor)
            .frame(maxWidth: .infinity)
            .disabled(!isEnabled || isExecuting || value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.08), lineWidth: 1)
        )
    }
}

private func capabilityActionRiskLabel(_ level: CapabilityRiskLevel) -> String {
    switch level {
    case .normal:
        return DemoStrings.tr("risk_level_normal", fallback: "Normal")
    case .destructive:
        return DemoStrings.tr("risk_level_destructive", fallback: "Destructive")
    case .connectivity:
        return DemoStrings.tr("risk_level_connectivity", fallback: "Connectivity")
    case .dataLoss:
        return DemoStrings.tr("risk_level_data_loss", fallback: "Data Loss")
    }
}

private func capabilityActionRiskText(_ action: CapabilityEntry) -> String {
    let level = capabilityActionRiskLabel(action.riskLevel)
    let hint = localizedCapabilityActionValueHint(action).trimmingCharacters(in: .whitespacesAndNewlines)
    switch action.riskLevel {
    case .normal:
        return hint.isEmpty
            ? DemoStrings.tr("device_action_risk_normal", fallback: "This action should be safe in normal demo flows.")
            : "\(DemoStrings.tr("device_action_risk_normal", fallback: "This action should be safe in normal demo flows.")) \(hint)"
    case .destructive:
        return DemoStrings.tr("device_action_risk_destructive", fallback: "This action may overwrite defaults or reset device state. Confirm the test unit is safe to modify.") + " [\(level)]"
    case .connectivity:
        return DemoStrings.tr("device_action_risk_connectivity", fallback: "This action may disconnect, reboot, or invalidate the current link.") + " [\(level)]"
    case .dataLoss:
        return DemoStrings.tr("device_action_risk_data_loss", fallback: "This action may clear or consume stored data on the device. Confirm data loss is acceptable.") + " [\(level)]"
    }
}

func localizedCapabilityActionTitle(_ action: CapabilityEntry) -> String {
    readableCapabilityKey(action.semanticKey.ifBlank(action.entryKey))
}

func localizedCapabilityActionValueHint(_ action: CapabilityEntry) -> String {
    let fallback = action.valueHint.trimmingCharacters(in: .whitespacesAndNewlines)
    return fallback.isEmpty ? "Enter action value" : fallback
}

private extension View {
    @ViewBuilder
    func applyOptionalAccessibilityIdentifier(_ identifier: String?) -> some View {
        if let identifier {
            accessibilityIdentifier(identifier)
        } else {
            self
        }
    }
}

private struct CapabilityEntryRow: View {
    let definition: CapabilityEntry
    let title: String
    let pathText: String
    let options: [DemoCapabilityOption]
    let latestValue: String?
    @Binding var draft: SettingDraft
    let canRead: Bool
    let canWrite: Bool
    @Binding var isDetailsExpanded: Bool
    let onRead: () -> Void
    let onResetDraft: () -> Void
    let onWrite: (SettingDraft) -> Void

    var body: some View {
        let validationError = validateSettingDraft(definition: definition, draft: draft, options: options)

        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.subheadline.weight(.medium))
                    Text(pathText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()

                VStack(alignment: .trailing, spacing: 6) {
                    SettingSupportCapsule(text: sourceLabel(definition.source))
                    HStack(spacing: 6) {
                        SettingSupportCapsule(text: "\(DemoStrings.tr("read")) \(supportFlag(definition.supportsRead))")
                        SettingSupportCapsule(text: "\(DemoStrings.tr("write")) \(supportFlag(definition.supportsWrite))")
                    }
                }
            }

            HStack(spacing: 8) {
                SettingMetaBlock(
                    title: DemoStrings.tr("setting_current_value", fallback: "Current Value"),
                    value: latestSummary
                )
                SettingMetaBlock(
                    title: DemoStrings.tr("setting_default_value", fallback: "Default Value"),
                    value: defaultSummary
                )
            }

            if !definition.notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(definition.notes)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            CapabilityEditor(definition: definition, draft: $draft, options: options)

            if let validationError {
                Text(validationError)
                    .font(.caption)
                    .foregroundStyle(.red)
            }

            HStack(spacing: 8) {
                if definition.supportsRead {
                    Button(DemoStrings.tr("read_setting_action", fallback: "Read")) {
                        onRead()
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                    .disabled(!canRead)
                }

                if definition.supportsWrite {
                    Button(writeButtonTitle) {
                        onWrite(draft)
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(!canWrite || validationError != nil)
                }
            }

            if definition.supportsWrite && definition.valueKind != .action {
                Toggle(DemoStrings.tr("persist_setting_write", fallback: "Persist Setting Write"), isOn: $draft.persist)
                    .font(.caption)
                    .disabled(!canWrite)

                Button(DemoStrings.tr("reset_setting_draft", fallback: "Reset Draft")) {
                    onResetDraft()
                }
                .buttonStyle(.bordered)
                .disabled(!canWrite)
            }

            DisclosureGroup(DemoStrings.tr("details"), isExpanded: $isDetailsExpanded) {
                Text(summary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var latestSummary: String {
        if let latestValue {
            return latestValue
        }
        return definition.supportsRead
            ? DemoStrings.tr("setting_value_not_loaded", fallback: "Value not loaded")
            : DemoStrings.tr("setting_write_only", fallback: "Write only")
    }

    private var defaultSummary: String {
        definition.defaultValue.ifBlank(DemoStrings.tr("setting_default_not_provided", fallback: "Default not provided"))
    }

    private var writeButtonTitle: String {
        switch definition.valueKind {
        case .action:
            return DemoStrings.tr("execute_setting_action", fallback: "Execute")
        default:
            return DemoStrings.tr("write_setting_action", fallback: "Write")
        }
    }

    private var summary: String {
        [
            title,
            valueKindLabel(definition.valueKind),
            sourceLabel(definition.source),
            pathText,
            "\(DemoStrings.tr("read"))=\(supportFlag(definition.supportsRead))",
            "\(DemoStrings.tr("write"))=\(supportFlag(definition.supportsWrite))",
        ].joined(separator: " / ")
    }

    private func sourceLabel(_ source: CapabilityEntrySource) -> String {
        switch source {
        case .master: return DemoStrings.tr("setting_source_master_short")
        case .module: return DemoStrings.tr("setting_source_module_short")
        case .session: return DemoStrings.tr("setting_source_master_short")
        }
    }

    private func valueKindLabel(_ kind: CapabilityValueKind) -> String {
        switch kind {
        case .unknown: return DemoStrings.tr("setting_kind_unknown")
        case .boolean: return DemoStrings.tr("setting_kind_boolean")
        case .enumeration: return DemoStrings.tr("setting_kind_enum")
        case .uint8: return "uint8"
        case .uint16: return "uint16"
        case .bytesAscii: return "ASCII"
        case .action: return DemoStrings.tr("setting_kind_action")
        case .complex: return DemoStrings.tr("setting_kind_complex")
        case .custom: return DemoStrings.tr("setting_kind_custom")
        case .object: return DemoStrings.tr("setting_kind_object")
        case .template: return DemoStrings.tr("setting_kind_template")
        }
    }
}

private struct SettingSectionCard<Content: View>: View {
    let title: String
    let subtitle: String
    let content: Content

    init(title: String, subtitle: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(title)
                    .font(.subheadline.weight(.semibold))

                Spacer()

                if !subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(subtitle)
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.trailing)
                }
            }

            Rectangle()
                .fill(Color.secondary.opacity(0.12))
                .frame(height: 1)

            content
        }
        .padding(12)
        .background(Color.secondary.opacity(0.06), in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.10), lineWidth: 1)
        )
    }
}

private struct SettingMetaBlock: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value.ifBlank(DemoStrings.emptyValue))
                .font(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
    }
}

private struct SettingSupportCapsule: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color.secondary.opacity(0.12), in: Capsule())
            .foregroundStyle(.secondary)
    }
}

private struct CapabilityEditor: View {
    let definition: CapabilityEntry
    @Binding var draft: SettingDraft
    let options: [DemoCapabilityOption]

    var body: some View {
        switch definition.valueKind {
        case .boolean:
            Toggle(
                DemoStrings.tr("setting_boolean_label", fallback: "Enabled"),
                isOn: Binding(
                    get: { parseBooleanDraft(draft.valueText) ?? false },
                    set: { draft.valueText = $0 ? "true" : "false" }
                )
            )
        case .enumeration:
            if options.isEmpty {
                TextField(
                    DemoStrings.tr("setting_enum_raw_value", fallback: "Enum Raw Value"),
                    text: $draft.valueText
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)
            } else {
                Picker(
                    DemoStrings.tr("setting_enum_raw_value", fallback: "Enum Value"),
                    selection: $draft.valueText
                ) {
                    ForEach(options) { option in
                        Text(option.label).tag(option.rawValue)
                    }
                }
                .pickerStyle(.menu)
            }
        case .uint8, .uint16, .bytesAscii, .complex, .custom, .object, .template, .unknown:
            TextField(settingEditorTitle(definition), text: $draft.valueText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)
        case .action:
            EmptyView()
        }
    }
}

struct DemoCapabilityOption: Identifiable {
    let rawValue: String
    let label: String

    var id: String { rawValue }

    init(rawValue: String, label: String) {
        self.rawValue = rawValue
        self.label = label
    }

    init(sdkOption: CapabilityOption) {
        rawValue = sdkOption.value
        label = sdkOption.value
    }
}

private func settingPath(_ definition: CapabilityEntry) -> String {
    [
        formatSettingGroupTitle(definition.domainKey),
        formatSettingGroupTitle(definition.familyKey),
        formatSettingGroupTitle(definition.sectionKey),
    ]
    .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    .reduce(into: [String]()) { result, value in
        if result.last != value {
            result.append(value)
        }
    }
    .joined(separator: " / ")
}

private func settingEditorTitle(_ definition: CapabilityEntry) -> String {
    switch definition.valueKind {
    case .uint8, .uint16:
        return DemoStrings.tr("setting_numeric_value", fallback: "Numeric Value")
    case .bytesAscii:
        return DemoStrings.tr("setting_ascii_value", fallback: "ASCII Value")
    default:
        return DemoStrings.tr("setting_raw_value", fallback: "Raw Value")
    }
}

private func validateSettingDraft(
    definition: CapabilityEntry,
    draft: SettingDraft,
    options: [DemoCapabilityOption]
) -> String? {
    do {
        _ = try encodeSettingDraft(definition: definition, draft: draft)
        return nil
    } catch {
        return DemoErrorFormatter.detail(error)
    }
}

private func settingDraftSummary(
    definition: CapabilityEntry,
    draft: SettingDraft,
    options: [DemoCapabilityOption]
) -> String {
    switch definition.valueKind {
    case .boolean:
        return supportFlag(parseBooleanDraft(draft.valueText) ?? false)
    case .enumeration:
        if let option = options.first(where: { $0.rawValue == draft.valueText }) {
            return option.label
        }
        return draft.valueText.ifBlank(DemoStrings.emptyValue)
    default:
        return draft.valueText.ifBlank(DemoStrings.emptyValue)
    }
}

private func encodeSettingDraft(definition: CapabilityEntry, draft: SettingDraft) throws -> CapabilityValue {
    let text = draft.valueText.trimmingCharacters(in: .whitespacesAndNewlines)

    switch definition.valueKind {
    case .boolean:
        guard let booleanValue = parseBooleanDraft(text) else {
            throw settingValidationError(
                DemoStrings.tr(
                    "setting_validation_boolean",
                    fallback: "Enter true/false, 1/0, on/off, or yes/no."
                )
            )
        }
        return .boolean(booleanValue)
    case .enumeration:
        guard let bytes = parseCapabilityOptionBytes(text) else {
            throw settingValidationError(
                DemoStrings.tr(
                    "setting_validation_enum_format",
                    fallback: "Enter a valid enum value from the list, a decimal number, or a hex value like 0x01."
                )
            )
        }
        return .bytes(.enumeration, bytes)
    case .uint8:
        guard let value = parseSettingInteger(text), value >= 0, value <= 0xFF else {
            throw settingValidationError(
                DemoStrings.tr(
                    "setting_validation_uint8",
                    fallback: "Enter a number from 0 to 255."
                )
            )
        }
        return .bytes(.uint8, Data([UInt8(value)]))
    case .uint16:
        guard let value = parseSettingInteger(text), value >= 0, value <= 0xFFFF else {
            throw settingValidationError(
                DemoStrings.tr(
                    "setting_validation_uint16",
                    fallback: "Enter a number from 0 to 65535."
                )
            )
        }
        return .bytes(.uint16, Data([UInt8((value >> 8) & 0xFF), UInt8(value & 0xFF)]))
    case .bytesAscii:
        return .asciiText(draft.valueText)
    case .action:
        return .bytes(.action, Data())
    case .complex, .custom, .object, .template, .unknown:
        guard let bytes = parseSettingBytes(text) else {
            throw settingValidationError(
                DemoStrings.tr(
                    "setting_validation_hex_invalid",
                    fallback: "Enter hex bytes like 0A, 0A0B, or 0A 0B."
                )
            )
        }
        return .bytes(definition.valueKind, bytes)
    }
}

private func parseBooleanDraft(_ text: String) -> Bool? {
    switch text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
    case "true", "1", "on", "enable", "enabled", "yes":
        return true
    case "false", "0", "off", "disable", "disabled", "no":
        return false
    default:
        return nil
    }
}

private func parseSettingInteger(_ raw: String) -> Int? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.lowercased().hasPrefix("0x") {
        return Int(trimmed.dropFirst(2), radix: 16)
    }
    if trimmed.range(of: #"^[0-9A-Fa-f]{2,4}$"#, options: .regularExpression) != nil,
       trimmed.rangeOfCharacter(from: CharacterSet.letters) != nil {
        return Int(trimmed, radix: 16)
    }
    return Int(trimmed)
}

private func parseSettingBytes(_ raw: String) -> Data? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.isEmpty {
        return Data()
    }

    let normalized = trimmed
        .replacingOccurrences(of: ",", with: " ")
        .replacingOccurrences(of: "0x", with: "", options: [.caseInsensitive])
    let compact = normalized.replacingOccurrences(of: " ", with: "")

    if compact.range(of: #"^[0-9A-Fa-f]+$"#, options: .regularExpression) != nil,
       compact.count.isMultiple(of: 2) {
        var bytes: [UInt8] = []
        var index = compact.startIndex
        while index < compact.endIndex {
            let nextIndex = compact.index(index, offsetBy: 2)
            let byteString = String(compact[index..<nextIndex])
            guard let byte = UInt8(byteString, radix: 16) else { return nil }
            bytes.append(byte)
            index = nextIndex
        }
        return Data(bytes)
    }

    return nil
}

func parseCapabilityOptionBytes(_ raw: String) -> Data? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.isEmpty {
        return nil
    }
    if trimmed.lowercased().hasPrefix("0x") {
        let hex = String(trimmed.dropFirst(2))
        let normalized = hex.count.isMultiple(of: 2) ? hex : "0\(hex)"
        return parseSettingBytes(normalized)
    }
    if let value = Int(trimmed) {
        switch value {
        case 0...0xFF:
            return Data([UInt8(value & 0xFF)])
        case 0x100...0xFFFF:
            return Data([UInt8((value >> 8) & 0xFF), UInt8(value & 0xFF)])
        default:
            return nil
        }
    }
    return Data(trimmed.utf8)
}

private func settingValidationError(_ description: String) -> NSError {
    NSError(
        domain: "ScannerSDKiOSDemo.SettingValidation",
        code: 1,
        userInfo: [NSLocalizedDescriptionKey: description]
    )
}

private struct ScanSummaryPanel: View {
    let status: String
    let scanCount: Int
    let lastScanText: String
    let lastScanMeta: String
    let lastScanRawHex: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(status)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer()
                Text("\(DemoStrings.tr("scan_count")): \(scanCount)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(DemoStrings.tr("last_scan"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(lastScanText)
                    .font(.title3.weight(.semibold))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(3)
                Text(lastScanMeta)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Text("\(DemoStrings.tr("raw_hex")): \(lastScanRawHex)")
                    .font(.caption.monospaced())
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct ConsoleFeedbackBanner: View {
    let text: String
    let background: Color
    let foreground: Color

    var body: some View {
        Text(text)
            .font(.subheadline)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(background, in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(foreground)
    }
}

private struct ConsoleGroup<Content: View>: View {
    let title: String
    let summary: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Text(summary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            content()
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct ConsoleSectionCard<Content: View>: View {
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
