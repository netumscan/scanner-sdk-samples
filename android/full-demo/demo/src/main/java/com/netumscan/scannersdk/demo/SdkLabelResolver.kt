package com.netumscan.scannersdk.demo

import java.util.Locale

object SdkLabelResolver {
    fun resolve(localizationKey: String, fallbackDisplayName: String): String {
        val normalized = localizationKey
            .replace('.', '_')
            .replace('-', '_')
            .lowercase(Locale.US)
        val isResourceLike = normalized.startsWith("nsdk_") && normalized.matches(Regex("[a-z0-9_]+"))
        if (!isResourceLike) {
            return fallbackDisplayName
        }
        return dynamicString(normalized).ifEmpty { fallbackDisplayName }
    }

    private fun dynamicString(name: String): String {
        val context = DemoLocaleController.applicationResourceContext()
        if (context != null) {
            val id = context.resources.getIdentifier(name, "string", context.packageName)
            if (id != 0) {
                return context.getString(id)
            }
            return ""
        }
        return DemoStringFallbacks.string(name)
    }
}
