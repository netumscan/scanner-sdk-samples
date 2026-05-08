import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

final class DemoRecoveryActionTests: XCTestCase {
    func testDiscoveryFailureMapsBluetoothAvailabilityToSettingsAction() {
        let failure = DiscoveryFailure(
            transportType: .bleGatt,
            code: .bleAdapterDisabled,
            message: "Bluetooth is off",
            bleScanIssue: nil,
            platformErrorCode: nil,
            recoverable: false
        )

        XCTAssertEqual(demoRecoveryAction(for: failure), .enableBluetooth)
    }

    func testRecoverableDiscoveryFailureDoesNotExposeSettingsAction() {
        let failure = DiscoveryFailure(
            transportType: .bleGatt,
            code: .bleFilteredScanFailed,
            message: "fallback",
            bleScanIssue: .throttled,
            platformErrorCode: nil,
            recoverable: true
        )

        XCTAssertNil(demoRecoveryAction(for: failure))
    }

    func testSessionPlatformGattFailureMapsToSettingsAction() {
        let failure = SessionFailure(
            sessionHandle: 1,
            deviceId: "device-1",
            transportType: .bleGatt,
            code: .gattFailure,
            message: "GATT failed",
            bleTransportIssue: .gattFailure,
            platformErrorCode: nil
        )

        XCTAssertEqual(demoRecoveryAction(for: failure), .recoverBleConnection)
    }
}
