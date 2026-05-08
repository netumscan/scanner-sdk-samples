package com.netumscan.scannersdk.demo

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.netumscan.scannersdk.model.Ntc06hSettingDefinition
import com.netumscan.scannersdk.model.Ntc06hTemplateInputSpec
import com.netumscan.scannersdk.model.Ntc06hTemplateInputType
import com.netumscan.scannersdk.model.ntc06hNormalizeTemplateValue
import com.netumscan.scannersdk.model.ntc06hTemplateInputSpec

@Composable
internal fun Ntc06hTemplateEditorFields(
    setting: Ntc06hSettingDefinition,
    settingCode: String,
    templateValue: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSettingCodeChange: (String) -> Unit,
    onTemplateValueChange: (String) -> Unit,
) {
    val templateInputSpec = ntc06hTemplateInputSpec(setting)
    if (templateInputSpec != null) {
        OutlinedTextField(
            value = templateValue,
            onValueChange = { onTemplateValueChange(ntc06hNormalizeTemplateValue(it, templateInputSpec)) },
            modifier = modifier,
            label = { Text(ntc06hTemplateInputLabel(templateInputSpec)) },
            supportingText = {
                Text(ntc06hTemplateInputSupportText(setting, templateInputSpec))
            },
            enabled = enabled,
            singleLine = true,
        )
        OutlinedTextField(
            value = settingCode,
            onValueChange = {},
            modifier = modifier,
            label = { Text(demoStringResource(R.string.setting_code_preview)) },
            enabled = false,
            singleLine = true,
        )
    } else {
        OutlinedTextField(
            value = settingCode,
            onValueChange = onSettingCodeChange,
            modifier = modifier,
            label = { Text(demoStringResource(R.string.setting_code)) },
            enabled = enabled,
            singleLine = true,
        )
    }
}

@Composable
internal fun ntc06hTemplateInputLabel(spec: Ntc06hTemplateInputSpec): String =
    when (spec.inputType) {
        Ntc06hTemplateInputType.HEX_UINT8 -> demoStringResource(R.string.template_value_hex, spec.placeholder.uppercase())
        Ntc06hTemplateInputType.DECIMAL_RANGE -> demoStringResource(R.string.template_value_dec, spec.placeholder.uppercase())
        else -> demoStringResource(R.string.template_value, spec.placeholder.uppercase())
    }

@Composable
internal fun ntc06hTemplateInputSupportText(setting: Ntc06hSettingDefinition, spec: Ntc06hTemplateInputSpec): String =
    when (spec.inputType) {
        Ntc06hTemplateInputType.HEX_UINT8 -> demoStringResource(
            R.string.template_hex_support,
            spec.minValue.toString(16).uppercase().padStart(spec.width, '0'),
            spec.maxValue.toString(16).uppercase().padStart(spec.width, '0'),
            setting.displayCode,
            setting.templateExampleCode,
        )
        else -> demoStringResource(
            R.string.template_support,
            setting.displayCode,
            setting.templateExampleCode,
        )
    }
