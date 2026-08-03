import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

final class DeviceStateSummaryFormatterTests: XCTestCase {
    private var originalLanguage: DemoLanguage!

    override func setUp() {
        super.setUp()
        originalLanguage = DemoLocalization.shared.language
        DemoLocalization.shared.language = .zh
    }

    override func tearDown() {
        DemoLocalization.shared.language = originalLanguage
        originalLanguage = nil
        super.tearDown()
    }

    func testFormatReturnsInfoSummaryAndHidesUnqueriedConfigFields() {
        let info = ScannerInfo(
            deviceId: "device-1",
            name: "scanner-name",
            serialNumber: "SN123",
            firmwareVersion: "FW1.0",
            hardwareVersion: "HW2.0",
            manufacturer: "NLS",
            versionFormatFamily: "family",
            versionBootCode: "boot",
            versionSeriesCode: "customer",
            versionTransportCode: "transport",
            versionTransportSuffix: "suffix",
            versionWirelessCode: "wireless",
            versionBluetoothCode: "btcode",
            versionChipsetCode: "chip",
            versionChipsetSuffix: "chipsfx",
            versionReleaseCode: "release",
            versionExtensionCode: "ext",
            bluetoothName: "scanner-bt",
            bluetoothFirmwareVersion: "bt-fw"
        )
        let summary = DeviceStateSummaryFormatter.format(info: info)

        XCTAssertEqual(summary.infoSummary, "固件=FW1.0  硬件=HW2.0  系列码=customer  蓝牙名称=scanner-bt  蓝牙固件版本=bt-fw")
        XCTAssertEqual(summary.deviceCharsetSummary, "未读取")
        XCTAssertEqual(summary.deviceTerminalSummary, "未读取")
    }

    func testFormatFallsBackToPlaceholderTextWhenInfoIsEmpty() {
        let info = ScannerInfo(
            deviceId: "device-1",
            name: "",
            serialNumber: "",
            firmwareVersion: "",
            hardwareVersion: "",
            manufacturer: "",
            versionFormatFamily: "",
            versionBootCode: "",
            versionSeriesCode: "",
            versionTransportCode: "",
            versionTransportSuffix: "",
            versionWirelessCode: "",
            versionBluetoothCode: "",
            versionChipsetCode: "",
            versionChipsetSuffix: "",
            versionReleaseCode: "",
            versionExtensionCode: "",
            bluetoothName: "",
            bluetoothFirmwareVersion: ""
        )
        let summary = DeviceStateSummaryFormatter.format(info: info)

        XCTAssertEqual(summary.infoSummary, "固件=-  硬件=-  系列码=-  蓝牙名称=-  蓝牙固件版本=-")
        XCTAssertEqual(summary.deviceCharsetSummary, "未读取")
        XCTAssertEqual(summary.deviceTerminalSummary, "未读取")
    }
}
