package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DeviceCapabilitySummary
import com.netumscan.scannersdk.model.localizedLabel as localizedSdkLabel

internal data class CapabilitySummaryUi(
    val commandSet: UiText,
    val formFactor: UiText,
    val supportsMasterCommands: Boolean,
    val supportsScannerMaster: Boolean,
    val status: UiText,
) {
    fun asText(): UiText = uiText(
        R.string.capability_summary_format,
        commandSet,
        formFactor,
        supportFlagText(supportsMasterCommands),
        supportFlagText(supportsScannerMaster),
        status,
    )
}

internal fun DeviceCapabilitySummary.toCapabilitySummaryUi(): CapabilitySummaryUi {
    val commandSetLabel = defaultCommandSet.localizedSdkLabel()
    val formFactorLabel = formFactor.localizedSdkLabel()
    val statusLabel = supportStatus.localizedSdkLabel()
    return CapabilitySummaryUi(
        commandSet = sdkText(commandSetLabel.localizationKey, commandSetLabel.fallbackDisplayName),
        formFactor = sdkText(formFactorLabel.localizationKey, formFactorLabel.fallbackDisplayName),
        supportsMasterCommands = supportsMasterCommands,
        supportsScannerMaster = supportsScannerMaster,
        status = sdkText(statusLabel.localizationKey, statusLabel.fallbackDisplayName),
    )
}

internal fun supportFlagText(value: Boolean): UiText =
    uiText(if (value) R.string.support_yes else R.string.support_no)
