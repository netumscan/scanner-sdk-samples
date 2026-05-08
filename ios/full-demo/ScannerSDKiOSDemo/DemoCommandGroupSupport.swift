import ScannerSDK

extension DemoCommandCatalog {
    static var groups: [DemoCommandGroup] {
        [
        DemoCommandGroup(title: groupTitle("console_group_basic", fallback: "Basic"), commands: [
            basic(.getVersion),
            basic(.factoryReset, dangerous: true),
            basic(.writeCustomDefaults, dangerous: true),
            basic(.restoreCustomDefaults, dangerous: true),
            basic(.normalMode, dangerous: true),
            basic(.storeMode, dangerous: true),
            basic(.uploadMemoryData),
            basic(.getMemoryBarcodeCount),
            basic(.uploadMemoryDataAndClear, dangerous: true),
            basic(.getMemoryUsage),
            basic(.clearMemory, dangerous: true),
            basic(.autoStoreModeOff, dangerous: true),
            basic(.autoStoreModeOn, dangerous: true),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_power", fallback: "Power"), commands: [
            master(.readSleepTime),
            master(.powerOff, dangerous: true),
            master(.sleepTime1Min),
            master(.sleepTime3Min),
            master(.sleepTime5Min),
            master(.sleepTime10Min),
            master(.sleepTime30Min),
            master(.sleepTime1Hour),
            master(.sleepTime2Hour),
            master(.neverSleep),
            master(.disableTimeStamp),
            master(.enableTimeStamp),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_scan_mode", fallback: "Scan Mode"), commands: [
            master(.keyScanModeDefault),
            master(.continueScanMode),
            master(.keyPulseScanMode),
            master(.hostTriggerMode),
            master(.decodeOvertime3S),
            master(.decodeOvertime6S),
            master(.intervalTime500Ms),
            master(.intervalTime1000Ms),
            master(.immediateScan1S),
            master(.immediateScan2S),
            master(.immediateScan3S),
            master(.immediateScan4S),
            master(.immediateScan5S),
            master(.immediateScan6S),
            master(.immediateScan7S),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_feedback", fallback: "Feedback"), commands: [
            master(.mute),
            master(.highVolume),
            master(.middleVolume),
            master(.lowVolume),
            master(.highTone),
            master(.lowTone),
            master(.baseConnectBeepPromptToggle),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_beep_fx", fallback: "Beep FX"), commands: [
            master(.sdkBeepB0),
            master(.sdkBeepB1),
            master(.sdkBeepB2),
            master(.sdkBeepB3),
            master(.sdkBeepB4),
            master(.sdkBeepB5),
            master(.sdkBeepB6),
            master(.sdkBeepB7),
            master(.sdkBeepB8),
            master(.sdkBeepB9),
            master(.sdkBeepBColon),
            master(.sdkBeepBSemicolon),
            master(.sdkBeepBLessThan),
            master(.sdkBeepBEquals),
            master(.sdkBeepBGreaterThan),
            master(.sdkBeepBQuestion),
            master(.sdkBeepBAt),
            master(.sdkBeepBA),
            master(.sdkBeepBB),
            master(.sdkBeepBC),
            master(.sdkBeepBD),
            master(.sdkBeepBE),
            master(.sdkBeepBF),
            master(.sdkBeepBG),
            master(.sdkBeepBH),
            master(.sdkBeepBI),
            master(.sdkBeepBJ),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_rf", fallback: "RF"), commands: [
            master(.readInterfaceSetting),
            master(.switchRf24GTransport, dangerous: true),
            master(.rfPair, dangerous: true),
            master(.oneToOnePairing, dangerous: true),
            master(.oneDongleManyScannersPairing, dangerous: true),
            master(.rfDongleCompositeDevice),
            master(.rfDongleVirtualCom),
            master(.rfKeyboardSpeedHigh),
            master(.readKeyboardSpeed),
            master(.rfKeyboardSpeedMedium),
            master(.rfKeyboardSpeedLow),
            master(.sramBufferToggle),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_bt", fallback: "BT"), commands: [
            master(.readInterfaceSetting),
            master(.switchBluetoothTransport, dangerous: true),
            master(.bluetoothHid, dangerous: true),
            master(.bluetoothSpp, dangerous: true),
            master(.bluetoothBle, dangerous: true),
            master(.readBtFirmwareVersion),
            master(.readBtName),
            master(.readBtAddress),
            master(.rebootBt, dangerous: true),
            master(.restoreBtFactorySettings, dangerous: true),
            master(.disconnectCurrentBt, dangerous: true),
            master(.btDongleTransportMode),
            master(.unpairBluetoothHid, dangerous: true),
            master(.iosPopupHideKeyboard),
            master(.holdTrigger4Seconds),
            master(.doubleClickTrigger),
            master(.btHidCapsLockIgnore),
            master(.holdTrigger8SecondsSwapRfBt, dangerous: true),
            master(.readBtHidDelay),
            master(.btHidDelayHigh),
            master(.btHidDelayValue6),
            master(.btHidDelayMedium),
            master(.btHidDelayValue18),
            master(.btHidDelayLow),
            master(.btHidDelayValue30),
            master(.btConnectedNotSleep),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_decoder_module", fallback: "Decoder Module"), commands: [
            master(.readDecoderModule),
            master(.setDecoderModule0, dangerous: true),
            master(.setDecoderModule1, dangerous: true),
            master(.setDecoderModule2, dangerous: true),
            master(.setDecoderModule3, dangerous: true),
            master(.setDecoderModule4, dangerous: true),
            master(.setDecoderModule5, dangerous: true),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_usb", fallback: "USB"), commands: [
            master(.switchUsbKeyboardMode, dangerous: true),
            master(.switchUsbVirtualComMode, dangerous: true),
            master(.usbAutoInterfaceSelectOn, dangerous: true),
            master(.usbAutoInterfaceSelectOff, dangerous: true),
            master(.usbKeyboardSpeedLowDelay),
            master(.usbKeyboardSpeedHigh),
            master(.usbKeyboardSpeedValue4),
            master(.usbKeyboardSpeedMedium),
            master(.usbKeyboardSpeedValue9),
            master(.usbKeyboardSpeedLow),
            master(.usbHidMultiKeyOn),
            master(.usbHidMultiKeyOff),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_hid_keys", fallback: "HID Keys"), commands: [
            master(.ctrlKeyPrefixOn),
            master(.combineKeyOff),
            master(.altKeyPrefixOn),
            master(.normalKeyConfig2),
            master(.caseStrategyNormal),
            master(.caseStrategySwap),
            master(.caseStrategyUpper),
            master(.caseStrategyLower),
            master(.numLockOff),
            master(.numLockOn),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_charset", fallback: "Charset"), commands: [
            master(.readCurrentCharset),
            master(.charsetAuto),
            master(.charsetGbk),
            master(.charsetUtf8Word),
            master(.charsetIso8859),
            master(.charsetNormal),
            master(.charsetUtf8Txt),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_receive_device_short", fallback: "Recv Dev"), commands: [
            master(.readCurrentReceiveDevice),
            master(.receiveDeviceWindows),
            master(.receiveDeviceMacOsIos),
            master(.receiveDeviceAndroid),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_layouts", fallback: "Layouts"), commands: [
            master(.readKeyboardLayout),
            master(.keyboardLayoutEN),
            master(.keyboardLayoutFR),
            master(.keyboardLayoutGE),
            master(.keyboardLayoutIT),
            master(.keyboardLayoutPT),
            master(.keyboardLayoutES),
            master(.keyboardLayoutTK),
            master(.keyboardLayoutTF),
            master(.keyboardLayoutUK),
            master(.keyboardLayoutCS),
            master(.keyboardLayoutCY),
            master(.keyboardLayoutHU),
            master(.keyboardLayoutFB),
            master(.keyboardLayoutPB),
            master(.keyboardLayoutFC),
            master(.keyboardLayoutHR),
            master(.keyboardLayoutSK),
            master(.keyboardLayoutSQ),
            master(.keyboardLayoutDA),
            master(.keyboardLayoutFI),
            master(.keyboardLayoutEL),
            master(.keyboardLayoutNL),
            master(.keyboardLayoutNO),
            master(.keyboardLayoutPL),
            master(.keyboardLayoutSR),
            master(.keyboardLayoutSL),
            master(.keyboardLayoutSV),
            master(.keyboardLayoutDS),
            master(.keyboardLayoutJP),
            master(.keyboardLayoutTH),
            master(.keyboardLayoutAG),
            master(.keyboardLayoutRU),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_data_rules", fallback: "Data Rules"), commands: [
            text(DemoStrings.tr("data_rule_clear_prefix"), "$SCAN#4"),
            text(DemoStrings.tr("data_rule_clear_suffix"), "$SCAN#3"),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_output", fallback: "Output"), commands: [
            master(.clearOutputFormat),
            text(DemoStrings.tr("data_rule_suffix_on"), "$DATA#1"),
            text(DemoStrings.tr("data_rule_prefix_on"), "$DATA#2"),
            master(.enableHideEndOutput),
            master(.enableHideMiddleOutput),
            master(.enableHideStartOutput),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_replace", fallback: "Replace"), commands: [
            master(.readReplaceSet),
            master(.clearReplaceSet),
        ]),
        DemoCommandGroup(title: groupTitle("console_group_terminal", fallback: "Terminal"), commands: [
            master(.extraTerminalNone),
            master(.extraTerminalCr),
            master(.extraTerminalTab),
            master(.extraTerminalCrLf),
            master(.extraTerminalLf),
        ]),
        ]
    }

    private static func groupTitle(_ key: String, fallback: String) -> String {
        DemoStrings.tr(key, fallback: fallback)
    }

    private static func basic(_ command: BasicDeviceCommand, dangerous: Bool = false) -> DemoCommandEntry {
        DemoCommandEntry(title: basicDeviceCommandTitle(command), command: .basic(command), isDangerous: dangerous)
    }

    private static func master(_ command: MasterCommand, dangerous: Bool? = nil) -> DemoCommandEntry {
        let descriptor = try? ScannerSDK.shared.getMasterCommandDescriptor(command)
        let label = try? ScannerSDK.shared.getMasterCommandLabel(command)
        let title = label.map { DemoStrings.sdk($0.localizationKey, fallback: $0.fallbackDisplayName) } ?? "\(command)"
        return DemoCommandEntry(title: title, command: .master(command), isDangerous: dangerous ?? (descriptor?.isDangerous ?? false))
    }

    private static func text(_ title: String, _ command: String, dangerous: Bool = false) -> DemoCommandEntry {
        DemoCommandEntry(title: title, command: .text(command), isDangerous: dangerous)
    }

    static func textCommandTitle(commandText: String, fallback: String) -> String {
        groups
            .flatMap(\.commands)
            .first { $0.command == .text(commandText) }?
            .title ?? fallback
    }

    private static func basicDeviceCommandTitle(_ command: BasicDeviceCommand) -> String {
        guard let label = try? ScannerSDK.shared.getBasicDeviceCommandLabel(command) else {
            return "\(command)"
        }
        return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
    }
}
