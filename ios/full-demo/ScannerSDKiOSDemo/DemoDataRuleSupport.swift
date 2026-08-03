import Foundation
import ScannerSDK

enum DataRuleFormMode: String, CaseIterable, Identifiable {
    case prefix
    case suffix
    case hideStart
    case hideMiddle
    case hideEnd
    case replace

    var id: String { rawValue }

    var kind: DataRuleKind {
        switch self {
        case .prefix: return .prefix
        case .suffix: return .suffix
        case .hideStart: return .hideStart
        case .hideMiddle: return .hideMiddle
        case .hideEnd: return .hideEnd
        case .replace: return .replace
        }
    }

    var title: String {
        if let label = try? ScannerSDK.shared.getDataRuleKindLabel(kind) {
            return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
        }
        switch self {
        case .prefix:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.prefix", fallback: "Prefix")
        case .suffix:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.suffix", fallback: "Suffix")
        case .hideStart:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.hide_start", fallback: "Hide Start")
        case .hideMiddle:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.hide_middle", fallback: "Hide Middle")
        case .hideEnd:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.hide_end", fallback: "Hide End")
        case .replace:
            return DemoStrings.sdk("nsdk.data_rule_command_kind.replace", fallback: "Replace")
        }
    }
}

extension DemoCommandCatalog {
    static func buildDataRule(
        mode: DataRuleFormMode,
        valueA: String,
        valueB: String
    ) throws -> DataRule {
        switch mode {
        case .prefix:
            return try .prefix(parseAffixBytes(valueA, name: "prefix"))
        case .suffix:
            return try .suffix(parseAffixBytes(valueA, name: "suffix"))
        case .hideStart:
            return try .hideStart(Int(parseSingleByteValue(valueA)))
        case .hideMiddle:
            return try .hideMiddle(
                start: Int(parseSingleByteValue(valueA)),
                length: Int(parseSingleByteValue(valueB))
            )
        case .hideEnd:
            return try .hideEnd(Int(parseSingleByteValue(valueA)))
        case .replace:
            let source = try parseASCIIEscapes(valueA)
            let target = try parseASCIIEscapes(valueB)
            guard !source.isEmpty, source.count <= 6, target.count <= 5, source.count + target.count <= 6 else {
                throw NSError(domain: "DemoCommandCatalog", code: 4, userInfo: [
                    NSLocalizedDescriptionKey: DemoStrings.tr("data_rule_error_replace_length")
                ])
            }
            return try .replace(source: source, target: target)
        }
    }

    private static func parseAffixBytes(_ text: String, name: String) throws -> Data {
        let bytes = try parseASCIIEscapes(text)
        guard !bytes.isEmpty, bytes.count <= 10 else {
            throw NSError(domain: "DemoCommandCatalog", code: 5, userInfo: [
                NSLocalizedDescriptionKey: "\(name) supports 1-10 bytes"
            ])
        }
        return bytes
    }

    private static func parseSingleByteValue(_ text: String) throws -> UInt8 {
        guard let value = Int(text.trimmingCharacters(in: .whitespacesAndNewlines)),
              value >= 1, value <= 255 else {
            throw NSError(domain: "DemoCommandCatalog", code: 1, userInfo: [
                NSLocalizedDescriptionKey: DemoStrings.tr("data_rule_error_count_range")
            ])
        }
        return UInt8(value)
    }

    private static func parseASCIIEscapes(_ text: String) throws -> Data {
        var bytes: [UInt8] = []
        let chars = Array(text)
        var index = 0
        while index < chars.count {
            let ch = chars[index]
            if ch == "\\", index + 3 < chars.count, chars[index + 1] == "x" {
                let hex = String(chars[(index + 2)...(index + 3)])
                guard let value = UInt8(hex, radix: 16) else {
                    throw NSError(domain: "DemoCommandCatalog", code: 2, userInfo: [
                        NSLocalizedDescriptionKey: DemoStrings.format("data_rule_error_invalid_hex_escape", hex)
                    ])
                }
                bytes.append(value)
                index += 4
                continue
            }
            guard ch.asciiValue != nil else {
                throw NSError(domain: "DemoCommandCatalog", code: 3, userInfo: [
                    NSLocalizedDescriptionKey: DemoStrings.tr("data_rule_error_ascii_only")
                ])
            }
            bytes.append(ch.asciiValue!)
            index += 1
        }
        return Data(bytes)
    }
}
