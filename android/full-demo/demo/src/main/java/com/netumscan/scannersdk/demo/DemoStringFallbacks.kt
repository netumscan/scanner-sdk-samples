package com.netumscan.scannersdk.demo

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import java.util.ServiceLoader

internal object DemoStringFallbacks {
    private val resourceNames: Map<Int, String> by lazy { loadResourceNames() }
    private val provider: DemoStringFallbackProvider? by lazy { loadProvider() }

    fun string(@StringRes id: Int, vararg formatArgs: Any): String {
        val name = resourceNames[id] ?: return ""
        return string(name, *formatArgs)
    }

    fun string(name: String, vararg formatArgs: Any): String {
        val language = DemoLocaleController.effectiveLanguage()
        val raw = provider?.string(language, name)
            ?: provider?.string(DemoLanguage.EN, name)
            ?: return ""
        return if (formatArgs.isEmpty()) {
            raw
        } else {
            raw.format(DemoLocaleController.formatLocale(), *formatArgs)
        }
    }

    private fun loadProvider(): DemoStringFallbackProvider? {
        return runCatching {
            ServiceLoader.load(DemoStringFallbackProvider::class.java).firstOrNull()
        }.getOrNull()
    }

    private fun loadResourceNames(): Map<Int, String> {
        return R.string::class.java.fields.mapNotNull { field ->
            runCatching { field.getInt(null) to field.name }.getOrNull()
        }.toMap()
    }
}

@VisibleForTesting
internal interface DemoStringFallbackProvider {
    fun string(language: DemoLanguage, name: String): String?
}
