package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.DeviceModelId
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.displayName
import com.netumscan.scannersdk.model.localizedLabel

internal fun displayModelLabel(modelId: DeviceModelId): String = modelId.displayName()

internal fun displayModuleFamilyLabel(family: ModuleFamily): String {
    val label = family.localizedLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

internal fun resolveConnectionModelId(
    selectedModelId: DeviceModelId,
    discoveredModelId: DeviceModelId,
): DeviceModelId {
    return selectedModelId.takeIf { it != DeviceModelId.UNKNOWN } ?: discoveredModelId
}

internal fun formatSelectedModelSummary(selectedModelId: DeviceModelId): String {
    return formatSelectedModelSummaryText(selectedModelId).asStringForCurrentLanguage()
}

internal fun formatSelectedModelSummaryText(selectedModelId: DeviceModelId): UiText {
    return if (selectedModelId == DeviceModelId.UNKNOWN) {
        uiText(R.string.selected_model_not_provided)
    } else {
        joinedText(
            uiText(R.string.customer_selected_model),
            rawDisplayText(": ${displayModelLabel(selectedModelId)}"),
        )
    }
}

internal fun formatSdkResolvedModelSummary(
    selectedModelId: DeviceModelId,
    resolvedModel: DeviceModelId,
): String {
    return formatSdkResolvedModelSummaryText(selectedModelId, resolvedModel).asStringForCurrentLanguage()
}

internal fun formatSdkResolvedModelSummaryText(
    selectedModelId: DeviceModelId,
    resolvedModel: DeviceModelId,
): UiText {
    val resolvedValue = if (resolvedModel == DeviceModelId.UNKNOWN) {
        uiText(R.string.not_resolved)
    } else {
        rawDisplayText(displayModelLabel(resolvedModel))
    }
    val detail = when {
        selectedModelId == DeviceModelId.UNKNOWN || resolvedModel == DeviceModelId.UNKNOWN -> resolvedValue
        selectedModelId == resolvedModel -> resolvedValue
        else -> uiText(R.string.sdk_model_mismatch_detail, resolvedValue, displayModelLabel(selectedModelId))
    }
    return joinedText(
        uiText(R.string.sdk_resolved_model),
        rawDisplayText(": "),
        detail,
    )
}
