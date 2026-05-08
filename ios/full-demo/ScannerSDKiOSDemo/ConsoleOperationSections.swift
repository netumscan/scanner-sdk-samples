import SwiftUI
import ScannerSDK

struct ConsoleStatusSection: View {
    @ObservedObject var viewModel: AppViewModel

    var body: some View {
        Section {
            ConsoleSummaryCard(
                title: DemoStrings.tr("current_status"),
                isExecuting: viewModel.isExecuting,
                onDisconnect: viewModel.disconnect,
                rows: [
                    (DemoStrings.tr("status"), viewModel.statusSummary),
                    (DemoStrings.tr("device_selected_model"), "\(viewModel.deviceSummary)\n\(viewModel.selectedModelSummary)"),
                    (DemoStrings.tr("protocol_mode"), viewModel.protocolChannelKindSummary),
                    (DemoStrings.tr("capability_sdk_resolved"), "\(viewModel.capabilitySummary)\n\(viewModel.sdkResolvedModelSummary)"),
                    (DemoStrings.tr("diagnostics"), viewModel.diagnosticsSummary),
                    (DemoStrings.tr("module_capability"), viewModel.moduleSummary),
                    (DemoStrings.tr("module_commands_ready"), viewModel.moduleCommandAvailabilitySummary),
                    (DemoStrings.tr("info"), viewModel.infoSummary),
                    (DemoStrings.tr("battery"), viewModel.batterySummary),
                ]
            )
            .accessibilityIdentifier(DemoAccessibility.consoleSummaryCard)
            DemoScenarioPresetView()
            if let lastAction = viewModel.lastActionResult {
                ConsoleFeedbackBanner(
                    text: lastAction,
                    background: Color.accentColor.opacity(0.12),
                    foreground: .primary
                )
                .accessibilityIdentifier(DemoAccessibility.consoleLastActionBanner)
            }
            if let errorText = viewModel.errorText {
                ConsoleFeedbackBanner(
                    text: errorText,
                    background: Color.red.opacity(0.12),
                    foreground: .red
                )
                .accessibilityIdentifier(DemoAccessibility.consoleErrorBanner)
            }
        }
    }
}

struct ConsoleOperationsSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var parseStateExpanded: Bool
    @Binding var quickActionsExpanded: Bool
    @Binding var commandsExpanded: Bool
    @Binding var builderExpanded: Bool
    @Binding var selectedGroupIndex: Int
    @Binding var selectedOperationScope: ConsoleOperationScope
    @Binding var pendingDangerAction: DemoPendingDangerAction?
    @Binding var builderMode: DataRuleFormMode
    @Binding var builderValueA: String
    @Binding var builderValueB: String
    @Binding var moduleDomainIndex: Int
    @Binding var inlineModulePresetID: String?
    @Binding var moduleParameterIDText: String
    @Binding var modulePayloadHexText: String
    @Binding var moduleNumericInputText: String
    @Binding var modulePersistWrite: Bool

    var body: some View {
        Section(DemoStrings.tr("operations")) {
            ConsoleSubcard(title: DemoStrings.tr("operation_scope")) {
                ConsoleOperationScopeRow(
                    selectedScope: selectedOperationScope,
                    onSelect: { selectedOperationScope = $0 }
                )
            }

            switch selectedOperationScope {
            case .master:
                ConsoleMasterScopeSection(
                    viewModel: viewModel,
                    parseStateExpanded: $parseStateExpanded,
                    quickActionsExpanded: $quickActionsExpanded,
                    commandsExpanded: $commandsExpanded,
                    builderExpanded: $builderExpanded,
                    selectedGroupIndex: $selectedGroupIndex,
                    pendingDangerAction: $pendingDangerAction,
                    builderMode: $builderMode,
                    builderValueA: $builderValueA,
                    builderValueB: $builderValueB
                )
            case .module:
                ConsoleModuleScopeSection(
                    viewModel: viewModel,
                    commandsExpanded: $commandsExpanded,
                    pendingDangerAction: $pendingDangerAction,
                    moduleDomainIndex: $moduleDomainIndex,
                    inlineModulePresetID: $inlineModulePresetID,
                    moduleParameterIDText: $moduleParameterIDText,
                    modulePayloadHexText: $modulePayloadHexText,
                    moduleNumericInputText: $moduleNumericInputText,
                    modulePersistWrite: $modulePersistWrite
                )
            }
        }
    }
}

private struct ConsoleMasterScopeSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var parseStateExpanded: Bool
    @Binding var quickActionsExpanded: Bool
    @Binding var commandsExpanded: Bool
    @Binding var builderExpanded: Bool
    @Binding var selectedGroupIndex: Int
    @Binding var pendingDangerAction: DemoPendingDangerAction?
    @Binding var builderMode: DataRuleFormMode
    @Binding var builderValueA: String
    @Binding var builderValueB: String

    var body: some View {
        ConsoleMasterQuickActionsSection(
            viewModel: viewModel,
            isExpanded: $quickActionsExpanded,
            pendingDangerAction: $pendingDangerAction
        )

        ConsoleMasterCommandsSection(
            viewModel: viewModel,
            isExpanded: $commandsExpanded,
            selectedGroupIndex: $selectedGroupIndex,
            pendingDangerAction: $pendingDangerAction
        )

        if selectedGroupIsDataRules {
            ConsoleDataRuleBuilderSection(
                viewModel: viewModel,
                isExpanded: $builderExpanded,
                builderMode: $builderMode,
                builderValueA: $builderValueA,
                builderValueB: $builderValueB
            )
        }

        ConsoleParsingAdvancedSection(
            viewModel: viewModel,
            isExpanded: $parseStateExpanded
        )
    }

    private var selectedGroupIsDataRules: Bool {
        guard DemoCommandCatalog.groups.indices.contains(selectedGroupIndex) else { return false }
        return DemoCommandCatalog.groups[selectedGroupIndex].commands.contains { entry in
            if case .text(let commandText) = entry.command {
                return commandText == "$SCAN#4" || commandText == "$SCAN#3"
            }
            return false
        }
    }
}

private struct ConsoleMasterQuickActionsSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var isExpanded: Bool
    @Binding var pendingDangerAction: DemoPendingDangerAction?

    var body: some View {
        DisclosureGroup(isExpanded: $isExpanded) {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(DemoCommandCatalog.quickActionRows) { row in
                    ConsoleGroupCard(
                        title: row.title,
                        summary: DemoStrings.format("quick_action_count", row.actions.count),
                        isDangerous: row.actions.contains(where: \.isDangerous)
                    ) {
                        ConsoleActionRows(
                            labels: row.actions.map(\.title),
                            isDangerous: { index in row.actions[index].isDangerous },
                            action: { index in
                                let action = row.actions[index]
                                if action.isDangerous {
                                    pendingDangerAction = .quickAction(action)
                                } else {
                                    viewModel.performQuickAction(action)
                                }
                            },
                            disabled: !viewModel.canExecuteMasterCommands,
                            accessibilityIdentifier: { index in
                                DemoAccessibility.consoleQuickActionButton(row.actions[index].id)
                            }
                        )
                    }
                }
            }
            .padding(.top, 8)
        } label: {
            Text(DemoStrings.tr("master_quick_actions"))
                .accessibilityIdentifier(DemoAccessibility.consoleQuickActionsSection)
        }
    }
}

private struct ConsoleMasterCommandsSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var isExpanded: Bool
    @Binding var selectedGroupIndex: Int
    @Binding var pendingDangerAction: DemoPendingDangerAction?

    var body: some View {
        let selectedGroup = DemoCommandCatalog.groups[selectedGroupIndex]
        let selectedGroupHasDanger = selectedGroup.commands.contains(where: \.isDangerous)

        DisclosureGroup(isExpanded: $isExpanded) {
            VStack(alignment: .leading, spacing: 12) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(DemoCommandCatalog.groups.enumerated()), id: \.offset) { index, group in
                            Button(group.title) {
                                selectedGroupIndex = index
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(selectedGroupIndex == index ? .accentColor : .gray.opacity(0.5))
                        }
                    }
                }

                ConsoleGroupCard(
                    title: selectedGroup.title,
                    summary: DemoStrings.format("command_count", selectedGroup.commands.count),
                    isDangerous: selectedGroupHasDanger
                ) {
                    ConsoleActionRows(
                        labels: selectedGroup.commands.map(\.title),
                        isDangerous: { index in selectedGroup.commands[index].isDangerous },
                        action: { index in
                            let entry = selectedGroup.commands[index]
                            if entry.isDangerous {
                                pendingDangerAction = .command(entry)
                            } else {
                                viewModel.executeCommand(entry)
                            }
                        },
                        disabled: !viewModel.canExecuteMasterCommands
                    )
                }
            }
            .padding(.top, 8)
        } label: {
            Text(DemoStrings.tr("master_commands"))
                .accessibilityIdentifier(DemoAccessibility.consoleMasterCommandsSection)
        }
    }
}

private struct ConsoleParsingAdvancedSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var isExpanded: Bool

    private let gridColumns = [
        GridItem(.adaptive(minimum: 120), spacing: 10),
    ]

    var body: some View {
        DisclosureGroup(isExpanded: $isExpanded) {
            VStack(alignment: .leading, spacing: 14) {
                ConsoleSubcard(title: DemoStrings.tr("device_state")) {
                    ConsoleSummaryChip(
                        label: DemoStrings.tr("device_charset"),
                        value: viewModel.deviceCharsetSummary
                    )
                    ConsoleActionRows(
                        labels: [DemoStrings.tr("send_charset")],
                        isDangerous: { _ in false },
                        action: { _ in viewModel.readCharset() },
                        disabled: !viewModel.canExecuteMasterCommands
                    )
                    ConsoleSummaryChip(
                        label: DemoStrings.tr("device_terminal"),
                        value: viewModel.deviceTerminalSummary
                    )
                }

                ConsoleSubcard(title: DemoStrings.tr("local_sdk_parse")) {
                    ConsoleSummaryChip(
                        label: DemoStrings.tr("local_charset"),
                        value: viewModel.localCharsetSummary
                    )
                    Picker(DemoStrings.tr("scan_charset"), selection: Binding(
                        get: { viewModel.localCharset },
                        set: { viewModel.applyLocalCharset($0) }
                    )) {
                        ForEach(DemoCommandCatalog.charsetOptions, id: \.self) { charset in
                            Text(charset.displayName).tag(charset)
                        }
                    }
                    .pickerStyle(.menu)

                    ConsoleSummaryChip(
                        label: DemoStrings.tr("local_terminator"),
                        value: viewModel.localTerminatorSummary
                    )
                    LazyVGrid(columns: gridColumns, spacing: 10) {
                        ForEach(DemoCommandCatalog.terminatorPresets) { preset in
                            ConsoleActionButton(
                                label: buttonLabel(for: preset),
                                dangerous: false,
                                disabled: viewModel.isExecuting,
                                action: { viewModel.applyLocalTerminator(preset) }
                            )
                        }
                    }
                }
            }
            .padding(.top, 8)
        } label: {
            Text(DemoStrings.tr("parsing_advanced"))
                .accessibilityIdentifier(DemoAccessibility.consoleParsingAdvancedSection)
        }
    }

    private func buttonLabel(for preset: ScanTerminatorPreset) -> String {
        viewModel.localTerminatorSummary == preset.summary ? "[\(preset.label)]" : preset.label
    }
}

private struct ConsoleDataRuleBuilderSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var isExpanded: Bool
    @Binding var builderMode: DataRuleFormMode
    @Binding var builderValueA: String
    @Binding var builderValueB: String

    var body: some View {
        DisclosureGroup(isExpanded: $isExpanded) {
            let buildError = builderError()
            ConsoleSubcard(title: DemoStrings.tr("builder")) {
                DataRuleModeSelector(
                    selectedMode: builderMode,
                    onSelect: { builderMode = $0 }
                )

                builderFields

                if let buildError {
                    ConsoleFeedbackBanner(
                        text: buildError,
                        background: Color.red.opacity(0.12),
                        foreground: .red
                    )
                }

                Button(DemoStrings.tr("build_and_send")) {
                    viewModel.executeDataRule(mode: builderMode, valueA: builderValueA, valueB: builderValueB)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.canExecuteMasterCommands || buildError != nil)
            }
            .padding(.top, 8)
        } label: {
            Text(DemoStrings.tr("data_rule_builder"))
                .accessibilityIdentifier(DemoAccessibility.consoleDataRuleBuilderSection)
        }
    }

    @ViewBuilder
    private var builderFields: some View {
        switch builderMode {
        case .prefix, .suffix:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_text_ascii_hint"),
                text: $builderValueA
            )
        case .hideStart, .hideEnd:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_length_hint"),
                text: $builderValueA,
                keyboard: .numberPad
            )
        case .hideMiddle:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_start_hint"),
                text: $builderValueA,
                keyboard: .numberPad
            )
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_length_hint"),
                text: $builderValueB,
                keyboard: .numberPad
            )
        case .replace:
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_source_ascii_hint"),
                text: $builderValueA
            )
            ConsoleBuilderField(
                title: DemoStrings.tr("data_rule_target_ascii_hint"),
                text: $builderValueB
            )
        }
    }

    private func builderError() -> String? {
        do {
            _ = try DemoCommandCatalog.buildDataRule(mode: builderMode, valueA: builderValueA, valueB: builderValueB)
            return nil
        } catch {
            return error.localizedDescription
        }
    }
}

private struct ConsoleModuleScopeSection: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var commandsExpanded: Bool
    @Binding var pendingDangerAction: DemoPendingDangerAction?
    @Binding var moduleDomainIndex: Int
    @Binding var inlineModulePresetID: String?
    @Binding var moduleParameterIDText: String
    @Binding var modulePayloadHexText: String
    @Binding var moduleNumericInputText: String
    @Binding var modulePersistWrite: Bool

    private var activeModuleRows: [DemoModuleActionRow] {
        DemoCommandCatalog.moduleActionRows.filter {
            $0.title == displayModuleFamilyLabel(viewModel.currentModuleFamily)
        }
    }

    var body: some View {
        DisclosureGroup(isExpanded: $commandsExpanded) {
            VStack(alignment: .leading, spacing: 12) {
                moduleRoutingCard
                moduleActionRows

                ModuleTestGuideCard(
                    family: viewModel.currentModuleFamily,
                    recommendations: DemoModuleSettingsCatalog.recommendations(for: viewModel.currentModuleFamily)
                )

                moduleParameterCatalog
            }
            .padding(.top, 8)
        } label: {
            Text(DemoStrings.tr("module_commands"))
                .accessibilityIdentifier(DemoAccessibility.consoleModuleCommandsSection)
        }
    }

    private var moduleRoutingCard: some View {
        ConsoleSubcard(title: DemoStrings.tr("module_routing")) {
            ConsoleSummaryChip(
                label: DemoStrings.tr("current_protocol"),
                value: viewModel.protocolChannelKindSummary
            )
            ConsoleSummaryChip(
                label: DemoStrings.tr("current_module"),
                value: displayModuleFamilyLabel(viewModel.currentModuleFamily)
            )
            ConsoleSummaryChip(
                label: DemoStrings.tr("module_command_state"),
                value: viewModel.moduleCommandAvailabilitySummary
            )
            ConsoleSummaryChip(
                label: DemoStrings.tr("module_capability_summary"),
                value: viewModel.moduleSummary
            )

            if viewModel.supportsModuleCommands && !viewModel.canExecuteModuleCommands {
                ConsoleFeedbackBanner(
                    text: DemoStrings.tr("module_commands_unavailable"),
                    background: Color.red.opacity(0.12),
                    foreground: .red
                )
            } else if !viewModel.supportsModuleCommands {
                ConsoleHintBanner(
                    text: DemoStrings.tr("module_capability_not_detected")
                )
            }
        }
    }

    @ViewBuilder
    private var moduleActionRows: some View {
        if activeModuleRows.isEmpty {
            ConsoleHintBanner(
                text: DemoStrings.tr("no_quick_actions")
            )
        } else {
            ForEach(activeModuleRows) { row in
                ConsoleGroupCard(
                    title: row.title,
                    summary: DemoStrings.format("module_action_count", row.actions.count),
                    isDangerous: row.actions.contains(where: \.isDangerous)
                ) {
                    ConsoleActionRows(
                        labels: row.actions.map(\.title),
                        isDangerous: { index in row.actions[index].isDangerous },
                        action: { index in
                            let action = row.actions[index]
                            if action.isDangerous {
                                pendingDangerAction = .moduleAction(action)
                            } else {
                                viewModel.performModuleAction(action)
                            }
                        },
                        disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                    )
                }
            }
        }
    }

    private var moduleParameterCatalog: some View {
        ConsoleExpandableBlock(
            title: DemoStrings.tr("module_parameter_catalog"),
            accessibilityIdentifier: DemoAccessibility.consoleModuleParameterCatalog,
            isExpanded: $viewModel.consoleModuleCatalogExpanded
        ) {
            if viewModel.currentModuleFamily == .ntc06h {
                Ntc06hModuleCatalogView(
                    viewModel: viewModel,
                    selectedDomainIndex: $viewModel.consoleNtc06hDomainIndex,
                    expandedFamilyKeys: $viewModel.consoleNtc06hExpandedFamilyKeys,
                    inlineSettingKey: $viewModel.consoleInlineNtc06hSettingKey,
                    customCode: $viewModel.consoleNtc06hCustomCode,
                    templateValue: $viewModel.consoleNtc06hTemplateValue,
                    saveAfterWrite: $viewModel.consoleNtc06hSaveAfterWrite
                )
            } else {
                GenericModuleCatalogView(
                    viewModel: viewModel,
                    selectedDomainIndex: $moduleDomainIndex,
                    inlinePresetID: $inlineModulePresetID,
                    parameterIDText: $moduleParameterIDText,
                    payloadHexText: $modulePayloadHexText,
                    numericInputText: $moduleNumericInputText,
                    persistWrite: $modulePersistWrite
                )
            }
        }
    }
}
