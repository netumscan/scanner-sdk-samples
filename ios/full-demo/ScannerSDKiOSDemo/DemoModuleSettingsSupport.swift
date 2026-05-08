import Foundation
import ScannerSDK

enum DemoModuleParameterKind: String, Hashable {
    case unknown
    case bool
    case enumeration
    case uint8
    case uint16
    case bytesASCII
    case action
    case complex
    case custom
    case object
}

struct DemoModuleEnumOption: Identifiable, Hashable {
    let payloadHex: String
    let rawLabel: String
    let localizationKey: String
    let fallbackDisplayName: String

    var id: String { "\(payloadHex):\(rawLabel):\(localizationKey):\(fallbackDisplayName)" }
    var label: String { DemoStrings.sdk(localizationKey, fallback: fallbackDisplayName) }
}

struct DemoModuleQuickValueOption: Identifiable, Hashable {
    let payloadHex: String
    let localizationKey: String
    let fallbackDisplayName: String

    var id: String { "\(payloadHex):\(localizationKey):\(fallbackDisplayName)" }
    var label: String { DemoStrings.sdk(localizationKey, fallback: fallbackDisplayName) }
}

enum DemoModuleNumericInputKind: Hashable {
    case tenthsSeconds
    case uint8Decimal
    case uint16Milliseconds
}

struct DemoModuleNumericInputSpec: Hashable {
    let kind: DemoModuleNumericInputKind
    let localizationKey: String
    let fallbackDisplayName: String
    let minValue: Int
    let maxValue: Int
    let stepHintKey: String
    let stepHintFallback: String

    var label: String { DemoStrings.sdk(localizationKey, fallback: fallbackDisplayName) }
    var rangeHint: String {
        DemoStrings.format("module_range_hint", minValue, maxValue, DemoStrings.sdk(stepHintKey, fallback: stepHintFallback))
    }
}

struct DemoModuleParameterDefinition: Hashable, ModuleTaxonomyMember {
    let moduleFamily: ModuleFamily
    let parameterID: UInt32
    let key: String
    let semanticName: String
    let displayName: String
    let symbol: String
    let group: String
    let domainKey: String
    let familyKey: String
    let sectionKey: String
    let defaultValue: String
    let notes: String
    let optionsJSON: String
    let kind: DemoModuleParameterKind
    let isPlaceholder: Bool
}

struct DemoModuleSettingPreset: Identifiable, Hashable, ModuleTaxonomyMember {
    let definition: DemoModuleParameterDefinition

    var id: String { "\(definition.moduleFamily.rawValue):\(definition.parameterID)" }
    var parameterID: UInt32 { definition.parameterID }
    var group: String { definition.group }
    var domainKey: String { definition.domainKey }
    var familyKey: String { definition.familyKey }
    var sectionKey: String { definition.sectionKey }
    var title: String { demoModuleTitle(definition) }
    var formattedParameterID: String { formatModuleParameterID(parameterID) }
    var writeOnHex: String { booleanPayloadPair?.on ?? "01" }
    var writeOffHex: String { booleanPayloadPair?.off ?? "00" }
    var supportsBooleanToggle: Bool { definition.kind == .bool }
    var enumOptions: [DemoModuleEnumOption] {
        definition.kind == .enumeration ? demoEnumOptions(for: definition) : []
    }
    var quickValueOptions: [DemoModuleQuickValueOption] {
        demoQuickValueOptions(for: definition)
    }
    var numericInputSpec: DemoModuleNumericInputSpec? {
        demoNumericInputSpec(for: definition)
    }
    var defaultPayloadHex: String? {
        defaultPayload(for: definition).map(hexEditorText)
    }
    var hint: String {
        let defaultValue = definition.defaultValue.ifBlank(DemoStrings.tr("module_default_not_provided"))
        let quickHint = quickValueOptions.map(\.label).joined(separator: " / ")
        let base = DemoStrings.format("module_parameter_hint_format", moduleDomainTitle(domainKey), moduleFamilyTitle(familyKey), moduleSectionTitle(sectionKey), moduleKindLabel(definition.kind), defaultValue)
        return quickHint.isEmpty ? base : DemoStrings.format("module_parameter_hint_quick_values_format", base, quickHint)
    }

    private var booleanPayloadPair: (off: String, on: String)? {
        guard definition.kind == .bool else { return nil }
        guard let pair = try? ScannerSDK.shared.getModuleParameterBooleanPayloadPair(
            family: definition.moduleFamily,
            parameterID: definition.parameterID
        ) else {
            return nil
        }
        return (pair.offPayloadHex, pair.onPayloadHex)
    }
}

extension ModuleTestRecommendation {
    var demoTitle: String { DemoStrings.sdk(titleKey, fallback: titleFallback) }
    var demoDetail: String { DemoStrings.sdk(detailKey, fallback: detailFallback) }
}

func ntc06hSettingTitle(_ setting: Ntc06hSettingDefinition) -> String {
    let label = setting.localizedTitleLabel
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}

struct DemoNtc06hTemplateInputSpec: Hashable {
    let prefix: String
    let suffix: String
    let placeholder: String
    let inputType: Ntc06hTemplateInputType
    let width: Int
    let minValue: Int
    let maxValue: Int
}

enum DemoModuleSettingsCatalog {
    static func presets(for moduleFamily: ModuleFamily) -> [DemoModuleSettingPreset] {
        switch moduleFamily {
        case .nt212x:
            return Nt212xParameterCatalog.all
                .map(DemoModuleParameterDefinition.init)
                .sorted(by: compareDefinitions)
                .map(DemoModuleSettingPreset.init)
        case .nt280h:
            return Nt280hParameterCatalog.all
                .map(DemoModuleParameterDefinition.init)
                .filter { !$0.isPlaceholder }
                .sorted(by: compareDefinitions)
                .map(DemoModuleSettingPreset.init)
        case .se4750:
            return Se4750ParameterCatalog.all
                .map(DemoModuleParameterDefinition.init)
                .sorted(by: compareDefinitions)
                .map(DemoModuleSettingPreset.init)
        default:
            return []
        }
    }

    static func domainGroups(for moduleFamily: ModuleFamily) -> [ModuleTaxonomyDomainGroup<DemoModuleSettingPreset>] {
        ModuleTaxonomyGrouping.domainGroups(presets(for: moduleFamily)) { lhs, rhs in
            if lhs.parameterID != rhs.parameterID {
                return lhs.parameterID < rhs.parameterID
            }
            return lhs.definition.displayName < rhs.definition.displayName
        }
    }

    static func ntc06hSettings() -> [Ntc06hSettingDefinition] {
        (try? ScannerSDK.shared.getNtc06hSettingDefinitions()) ?? []
    }

    static func ntc06hDomainGroups() -> [ModuleTaxonomyDomainGroup<Ntc06hSettingDefinition>] {
        ModuleTaxonomyGrouping.domainGroups(ntc06hSettings()) { lhs, rhs in
            lhs.displayCode < rhs.displayCode
        }
    }

    static func recommendations(for family: ModuleFamily) -> [ModuleTestRecommendation] {
        (try? ScannerSDK.shared.getModuleTestRecommendations(family: family)) ?? []
    }

    private static func compareDefinitions(_ lhs: DemoModuleParameterDefinition, _ rhs: DemoModuleParameterDefinition) -> Bool {
        let lhsRank = ModuleTaxonomyCatalog.rank(of: .group, key: lhs.group) ?? Int32.max
        let rhsRank = ModuleTaxonomyCatalog.rank(of: .group, key: rhs.group) ?? Int32.max
        if lhsRank != rhsRank {
            return lhsRank < rhsRank
        }
        if lhs.group != rhs.group {
            return lhs.group < rhs.group
        }
        return lhs.parameterID < rhs.parameterID
    }
}

extension DemoModuleParameterDefinition {
    init(_ definition: Nt212xParameterDefinition) {
        self.init(
            moduleFamily: .nt212x,
            parameterID: UInt32(definition.parameterID),
            key: definition.key,
            semanticName: definition.aliasName ?? definition.symbol,
            displayName: definition.displayName,
            symbol: definition.symbol,
            group: definition.group,
            domainKey: definition.domainKey,
            familyKey: definition.familyKey,
            sectionKey: definition.sectionKey,
            defaultValue: definition.defaultValue,
            notes: definition.notes,
            optionsJSON: definition.optionsJSON,
            kind: DemoModuleParameterKind(definition.kind),
            isPlaceholder: false
        )
    }

    init(_ definition: Nt280hParameterDefinition) {
        self.init(
            moduleFamily: .nt280h,
            parameterID: UInt32(definition.parameterID),
            key: definition.key,
            semanticName: definition.semanticName,
            displayName: definition.displayName,
            symbol: definition.symbol,
            group: definition.group,
            domainKey: definition.domainKey,
            familyKey: definition.familyKey,
            sectionKey: definition.sectionKey,
            defaultValue: definition.defaultValue,
            notes: definition.notes,
            optionsJSON: definition.optionsJSON,
            kind: DemoModuleParameterKind(definition.kind),
            isPlaceholder: definition.isPlaceholder
        )
    }

    init(_ definition: Se4750ParameterDefinition) {
        self.init(
            moduleFamily: .se4750,
            parameterID: definition.parameterID,
            key: definition.key,
            semanticName: definition.semanticName,
            displayName: definition.displayName,
            symbol: definition.symbol,
            group: definition.group,
            domainKey: definition.domainKey,
            familyKey: definition.familyKey,
            sectionKey: definition.sectionKey,
            defaultValue: definition.defaultValue,
            notes: definition.notes,
            optionsJSON: definition.optionsJSON,
            kind: DemoModuleParameterKind(definition.kind),
            isPlaceholder: false
        )
    }
}

extension DemoModuleParameterKind {
    init(_ kind: Nt212xParameterKind) {
        switch kind {
        case .bool: self = .bool
        case .enum: self = .enumeration
        case .uint8: self = .uint8
        case .uint16: self = .uint16
        case .bytesAscii: self = .bytesASCII
        case .action: self = .action
        case .complex: self = .complex
        case .custom: self = .custom
        case .unknown: self = .unknown
        }
    }

    init(_ kind: Nt280hParameterKind) {
        switch kind {
        case .bool: self = .bool
        case .enum: self = .enumeration
        case .action: self = .action
        case .object: self = .object
        case .unknown: self = .unknown
        }
    }

    init(_ kind: Se4750ParameterKind) {
        switch kind {
        case .bool: self = .bool
        case .enum: self = .enumeration
        case .uint8: self = .uint8
        case .uint16: self = .uint16
        case .bytesAscii: self = .bytesASCII
        case .unknown: self = .unknown
        }
    }
}

func moduleDomainTitle(_ key: String) -> String {
    localizedModuleTaxonomyTitle(.domain, key: key, fallback: DemoStrings.tr("module_taxonomy_other"))
}

func moduleFamilyTitle(_ key: String) -> String {
    localizedModuleTaxonomyTitle(.family, key: key, fallback: DemoStrings.tr("module_taxonomy_other"))
}

func moduleSectionTitle(_ key: String) -> String {
    localizedModuleTaxonomyTitle(.section, key: key, fallback: DemoStrings.tr("module_taxonomy_general"))
}

private func localizedModuleTaxonomyTitle(
    _ kind: ModuleTaxonomyKind,
    key: String,
    fallback: String
) -> String {
    guard let entry = ModuleTaxonomyCatalog.entry(of: kind, key: key) else {
        return fallback
    }
    return DemoStrings.sdk(entry.localizationKey, fallback: entry.fallbackDisplayName)
}

func moduleKindLabel(_ kind: DemoModuleParameterKind) -> String {
    switch kind {
    case .bool: return sdkModuleKindLabel(.bool)
    case .enumeration: return sdkModuleKindLabel(.enumeration)
    case .uint8: return sdkModuleKindLabel(.uint8)
    case .uint16: return sdkModuleKindLabel(.uint16)
    case .bytesASCII: return sdkModuleKindLabel(.bytesAscii)
    case .action: return sdkModuleKindLabel(.action)
    case .complex: return sdkModuleKindLabel(.complex)
    case .custom: return sdkModuleKindLabel(.custom)
    case .object: return sdkModuleKindLabel(.object)
    case .unknown: return sdkModuleKindLabel(.unknown)
    }
}

private func sdkModuleKindLabel(_ kind: ModuleParameterKind) -> String {
    guard let label = try? ScannerSDK.shared.getModuleParameterKindLabel(kind) else {
        return kind == .unknown ? DemoStrings.tr("unknown") : "\(kind)"
    }
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}

func formatModuleParameterID(_ parameterID: UInt32) -> String {
    parameterID <= 0xFF
        ? String(format: "%02X", parameterID)
        : String(format: "%04X", parameterID)
}

func parseModuleParameterID(_ input: String) throws -> UInt32 {
    let normalized = input
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .replacingOccurrences(of: "0x", with: "", options: .caseInsensitive)
        .replacingOccurrences(of: "_", with: "")
    guard !normalized.isEmpty else {
        throw NSError(domain: "DemoModuleSettings", code: 1, userInfo: [NSLocalizedDescriptionKey: "parameter id is empty"])
    }
    guard let value = UInt32(normalized, radix: 16) else {
        throw NSError(domain: "DemoModuleSettings", code: 2, userInfo: [NSLocalizedDescriptionKey: "invalid parameter id hex: \(input)"])
    }
    return value
}

func parseModulePayloadHex(_ input: String) throws -> Data {
    let normalized = input
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .replacingOccurrences(of: "0x", with: "", options: .caseInsensitive)
        .replacingOccurrences(of: " ", with: "")
        .replacingOccurrences(of: "_", with: "")
    guard normalized.count % 2 == 0 else {
        throw NSError(domain: "DemoModuleSettings", code: 3, userInfo: [NSLocalizedDescriptionKey: "payload hex length must be even"])
    }
    guard !normalized.isEmpty else { return Data() }
    var bytes: [UInt8] = []
    var index = normalized.startIndex
    while index < normalized.endIndex {
        let next = normalized.index(index, offsetBy: 2)
        let chunk = String(normalized[index..<next])
        guard let byte = UInt8(chunk, radix: 16) else {
            throw NSError(domain: "DemoModuleSettings", code: 4, userInfo: [NSLocalizedDescriptionKey: "invalid payload hex: \(input)"])
        }
        bytes.append(byte)
        index = next
    }
    return Data(bytes)
}

func hexEditorText(_ data: Data) -> String {
    data.map { String(format: "%02X", $0) }.joined(separator: " ")
}

func moduleNumericInputText(fromPayload payloadHex: String, spec: DemoModuleNumericInputSpec) -> String? {
    guard let bytes = try? parseModulePayloadHex(payloadHex) else { return nil }
    let value: Int?
    switch spec.kind {
    case .tenthsSeconds, .uint8Decimal:
        value = bytes.count == 1 ? Int(bytes[bytes.startIndex]) : nil
    case .uint16Milliseconds:
        if bytes.count == 2 {
            value = (Int(bytes[bytes.startIndex]) << 8) | Int(bytes[bytes.index(after: bytes.startIndex)])
        } else {
            value = nil
        }
    }
    return value.map(String.init)
}

func moduleNumericInputToPayloadHex(_ text: String, spec: DemoModuleNumericInputSpec) -> String? {
    guard let value = Int(text), value >= spec.minValue, value <= spec.maxValue else {
        return nil
    }
    switch spec.kind {
    case .tenthsSeconds, .uint8Decimal:
        return String(format: "%02X", value)
    case .uint16Milliseconds:
        return String(format: "%04X", value)
    }
}

func moduleNumericInputError(_ text: String, spec: DemoModuleNumericInputSpec) -> String? {
    if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return nil
    }
    guard let value = Int(text) else {
        return DemoStrings.tr("enter_decimal_integer")
    }
    guard value >= spec.minValue, value <= spec.maxValue else {
        return DemoStrings.format("value_out_of_range", spec.minValue, spec.maxValue)
    }
    return nil
}

func formatModuleValuePreview(kind: DemoModuleParameterKind, valueBytes: Data) -> String {
    let hex = hexEditorText(valueBytes).ifBlank(DemoStrings.tr("module_value_empty"))
    switch kind {
    case .bool:
        let boolValue = valueBytes.first.map { $0 != 0 }
        let boolText = boolValue.map(String.init) ?? DemoStrings.tr("module_value_invalid")
        return DemoStrings.format("module_value_bool", hex, boolText)
    case .bytesASCII:
        let text = String(decoding: valueBytes, as: UTF8.self)
        return DemoStrings.format("module_value_ascii", hex, escapeControlText(text))
    default:
        return DemoStrings.format("module_value", hex)
    }
}

func ntc06hTemplateInputSpec(_ setting: Ntc06hSettingDefinition) -> DemoNtc06hTemplateInputSpec? {
    guard setting.isTemplate,
          let range = setting.displayCode.range(of: #"h+"#, options: [.regularExpression, .caseInsensitive]) else {
        return nil
    }
    let placeholder = String(setting.displayCode[range])
    return DemoNtc06hTemplateInputSpec(
        prefix: String(setting.displayCode[..<range.lowerBound]),
        suffix: String(setting.displayCode[range.upperBound...]),
        placeholder: placeholder,
        inputType: setting.templateInputType,
        width: setting.templateInputWidth > 0 ? setting.templateInputWidth : placeholder.count,
        minValue: setting.templateInputMin,
        maxValue: setting.templateInputMax
    )
}

func ntc06hTemplateValueOrEmpty(_ setting: Ntc06hSettingDefinition) -> String {
    guard let spec = ntc06hTemplateInputSpec(setting) else { return "" }
    let example = setting.templateExampleCode.ifBlank(setting.displayCode)
    guard example.hasPrefix(spec.prefix), example.hasSuffix(spec.suffix) else { return "" }
    let end = example.index(example.endIndex, offsetBy: -spec.suffix.count)
    guard end >= example.index(example.startIndex, offsetBy: spec.prefix.count) else { return "" }
    let start = example.index(example.startIndex, offsetBy: spec.prefix.count)
    return String(example[start..<end]).uppercased()
}

func ntc06hNormalizeTemplateValue(_ value: String, spec: DemoNtc06hTemplateInputSpec) -> String {
    switch spec.inputType {
    case .hexUInt8:
        return String(value.filter { $0.isNumber || ("a"..."f").contains(String($0).lowercased()) }
            .uppercased()
            .prefix(spec.width))
    default:
        return String(value.prefix(spec.width))
    }
}

func ntc06hTemplateValueIsValid(_ value: String, spec: DemoNtc06hTemplateInputSpec) -> Bool {
    let normalized = ntc06hNormalizeTemplateValue(value, spec: spec)
    guard normalized.count == spec.width else { return false }
    switch spec.inputType {
    case .hexUInt8:
        guard let parsed = Int(normalized, radix: 16) else { return false }
        return parsed >= spec.minValue && parsed <= spec.maxValue
    default:
        return !normalized.isEmpty
    }
}

func ntc06hBuildTemplateCode(_ spec: DemoNtc06hTemplateInputSpec, value: String) -> String {
    spec.prefix + ntc06hNormalizeTemplateValue(value, spec: spec) + spec.suffix
}

private func demoModuleTitle(_ definition: DemoModuleParameterDefinition) -> String {
    guard let label = try? ScannerSDK.shared.getModuleParameterTitleLabel(
        family: definition.moduleFamily,
        parameterID: definition.parameterID,
        aliasName: definition.semanticName,
        displayName: definition.displayName
    ) else {
        return definition.displayName
    }
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}

private func defaultPayload(for definition: DemoModuleParameterDefinition) -> Data? {
    let raw = definition.defaultValue.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !raw.isEmpty else { return nil }
    if raw.hasPrefix("0x") || raw.hasPrefix("0X") {
        return try? parseModulePayloadHex(raw)
    }
    switch definition.kind {
    case .uint8 where raw.allSatisfy(\.isNumber):
        return UInt8(raw).map { Data([$0]) }
    case .uint16 where raw.allSatisfy(\.isNumber):
        guard let value = UInt16(raw) else { return nil }
        return Data([UInt8((value >> 8) & 0xFF), UInt8(value & 0xFF)])
    case .bytesASCII:
        return Data(raw.utf8)
    default:
        return nil
    }
}

private func demoEnumOptions(for definition: DemoModuleParameterDefinition) -> [DemoModuleEnumOption] {
    let optionsJSON = definition.optionsJSON
    let trimmed = optionsJSON.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty, trimmed != "{}",
          let data = trimmed.data(using: .utf8),
          let object = try? JSONSerialization.jsonObject(with: data) as? [String: String] else {
        return []
    }
    return object.sorted { lhs, rhs in
        let lhsValue = Int(lhs.key.replacingOccurrences(of: "0x", with: "", options: .caseInsensitive), radix: 16) ?? Int.max
        let rhsValue = Int(rhs.key.replacingOccurrences(of: "0x", with: "", options: .caseInsensitive), radix: 16) ?? Int.max
        return lhsValue < rhsValue
    }.map { key, value in
        let normalized = key.replacingOccurrences(of: "0x", with: "", options: .caseInsensitive).uppercased()
        let payloadHex = normalized.count.isMultiple(of: 2) ? normalized : "0\(normalized)"
        let rawLabel = value.ifBlank(key).trimmingCharacters(in: .whitespacesAndNewlines)
        let sdkLabel = try? ScannerSDK.shared.getModuleParameterEnumLabel(
            family: definition.moduleFamily,
            parameterID: definition.parameterID,
            rawLabel: rawLabel
        )
        return DemoModuleEnumOption(
            payloadHex: payloadHex,
            rawLabel: rawLabel,
            localizationKey: sdkLabel?.localizationKey ?? demoModuleEnumOptionLocalizationKey(for: definition, payloadHex: payloadHex),
            fallbackDisplayName: sdkLabel?.fallbackDisplayName ?? rawLabel
        )
    }
}

func demoModuleEnumOptionLocalizationKey(for definition: DemoModuleParameterDefinition, payloadHex: String) -> String {
    let family: String
    switch definition.moduleFamily {
    case .ntc06h:
        family = "ntc06h"
    case .nt280h:
        family = "nt280h"
    case .nt212x:
        family = "nt212x"
    case .se4750:
        family = "se4750"
    case .unknown:
        family = "unknown"
    }
    let parameter = String(format: "%04x", definition.parameterID)
    let payload = payloadHex
        .lowercased()
        .replacingOccurrences(of: #"[^a-z0-9]+"#, with: "_", options: .regularExpression)
        .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
    return "nsdk.module_parameter.enum_option.\(family).\(parameter).\(payload.isEmpty ? "value" : payload)"
}

private func demoQuickValueOptions(for definition: DemoModuleParameterDefinition) -> [DemoModuleQuickValueOption] {
    guard let values = try? ScannerSDK.shared.getModuleParameterQuickValues(
        family: definition.moduleFamily,
        parameterID: definition.parameterID
    ) else {
        return []
    }
    return values.map {
        DemoModuleQuickValueOption(payloadHex: $0.payloadHex, localizationKey: $0.localizationKey, fallbackDisplayName: $0.fallbackDisplayName)
    }
}

private func demoNumericInputSpec(for definition: DemoModuleParameterDefinition) -> DemoModuleNumericInputSpec? {
    guard let spec = try? ScannerSDK.shared.getModuleParameterNumericInputSpec(
        family: definition.moduleFamily,
        parameterID: definition.parameterID
    ) else {
        return nil
    }
    return DemoModuleNumericInputSpec(sdkSpec: spec)
}

private extension DemoModuleNumericInputSpec {
    init?(sdkSpec: ModuleParameterNumericInputSpec) {
        let demoKind: DemoModuleNumericInputKind
        switch sdkSpec.kind {
        case .tenthsSeconds:
            demoKind = .tenthsSeconds
        case .uint8Decimal:
            demoKind = .uint8Decimal
        case .uint16Milliseconds:
            demoKind = .uint16Milliseconds
        case .unknown:
            return nil
        }
        self.init(
            kind: demoKind,
            localizationKey: sdkSpec.localizationKey,
            fallbackDisplayName: sdkSpec.fallbackDisplayName,
            minValue: Int(sdkSpec.minValue),
            maxValue: Int(sdkSpec.maxValue),
            stepHintKey: sdkSpec.stepHintKey,
            stepHintFallback: sdkSpec.stepHintFallback
        )
    }
}
