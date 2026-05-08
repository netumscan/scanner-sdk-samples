import Combine
import Foundation

enum DemoLanguage: String, CaseIterable, Identifiable {
    case system
    case zh
    case en

    var id: String { rawValue }

    var resourceName: String? {
        switch self {
        case .system: return nil
        case .zh: return "zh-Hans"
        case .en: return "en"
        }
    }

    var label: String {
        switch self {
        case .system: return DemoStrings.tr("language_label_system")
        case .zh: return DemoStrings.tr("language_label_zh", fallback: "中文")
        case .en: return DemoStrings.tr("language_label_en", fallback: "English")
        }
    }
}

final class DemoLocalization: ObservableObject {
    static let shared = DemoLocalization()
    private static let userDefaultsKey = "scanner-sdk-demo-language"

    @Published var language: DemoLanguage {
        didSet {
            UserDefaults.standard.set(language.rawValue, forKey: Self.userDefaultsKey)
        }
    }

    private init() {
        if let launchLanguage = Self.launchLanguageOverride() {
            language = launchLanguage
        } else if let rawValue = UserDefaults.standard.string(forKey: Self.userDefaultsKey),
           let storedLanguage = DemoLanguage(rawValue: rawValue) {
            language = storedLanguage
        } else {
            language = .system
        }
    }

    private static func launchLanguageOverride() -> DemoLanguage? {
#if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        guard let flagIndex = arguments.firstIndex(of: "--scanner-sdk-demo-language") else {
            return nil
        }
        let valueIndex = arguments.index(after: flagIndex)
        guard arguments.indices.contains(valueIndex) else {
            return nil
        }
        return DemoLanguage(rawValue: arguments[valueIndex])
#else
        nil
#endif
    }
}

enum DemoStrings {
    private enum Table {
        static let ui = "Localizable"
        static let sdk = "Nsdk"
    }

    static func tr(_ key: String) -> String {
        tr(key, fallback: key)
    }

    static func tr(_ key: String, fallback: String) -> String {
        lookup(key, tableName: Table.ui, fallback: fallback)
    }

    static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: tr(key), arguments: arguments)
    }

    static func format(_ key: String, fallback: String, _ arguments: CVarArg...) -> String {
        String(format: tr(key, fallback: fallback), arguments: arguments)
    }

    static func sdk(_ localizationKey: String, fallback: String) -> String {
        let normalized = normalizeSdkKey(localizationKey)
        guard isSdkResourceKey(normalized) else {
            return fallback
        }
        return lookup(normalized, tableName: Table.sdk, fallback: fallback)
    }

    static func withLocalizedValue(_ key: String, fallback: String, value: String) -> String {
        "\(tr(key, fallback: fallback)): \(value)"
    }

    static var unknownDeviceName: String { tr("unknown_device_name") }
    static var unnamedDevice: String { tr("unnamed_device_name") }
    static var emptyValue: String { tr("empty_value") }

    private static func normalizeSdkKey(_ key: String) -> String {
        key
            .replacingOccurrences(of: ".", with: "_")
            .replacingOccurrences(of: "-", with: "_")
            .lowercased()
    }

    private static func isSdkResourceKey(_ key: String) -> Bool {
        key.hasPrefix("nsdk_")
            && key.range(of: #"^[a-z0-9_]+$"#, options: .regularExpression) != nil
    }

    private static func lookup(_ key: String, tableName: String, fallback: String) -> String {
        NSLocalizedString(key, tableName: tableName, bundle: selectedBundle, value: fallback, comment: "")
    }

    private static var selectedBundle: Bundle {
        guard let resourceName = DemoLocalization.shared.language.resourceName else {
            return .main
        }
        guard let path = Bundle.main.path(forResource: resourceName, ofType: "lproj"),
              let bundle = Bundle(path: path) else {
            return .main
        }
        return bundle
    }

}
