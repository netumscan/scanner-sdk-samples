package com.netumscan.scannersdk.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntrySource
import com.netumscan.scannersdk.model.CapabilityRiskLevel
import com.netumscan.scannersdk.model.CapabilityValueKind

@Composable
internal fun ProductSettingsSectionControls(
    vm: CommandConsoleViewModel,
    state: ProductSettingsSectionState,
    filters: List<ProductSettingDomainFilter>,
    familyFilters: List<ProductSettingFamilyFilter>,
    selectedDomainKey: String?,
    selectedFamilyKey: String?,
    displayedSettings: List<ProductSettingListItem>,
    onDomainSelect: (String?) -> Unit,
    onFamilySelect: (String?) -> Unit,
) {
    if (!state.hasReadySession) {
        ConsoleFeedbackBanner(
            text = demoStringResource(R.string.settings_requires_ready_session),
            tone = ConsoleBannerTone.Error,
        )
        return
    }

    if (state.settingGroups.isEmpty()) {
        ConsoleFeedbackBanner(
            text = demoStringResource(R.string.no_public_settings_available),
            tone = ConsoleBannerTone.Error,
        )
        return
    }

    Text(
        text = demoStringResource(
            R.string.settings_overview_summary,
            state.settingDefinitions.count { it.supportsRead },
            state.settingDefinitions.count { it.supportsWrite },
        ),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 11.sp,
        color = DemoColors.TextSecondary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProductSettingFilterDropdown(
            label = demoStringResource(R.string.setting_category_filter),
            filters = filters,
            selectedKey = selectedDomainKey,
            onSelect = onDomainSelect,
            modifier = Modifier.weight(1f),
        )
        ProductSettingFilterDropdown(
            label = demoStringResource(R.string.setting_subcategory_filter),
            filters = familyFilters,
            selectedKey = selectedFamilyKey,
            onSelect = onFamilySelect,
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        text = demoStringResource(R.string.setting_list_count, displayedSettings.size),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 11.sp,
        color = DemoColors.TextTertiary,
    )

    if (displayedSettings.isEmpty()) {
        ConsoleHint(demoStringResource(R.string.setting_search_empty))
        return
    }

    val readableDefinitions = displayedSettings
        .map { it.definition }
        .filter { it.supportsRead }
    if (readableDefinitions.isNotEmpty()) {
        OutlinedButton(
            onClick = { vm.readAllReadableSettings(readableDefinitions) },
            enabled = !state.isExecuting && !state.isLoadingSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.read_visible_settings))
        }
    }
}

internal data class ProductSettingListItem(
    val definition: CapabilityEntry,
    val title: String,
    val domainKey: String,
    val domainTitle: String,
    val familyKey: String,
    val familyTitle: String,
    val sectionKey: String,
    val sectionTitle: String,
)

internal interface ProductSettingFilterOption {
    val key: String
    val title: String
}

internal data class ProductSettingDomainFilter(
    override val key: String,
    override val title: String,
) : ProductSettingFilterOption

internal data class ProductSettingFamilyFilter(
    override val key: String,
    override val title: String,
) : ProductSettingFilterOption

internal fun flattenProductSettingGroups(
    groups: List<ProductSettingGroup>,
): List<ProductSettingListItem> =
    groups.flatMap { group ->
        group.domains.flatMap { domain ->
            domain.families.flatMap { family ->
                family.sections.flatMap { section ->
                    section.items.map { item ->
                        ProductSettingListItem(
                            definition = item.definition,
                            title = item.title,
                            domainKey = domain.key,
                            domainTitle = domain.title,
                            familyKey = family.key,
                            familyTitle = family.title,
                            sectionKey = section.key,
                            sectionTitle = section.title,
                        )
                    }
                }
            }
        }
    }

internal fun filterProductSettingItems(
    items: List<ProductSettingListItem>,
    domainKey: String?,
    familyKey: String?,
): List<ProductSettingListItem> =
    items.filter { item ->
        (domainKey == null || item.domainKey == domainKey) &&
            (familyKey == null || item.familyKey == familyKey)
    }

@Composable
private fun ProductSettingFilterDropdown(
    label: String,
    filters: List<ProductSettingFilterOption>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedTitle = filters.firstOrNull { it.key == selectedKey }?.title
        ?: demoStringResource(R.string.all)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = DemoColors.TextTertiary,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedTitle)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(demoStringResource(R.string.all)) },
                    onClick = {
                        menuExpanded = false
                        onSelect(null)
                    },
                )
                filters.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.title) },
                        onClick = {
                            menuExpanded = false
                            onSelect(filter.key)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProductSettingItemRow(
    vm: CommandConsoleViewModel,
    item: ProductSettingListItem,
    state: ProductSettingsSectionState,
) {
    val definition = item.definition
    var editorOpen by remember(definition.entryKey) { mutableStateOf(false) }
    var pendingRiskDraft by remember(definition.entryKey) { mutableStateOf<ProductSettingDraft?>(null) }
    val options = state.settingOptionsByKey[definition.entryKey].orEmpty()
    val draft = state.settingDrafts[definition.entryKey] ?: defaultDraftForSetting(definition, options)
    val validationError = state.settingValidationErrors[definition.entryKey]
    val currentValue = state.capabilityReadValues[definition.entryKey]
    val operation = state.settingOperations[definition.entryKey]
    val isReading = operation == ProductSettingOperation.READING
    val isWriting = operation == ProductSettingOperation.WRITING
    val highlightCurrentValue = definition.entryKey in state.settingRecentlyUpdatedKeys
    val currentSummary = currentValue?.let { formatCapabilityValueSummary(definition, it, options) }
        ?: if (definition.supportsRead) demoStringResource(R.string.setting_value_not_loaded) else demoStringResource(R.string.setting_write_only)
    val defaultSummary = formatSettingDefaultSummary(definition, options)

    pendingRiskDraft?.let { riskDraft ->
        DangerDialog(
            label = item.title,
            message = capabilityRiskText(definition),
            onDismiss = {
                vm.logRiskActionCancelled(item.title)
                pendingRiskDraft = null
            },
            onConfirm = {
                pendingRiskDraft = null
                vm.updateSettingDraft(definition.entryKey, riskDraft)
                vm.writeCapabilitySetting(definition, riskDraft)
            },
        )
    }

    if (editorOpen) {
        ProductSettingEditorDialog(
            vm = vm,
            item = item,
            state = state,
            draft = draft,
            options = options,
            currentSummary = currentSummary,
            defaultSummary = defaultSummary,
            validationError = validationError,
            isReading = isReading,
            isWriting = isWriting,
            highlighted = highlightCurrentValue,
            onDismiss = { editorOpen = false },
            onWrite = {
                editorOpen = false
                if (definition.riskLevel == CapabilityRiskLevel.NORMAL) {
                    vm.writeCapabilitySetting(definition, draft)
                } else {
                    pendingRiskDraft = draft
                }
            },
        )
    }

    val rowEnabled = state.hasReadySession && !state.isExecuting && !isReading && !isWriting
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { editorOpen = true }
            .demoTestTag(DemoTestTags.capabilitySetting(definition.entryKey))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = compactSettingSupportLabel(definition),
                fontSize = 10.sp,
                color = if (definition.riskLevel == CapabilityRiskLevel.NORMAL) {
                    DemoColors.TextTertiary
                } else {
                    DemoColors.Danger
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CompactSettingTrailing(
            definition = definition,
            draft = draft,
            currentSummary = currentSummary,
            hasCurrentValue = currentValue != null,
            isReading = isReading,
            isWriting = isWriting,
            highlighted = highlightCurrentValue,
            enabled = rowEnabled,
            onRead = { vm.readCapabilitySetting(definition) },
            onBooleanChange = { nextValue ->
                val nextDraft = draft.copy(valueText = if (nextValue) "true" else "false")
                if (definition.riskLevel == CapabilityRiskLevel.NORMAL) {
                    vm.updateSettingDraft(definition.entryKey, nextDraft)
                    vm.writeCapabilitySetting(definition, nextDraft)
                } else {
                    pendingRiskDraft = nextDraft
                }
            },
        )
    }
}

@Composable
private fun CompactSettingTrailing(
    definition: CapabilityEntry,
    draft: ProductSettingDraft,
    currentSummary: String,
    hasCurrentValue: Boolean,
    isReading: Boolean,
    isWriting: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    onRead: () -> Unit,
    onBooleanChange: (Boolean) -> Unit,
) {
    if (isReading || isWriting) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = DemoColors.AccentStrong,
            strokeWidth = 2.dp,
        )
        return
    }
    if (definition.valueKind == CapabilityValueKind.BOOLEAN && (hasCurrentValue || !definition.supportsRead)) {
        val checked = draft.valueText.equals("true", ignoreCase = true) || draft.valueText == "1"
        Switch(
            checked = checked,
            onCheckedChange = if (definition.supportsWrite && enabled) onBooleanChange else null,
            enabled = definition.supportsWrite && enabled,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentSummary,
            modifier = Modifier.widthIn(max = 108.dp),
            fontSize = 12.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlighted) DemoColors.AccentStrong else DemoColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!definition.supportsWrite && definition.supportsRead) {
            TextButton(
                onClick = onRead,
                enabled = enabled,
            ) {
                Text(demoStringResource(R.string.read_setting_action), fontSize = 11.sp)
            }
        } else {
            Text(text = "›", fontSize = 24.sp, color = DemoColors.TextSecondary)
        }
    }
}

@Composable
private fun ProductSettingEditorDialog(
    vm: CommandConsoleViewModel,
    item: ProductSettingListItem,
    state: ProductSettingsSectionState,
    draft: ProductSettingDraft,
    options: List<ProductSettingOption>,
    currentSummary: String,
    defaultSummary: String,
    validationError: String?,
    isReading: Boolean,
    isWriting: Boolean,
    highlighted: Boolean,
    onDismiss: () -> Unit,
    onWrite: () -> Unit,
) {
    val definition = item.definition
    val enabled = state.hasReadySession && !state.isExecuting && !isReading && !isWriting
    val operationStatus = when {
        isReading -> demoStringResource(R.string.setting_reading_in_progress)
        isWriting -> demoStringResource(R.string.setting_writing_in_progress)
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .demoTestTag(DemoTestTags.capabilitySettingDialog(definition.entryKey)),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    color = DemoColors.TextPrimary,
                )
                Text(
                    text = listOf(item.domainTitle, item.familyTitle, item.sectionTitle)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" / "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = DemoColors.TextSecondary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingSupportBadgeRow(
                    source = definition.source,
                    supportsRead = definition.supportsRead,
                    supportsWrite = definition.supportsWrite,
                )
                operationStatus?.let { status ->
                    SettingOperationStatus(status)
                }
                SettingDialogSectionLabel(demoStringResource(R.string.setting_value_overview))
                SettingMetaRow(
                    label = demoStringResource(R.string.setting_current_value),
                    value = currentSummary,
                    highlighted = highlighted,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingMetaRow(
                    label = demoStringResource(R.string.setting_default_value),
                    value = defaultSummary,
                    modifier = Modifier.fillMaxWidth(),
                )
                definition.notes.takeIf { it.isNotBlank() }?.let { notes ->
                    SettingDescription(notes)
                }
                if (definition.supportsWrite) {
                    SettingDialogSectionLabel(demoStringResource(R.string.setting_new_value))
                    ProductSettingEditor(
                        definition = definition,
                        draft = draft,
                        options = options,
                        enabled = enabled,
                        validationError = validationError,
                        onDraftChange = { vm.updateSettingDraft(definition.entryKey, it) },
                    )
                }
                if (definition.supportsWrite && definition.valueKind != CapabilityValueKind.ACTION) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = demoStringResource(R.string.persist_setting_write),
                            fontSize = 11.sp,
                            color = DemoColors.TextSecondary,
                        )
                        Switch(
                            checked = draft.persist,
                            onCheckedChange = { vm.updateSettingDraft(definition.entryKey, draft.copy(persist = it)) },
                            enabled = enabled,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (definition.supportsRead) {
                        OutlinedButton(
                            onClick = { vm.readCapabilitySetting(definition) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(demoStringResource(R.string.read_setting_action), fontSize = 12.sp)
                        }
                    }
                    if (definition.supportsWrite) {
                        Button(
                            onClick = onWrite,
                            enabled = enabled && validationError.isNullOrBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = when (definition.valueKind) {
                                    CapabilityValueKind.ACTION -> demoStringResource(R.string.execute_setting_action)
                                    else -> demoStringResource(R.string.write_setting_action)
                                },
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                if (definition.supportsWrite && definition.valueKind != CapabilityValueKind.ACTION) {
                    TextButton(
                        onClick = { vm.resetSettingDraft(definition.entryKey) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(demoStringResource(R.string.reset_setting_draft), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(demoStringResource(R.string.setting_dialog_close))
            }
        },
    )
}

@Composable
private fun compactSettingSupportLabel(definition: CapabilityEntry): String {
    val source = when (definition.source) {
        CapabilityEntrySource.MASTER -> demoStringResource(R.string.setting_source_master_short)
        CapabilityEntrySource.MODULE -> demoStringResource(R.string.setting_source_module_short)
        CapabilityEntrySource.SESSION -> demoStringResource(R.string.setting_source_session_short)
    }
    val support = when {
        definition.supportsRead && definition.supportsWrite ->
            "${demoStringResource(R.string.setting_support_read)} · ${demoStringResource(R.string.setting_support_write)}"
        definition.supportsRead -> demoStringResource(R.string.setting_support_read_only)
        definition.supportsWrite -> demoStringResource(R.string.setting_support_write_only)
        else -> ""
    }
    val risk = if (definition.riskLevel == CapabilityRiskLevel.NORMAL) "" else capabilityRiskLabel(definition.riskLevel)
    return listOf(source, support, risk).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun ProductSettingEditor(
    definition: CapabilityEntry,
    draft: ProductSettingDraft,
    options: List<ProductSettingOption>,
    enabled: Boolean,
    validationError: String?,
    onDraftChange: (ProductSettingDraft) -> Unit,
) {
    when (definition.valueKind) {
        CapabilityValueKind.BOOLEAN -> {
            val checked = draft.valueText.equals("true", ignoreCase = true) || draft.valueText == "1"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = demoStringResource(R.string.setting_boolean_label),
                    fontSize = 13.sp,
                    color = DemoColors.TextPrimary,
                )
                Switch(
                    checked = checked,
                    onCheckedChange = { onDraftChange(draft.copy(valueText = if (it) "true" else "false")) },
                    enabled = enabled,
                )
            }
        }

        CapabilityValueKind.ENUM -> {
            if (options.isEmpty()) {
                OutlinedTextField(
                    value = draft.valueText,
                    onValueChange = { onDraftChange(draft.copy(valueText = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(demoStringResource(R.string.setting_enum_raw_value)) },
                    enabled = enabled,
                    isError = !validationError.isNullOrBlank(),
                    singleLine = true,
                )
                if (!validationError.isNullOrBlank()) {
                    SettingValidationText(validationError)
                }
            } else {
                var menuExpanded by remember(definition.entryKey) { mutableStateOf(false) }
                val selectedOption = options.firstOrNull { it.rawValue == draft.valueText }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { if (enabled) menuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                        ) {
                            Text(formatEnumOptionSummary(selectedOption, draft.valueText))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        menuExpanded = false
                                        onDraftChange(draft.copy(valueText = option.rawValue))
                                    },
                                )
                            }
                        }
                    }
                    if (!validationError.isNullOrBlank()) {
                        SettingValidationText(validationError)
                    }
                }
            }
        }

        CapabilityValueKind.ACTION -> {
            Text(
                text = demoStringResource(R.string.setting_action_description),
                fontSize = 12.sp,
                color = DemoColors.TextSecondary,
            )
        }

        else -> {
            OutlinedTextField(
                value = draft.valueText,
                onValueChange = { onDraftChange(draft.copy(valueText = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(editorLabel(definition.valueKind)) },
                enabled = enabled,
                isError = !validationError.isNullOrBlank(),
                singleLine = definition.valueKind != CapabilityValueKind.BYTES_ASCII,
            )
            if (!validationError.isNullOrBlank()) {
                SettingValidationText(validationError)
            }
        }
    }
}

@Composable
private fun SettingSupportBadgeRow(
    source: CapabilityEntrySource,
    supportsRead: Boolean,
    supportsWrite: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingSupportBadge(
            text = when (source) {
                CapabilityEntrySource.MASTER -> demoStringResource(R.string.setting_source_master_short)
                CapabilityEntrySource.MODULE -> demoStringResource(R.string.setting_source_module_short)
                CapabilityEntrySource.SESSION -> demoStringResource(R.string.setting_source_session_short)
            },
            color = DemoColors.TextSecondary,
            background = DemoColors.SurfaceMuted,
        )
        when {
            supportsRead && supportsWrite -> {
                SettingSupportBadge(
                    text = demoStringResource(R.string.setting_support_read),
                    color = DemoColors.AccentStrong,
                    background = DemoColors.SurfaceAccent,
                )
                SettingSupportBadge(
                    text = demoStringResource(R.string.setting_support_write),
                    color = DemoColors.Accent,
                    background = DemoColors.SurfaceMuted,
                )
            }

            supportsRead -> SettingSupportBadge(
                text = demoStringResource(R.string.setting_support_read_only),
                color = DemoColors.AccentStrong,
                background = DemoColors.SurfaceAccent,
            )

            supportsWrite -> SettingSupportBadge(
                text = demoStringResource(R.string.setting_support_write_only),
                color = DemoColors.Accent,
                background = DemoColors.SurfaceMuted,
            )
        }
    }
}

@Composable
private fun SettingSupportBadge(
    text: String,
    color: Color,
    background: Color,
) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(background, DemoShapes.chip)
            .border(androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)), DemoShapes.chip)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun SettingMetaRow(
    label: String,
    value: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (highlighted) DemoColors.AccentStrong else DemoColors.TextTertiary,
        )
        Text(
            text = value,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (highlighted) DemoColors.SurfaceAccent else DemoColors.SurfaceMuted,
                    DemoShapes.panel,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 13.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = DemoColors.TextPrimary,
        )
    }
}

@Composable
private fun SettingDialogSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = DemoColors.TextSecondary,
    )
}

@Composable
private fun SettingOperationStatus(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.SurfaceAccent, DemoShapes.panel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = DemoColors.AccentStrong,
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DemoColors.AccentStrong,
        )
    }
}

@Composable
private fun SettingDescription(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline), DemoShapes.panel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingDialogSectionLabel(demoStringResource(R.string.setting_description))
        Text(
            text = text,
            fontSize = 12.sp,
            color = DemoColors.TextSecondary,
        )
    }
}

@Composable
private fun SettingValidationText(message: String) {
    Text(
        text = message,
        fontSize = 11.sp,
        color = DemoColors.Danger,
    )
}

@Composable
private fun editorLabel(kind: CapabilityValueKind): String {
    return when (kind) {
        CapabilityValueKind.UINT8 -> demoStringResource(R.string.setting_uint8_label)
        CapabilityValueKind.UINT16 -> demoStringResource(R.string.setting_uint16_label)
        CapabilityValueKind.BYTES_ASCII -> demoStringResource(R.string.setting_ascii_label)
        CapabilityValueKind.CUSTOM,
        CapabilityValueKind.COMPLEX,
        CapabilityValueKind.OBJECT,
        CapabilityValueKind.TEMPLATE -> demoStringResource(R.string.setting_hex_label)
        else -> demoStringResource(R.string.setting_value_label)
    }
}

private fun formatEnumOptionSummary(
    selectedOption: ProductSettingOption?,
    rawValue: String,
): String {
    return when {
        selectedOption != null -> selectedOption.label
        rawValue.isNotBlank() -> rawValue
        else -> DemoStrings.text(R.string.setting_enum_raw_value)
    }
}
