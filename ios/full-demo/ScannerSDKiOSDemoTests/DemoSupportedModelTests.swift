import XCTest
import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoSupportedModelTests: XCTestCase {
    func testSharedAdvertisementSamplesHaveIdenticalCandidateOrder() throws {
        let fixture = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "ble-discovery", withExtension: "tsv"))
        let text = try String(contentsOf: fixture, encoding: .utf8)
        let rows = text.split(separator: "\n").filter { !$0.hasPrefix("#") }.map { $0.split(separator: "\t").map(String.init) }
        let devices = rows.map { row in
            DiscoveredDevice(deviceId: row[0], name: row[1] == "-" ? "" : row[1], transportType: .bleGatt,
                rssi: Int(row[2]), advertisementName: row[3] == "-" ? "" : row[3],
                connectable: row[9] == "-" ? nil : row[9] == "1")
        }
        let sorted = sortedDiscoveryDevices(devices)
        XCTAssertEqual(sorted.map(\.deviceId), rows.sorted { Int($0[10])! < Int($1[10])! }.map { $0[0] })
        XCTAssertEqual(sorted.count, 18)
        XCTAssertEqual(sorted.first { $0.deviceId == "anonymous" }?.name, "")
        XCTAssertEqual(sorted.first { $0.deviceId == "not-connectable" }?.connectable, false)
    }

    func testNormalizePreservesSdkOrderAndRemovesInvalidDuplicates() {
        let models = [
            DemoSupportedModel(
                modelKey: "CS7501",
                modelName: "CS7501",
                seriesKey: "c_pro",
                seriesName: "C Pro series"
            ),
            DemoSupportedModel(
                modelKey: "",
                modelName: "Invalid",
                seriesKey: "",
                seriesName: ""
            ),
            DemoSupportedModel(
                modelKey: "NT-91",
                modelName: "NT-91",
                seriesKey: "fixed",
                seriesName: "Fixed scanner module"
            ),
            DemoSupportedModel(
                modelKey: "cs7501",
                modelName: "Duplicate",
                seriesKey: "c_pro",
                seriesName: "C Pro series"
            ),
        ]

        XCTAssertEqual(
            normalizeSupportedModels(models).map(\.modelKey),
            ["CS7501", "NT-91"]
        )
    }

    func testSelectionKeepsCurrentModelOrFallsBackToFirstSdkModel() {
        let models = [
            DemoSupportedModel(
                modelKey: "CS7501",
                modelName: "CS7501",
                seriesKey: "c_pro",
                seriesName: "C Pro series"
            ),
            DemoSupportedModel(
                modelKey: "NT-91",
                modelName: "NT-91",
                seriesKey: "fixed",
                seriesName: "Fixed scanner module"
            ),
        ]

        XCTAssertEqual(selectSupportedModel(models, currentModelKey: "nt-91"), "NT-91")
        XCTAssertEqual(selectSupportedModel(models, currentModelKey: "missing"), "CS7501")
        XCTAssertEqual(selectSupportedModel([], currentModelKey: "CS7501"), "")
    }

    func testDisplayLabelsUseSdkModelAndSeriesNames() {
        let model = DemoSupportedModel(
            modelKey: "NT212X",
            modelName: "NT212X module",
            seriesKey: "fixed_scanner_module",
            seriesName: "Fixed scanner module"
        )

        XCTAssertEqual(model.primaryLabel, "NT212X · NT212X module")
        XCTAssertEqual(model.secondaryLabel, "Fixed scanner module")
    }

    func testViewModelExposesFailureAndAllowsRetry() async {
        let probe = SupportedModelsLoaderProbe()
        let viewModel = AppViewModel(supportedModelsLoader: {
            probe.attempts += 1
            throw NSError(domain: "test", code: 1)
        })

        viewModel.reloadSupportedModels()
        await waitForSupportedModelsLoad(viewModel)

        XCTAssertNotNil(viewModel.supportedModelsError)
        XCTAssertTrue(viewModel.supportedModels.isEmpty)
        XCTAssertFalse(viewModel.isLoadingSupportedModels)

        viewModel.reloadSupportedModels()
        await waitForSupportedModelsLoad(viewModel)

        XCTAssertEqual(probe.attempts, 2)
        XCTAssertNotNil(viewModel.supportedModelsError)
    }

    private func waitForSupportedModelsLoad(_ viewModel: AppViewModel) async {
        for _ in 0..<20 where viewModel.isLoadingSupportedModels {
            await Task.yield()
        }
    }
}

private final class SupportedModelsLoaderProbe: @unchecked Sendable {
    var attempts = 0
}
