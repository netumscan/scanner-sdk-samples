import SwiftUI
import ScannerSDK

struct ModuleTestGuideCard: View {
    let family: ModuleFamily
    let recommendations: [ModuleTestRecommendation]

    var body: some View {
        if !recommendations.isEmpty {
            ConsoleGroupCard(
                title: DemoStrings.tr("recommended_first_tests"),
                summary: displayModuleFamilyLabel(family),
                isDangerous: false
            ) {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(recommendations.enumerated()), id: \.element.recommendationID) { index, item in
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(index + 1). \(item.demoTitle)")
                                .font(.footnote.weight(.semibold))
                            Text(item.demoDetail)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
    }
}

struct GenericModuleCatalogView: View {
    @ObservedObject var viewModel: AppViewModel
    @Binding var selectedDomainIndex: Int
    @Binding var inlinePresetID: String?
    @Binding var parameterIDText: String
    @Binding var payloadHexText: String
    @Binding var numericInputText: String
    @Binding var persistWrite: Bool

    var body: some View {
        let groups = DemoModuleSettingsCatalog.domainGroups(for: viewModel.currentModuleFamily)
        VStack(alignment: .leading, spacing: 12) {
            if groups.isEmpty {
                ConsoleHintBanner(
                    text: DemoStrings.tr("module_parameter_catalog_unavailable")
                )
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
                    DisclosureGroup(moduleFamilyTitle(family.key)) {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(family.sections, id: \.key) { section in
                                ConsoleSubcard(
                                    title: moduleSectionTitle(section.key),
                                    subtitle: DemoStrings.format("parameter_count", section.items.count)
                                ) {
                                    ForEach(section.items) { preset in
                                        ModulePresetCommandCard(
                                            viewModel: viewModel,
                                            preset: preset,
                                            inlinePresetID: $inlinePresetID,
                                            parameterIDText: $parameterIDText,
                                            payloadHexText: $payloadHexText,
                                            numericInputText: $numericInputText,
                                            persistWrite: $persistWrite
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
}

private struct ModulePresetCommandCard: View {
    @ObservedObject var viewModel: AppViewModel
    let preset: DemoModuleSettingPreset
    @Binding var inlinePresetID: String?
    @Binding var parameterIDText: String
    @Binding var payloadHexText: String
    @Binding var numericInputText: String
    @Binding var persistWrite: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(preset.title)
                .font(.footnote.weight(.semibold))
            Text("0x\(preset.formattedParameterID) / \(moduleFamilyTitle(preset.familyKey)) / \(moduleSectionTitle(preset.sectionKey))")
                .font(.caption)
                .foregroundStyle(.secondary)

            ConsoleActionRows(
                labels: [
                    DemoStrings.tr("read"),
                    DemoStrings.tr("custom_edit"),
                ],
                isDangerous: { _ in false },
                action: { index in
                    if index == 0 {
                        viewModel.readModuleParameter(preset)
                    } else {
                        loadCustomEditor()
                    }
                },
                disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
            )

            if preset.supportsBooleanToggle {
                ConsoleActionRows(
                    labels: [
                        DemoStrings.tr("on"),
                        DemoStrings.tr("off"),
                    ],
                    isDangerous: { _ in true },
                    action: { index in
                        let payload = index == 0 ? preset.writeOnHex : preset.writeOffHex
                        viewModel.writeModuleParameter(preset, payloadHex: payload, persist: true)
                    },
                    disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                )
            } else if !preset.enumOptions.isEmpty {
                ForEach(Array(preset.enumOptions.chunked(into: 2).enumerated()), id: \.offset) { _, chunk in
                    ConsoleActionRows(
                        labels: chunk.map(\.label),
                        isDangerous: { _ in true },
                        action: { index in
                            viewModel.writeModuleParameter(preset, payloadHex: chunk[index].payloadHex, persist: true)
                        },
                        disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                    )
                }
            } else if !preset.quickValueOptions.isEmpty {
                ForEach(Array(preset.quickValueOptions.chunked(into: 2).enumerated()), id: \.offset) { _, chunk in
                    ConsoleActionRows(
                        labels: chunk.map(\.label),
                        isDangerous: { _ in true },
                        action: { index in
                            viewModel.writeModuleParameter(preset, payloadHex: chunk[index].payloadHex, persist: true)
                        },
                        disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                    )
                }
            } else if let defaultPayloadHex = preset.defaultPayloadHex {
                ConsoleActionRows(
                    labels: [DemoStrings.tr("write_default")],
                    isDangerous: { _ in true },
                    action: { _ in viewModel.writeModuleParameter(preset, payloadHex: defaultPayloadHex, persist: true) },
                    disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands
                )
            }

            if inlinePresetID == preset.id {
                ModuleCustomEditorCard(
                    viewModel: viewModel,
                    preset: preset,
                    inlinePresetID: $inlinePresetID,
                    parameterIDText: $parameterIDText,
                    payloadHexText: $payloadHexText,
                    numericInputText: $numericInputText,
                    persistWrite: $persistWrite
                )
            }
        }
        .padding(.vertical, 8)
    }

    private func loadCustomEditor() {
        inlinePresetID = inlinePresetID == preset.id ? nil : preset.id
        parameterIDText = preset.formattedParameterID
        payloadHexText = preset.defaultPayloadHex ?? ""
        numericInputText = preset.numericInputSpec
            .flatMap { moduleNumericInputText(fromPayload: payloadHexText, spec: $0) }
            ?? ""
        persistWrite = true
    }
}

private struct ModuleCustomEditorCard: View {
    @ObservedObject var viewModel: AppViewModel
    let preset: DemoModuleSettingPreset
    @Binding var inlinePresetID: String?
    @Binding var parameterIDText: String
    @Binding var payloadHexText: String
    @Binding var numericInputText: String
    @Binding var persistWrite: Bool

    private var parameterError: String? {
        do {
            _ = try parseModuleParameterID(parameterIDText)
            return nil
        } catch {
            return error.localizedDescription
        }
    }

    private var payloadError: String? {
        do {
            _ = try parseModulePayloadHex(payloadHexText)
            return nil
        } catch {
            return error.localizedDescription
        }
    }

    private var numericInputError: String? {
        guard let spec = preset.numericInputSpec else { return nil }
        return moduleNumericInputError(numericInputText, spec: spec)
    }

    var body: some View {
        ConsoleSubcard(
            title: DemoStrings.tr("custom_parameter_editor")
        ) {
            HStack(spacing: 8) {
                Button(DemoStrings.tr("close_custom_editor")) {
                    inlinePresetID = nil
                }
                .buttonStyle(.bordered)
                Spacer()
            }
            ConsoleBuilderField(title: DemoStrings.tr("parameter_id_hex"), text: $parameterIDText)
            ConsoleBuilderField(title: DemoStrings.tr("write_value_hex"), text: $payloadHexText)

            if let spec = preset.numericInputSpec {
                ConsoleBuilderField(title: "\(spec.label) / \(spec.rangeHint)", text: $numericInputText, keyboard: .numberPad)
                    .onChange(of: numericInputText) { next in
                        if let payload = moduleNumericInputToPayloadHex(next, spec: spec) {
                            payloadHexText = payload
                        }
                    }
            }

            ForEach([parameterError, payloadError, numericInputError].compactMap { $0 }, id: \.self) { error in
                ConsoleFeedbackBanner(text: error, background: Color.red.opacity(0.12), foreground: .red)
            }

            let quickValues = preset.quickValueOptions
            if !quickValues.isEmpty {
                ForEach(Array(quickValues.chunked(into: 2).enumerated()), id: \.offset) { _, chunk in
                    ConsoleActionRows(
                        labels: chunk.map { DemoStrings.format("fill_value", $0.label) },
                        isDangerous: { _ in false },
                        action: { index in payloadHexText = chunk[index].payloadHex },
                        disabled: false
                    )
                }
            } else {
                ConsoleActionRows(
                    labels: [
                        DemoStrings.tr("fill_01"),
                        DemoStrings.tr("fill_00"),
                    ],
                    isDangerous: { _ in false },
                    action: { index in payloadHexText = index == 0 ? preset.writeOnHex : preset.writeOffHex },
                    disabled: false
                )
            }

            Toggle(isOn: $persistWrite) {
                Text(persistWrite ? DemoStrings.tr("persist_write_on") : DemoStrings.tr("persist_write_off"))
            }
            .font(.footnote)

            ConsoleActionRows(
                labels: [
                    DemoStrings.tr("read_parameter"),
                    DemoStrings.tr("write_parameter"),
                ],
                isDangerous: { index in index == 1 },
                action: { index in
                    if index == 0 {
                        viewModel.readModuleParameter(
                            family: preset.definition.moduleFamily,
                            parameterIDText: parameterIDText,
                            label: preset.title
                        )
                    } else {
                        viewModel.writeModuleParameter(
                            family: preset.definition.moduleFamily,
                            parameterIDText: parameterIDText,
                            payloadHex: payloadHexText,
                            persist: persistWrite,
                            label: preset.title
                        )
                    }
                },
                disabled: viewModel.isExecuting || !viewModel.canExecuteModuleCommands || parameterError != nil || payloadError != nil || numericInputError != nil
            )

            if let payload = try? parseModulePayloadHex(payloadHexText) {
                ConsoleHintBanner(text: formatModuleValuePreview(kind: preset.definition.kind, valueBytes: payload))
            }
        }
    }
}
