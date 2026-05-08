package com.netumscan.scannersdk.demo

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface UiText {
    data class Res(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class SdkLabel(
        val localizationKey: String,
        val fallbackDisplayName: String,
    ) : UiText

    data class Raw(val value: String) : UiText

    data class Dynamic(val provider: () -> String) : UiText

    data class Joined(
        val parts: List<UiText>,
        val separator: String = "",
    ) : UiText
}

internal fun uiText(@StringRes id: Int, vararg args: Any): UiText =
    UiText.Res(id = id, args = args.toList())

internal fun rawDisplayText(value: String): UiText = UiText.Raw(value)

internal fun dynamicText(provider: () -> String): UiText = UiText.Dynamic(provider)

internal fun sdkText(localizationKey: String, fallbackDisplayName: String): UiText =
    UiText.SdkLabel(localizationKey, fallbackDisplayName)

internal fun joinedText(vararg parts: UiText, separator: String = ""): UiText =
    UiText.Joined(parts = parts.toList(), separator = separator)

internal fun UiText.asString(context: Context): String {
    return when (this) {
        is UiText.Res -> DemoStrings.fromContext(context, id, *args.map { arg ->
            if (arg is UiText) arg.asString(context) else arg
        }.toTypedArray())
        is UiText.SdkLabel -> SdkLabelResolver.resolve(localizationKey, fallbackDisplayName)
        is UiText.Raw -> value
        is UiText.Dynamic -> provider()
        is UiText.Joined -> parts.joinToString(separator = separator) { it.asString(context) }
    }
}

internal fun UiText.asStringForCurrentLanguage(): String {
    return when (this) {
        is UiText.Res -> DemoStrings.text(id, *args.map { arg ->
            if (arg is UiText) arg.asStringForCurrentLanguage() else arg
        }.toTypedArray())
        is UiText.SdkLabel -> SdkLabelResolver.resolve(localizationKey, fallbackDisplayName)
        is UiText.Raw -> value
        is UiText.Dynamic -> provider()
        is UiText.Joined -> parts.joinToString(separator = separator) { it.asStringForCurrentLanguage() }
    }
}

internal fun UiText?.orEmpty(): String = this?.asStringForCurrentLanguage().orEmpty()

@Composable
internal fun UiText.asString(): String {
    val context = LocalContext.current
    DemoLocaleController.currentLanguage
    return asString(context)
}
