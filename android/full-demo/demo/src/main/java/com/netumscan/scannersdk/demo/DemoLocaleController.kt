package com.netumscan.scannersdk.demo

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class DemoLanguage(val tag: String) {
    SYSTEM("system"),
    ZH("zh-CN"),
    EN("en"),
}

object DemoLocaleController {
    private var initialized = false
    private var appContext: Context? = null

    var currentLanguage by mutableStateOf(DemoLanguage.SYSTEM)
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        currentLanguage = languageForAppLocales(AppCompatDelegate.getApplicationLocales())
        initialized = true
    }

    @VisibleForTesting
    fun setLanguageForTest(language: DemoLanguage) {
        currentLanguage = language
        initialized = true
    }

    @VisibleForTesting
    fun setLanguageForTest(context: Context, language: DemoLanguage) {
        appContext = context.applicationContext
        currentLanguage = language
        initialized = true
    }

    fun setLanguage(context: Context, language: DemoLanguage) {
        appContext = context.applicationContext
        currentLanguage = language
        initialized = true
        AppCompatDelegate.setApplicationLocales(language.toLocaleListCompat())
    }

    fun languageLabel(language: DemoLanguage = currentLanguage): String = when (language) {
        DemoLanguage.SYSTEM -> DemoStrings.text(R.string.language_label_system)
        DemoLanguage.ZH -> DemoStrings.text(R.string.language_label_zh, "中文")
        DemoLanguage.EN -> DemoStrings.text(R.string.language_label_en, "English")
    }

    internal fun effectiveLanguage(): DemoLanguage {
        if (currentLanguage != DemoLanguage.SYSTEM) return currentLanguage
        val locale = appContext
            ?.resources
            ?.configuration
            ?.locales
            ?.get(0)
            ?: Locale.getDefault()
        return if (locale.language.equals("zh", ignoreCase = true)) DemoLanguage.ZH else DemoLanguage.EN
    }

    internal fun applicationResourceContext(): Context? {
        return appContext?.let { resourceContext(it) }
    }

    internal fun resourceContext(context: Context): Context {
        val locale = currentLanguage.resourceLocale() ?: return context
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    internal fun formatLocale(): Locale {
        return effectiveLanguage().formatLocale()
    }
}

private fun DemoLanguage.resourceLocale(): Locale? {
    return when (this) {
        DemoLanguage.SYSTEM -> null
        DemoLanguage.ZH -> Locale.forLanguageTag(tag)
        DemoLanguage.EN -> Locale.forLanguageTag(tag)
    }
}

private fun DemoLanguage.formatLocale(): Locale {
    return when (this) {
        DemoLanguage.SYSTEM -> Locale.getDefault()
        DemoLanguage.ZH -> Locale.CHINA
        DemoLanguage.EN -> Locale.ENGLISH
    }
}

private fun DemoLanguage.toLocaleListCompat(): LocaleListCompat {
    return when (this) {
        DemoLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        DemoLanguage.ZH -> LocaleListCompat.forLanguageTags(tag)
        DemoLanguage.EN -> LocaleListCompat.forLanguageTags(tag)
    }
}

private fun languageForAppLocales(locales: LocaleListCompat): DemoLanguage {
    val tag = locales[0]?.toLanguageTag() ?: return DemoLanguage.SYSTEM
    return when {
        tag.equals(DemoLanguage.ZH.tag, ignoreCase = true) ||
            tag.startsWith("zh", ignoreCase = true) -> DemoLanguage.ZH
        tag.equals(DemoLanguage.EN.tag, ignoreCase = true) ||
            tag.startsWith("en", ignoreCase = true) -> DemoLanguage.EN
        else -> DemoLanguage.SYSTEM
    }
}
