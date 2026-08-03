package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerSdk
import java.util.Locale

object SdkLabelResolver {
    fun resolve(localizationKey: String, fallbackDisplayName: String): String {
        return ScannerSdk.localize(localizationKey, fallbackDisplayName, selectedLocale())
    }

    private fun selectedLocale(): Locale = when (DemoLocaleController.effectiveLanguage()) {
        DemoLanguage.ZH -> Locale.forLanguageTag("zh-Hans")
        DemoLanguage.EN -> Locale.ENGLISH
        DemoLanguage.SYSTEM -> Locale.getDefault()
    }
}
