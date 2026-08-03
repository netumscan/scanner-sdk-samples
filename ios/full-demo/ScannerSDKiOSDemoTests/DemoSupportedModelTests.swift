import XCTest
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoSupportedModelTests: XCTestCase {
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
