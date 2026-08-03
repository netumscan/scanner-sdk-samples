package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.SessionOperationSupport

internal enum class DemoSessionOperation {
    REFRESH_INFO,
    INITIALIZE_SESSION,
    GET_BATTERY_INFO,
    TRIGGER_SCAN,
    APPLY_DATA_RULE,
    SET_ACK_BEEP_ENABLED,
    SET_VIBRATION_ENABLED,
}

internal fun SessionOperationSupport.supports(operation: DemoSessionOperation): Boolean =
    when (operation) {
        DemoSessionOperation.REFRESH_INFO -> supportsRefreshInfo
        DemoSessionOperation.INITIALIZE_SESSION -> supportsInitializeSession
        DemoSessionOperation.GET_BATTERY_INFO -> supportsGetBatteryInfo
        DemoSessionOperation.TRIGGER_SCAN -> supportsTriggerScan
        DemoSessionOperation.APPLY_DATA_RULE -> supportsApplyDataRule
        DemoSessionOperation.SET_ACK_BEEP_ENABLED -> supportsSetAckBeepEnabled
        DemoSessionOperation.SET_VIBRATION_ENABLED -> supportsSetVibrationEnabled
    }

internal fun unsupportedDemoSessionOperationReason(operation: DemoSessionOperation): String =
    when (operation) {
        DemoSessionOperation.REFRESH_INFO ->
            DemoStrings.text(R.string.unsupported_refresh_info)
        DemoSessionOperation.INITIALIZE_SESSION ->
            DemoStrings.text(R.string.unsupported_initialize_session)
        DemoSessionOperation.GET_BATTERY_INFO ->
            DemoStrings.text(R.string.unsupported_get_battery_info)
        DemoSessionOperation.TRIGGER_SCAN ->
            DemoStrings.text(R.string.unsupported_trigger_scan)
        DemoSessionOperation.APPLY_DATA_RULE ->
            DemoStrings.text(R.string.unsupported_data_rule_commands)
        DemoSessionOperation.SET_ACK_BEEP_ENABLED ->
            DemoStrings.text(R.string.unsupported_beep)
        DemoSessionOperation.SET_VIBRATION_ENABLED ->
            DemoStrings.text(R.string.unsupported_vibration)
}
