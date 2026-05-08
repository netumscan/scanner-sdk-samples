import SwiftUI
import ScannerSDK

struct Ntc06hModuleCatalogView: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var selectedDomainIndex: Int
    @Binding var expandedFamilyKeys: Set<String>
    @Binding var inlineSettingKey: String?
    @Binding var customCode: String
    @Binding var templateValue: String
    @Binding var saveAfterWrite: Bool

    var body: some View {
        let groups = DemoModuleSettingsCatalog.ntc06hDomainGroups()
        VStack(alignment: .leading, spacing: 12) {
            if groups.isEmpty {
                ConsoleHintBanner(text: DemoStrings.tr("ntc06h_setting_catalog_unavailable"))
            } else {
                let safeIndex = selectedDomainIndex.clamped(to: 0...(groups.count - 1))
                let domain = groups[safeIndex]
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(groups.enumerated()), id: \.offset) { index, group in
                            Button(moduleDomainTitle(group.key)) {
                                selectedDomainIndex = index
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(safeIndex == index ? .accentColor : .gray.opacity(0.5))
                        }
                    }
                }

                ForEach(domain.families, id: \.key) { family in
                    ConsoleExpandableBlock(
                        title: moduleFamilyTitle(family.key),
                        isExpanded: expandedBinding(for: family.key)
                    ) {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(family.sections, id: \.key) { section in
                                ConsoleSubcard(
                                    title: moduleSectionTitle(section.key),
                                    subtitle: DemoStrings.format("setting_count", section.items.count)
                                ) {
                                    ForEach(section.items, id: \.key) { setting in
                                        Ntc06hSettingCommandCard(
                                            viewModel: viewModel,
                                            setting: setting,
                                            inlineSettingKey: $inlineSettingKey,
                                            customCode: $customCode,
                                            templateValue: $templateValue,
                                            saveAfterWrite: $saveAfterWrite
                                        )
                                    }
                                }
                            }
                        }
                        .padding(.top, 8)
                    }
                }
            }
        }
        .padding(.top, 8)
    }

    private func expandedBinding(for familyKey: String) -> Binding<Bool> {
        Binding(
            get: { expandedFamilyKeys.contains(familyKey) },
            set: { isExpanded in
                if isExpanded {
                    expandedFamilyKeys.insert(familyKey)
                } else {
                    expandedFamilyKeys.remove(familyKey)
                }
            }
        )
    }
}

private struct Ntc06hSettingCommandCard: View {
    @ObservedObject var viewModel: AppViewModel
    let setting: Ntc06hSettingDefinition
    @Binding var inlineSettingKey: String?
    @Binding var customCode: String
    @Binding var templateValue: String
    @Binding var saveAfterWrite: Bool

    var body: some View {
        let settingTitle = ntc06hSettingTitle(setting)
        VStack(alignment: .leading, spacing: 10) {
            Text(settingTitle)
                .font(.footnote.weight(.semibold))
            Text("\(setting.displayCode) / \(moduleFamilyTitle(setting.familyKey)) / \(moduleSectionTitle(setting.sectionKey))")
                .font(.caption)
                .foregroundStyle(.secondary)

            ConsoleActionRows(
                labels: [
                    setting.isTemplate ? DemoStrings.tr("edit_template") : DemoStrings.tr("send_setting"),
                    DemoStrings.tr("custom_edit"),
                ],
                isDangerous: { _ in true },
                action: { index in
                    if index == 0, !setting.isTemplate {
                        viewModel.writeNtc06hSetting(setting, saveAfterWrite: false)
                    } else {
                        loadCustomEditor()
                    }
                },
                disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
            )

            if setting.requiresSave && !setting.isTemplate {
                ConsoleActionRows(
                    labels: [DemoStrings.tr("send_save")],
                    isDangerous: { _ in true },
                    action: { _ in viewModel.writeNtc06hSetting(setting, saveAfterWrite: true) },
                    disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                )
            }

            if inlineSettingKey == setting.key {
                Ntc06hCustomSettingCard(
                    viewModel: viewModel,
                    setting: setting,
                    inlineSettingKey: $inlineSettingKey,
                    customCode: $customCode,
                    templateValue: $templateValue,
                    saveAfterWrite: $saveAfterWrite
                )
            }
        }
        .padding(.vertical, 8)
    }

    private func loadCustomEditor() {
        inlineSettingKey = inlineSettingKey == setting.key ? nil : setting.key
        saveAfterWrite = setting.requiresSave
        if let spec = ntc06hTemplateInputSpec(setting) {
            templateValue = ntc06hTemplateValueOrEmpty(setting)
            customCode = ntc06hBuildTemplateCode(spec, value: templateValue)
        } else {
            templateValue = ""
            customCode = setting.templateExampleCode.ifBlank(setting.displayCode)
        }
    }
}

private struct Ntc06hCustomSettingCard: View {
    @ObservedObject var viewModel: AppViewModel
    let setting: Ntc06hSettingDefinition
    @Binding var inlineSettingKey: String?
    @Binding var customCode: String
    @Binding var templateValue: String
    @Binding var saveAfterWrite: Bool

    private var templateSpec: DemoNtc06hTemplateInputSpec? {
        ntc06hTemplateInputSpec(setting)
    }

    private var canWrite: Bool {
        guard !customCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return false
        }
        guard let templateSpec else {
            return true
        }
        return ntc06hTemplateValueIsValid(templateValue, spec: templateSpec)
    }

    var body: some View {
        ConsoleSubcard(
            title: DemoStrings.tr("ntc06h_setting_editor")
        ) {
            Button(DemoStrings.tr("close_custom_editor")) {
                inlineSettingKey = nil
            }
            .buttonStyle(.bordered)

            if let spec = templateSpec {
                ConsoleBuilderField(
                    title: DemoStrings.format("template_value_hex", spec.placeholder.uppercased()),
                    text: $templateValue
                )
                .onChange(of: templateValue) { next in
                    let normalized = ntc06hNormalizeTemplateValue(next, spec: spec)
                    if normalized != next {
                        templateValue = normalized
                    }
                    customCode = ntc06hBuildTemplateCode(spec, value: normalized)
                }
                ConsoleSummaryChip(label: DemoStrings.tr("setting_code_preview"), value: customCode)
                if !ntc06hTemplateValueIsValid(templateValue, spec: spec) {
                    ConsoleFeedbackBanner(
                        text: DemoStrings.format("template_value_error", spec.width, String(spec.minValue, radix: 16).uppercased(), String(spec.maxValue, radix: 16).uppercased()),
                        background: Color.red.opacity(0.12),
                        foreground: .red
                    )
                }
            } else {
                ConsoleBuilderField(title: DemoStrings.tr("setting_code"), text: $customCode)
            }

            ConsoleActionRows(
                labels: [
                    DemoStrings.tr("fill_suggested"),
                    saveAfterWrite ? DemoStrings.tr("save_after_write_on") : DemoStrings.tr("save_after_write_off"),
                ],
                isDangerous: { _ in false },
                action: { index in
                    if index == 0 {
                        if let spec = templateSpec {
                            templateValue = ntc06hTemplateValueOrEmpty(setting)
                            customCode = ntc06hBuildTemplateCode(spec, value: templateValue)
                        } else {
                            customCode = setting.templateExampleCode.ifBlank(setting.displayCode)
                        }
                    } else {
                        saveAfterWrite.toggle()
                    }
                },
                disabled: false
            )

            ConsoleActionRows(
                labels: [
                    DemoStrings.tr("save_only"),
                    DemoStrings.tr("send_setting_code"),
                ],
                isDangerous: { _ in true },
                action: { index in
                    if index == 0 {
                        viewModel.saveNtc06hSettings()
                    } else {
                        viewModel.writeNtc06hRawSetting(
                            code: customCode,
                            label: ntc06hSettingTitle(setting),
                            saveAfterWrite: saveAfterWrite
                        )
                    }
                },
                disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands || !canWrite
            )
        }
    }
}
