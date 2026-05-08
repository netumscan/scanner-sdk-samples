import XCTest

final class ScannerSDKiOSDemoUITests: XCTestCase {
    private var app: XCUIApplication!

    private enum ScreenshotLanguage: String, CaseIterable {
        case zh
        case en

        var appleLanguages: String {
            switch self {
            case .zh: return "(zh-Hans)"
            case .en: return "(en)"
            }
        }

        var appleLocale: String {
            switch self {
            case .zh: return "zh_CN"
            case .en: return "en_US"
            }
        }
    }

    private enum ScreenshotScenario: String, CaseIterable {
        case discovery
        case consoleMaster = "console_master"
        case consoleModuleParameters = "console_module_parameters"
        case dangerDialog = "danger_dialog"
        case errorRecovery = "error_recovery"
        case logsExport = "logs_export"
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
        launchApp()
    }

    func testDiscoveryScreenExposesPrimaryControls() {
        XCTAssertTrue(element("ios_demo.discovery.list").waitForExistence(timeout: 5))
        XCTAssertTrue(scrollToElement(element("ios_demo.discovery.init_sdk_button")))
        XCTAssertTrue(scrollToElement(element("ios_demo.discovery.start_discovery_button")))
        XCTAssertTrue(scrollToElement(element("ios_demo.discovery.stop_discovery_button")))
        XCTAssertTrue(scrollToElement(element("ios_demo.discovery.open_console_button")))
    }

    func testFakeDiscoveryLaunchArgumentShowsFakeDevice() {
        launchApp(extraArguments: ["--scanner-sdk-demo-fake"])

        let initButton = element("ios_demo.discovery.init_sdk_button")
        XCTAssertTrue(scrollToElement(initButton))
        initButton.tap()

        let startDiscoveryButton = element("ios_demo.discovery.start_discovery_button")
        XCTAssertTrue(scrollToElement(startDiscoveryButton))
        startDiscoveryButton.tap()

        XCTAssertTrue(scrollToElement(app.staticTexts["Fake BLE Scanner"]))
        XCTAssertTrue(scrollToElement(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "FAKE-BLE")).firstMatch))
    }

    func testCanOpenAppLogsFromDiscovery() {
        let logsButton = button("ios_demo.toolbar.app_logs_button")
        XCTAssertTrue(logsButton.waitForExistence(timeout: 5))

        logsButton.tap()

        XCTAssertTrue(element("ios_demo.logs.list").waitForExistence(timeout: 5))
        XCTAssertTrue(element("ios_demo.logs.export_filtered_button").exists)
        XCTAssertTrue(element("ios_demo.logs.export_all_button").exists)
    }

    func testConsoleExposesMasterSectionsAndDangerousActions() {
        launchApp(extraArguments: ["--scanner-sdk-demo-ui-test-console"])

        XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
        swipeUp(times: 1)
        XCTAssertTrue(element("ios_demo.console.operation_scope.master").exists)
        XCTAssertTrue(element("ios_demo.console.operation_scope.module").exists)
        XCTAssertTrue(element("ios_demo.console.quick_actions_section").exists)
        XCTAssertTrue(element("ios_demo.console.master_commands_section").exists)

        let ackBeepButton = element("ios_demo.console.quick_action.ackBeepOn")
        XCTAssertTrue(scrollToElement(ackBeepButton))
        XCTAssertFalse(ackBeepButton.isEnabled)
    }

    func testConsoleShowsDataRuleBuilderWhenDataRulesGroupIsSelected() {
        launchApp(extraArguments: [
            "--scanner-sdk-demo-ui-test-console",
            "--scanner-sdk-demo-ui-test-data-rules",
        ])

        XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
        swipeUp(times: 2)
        XCTAssertTrue(element("ios_demo.console.data_rule_builder_section").waitForExistence(timeout: 5))
    }

    func testConsoleShowsModuleScopeAndNtc06hCatalog() {
        launchApp(extraArguments: [
            "--scanner-sdk-demo-ui-test-console",
            "--scanner-sdk-demo-ui-test-module-scope",
        ])

        XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
        swipeUp(times: 1)
        XCTAssertTrue(element("ios_demo.console.module_commands_section").waitForExistence(timeout: 5))
        XCTAssertTrue(element("ios_demo.console.module_parameter_catalog").exists)
    }

    func testAppLogsExposeFiltersAndClearAction() {
        let logsButton = button("ios_demo.toolbar.app_logs_button")
        XCTAssertTrue(logsButton.waitForExistence(timeout: 5))

        logsButton.tap()

        XCTAssertTrue(element("ios_demo.logs.list").waitForExistence(timeout: 5))
        swipeUp(times: 1)
        XCTAssertTrue(element("ios_demo.logs.clear_button").exists)
    }

    func testLocalizationScreenshotSet() {
        for language in ScreenshotLanguage.allCases {
            for scenario in ScreenshotScenario.allCases {
                prepareScreenshotScenario(scenario, language: language)
                captureScreenshot(language: language, scenario: scenario)
            }
        }
    }

    private func prepareScreenshotScenario(_ scenario: ScreenshotScenario, language: ScreenshotLanguage) {
        switch scenario {
        case .discovery:
            launchApp(language: language)
            XCTAssertTrue(element("ios_demo.discovery.list").waitForExistence(timeout: 5))

        case .consoleMaster:
            launchApp(
                language: language,
                extraArguments: ["--scanner-sdk-demo-ui-test-console"]
            )
            XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
            swipeUp(times: 1)
            XCTAssertTrue(element("ios_demo.console.master_commands_section").waitForExistence(timeout: 5))

        case .consoleModuleParameters:
            launchApp(
                language: language,
                extraArguments: [
                    "--scanner-sdk-demo-ui-test-console",
                    "--scanner-sdk-demo-ui-test-module-scope",
                ]
            )
            XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
            swipeUp(times: 1)
            XCTAssertTrue(element("ios_demo.console.module_parameter_catalog").waitForExistence(timeout: 5))

        case .dangerDialog:
            launchApp(
                language: language,
                extraArguments: [
                    "--scanner-sdk-demo-ui-test-console",
                    "--scanner-sdk-demo-ui-test-danger-dialog",
                ]
            )
            XCTAssertTrue(element("ios_demo.console.list").waitForExistence(timeout: 5))
            XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))

        case .errorRecovery:
            launchApp(
                language: language,
                extraArguments: ["--scanner-sdk-demo-ui-test-discovery-error"]
            )
            XCTAssertTrue(element("ios_demo.discovery.error_banner").waitForExistence(timeout: 5))

        case .logsExport:
            launchApp(language: language)
            let logsButton = button("ios_demo.toolbar.app_logs_button")
            XCTAssertTrue(logsButton.waitForExistence(timeout: 5))
            logsButton.tap()
            XCTAssertTrue(element("ios_demo.logs.list").waitForExistence(timeout: 5))
        }
    }

    private func captureScreenshot(language: ScreenshotLanguage, scenario: ScreenshotScenario) {
        let name = "ios_\(language.rawValue)_\(scenario.rawValue)"
        let screenshot = app.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        let outputDirectory = screenshotOutputDirectory()
        let outputURL = outputDirectory.appendingPathComponent("\(name).png")
        do {
            try FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)
            try screenshot.pngRepresentation.write(to: outputURL)
        } catch {
            XCTFail("Failed to write screenshot \(name): \(error)")
        }
    }

    private func screenshotOutputDirectory() -> URL {
        if let directory = ProcessInfo.processInfo.environment["SCANNER_SDK_DEMO_SCREENSHOT_DIR"],
           !directory.isEmpty {
            return URL(fileURLWithPath: directory, isDirectory: true)
        }

        let testFile = URL(fileURLWithPath: #filePath)
        let repoRoot = testFile
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        return repoRoot
            .appendingPathComponent("artifacts", isDirectory: true)
            .appendingPathComponent("demo-localization-screenshots", isDirectory: true)
    }

    private func swipeUp(times: Int) {
        for _ in 0..<times {
            app.swipeUp()
        }
    }

    private func scrollToElement(_ target: XCUIElement, maxSwipes: Int = 4) -> Bool {
        if target.exists {
            return true
        }
        for _ in 0..<maxSwipes {
            app.swipeUp()
            if target.waitForExistence(timeout: 1) {
                return true
            }
        }
        return target.exists
    }

    private func launchApp(
        language: ScreenshotLanguage = .en,
        extraArguments: [String] = []
    ) {
        if app != nil {
            app.terminate()
        }
        app = XCUIApplication()
        app.launchArguments = [
            "-AppleLanguages",
            language.appleLanguages,
            "-AppleLocale",
            language.appleLocale,
            "--scanner-sdk-demo-language",
            language.rawValue,
        ] + extraArguments
        app.launch()
    }

    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    private func button(_ identifier: String) -> XCUIElement {
        app.buttons.matching(identifier: identifier).firstMatch
    }
}
