package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.localizedLabel as localizedSdkLabel

internal data class CapabilitySummaryUi(
    val supportsDeviceCommands: Boolean,
    val supportsSettingsRead: Boolean,
    val supportsSettingsWrite: Boolean,
    val supportsDataRules: Boolean,
    val supportsBattery: Boolean,
    val status: UiText,
) {
    fun asText(): UiText = uiText(
        R.string.capability_summary_public_format,
        supportFlagText(supportsDeviceCommands),
        supportFlagText(supportsSettingsRead),
        supportFlagText(supportsSettingsWrite),
        supportFlagText(supportsDataRules),
        supportFlagText(supportsBattery),
        status,
    )
}

internal fun DeviceCapabilitySummary.toCapabilitySummaryUi(): CapabilitySummaryUi {
    val statusLabel = supportStatus.localizedSdkLabel()
    return CapabilitySummaryUi(
        supportsDeviceCommands = supportsDeviceCommands,
        supportsSettingsRead = supportsSettingsRead,
        supportsSettingsWrite = supportsSettingsWrite,
        supportsDataRules = supportsDataRules,
        supportsBattery = supportsBattery,
        status = sdkText(statusLabel.localizationKey, statusLabel.fallbackDisplayName),
    )
}

internal fun supportFlagText(value: Boolean): UiText =
    uiText(if (value) R.string.support_yes else R.string.support_no)
