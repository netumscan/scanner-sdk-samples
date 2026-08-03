package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.SupportedDeviceModel

data class DemoSupportedModel(
    val modelKey: String,
    val modelName: String,
    val seriesKey: String,
    val seriesName: String,
) {
    val primaryLabel: String
        get() {
            val name = modelName.ifBlank { modelKey }
            return if (name.equals(modelKey, ignoreCase = true)) name else "$modelKey · $name"
        }

    val secondaryLabel: String
        get() = seriesName.ifBlank { seriesKey }
}

internal fun SupportedDeviceModel.toDemoSupportedModel(): DemoSupportedModel = DemoSupportedModel(
    modelKey = modelKey,
    modelName = modelName,
    seriesKey = seriesKey,
    seriesName = seriesName,
)

internal fun normalizeSupportedModels(models: List<DemoSupportedModel>): List<DemoSupportedModel> {
    return models
        .filter { it.modelKey.isNotBlank() }
        .distinctBy { it.modelKey.uppercase() }
}

internal fun selectSupportedModel(
    models: List<DemoSupportedModel>,
    currentModelKey: String,
): String {
    return models.firstOrNull { it.modelKey.equals(currentModelKey, ignoreCase = true) }?.modelKey
        ?: models.firstOrNull()?.modelKey
        .orEmpty()
}
