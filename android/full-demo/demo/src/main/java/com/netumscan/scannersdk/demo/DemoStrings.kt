package com.netumscan.scannersdk.demo

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object DemoStrings {
    fun fromContext(context: Context, @StringRes id: Int): String {
        return DemoLocaleController.resourceContext(context).getString(id)
    }

    fun fromContext(context: Context, @StringRes id: Int, vararg formatArgs: Any): String {
        return DemoLocaleController.resourceContext(context).getString(id, *formatArgs)
    }

    fun text(@StringRes id: Int): String {
        return resourceString(id) ?: DemoStringFallbacks.string(id)
    }

    fun text(@StringRes id: Int, fallback: String): String {
        return text(id).ifEmpty { fallback }
    }

    fun text(@StringRes id: Int, vararg formatArgs: Any): String {
        return resourceString(id, *formatArgs) ?: DemoStringFallbacks.string(id, *formatArgs)
    }

    fun format(@StringRes id: Int, vararg formatArgs: Any): String {
        return text(id, *formatArgs)
    }

    fun text(@StringRes id: Int, fallback: String, vararg formatArgs: Any): String {
        return text(id, *formatArgs).ifEmpty {
            fallback.format(DemoLocaleController.formatLocale(), *formatArgs)
        }
    }

    val unknownDeviceName: String
        get() = text(R.string.unknown_device_name)

    val unnamedDevice: String
        get() = text(R.string.unnamed_device)

    val emptyValue: String
        get() = text(R.string.empty_value)

    private fun resourceString(@StringRes id: Int, vararg formatArgs: Any): String? {
        val context = DemoLocaleController.applicationResourceContext() ?: return null
        return if (formatArgs.isEmpty()) {
            context.getString(id)
        } else {
            context.getString(id, *formatArgs)
        }
    }
}

@Composable
fun demoStringResource(@StringRes id: Int): String {
    val context = LocalContext.current
    DemoLocaleController.currentLanguage
    return DemoStrings.fromContext(context, id)
}

@Composable
fun demoStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    DemoLocaleController.currentLanguage
    return DemoStrings.fromContext(context, id, *formatArgs)
}
