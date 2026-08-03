package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.model.DataRule
import com.netumscan.scannersdk.model.DataRuleKind
import com.netumscan.scannersdk.model.ScanTerminatorPreset
import com.netumscan.scannersdk.model.ScanTextCharset
import com.netumscan.scannersdk.model.localizedLabel

@Composable
internal fun CharsetSelectorRow(
    selected: String,
    enabled: Boolean,
    onSelect: (ScanTextCharset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(charsetButtonLabel(ScanTextCharset.UTF_8, selected), enabled) { onSelect(ScanTextCharset.UTF_8) },
                commandButtonAction(charsetButtonLabel(ScanTextCharset.GBK, selected), enabled) { onSelect(ScanTextCharset.GBK) },
            ),
        )
        CommandButtonRow(
            actions = listOf(
                commandButtonAction(charsetButtonLabel(ScanTextCharset.US_ASCII, selected), enabled) { onSelect(ScanTextCharset.US_ASCII) },
                commandButtonAction(charsetButtonLabel(ScanTextCharset.ISO_8859_1, selected), enabled) { onSelect(ScanTextCharset.ISO_8859_1) },
            ),
        )
    }
}

private fun charsetButtonLabel(charset: ScanTextCharset, selected: String): String {
    return if (charset.displayName == selected) {
        "[${charset.displayName}]"
    } else {
        charset.displayName
    }
}

@Composable
internal fun TerminatorSelectorRow(
    selected: String,
    enabled: Boolean,
    onSelect: (ScanTerminatorPreset) -> Unit,
) {
    val rows = ScanTerminatorPreset.DEFAULTS.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            CommandButtonRow(
                actions = row.map { preset ->
                    commandButtonAction(terminatorButtonLabel(preset, selected), enabled) { onSelect(preset) }
                },
            )
        }
    }
}

private fun terminatorButtonLabel(preset: ScanTerminatorPreset, selected: String): String {
    return if (preset.summary == selected) {
        "[${preset.label}]"
    } else {
        preset.label
    }
}

internal enum class DataRuleFormMode(val kind: DataRuleKind) {
    PREFIX(DataRuleKind.PREFIX),
    SUFFIX(DataRuleKind.SUFFIX),
    HIDE_START(DataRuleKind.HIDE_START),
    HIDE_MIDDLE(DataRuleKind.HIDE_MIDDLE),
    HIDE_END(DataRuleKind.HIDE_END),
    REPLACE(DataRuleKind.REPLACE),
}

@Composable
internal fun DataRuleBuilderCard(
    mode: DataRuleFormMode,
    valueA: String,
    valueB: String,
    enabled: Boolean,
    onModeChange: (DataRuleFormMode) -> Unit,
    onValueAChange: (String) -> Unit,
    onValueBChange: (String) -> Unit,
    onExecute: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScrollableTabRow(
            selectedTabIndex = dataRuleFormModeIndex(mode),
            containerColor = Color.Transparent,
            contentColor = DemoColors.TextPrimary,
            edgePadding = 0.dp,
        ) {
            DataRuleFormMode.entries.forEach { entry ->
                Tab(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    text = { Text(dataRuleFormModeLabel(entry)) },
                )
            }
        }

        val buildResult = runCatching { buildDataRuleCommand(mode, valueA, valueB) }
        val canBuild = buildResult.isSuccess
        val error = buildResult.exceptionOrNull()?.message
        val previewText = buildResult.getOrNull()?.let { dataRuleCommandPreviewText(it) }

        when (mode) {
            DataRuleFormMode.PREFIX,
            DataRuleFormMode.SUFFIX -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_text_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.HIDE_START,
            DataRuleFormMode.HIDE_END -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_length_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.HIDE_MIDDLE -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_start_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = valueB,
                    onValueChange = onValueBChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_length_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }

            DataRuleFormMode.REPLACE -> {
                OutlinedTextField(
                    value = valueA,
                    onValueChange = onValueAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_source_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = valueB,
                    onValueChange = onValueBChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.data_rule_target_ascii_hint)) },
                    enabled = enabled,
                    singleLine = true,
                )
            }
        }

        if (canBuild) {
            ConsoleHint(demoStringResource(R.string.data_rule_valid_hint))
            if (previewText != null) {
                DemoLabeledValueBlock(
                    label = demoStringResource(R.string.data_rule_command_preview),
                    value = previewText,
                    valueFontSize = 14.sp,
                    valueFontWeight = FontWeight.SemiBold,
                )
            }
        } else if (!error.isNullOrBlank()) {
            ConsoleFeedbackBanner(text = error, tone = ConsoleBannerTone.Error)
        }

        Button(
            onClick = onExecute,
            enabled = enabled && canBuild,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.build_and_send))
        }
    }
}

internal fun dataRuleFormModeLabel(mode: DataRuleFormMode): String {
    val label = mode.kind.localizedLabel()
    return SdkLabelResolver.resolve(label.localizationKey, label.fallbackDisplayName)
}

private fun dataRuleFormModeIndex(mode: DataRuleFormMode): Int = when (mode) {
    DataRuleFormMode.PREFIX -> 0
    DataRuleFormMode.SUFFIX -> 1
    DataRuleFormMode.HIDE_START -> 2
    DataRuleFormMode.HIDE_MIDDLE -> 3
    DataRuleFormMode.HIDE_END -> 4
    DataRuleFormMode.REPLACE -> 5
}

internal fun buildDataRuleCommand(
    mode: DataRuleFormMode,
    valueA: String,
    valueB: String,
): DataRule {
    return when (mode) {
        DataRuleFormMode.PREFIX -> DataRule.prefix(parseAffixBytes(valueA))
        DataRuleFormMode.SUFFIX -> DataRule.suffix(parseAffixBytes(valueA))
        DataRuleFormMode.HIDE_START -> DataRule.hideStart(parseDataRuleByte(valueA))
        DataRuleFormMode.HIDE_MIDDLE -> DataRule.hideMiddle(parseDataRuleByte(valueA), parseDataRuleByte(valueB))
        DataRuleFormMode.HIDE_END -> DataRule.hideEnd(parseDataRuleByte(valueA))
        DataRuleFormMode.REPLACE -> {
            val source = parseReplaceSourceBytes(valueA)
            val target = parseReplaceTargetBytes(valueB)
            require(source.size + target.size <= 6) { "replace source and target support at most 6 bytes total" }
            DataRule.replace(source = source, target = target)
        }
    }
}

internal fun dataRuleCommandPreviewText(command: DataRule): String {
    return when (command) {
        is DataRule.SetSuffix -> "Set suffix (${command.bytes.size} bytes)"
        is DataRule.SetPrefix -> "Set prefix (${command.bytes.size} bytes)"
        is DataRule.HideEnd -> "Hide last ${command.length} byte(s)"
        is DataRule.HideMiddle -> "Hide ${command.length} byte(s) from position ${command.start}"
        is DataRule.HideStart -> "Hide first ${command.length} byte(s)"
        is DataRule.Replace -> "Replace ${command.source.size} byte(s) with ${command.target.size} byte(s)"
    }
}

private fun parseAffixBytes(text: String): ByteArray {
    val bytes = parseAsciiEscapes(text)
    require(bytes.isNotEmpty()) { "data rule text must not be empty" }
    require(bytes.size <= 10) { "data rule text supports at most 10 bytes" }
    return bytes
}

private fun parseReplaceSourceBytes(text: String): ByteArray {
    val bytes = parseAsciiEscapes(text)
    require(bytes.isNotEmpty()) { "replace source must not be empty" }
    require(bytes.size <= 6) { "replace source supports at most 6 bytes" }
    return bytes
}

private fun parseReplaceTargetBytes(text: String): ByteArray {
    val bytes = parseAsciiEscapes(text)
    require(bytes.size <= 5) { "replace target supports at most 5 bytes" }
    return bytes
}

private fun parseDataRuleByte(text: String): Int {
    val value = text.trim().toInt()
    require(value in 1..255) { "value must be in range 1-255" }
    return value
}

private fun parseAsciiEscapes(text: String): ByteArray {
    val bytes = ArrayList<Byte>(text.length)
    var index = 0
    while (index < text.length) {
        val ch = text[index]
        if (ch == '\\' && index + 3 < text.length && text[index + 1] == 'x') {
            val hex = text.substring(index + 2, index + 4)
            val value = hex.toIntOrNull(16)
                ?: throw IllegalArgumentException("invalid hex escape: \\x$hex")
            bytes += value.toByte()
            index += 4
            continue
        }
        require(ch.code in 0x00..0x7F) { "only ASCII text is supported in builder" }
        bytes += ch.code.toByte()
        index += 1
    }
    return bytes.toByteArray()
}
