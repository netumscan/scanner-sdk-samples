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
    ) throws -> (kind: DataRuleKind, primary: Data, secondary: Data) {
        switch mode {
        case .prefix:
            return (.prefix, try parseASCIIEscapes(valueA), Data())
        case .suffix:
            return (.suffix, try parseASCIIEscapes(valueA), Data())
        case .hideStart:
            return (.hideStart, try parseSingleByteCount(valueA), Data())
        case .hideMiddle:
            return (.hideMiddle, try parseSingleByteCount(valueA), try parseSingleByteCount(valueB))
        case .hideEnd:
            return (.hideEnd, try parseSingleByteCount(valueA), Data())
        case .replace:
            return (.replace, try parseASCIIEscapes(valueA), try parseASCIIEscapes(valueB))
        }
    }

    private static func parseSingleByteCount(_ text: String) throws -> Data {
        guard let value = Int(text.trimmingCharacters(in: .whitespacesAndNewlines)),
              value >= 1, value <= 255 else {
            throw NSError(domain: "DemoCommandCatalog", code: 1, userInfo: [
                NSLocalizedDescriptionKey: DemoStrings.tr("data_rule_error_count_range")
            ])
        }
        return Data([UInt8(value)])
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
