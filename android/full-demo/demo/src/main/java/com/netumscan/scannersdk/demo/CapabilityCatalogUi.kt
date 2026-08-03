package com.netumscan.scannersdk.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes
import com.netumscan.scannersdk.model.CapabilityEntry
import com.netumscan.scannersdk.model.CapabilityEntryKind
import com.netumscan.scannersdk.model.CapabilityRiskLevel

@Composable
internal fun CapabilityDomainList(
    catalog: List<CapabilityCatalogDomain>,
    isLoading: Boolean,
    errorText: String?,
    onRetry: () -> Unit,
    onOpenDomain: (CapabilityCatalogDomain) -> Unit,
) {
    when {
        isLoading -> CapabilityCatalogLoading()
        errorText != null -> CapabilityCatalogError(errorText, onRetry)
        catalog.isEmpty() -> ConsoleHint(demoStringResource(R.string.capability_catalog_empty))
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            catalog.forEach { domain ->
                CapabilityDomainCard(domain = domain, onClick = { onOpenDomain(domain) })
            }
        }
    }
}

@Composable
private fun CapabilityCatalogLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = DemoColors.AccentStrong,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = demoStringResource(R.string.capability_catalog_loading),
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CapabilityCatalogError(errorText: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConsoleFeedbackBanner(text = errorText, tone = ConsoleBannerTone.Error)
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(demoStringResource(R.string.retry))
        }
    }
}

@Composable
private fun CapabilityDomainCard(
    domain: CapabilityCatalogDomain,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(DemoColors.Surface, DemoShapes.panel)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline.copy(alpha = 0.55f)),
                DemoShapes.panel,
            )
            .clickable(onClick = onClick)
            .demoTestTag(DemoTestTags.capabilityDomain(domain.definition.key))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DemoColors.SurfaceAccent, DemoShapes.panel),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_demo_settings),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                colorFilter = ColorFilter.tint(DemoColors.AccentStrong),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = domain.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = demoStringResource(
                    R.string.capability_domain_summary,
                    domain.readableCount,
                    domain.writableCount,
                    domain.actionCount,
                ),
                fontSize = 11.sp,
                color = DemoColors.TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = demoStringResource(R.string.capability_item_count, domain.items.size),
            modifier = Modifier
                .background(DemoColors.SurfaceAccent, DemoShapes.chip)
                .padding(horizontal = 9.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = DemoColors.AccentStrong,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "›", fontSize = 26.sp, color = DemoColors.TextSecondary)
    }
}

@Composable
internal fun CapabilityDomainDetailContent(
    vm: CommandConsoleViewModel,
    domain: CapabilityCatalogDomain,
    settingsState: ProductSettingsSectionState,
    actionInputs: Map<String, String>,
    onActionInputChange: (String, String) -> Unit,
    onPendingAction: (CapabilityEntry) -> Unit,
) {
    val readableSettings = domain.items
        .map { it.definition }
        .filter { it.kind == CapabilityEntryKind.SETTING && it.supportsRead }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = demoStringResource(
                R.string.capability_domain_detail_summary,
                domain.items.size,
                domain.readableCount,
                domain.writableCount,
                domain.actionCount,
            ),
            fontSize = 12.sp,
            color = DemoColors.TextSecondary,
        )
        if (readableSettings.isNotEmpty()) {
            OutlinedButton(
                onClick = { vm.readAllReadableSettings(readableSettings) },
                enabled = settingsState.hasReadySession && !settingsState.isExecuting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(demoStringResource(R.string.read_domain_settings))
            }
        }
        if (!settingsState.hasReadySession) {
            ConsoleFeedbackBanner(
                text = demoStringResource(R.string.settings_requires_ready_session),
                tone = ConsoleBannerTone.Error,
            )
        }
    }

    domain.sections.forEach { section ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = listOf(section.familyTitle, section.sectionTitle)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" / "),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DemoColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val settingItems = section.items.filter { it.definition.kind == CapabilityEntryKind.SETTING }
        val actionItems = section.items.filter { it.definition.kind == CapabilityEntryKind.ACTION }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settingItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DemoColors.Surface, DemoShapes.panel)
                        .border(
                            androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline.copy(alpha = 0.65f)),
                            DemoShapes.panel,
                        )
                        .demoTestTag(DemoTestTags.capabilitySettingGroup(section.familyKey, section.sectionKey)),
                ) {
                    settingItems.forEachIndexed { index, item ->
                        ProductSettingItemRow(
                            vm = vm,
                            item = item.toProductSettingListItem(domain),
                            state = settingsState,
                        )
                        if (index != settingItems.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .height(1.dp)
                                    .background(DemoColors.Outline.copy(alpha = 0.6f)),
                            )
                        }
                    }
                }
            }
            actionItems.forEach { item ->
                CapabilityActionItemCard(
                    vm = vm,
                    item = item,
                    hasReadySession = settingsState.hasReadySession,
                    isExecuting = settingsState.isExecuting,
                    inputValue = actionInputs[item.definition.entryKey].orEmpty(),
                    onInputChange = { onActionInputChange(item.definition.entryKey, it) },
                    onPendingAction = onPendingAction,
                )
            }
        }
    }
}

@Composable
private fun CapabilityActionItemCard(
    vm: CommandConsoleViewModel,
    item: CapabilityCatalogItem,
    hasReadySession: Boolean,
    isExecuting: Boolean,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onPendingAction: (CapabilityEntry) -> Unit,
) {
    val definition = item.definition
    val enabled = hasReadySession && !isExecuting && definition.supportsExecute
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.SurfaceMuted, DemoShapes.panel)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline.copy(alpha = 0.45f)),
                DemoShapes.panel,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                )
                Text(
                    text = capabilityRiskLabel(definition.riskLevel),
                    fontSize = 11.sp,
                    color = if (definition.riskLevel == CapabilityRiskLevel.NORMAL) {
                        DemoColors.TextSecondary
                    } else {
                        DemoColors.Danger
                    },
                )
            }
            Text(
                text = when (definition.source) {
                    com.netumscan.scannersdk.model.CapabilityEntrySource.MASTER -> demoStringResource(R.string.setting_source_master_short)
                    com.netumscan.scannersdk.model.CapabilityEntrySource.MODULE -> demoStringResource(R.string.setting_source_module_short)
                    com.netumscan.scannersdk.model.CapabilityEntrySource.SESSION -> demoStringResource(R.string.setting_source_session_short)
                },
                modifier = Modifier
                    .background(DemoColors.Surface, DemoShapes.chip)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 10.sp,
                color = DemoColors.TextSecondary,
            )
        }
        if (definition.requiresValue) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(deviceActionValueHint(definition)) },
                singleLine = true,
                enabled = enabled,
            )
        }
        definition.notes.takeIf { it.isNotBlank() }?.let { ConsoleHint(it) }
        Button(
            onClick = {
                if (definition.riskLevel == CapabilityRiskLevel.NORMAL) {
                    vm.executeCapabilityAction(definition, inputValue)
                } else {
                    onPendingAction(definition)
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(demoStringResource(R.string.execute_device_action))
        }
    }
}
