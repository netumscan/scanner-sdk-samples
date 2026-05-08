import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class DemoLocalizationTests: XCTestCase {
    func testScannerErrorDetailPreservesOperationAndCode() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .en
        let detail = DemoErrorFormatter.detail(
            ScannerError(code: 1, operation: "executeModuleRawFrame")
        )

        XCTAssertTrue(detail.contains("Invalid argument"))
        XCTAssertTrue(detail.contains("executeModuleRawFrame"))
        XCTAssertTrue(detail.contains("code=1"))
    }

    func testNativeErrorReasonUsesLocalizedStrings() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh

        XCTAssertEqual(DemoErrorFormatter.nativeErrorReason(5), "设备响应超时")
        XCTAssertEqual(DemoErrorFormatter.nativeErrorReason(-1), "SDK 内部错误")
    }

    func testErrorFormatterKeepsLocalizedNSErrorDescription() {
        let error = NSError(
            domain: "DemoModuleSettings",
            code: 4,
            userInfo: [NSLocalizedDescriptionKey: "invalid payload hex: GG"]
        )

        XCTAssertEqual(DemoErrorFormatter.detail(error), "invalid payload hex: GG")
    }

    func testResourceLocalizationFollowsSelectedDemoLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .en
        XCTAssertEqual(DemoStrings.tr("language", fallback: ""), "Language")

        localization.language = .zh
        XCTAssertEqual(DemoStrings.tr("language", fallback: ""), "语言")
        XCTAssertEqual(DemoStrings.sdk("nsdk.data_rule_command_kind.prefix", fallback: ""), "前缀")
    }

    func testSdkResourcesUseDedicatedTable() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .en

        XCTAssertEqual(DemoStrings.sdk("nsdk.data_rule_command_kind.prefix", fallback: ""), "Prefix")
        let sdkResourceKey = "nsdk_" + "data_rule_command_kind_prefix"
        XCTAssertEqual(DemoStrings.tr(sdkResourceKey, fallback: "fallback"), "fallback")
    }

    func testSystemLanguageOptionLabelFollowsSelectedDisplayLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .en
        XCTAssertEqual(DemoLanguage.system.label, "Follow System")

        localization.language = .zh
        XCTAssertEqual(DemoLanguage.system.label, "跟随系统")
    }

    func testDataRuleModeTitleUsesSdkRuntimeLabels() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        XCTAssertEqual(DataRuleFormMode.prefix.title, "前缀")
        XCTAssertEqual(DataRuleFormMode.replace.title, "替换")

        localization.language = .en
        XCTAssertEqual(DataRuleFormMode.prefix.title, "Prefix")
        XCTAssertEqual(DataRuleFormMode.replace.title, "Replace")
    }

    func testTextCommandTitleFollowsLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        XCTAssertEqual(
            DemoCommandCatalog.textCommandTitle(commandText: "$SCAN#4", fallback: ""),
            "清空前缀"
        )

        localization.language = .en
        XCTAssertEqual(
            DemoCommandCatalog.textCommandTitle(commandText: "$SCAN#4", fallback: ""),
            "Clear Prefix"
        )
    }

    func testCurrentFeedbackRefreshesAfterLanguageSwitch() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        localization.language = .zh
        viewModel.applySelectedModel(.nt91)
        XCTAssertEqual(viewModel.lastActionResult, "测试目标型号已切换；协议模式已按型号自动匹配")

        localization.language = .en
        viewModel.refreshLocalizedUi()

        XCTAssertEqual(viewModel.lastActionResult, "Test target model updated; protocol mode was matched by model")
    }

    func testDisconnectedFeedbackRefreshesAfterLanguageSwitch() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        localization.language = .zh
        viewModel.handleSessionClosed(
            status: .session(.disconnected),
            lastActionProvider: {
                DemoStrings.tr("device_disconnected")
            }
        )
        XCTAssertEqual(viewModel.lastActionResult, "设备已断开")

        localization.language = .en
        viewModel.refreshLocalizedUi()

        XCTAssertEqual(viewModel.lastActionResult, "Device disconnected")
    }

    func testCommandErrorFeedbackRefreshesAfterLanguageSwitch() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        localization.language = .zh
        _ = viewModel.requireActiveSession(title: "Cmd")
        XCTAssertEqual(viewModel.errorText, "Cmd: 当前没有活动会话，请先连接设备并等待会话就绪。")

        localization.language = .en
        viewModel.refreshLocalizedUi()

        XCTAssertEqual(viewModel.errorText, "Cmd: No active session. Connect a device and wait for the session to become ready first.")
    }

    func testDiscoveryFailureFeedbackRefreshesAfterLanguageSwitch() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let failure = DiscoveryFailure(
            transportType: .bleGatt,
            code: .bleAdapterDisabled,
            message: "Bluetooth is off",
            bleScanIssue: nil,
            platformErrorCode: nil,
            recoverable: false
        )
        let viewModel = AppViewModel()

        localization.language = .zh
        viewModel.applyDiscoveryFailure(failure)
        XCTAssertEqual(viewModel.errorText, "蓝牙未开启: Bluetooth is off")

        localization.language = .en
        viewModel.refreshLocalizedUi()

        XCTAssertEqual(viewModel.errorText, "Bluetooth disabled: Bluetooth is off")
    }

    func testSemanticStatusSummaryFollowsLanguageWithoutRefresh() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        viewModel.applyStatusSummary(.sdkInitialized)

        localization.language = .zh
        XCTAssertEqual(viewModel.statusSummary, "SDK 已初始化")

        localization.language = .en
        XCTAssertEqual(viewModel.statusSummary, "SDK initialized")
    }

    func testSemanticDeviceSummaryFollowsLanguageWithoutRefresh() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        viewModel.applyDeviceSummary(.device(name: "", deviceId: "abc"))

        localization.language = .zh
        XCTAssertEqual(viewModel.deviceSummary, "未知设备 / abc")

        localization.language = .en
        XCTAssertEqual(viewModel.deviceSummary, "Unknown / abc")
    }

    func testSemanticDeviceStateSummariesFollowLanguageWithoutRefresh() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let viewModel = AppViewModel()
        viewModel.applyInfoSummary(.notLoaded)
        viewModel.applySdkResolvedModelSummary(.notLoaded)
        viewModel.applyCapabilitySummary(.notLoaded)
        viewModel.applyModuleSummary(.notLoaded)
        viewModel.applyBatterySummary(.notLoaded)
        viewModel.applyDeviceCharsetSummary(.notReadCharsetQuery)
        viewModel.applyDeviceTerminalSummary(.notSetSessionCacheEmpty)

        localization.language = .zh
        XCTAssertEqual(viewModel.infoSummary, "设备信息未读取")
        XCTAssertEqual(viewModel.sdkResolvedModelSummary, "SDK 诊断型号未读取")
        XCTAssertEqual(viewModel.capabilitySummary, "能力摘要未读取")
        XCTAssertEqual(viewModel.moduleSummary, "模组能力未读取")
        XCTAssertEqual(viewModel.batterySummary, "电量未读取")
        XCTAssertEqual(viewModel.deviceCharsetSummary, "未读取（通过 %CHARSET# 查询）")
        XCTAssertEqual(viewModel.deviceTerminalSummary, "未设置（SDK 会话缓存为空）")

        localization.language = .en
        XCTAssertEqual(viewModel.infoSummary, "Device info not loaded")
        XCTAssertEqual(viewModel.sdkResolvedModelSummary, "SDK resolved model not loaded")
        XCTAssertEqual(viewModel.capabilitySummary, "Capability summary not loaded")
        XCTAssertEqual(viewModel.moduleSummary, "Module capability not loaded")
        XCTAssertEqual(viewModel.batterySummary, "Battery not loaded")
        XCTAssertEqual(viewModel.deviceCharsetSummary, "Not read (query via %CHARSET#)")
        XCTAssertEqual(viewModel.deviceTerminalSummary, "Not set (SDK session cache is empty)")
    }

    func testDeviceStateInfoSummarySourceFollowsLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let summary = DeviceStateSummary(
            infoSummarySource: .info(
                firmwareVersion: "FW1.0",
                hardwareVersion: "HW2.0",
                versionSeriesCode: "customer"
            ),
            deviceCharsetSummarySource: .notRead,
            deviceTerminalSummarySource: .notRead
        )

        localization.language = .zh
        XCTAssertEqual(summary.infoSummary, "固件=FW1.0  硬件=HW2.0  系列码=customer")

        localization.language = .en
        XCTAssertEqual(summary.infoSummary, "Firmware=FW1.0  Hardware=HW2.0  Series Code=customer")
    }

    func testDeviceStateFormatterPlaceholderSourcesFollowLanguage() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        let summary = DeviceStateSummary(
            infoSummarySource: .value(""),
            deviceCharsetSummarySource: .notRead,
            deviceTerminalSummarySource: .notRead
        )

        localization.language = .zh
        XCTAssertEqual(summary.deviceCharsetSummary, "未读取")
        XCTAssertEqual(summary.deviceTerminalSummary, "未读取")

        localization.language = .en
        XCTAssertEqual(summary.deviceCharsetSummary, "Not read")
        XCTAssertEqual(summary.deviceTerminalSummary, "Not read")
    }

    func testPendingDangerCommandRefreshesLabelsAfterLanguageSwitch() throws {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        let entry = try XCTUnwrap(
            DemoCommandCatalog.groups
                .flatMap(\.commands)
                .first { $0.isDangerous && $0.command == .master(.powerOff) }
        )
        let pendingAction = DemoPendingDangerAction.command(entry)
        XCTAssertEqual(pendingAction.title, "关机")
        XCTAssertEqual(pendingAction.riskText, "设备可能立即休眠或断开连接。")

        localization.language = .en

        XCTAssertEqual(pendingAction.title, "Power Off")
        XCTAssertEqual(pendingAction.riskText, "The device may sleep immediately or disconnect.")
    }

    func testPendingDangerQuickActionRefreshesLabelsAfterLanguageSwitch() {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        let pendingAction = DemoPendingDangerAction.quickAction(.ackBeepOff)
        XCTAssertEqual(pendingAction.title, "关闭确认蜂鸣")
        XCTAssertEqual(pendingAction.riskText, "这会关闭设备确认蜂鸣，并立即改变反馈行为。")

        localization.language = .en

        XCTAssertEqual(pendingAction.title, "Ack Beep Off")
        XCTAssertEqual(pendingAction.riskText, "This disables the device acknowledgment beep and changes feedback behavior immediately.")
    }

    func testPendingDangerModuleActionRefreshesLabelsAfterLanguageSwitch() throws {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        let action = try XCTUnwrap(
            DemoCommandCatalog.moduleActionRows
                .flatMap(\.actions)
                .first { $0.id == "ntc06h_save" }
        )
        let pendingAction = DemoPendingDangerAction.moduleAction(action)
        XCTAssertEqual(pendingAction.title, "保存设置")
        XCTAssertEqual(pendingAction.riskText, "这会把当前 NTC06H 设置持久保存到模组，后续重启仍会生效。")

        localization.language = .en

        XCTAssertEqual(pendingAction.title, "Save Settings")
        XCTAssertEqual(pendingAction.riskText, "This persists current NTC06H settings to the module and keeps them after reboot.")
    }

    func testDuplicateSdkFallbackUsesStableSdkResourceKey() throws {
        let localization = DemoLocalization.shared
        let originalLanguage = localization.language
        defer { localization.language = originalLanguage }

        localization.language = .zh
        let action = try XCTUnwrap(
            DemoCommandCatalog.moduleActionRows
                .flatMap(\.actions)
                .first { $0.id == "nt280h_scan_key" }
        )

        XCTAssertEqual(action.title, "按键扫描")
    }

    func testDynamicModuleEnumOptionUsesStableFallbackKey() {
        let definition = DemoModuleParameterDefinition(
            moduleFamily: .se4750,
            parameterID: 0x1234,
            key: "1234",
            semanticName: "VendorEnum",
            displayName: "Vendor Enum",
            symbol: "param_1234",
            group: "vendor",
            domainKey: "vendor",
            familyKey: "vendor",
            sectionKey: "vendor",
            defaultValue: "",
            notes: "",
            optionsJSON: "{\"0x2A\":\"Vendor Weird\"}",
            kind: .enumeration,
            isPlaceholder: false
        )

        XCTAssertEqual(
            demoModuleEnumOptionLocalizationKey(for: definition, payloadHex: "2A"),
            "nsdk.module_parameter.enum_option.se4750.1234.2a"
        )

        let lowParameterDefinition = DemoModuleParameterDefinition(
            moduleFamily: .se4750,
            parameterID: 0x10,
            key: "10",
            semanticName: "LowVendorEnum",
            displayName: "Low Vendor Enum",
            symbol: "param_10",
            group: "vendor",
            domainKey: "vendor",
            familyKey: "vendor",
            sectionKey: "vendor",
            defaultValue: "",
            notes: "",
            optionsJSON: "{\"0x2A\":\"Vendor Weird\"}",
            kind: .enumeration,
            isPlaceholder: false
        )

        XCTAssertEqual(
            demoModuleEnumOptionLocalizationKey(for: lowParameterDefinition, payloadHex: "2A"),
            "nsdk.module_parameter.enum_option.se4750.0010.2a"
        )
    }
}
