import ScannerSDK

enum DemoCommandCatalog {
    static let charsetOptions: [ScanTextCharset] = [.utf8, .gbk, .usASCII, .iso88591]
    static let terminatorPresets: [ScanTerminatorPreset] = ScanTerminatorPreset.defaults
}
