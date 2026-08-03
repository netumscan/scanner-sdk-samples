package com.netumscan.scannersdk.demo


internal fun displayModelLabel(modelKey: String): String = modelKey.ifBlank { "Unknown" }

internal fun resolveConnectionModelKey(
    selectedModelKey: String,
    discoveredModelKey: String,
): String {
    return selectedModelKey.takeIf { it != "" } ?: discoveredModelKey
}

internal fun formatSelectedModelSummary(selectedModelKey: String): String {
    return formatSelectedModelSummaryText(selectedModelKey).asStringForCurrentLanguage()
}

internal fun formatSelectedModelSummaryText(selectedModelKey: String): UiText {
    return if (selectedModelKey == "") {
        uiText(R.string.selected_model_not_provided)
    } else {
        joinedText(
            uiText(R.string.customer_selected_model),
            rawDisplayText(": ${displayModelLabel(selectedModelKey)}"),
        )
    }
}

internal fun formatSdkResolvedModelSummary(
    selectedModelKey: String,
    resolvedModel: String,
): String {
    return formatSdkResolvedModelSummaryText(selectedModelKey, resolvedModel).asStringForCurrentLanguage()
}

internal fun formatSdkResolvedModelSummaryText(
    selectedModelKey: String,
    resolvedModel: String,
): UiText {
    val resolvedValue = if (resolvedModel == "") {
        uiText(R.string.not_resolved)
    } else {
        rawDisplayText(displayModelLabel(resolvedModel))
    }
    val detail = when {
        selectedModelKey == "" || resolvedModel == "" -> resolvedValue
        selectedModelKey == resolvedModel -> resolvedValue
        else -> uiText(R.string.sdk_model_mismatch_detail, resolvedValue, displayModelLabel(selectedModelKey))
    }
    return joinedText(
        uiText(R.string.sdk_resolved_model),
        rawDisplayText(": "),
        detail,
    )
}
