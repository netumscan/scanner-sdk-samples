package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerException
import com.netumscan.scannersdk.getNt212xParameterValueBytes
import com.netumscan.scannersdk.getNt280hParameterValueBytes
import com.netumscan.scannersdk.getSe4750ParameterValueBytes
import com.netumscan.scannersdk.model.CommandResponse
import com.netumscan.scannersdk.model.ModuleCommandKind
import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleParameterUiKind
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.formatModuleParameterId
import java.nio.charset.Charset

internal object ModuleCommandPresentation {
    fun parameterCommandLabel(
        kind: ModuleCommandKind,
        family: ModuleFamily,
        parameterId: Int,
        fallback: String,
    ): String {
        val presetTitle = ModuleSettingsCatalog.presetsFor(family)
            .firstOrNull { it.parameterId == parameterId }
            ?.title()
            ?: return fallback
        val action = when (kind) {
            ModuleCommandKind.READ_PARAMETER -> DemoStrings.text(R.string.read_module_parameter)
            ModuleCommandKind.WRITE_PARAMETER -> DemoStrings.text(R.string.write_module_parameter)
            else -> return fallback
        }
        return "$action 0x${formatModuleParameterId(parameterId)} $presetTitle"
    }

    fun ntc06hSettingLabel(settingKey: String, fallback: String): String {
        val setting = Ntc06hSettingCatalog.findByKey(settingKey) ?: return fallback
        return "${DemoStrings.text(R.string.send_ntc06h_setting)} ${setting.displayCode} ${setting.localizedTitle()}"
    }

    fun ntc06hWriteTimeoutMessage(
        label: String,
        settingCode: String,
        saveAfterWrite: Boolean,
    ): String {
        return if (saveAfterWrite) {
            DemoStrings.format(R.string.ntc06h_write_setting_save_timeout, label, settingCode)
        } else {
            DemoStrings.format(R.string.ntc06h_write_setting_timeout, label, settingCode)
        }
    }

    fun isNtc06hSilentAckTimeout(error: Throwable): Boolean {
        if (error is ScannerException && error.errorCode == 5) {
            return true
        }
        val detail = error.message.orEmpty()
        return detail.contains("code=5") ||
            detail.contains("timeout", ignoreCase = true)
    }

    fun parameterDebugSummary(
        label: String,
        family: ModuleFamily,
        kind: ModuleCommandKind,
        parameterId: Int,
        payloadBytes: ByteArray,
        persist: Boolean,
    ): String {
        val payload = payloadBytes.joinToString(" ") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }.ifBlank { "<empty>" }
        return "$label: family=$family kind=$kind parameter=0x${formatModuleParameterId(parameterId)} persist=$persist payload=$payload"
    }

    fun moduleResponse(
        label: String,
        family: ModuleFamily,
        parameterId: Int,
        parameterKind: ModuleParameterUiKind,
        response: CommandResponse,
        commandTextCharset: Charset,
        recordCharset: Charset,
    ): String {
        val base = CommandResponseFormatter.format(label, response, commandTextCharset, recordCharset)
        val valueBytes = runCatching {
            when (family) {
                ModuleFamily.NT212X -> response.getNt212xParameterValueBytes(parameterId)
                ModuleFamily.NT280H -> response.getNt280hParameterValueBytes(parameterId)
                ModuleFamily.SE4750 -> response.getSe4750ParameterValueBytes(parameterId)
                else -> null
            }
        }.getOrNull()
            ?: return base
        return "$base ${formatModuleValuePreview(parameterKind, valueBytes)}"
    }
}
