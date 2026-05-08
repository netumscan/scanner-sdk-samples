import Foundation
import ScannerSDK

extension String {
    func ifBlank(_ fallback: String) -> String {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? fallback : self
    }
}

enum DemoErrorFormatter {
    static func detail(_ error: Error) -> String {
        if let scannerError = error as? ScannerError {
            return scannerErrorDetail(scannerError)
        }
        let localizedDescription = (error as NSError).localizedDescription
        return localizedDescription.isEmpty ? String(describing: error) : localizedDescription
    }

    static func nativeErrorReason(_ code: Int32) -> String {
        switch code {
        case 0:
            return DemoStrings.tr("sdk_error_success")
        case 1:
            return DemoStrings.tr("sdk_error_invalid_argument")
        case 2:
            return DemoStrings.tr("sdk_error_not_initialized")
        case 3:
            return DemoStrings.tr("sdk_error_not_supported")
        case 4:
            return DemoStrings.tr("sdk_error_busy")
        case 5:
            return DemoStrings.tr("sdk_error_timeout")
        case 6:
            return DemoStrings.tr("sdk_error_transport_open_failed")
        case 7:
            return DemoStrings.tr("sdk_error_transport_write_failed")
        case 8:
            return DemoStrings.tr("sdk_error_discovery_failed")
        case 9:
            return DemoStrings.tr("sdk_error_connect_failed")
        case 10:
            return DemoStrings.tr("sdk_error_disconnect_failed")
        case 11:
            return DemoStrings.tr("sdk_error_protocol_error")
        case 12:
            return DemoStrings.tr("sdk_error_device_not_ready")
        default:
            return DemoStrings.tr("sdk_error_internal")
        }
    }

    private static func scannerErrorDetail(_ error: ScannerError) -> String {
        DemoStrings.format(
            "sdk_native_error_detail",
            nativeErrorReason(error.code),
            error.operation,
            Int(error.code)
        )
    }
}
