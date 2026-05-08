package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.SessionOperationSupportSummary

internal enum class DemoSessionOperation {
    REFRESH_INFO,
    INITIALIZE_SESSION,
    GET_BATTERY_INFO,
    BASIC_DEVICE_COMMANDS,
    TEXT_COMMANDS,
    DATA_RULE_COMMANDS,
    BEEP,
    DISABLE_ACK_BEEP,
    VIBRATE_ON,
    VIBRATE_OFF,
}

internal fun SessionOperationSupportSummary.supports(operation: DemoSessionOperation): Boolean =
    when (operation) {
        DemoSessionOperation.REFRESH_INFO -> supportsRefreshInfo
        DemoSessionOperation.INITIALIZE_SESSION -> supportsInitializeSession
        DemoSessionOperation.GET_BATTERY_INFO -> supportsGetBatteryInfo
        DemoSessionOperation.BASIC_DEVICE_COMMANDS -> supportsExecuteBasicDeviceCommands
        DemoSessionOperation.TEXT_COMMANDS -> supportsExecuteTextCommands
        DemoSessionOperation.DATA_RULE_COMMANDS -> supportsExecuteDataRuleCommands
        DemoSessionOperation.BEEP -> supportsBeep
        DemoSessionOperation.DISABLE_ACK_BEEP -> supportsDisableAckBeep
        DemoSessionOperation.VIBRATE_ON -> supportsVibrateOn
        DemoSessionOperation.VIBRATE_OFF -> supportsVibrateOff
    }

internal fun unsupportedDemoSessionOperationReason(operation: DemoSessionOperation): String =
    when (operation) {
        DemoSessionOperation.REFRESH_INFO ->
            DemoStrings.text(R.string.unsupported_refresh_info)
        DemoSessionOperation.INITIALIZE_SESSION ->
            DemoStrings.text(R.string.unsupported_initialize_session)
        DemoSessionOperation.GET_BATTERY_INFO ->
            DemoStrings.text(R.string.unsupported_get_battery_info)
        DemoSessionOperation.BASIC_DEVICE_COMMANDS ->
            DemoStrings.text(R.string.unsupported_basic_device_commands)
        DemoSessionOperation.TEXT_COMMANDS ->
            DemoStrings.text(R.string.unsupported_text_commands)
        DemoSessionOperation.DATA_RULE_COMMANDS ->
            DemoStrings.text(R.string.unsupported_data_rule_commands)
        DemoSessionOperation.BEEP ->
            DemoStrings.text(R.string.unsupported_beep)
        DemoSessionOperation.DISABLE_ACK_BEEP ->
            DemoStrings.text(R.string.unsupported_disable_ack_beep)
        DemoSessionOperation.VIBRATE_ON,
        DemoSessionOperation.VIBRATE_OFF ->
            DemoStrings.text(R.string.unsupported_vibration)
    }

internal fun unsupportedMasterCommandReason(): String =
    DemoStrings.text(R.string.unsupported_master_command)
