import XCTest
@testable import ScannerSDK
@testable import ScannerSDKiOSDemo

@MainActor
final class ModelSelectionSupportTests: XCTestCase {
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

    func testDefaultSupportedModelMatchesDiscoveryDefault() {
        let viewModel = AppViewModel()

        XCTAssertEqual(viewModel.selectedModelId, .cs7501)
        XCTAssertEqual(viewModel.selectedModelSummary, "客户选择型号: CS7501")
    }

    func testDisplayModelLabelMatchesDemoVocabulary() {
        XCTAssertEqual(displayModelLabel(.cs7501), "CS7501")
        XCTAssertEqual(displayModelLabel(.cs8501), "CS8501")
        XCTAssertEqual(displayModelLabel(.c740), "C740")
        XCTAssertEqual(displayModelLabel(.c750), "C750")
        XCTAssertEqual(displayModelLabel(.cs9501), "CS9501")
        XCTAssertEqual(displayModelLabel(.nt91), "NT-91")
        XCTAssertEqual(displayModelLabel(.nt1228bc), "NT-1228BC")
    }

    func testFormatSelectedModelSummaryUsesSelectedModelLabel() {
        XCTAssertEqual(formatSelectedModelSummary(.cs7501), "客户选择型号: CS7501")
    }

    func testDefaultProtocolChannelMatchesSelectedModel() {
        let viewModel = AppViewModel()

        XCTAssertEqual(viewModel.selectedChannelKind, .scannerMaster)

        viewModel.applySelectedModel(.nt91)
        XCTAssertEqual(viewModel.selectedChannelKind, .modulePassthrough)

        viewModel.applySelectedModel(.cs7501)
        XCTAssertEqual(viewModel.selectedChannelKind, .scannerMaster)
    }

    func testProtocolChannelKindIsResolvedFromCapability() {
        let moduleOnly = DeviceCapabilitySummary(
            modelId: .nt91,
            modelName: "NT-91",
            defaultCommandSet: .moduleOnly,
            formFactor: .singleModule,
            moduleFamily: .nt212x,
            supportsBasicDeviceCommands: true,
            supportsMasterCommands: false,
            supportsNativeModuleCommands: true,
            supportsModuleCommandBridge: false,
            supportsModuleCommands: true,
            supportsScannerMaster: false,
            supportsModulePassthrough: true,
            supportStatus: .verified
        )
        let masterWithModuleInfo = DeviceCapabilitySummary(
            modelId: .cs7501,
            modelName: "CS7501",
            defaultCommandSet: .masterWithModuleInfo,
            formFactor: .masterWithModule,
            moduleFamily: .nt212x,
            supportsBasicDeviceCommands: true,
            supportsMasterCommands: true,
            supportsNativeModuleCommands: false,
            supportsModuleCommandBridge: true,
            supportsModuleCommands: true,
            supportsScannerMaster: true,
            supportsModulePassthrough: false,
            supportStatus: .codeOnly
        )

        XCTAssertEqual(resolveProtocolChannelKind(for: nil), .scannerMaster)
        XCTAssertEqual(resolveProtocolChannelKind(for: moduleOnly), .modulePassthrough)
        XCTAssertEqual(resolveProtocolChannelKind(for: masterWithModuleInfo), .scannerMaster)
    }

    func testFormatSdkResolvedModelSummaryMarksMismatch() {
        XCTAssertEqual(
            formatSdkResolvedModelSummary(selectedModelId: .cs7501, resolvedModel: .c740),
            "SDK 诊断型号: C740（与客户选择 CS7501 不一致）"
        )
    }

    func testMergePreferredCapabilityWithRuntimeKeepsPreferredIdentityAndRuntimeSupportFlags() {
        let preferred = DeviceCapabilitySummary(
            modelId: .cs7501,
            modelName: "CS7501",
            defaultCommandSet: .masterWithModuleInfo,
            formFactor: .masterWithModule,
            moduleFamily: .se4750,
            supportsBasicDeviceCommands: false,
            supportsMasterCommands: false,
            supportsNativeModuleCommands: false,
            supportsModuleCommandBridge: false,
            supportsModuleCommands: false,
            supportsScannerMaster: false,
            supportsModulePassthrough: false,
            supportStatus: .codeOnly
        )
        let runtime = DeviceCapabilitySummary(
            modelId: .c740,
            modelName: "C740",
            defaultCommandSet: .moduleOnly,
            formFactor: .singleModule,
            moduleFamily: .nt212x,
            supportsBasicDeviceCommands: true,
            supportsMasterCommands: true,
            supportsNativeModuleCommands: true,
            supportsModuleCommandBridge: true,
            supportsModuleCommands: true,
            supportsScannerMaster: true,
            supportsModulePassthrough: true,
            supportStatus: .verified
        )

        let merged = mergePreferredCapabilityWithRuntime(preferredCapability: preferred, runtimeCapability: runtime)

        XCTAssertEqual(merged.modelId, .cs7501)
        XCTAssertEqual(merged.modelName, "CS7501")
        XCTAssertEqual(merged.moduleFamily, .se4750)
        XCTAssertEqual(merged.supportStatus, .codeOnly)
        XCTAssertTrue(merged.supportsMasterCommands)
        XCTAssertTrue(merged.supportsModuleCommands)
        XCTAssertTrue(merged.supportsNativeModuleCommands)
    }

    func testSessionOperationSupportMappingUsesSharedSummaryFlags() {
        let support = SessionOperationSupportSummary(
            supportsRefreshInfo: false,
            supportsInitializeSession: true,
            supportsGetBatteryInfo: false,
            supportsExecuteBasicDeviceCommands: true,
            supportsExecuteTextCommands: false,
            supportsExecuteDataRuleCommands: true,
            supportsDefaultModuleCommandProbe: true,
            supportsTriggerScan: false,
            supportsBeep: true,
            supportsDisableAckBeep: false,
            supportsVibrateOn: true,
            supportsVibrateOff: false
        )

        XCTAssertTrue(supportsSessionOperation(.initializeSession, support: support))
        XCTAssertFalse(supportsSessionOperation(.getBatteryInfo, support: support))
        XCTAssertTrue(supportsSessionOperation(.basicDeviceCommands, support: support))
        XCTAssertFalse(supportsSessionOperation(.textCommands, support: support))
        XCTAssertTrue(supportsSessionOperation(.dataRuleCommands, support: support))
        XCTAssertTrue(supportsSessionOperation(.beep, support: support))
        XCTAssertFalse(supportsSessionOperation(.disableAckBeep, support: support))
        XCTAssertTrue(supportsSessionOperation(.vibrateOn, support: support))
        XCTAssertFalse(supportsSessionOperation(.vibrateOff, support: support))
    }

    func testFormatDiscoveryDeviceSummaryIncludesModelTransportAndSignal() {
        let device = DiscoveredDevice(
            deviceId: "demo-1",
            name: "Scanner Demo",
            transportType: .bleGatt,
            modelId: .cs7501,
            matchReason: nil,
            rssi: -48
        )

        XCTAssertEqual(
            formatDiscoveryDeviceSummary(device),
            "型号: CS7501 / 传输: BLE GATT / 信号: RSSI -48"
        )
    }

    func testModulePayloadHexParserAcceptsSpacedHex() throws {
        XCTAssertEqual(try parseModuleParameterID("0xF025"), 0xF025)
        XCTAssertEqual(try parseModulePayloadHex("01 0D 0A"), Data([0x01, 0x0D, 0x0A]))
        XCTAssertThrowsError(try parseModulePayloadHex("0"))
    }

    func testSe4750CatalogExposesQuickValuesAndNumericInput() throws {
        let originalLanguage = DemoLocalization.shared.language
        DemoLocalization.shared.language = .en
        defer { DemoLocalization.shared.language = originalLanguage }

        let presets = Dictionary(
            uniqueKeysWithValues: DemoModuleSettingsCatalog.presets(for: .se4750).map { ($0.parameterID, $0) }
        )

        XCTAssertEqual(presets[0x88]?.quickValueOptions.map(\.label), ["0.5s", "1.0s", "3.0s", "5.0s", "9.9s"])
        XCTAssertEqual(presets[0xF1D0]?.quickValueOptions.map(\.label), ["0ms", "200ms", "400ms", "1000ms"])

        let decodeTimeoutSpec = try XCTUnwrap(presets[0x88]?.numericInputSpec)
        XCTAssertEqual(moduleNumericInputToPayloadHex("5", spec: decodeTimeoutSpec), "05")
        XCTAssertEqual(moduleNumericInputText(fromPayload: "05", spec: decodeTimeoutSpec), "5")
        XCTAssertTrue(moduleNumericInputError("4", spec: decodeTimeoutSpec)?.contains("5-99") ?? false)
    }

    func testBooleanPayloadsUseSdkMetadataForTransmitLabels() throws {
        let originalLanguage = DemoLocalization.shared.language
        DemoLocalization.shared.language = .zh
        defer { DemoLocalization.shared.language = originalLanguage }

        let preset = try XCTUnwrap(
            DemoModuleSettingsCatalog.presets(for: .nt280h).first { $0.definition.key == "B1_02" }
        )

        XCTAssertEqual(preset.writeOffHex, "0D")
        XCTAssertEqual(preset.writeOnHex, "0E")
    }

    func testEnumOptionLabelsUseSdkMetadata() throws {
        let originalLanguage = DemoLocalization.shared.language
        DemoLocalization.shared.language = .zh
        defer { DemoLocalization.shared.language = originalLanguage }

        let preset = try XCTUnwrap(
            DemoModuleSettingsCatalog.presets(for: .nt280h).first { $0.definition.key == "A0_00" }
        )

        XCTAssertEqual(preset.enumOptions.map(\.label), ["关闭 ACK 响应", "开启 ACK 响应"])
    }

    func testNt212xLengthPresetUsesDecimalNumericInput() throws {
        let presets = Dictionary(
            uniqueKeysWithValues: DemoModuleSettingsCatalog.presets(for: .nt212x).map { ($0.parameterID, $0) }
        )

        let lengthSpec = try XCTUnwrap(presets[0x12]?.numericInputSpec)
        XCTAssertEqual(moduleNumericInputText(fromPayload: "0E", spec: lengthSpec), "14")
        XCTAssertEqual(moduleNumericInputToPayloadHex("14", spec: lengthSpec), "0E")
        XCTAssertTrue(moduleNumericInputError("100", spec: lengthSpec)?.contains("0-99") ?? false)
    }

    func testNtc06hTemplateHelpersBuildSettingCode() throws {
        let setting = try XCTUnwrap(DemoModuleSettingsCatalog.ntc06hSettings().first { $0.displayCode == "0087hh" })
        let spec = try XCTUnwrap(ntc06hTemplateInputSpec(setting))

        XCTAssertEqual(ntc06hTemplateValueOrEmpty(setting), "04")
        XCTAssertEqual(ntc06hNormalizeTemplateValue("0g4", spec: spec), "04")
        XCTAssertTrue(ntc06hTemplateValueIsValid("04", spec: spec))
        XCTAssertEqual(ntc06hBuildTemplateCode(spec, value: "04"), "008704")
    }

    func testNtc06hSettingTitleUsesGeneratedSdkResources() throws {
        let setting = try XCTUnwrap(DemoModuleSettingsCatalog.ntc06hSettings().first { $0.key == "system_000b0" })

        DemoLocalization.shared.language = .zh
        XCTAssertEqual(ntc06hSettingTitle(setting), "恢复出厂值")

        DemoLocalization.shared.language = .en
        XCTAssertEqual(ntc06hSettingTitle(setting), "Factory Reset")
    }
}
