package com.netumscan.scannersdk.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes

@Composable
internal fun ExpandableSectionHeader(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.SurfaceMuted, DemoShapes.panel)
            .clickable(onClick = onToggle)
            .demoTestTag(testTag)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            subtitle?.let {
                Text(text = it, fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
        }
        Text(
            text = if (expanded) {
                DemoStrings.text(R.string.collapse)
            } else {
                DemoStrings.text(R.string.expand)
            },
            fontSize = 12.sp,
            color = DemoColors.AccentStrong,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun CommandFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    DemoSelectableChipButton(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        testTag = testTag,
        selectedColors = ButtonDefaults.buttonColors(
            containerColor = DemoColors.SurfaceBrand,
            contentColor = Color.White,
        ),
        unselectedBorder = androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline),
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun GroupedCommandCard(
    title: String,
    summary: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.panel,
        colors = CardDefaults.cardColors(containerColor = DemoColors.SurfaceMuted),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                Text(text = summary, fontSize = 11.sp, color = DemoColors.TextTertiary)
            }
            content()
        }
    }
}

@Composable
internal fun CollapsibleGroupedCommandCard(
    title: String,
    summary: String,
    danger: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    headerTestTag: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.panel,
        colors = CardDefaults.cardColors(containerColor = DemoColors.SurfaceMuted),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .demoTestTag(headerTestTag)
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                        if (danger) {
                            Text(
                                text = DemoStrings.text(R.string.high_risk),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DemoColors.Danger,
                                modifier = Modifier
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Danger), DemoShapes.chip)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(text = summary, fontSize = 11.sp, color = DemoColors.TextTertiary)
                }
                Text(
                    text = if (expanded) {
                        DemoStrings.text(R.string.collapse)
                    } else {
                        DemoStrings.text(R.string.expand)
                    },
                    fontSize = 12.sp,
                    color = DemoColors.AccentStrong,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
internal fun ConsoleStatusCard(
    statusSummary: String,
    deviceSummary: String,
    selectedModelSummary: String,
    sdkResolvedModelSummary: String,
    capabilitySummary: String,
    diagnosticsSummary: String,
    lastActionResult: String?,
    errorMessage: String?,
    isExecuting: Boolean,
    onDisconnect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.card,
        colors = CardDefaults.cardColors(containerColor = DemoColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DemoStrings.text(R.string.current_status),
                        modifier = Modifier.demoTestTag(DemoTestTags.CONSOLE_STATUS_TITLE),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DemoColors.TextPrimary,
                    )
                    Text(
                        text = DemoStrings.text(R.string.connection_model_capability_summary),
                        fontSize = 11.sp,
                        color = DemoColors.TextSecondary,
                    )
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    enabled = !isExecuting,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Danger),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DemoColors.Danger,
                        disabledContentColor = DemoColors.Danger.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(DemoStrings.text(R.string.disconnect))
                }
            }

            ConsoleStatusChip(
                modifier = Modifier.fillMaxWidth(),
                label = DemoStrings.text(R.string.status),
                value = statusSummary,
            )

            ConsoleStatusChip(
                modifier = Modifier.fillMaxWidth(),
                label = DemoStrings.text(R.string.device_selected_model),
                value = "$deviceSummary\n$selectedModelSummary",
            )

            ConsoleStatusChip(
                modifier = Modifier.fillMaxWidth(),
                label = DemoStrings.text(R.string.capability_sdk_resolved),
                value = "$capabilitySummary\n$sdkResolvedModelSummary",
            )

            ConsoleStatusChip(
                modifier = Modifier.fillMaxWidth(),
                label = DemoStrings.text(R.string.diagnostics),
                value = diagnosticsSummary,
            )

            when {
                errorMessage != null -> ConsoleFeedbackBanner(text = errorMessage, tone = ConsoleBannerTone.Error)
                lastActionResult != null -> ConsoleFeedbackBanner(text = lastActionResult, tone = ConsoleBannerTone.Success)
            }
        }
    }
}

@Composable
private fun ConsoleStatusChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    DemoLabeledValueBlock(
        label = label,
        value = value,
        modifier = modifier,
        valueLineHeight = 18.sp,
    )
}

internal data class CommandButtonAction(
    val label: String,
    val enabled: Boolean,
    val action: () -> Unit,
    val isDangerous: Boolean = false,
)

internal fun commandButtonAction(
    label: String,
    enabled: Boolean,
    action: () -> Unit,
): CommandButtonAction = CommandButtonAction(label, enabled, action)

internal fun commandButtonAction(
    label: String,
    enabled: Boolean,
    isDangerous: Boolean,
    action: () -> Unit,
): CommandButtonAction = CommandButtonAction(label, enabled, action, isDangerous)

@Composable
internal fun CommandButtonRow(
    actions: List<CommandButtonAction>,
    testTagForLabel: (String) -> String? = { null },
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { buttonAction ->
            if (buttonAction.isDangerous) {
                Button(
                    onClick = buttonAction.action,
                    enabled = buttonAction.enabled,
                    modifier = Modifier.weight(1f).demoTestTag(testTagForLabel(buttonAction.label)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.SurfaceDanger,
                        contentColor = DemoColors.Danger,
                        disabledContainerColor = DemoColors.SurfaceDanger.copy(alpha = 0.55f),
                        disabledContentColor = DemoColors.Danger.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(buttonAction.label)
                }
            } else {
                OutlinedButton(
                    onClick = buttonAction.action,
                    enabled = buttonAction.enabled,
                    modifier = Modifier.weight(1f).demoTestTag(testTagForLabel(buttonAction.label)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline),
                ) {
                    Text(buttonAction.label, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
internal fun ParseStateCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = DemoShapes.card,
        colors = CardDefaults.cardColors(containerColor = DemoColors.SurfaceMuted),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
            )
            content()
        }
    }
}

@Composable
internal fun <T> CommandGrid(
    commands: List<Pair<String, T>>,
    enabled: Boolean,
    isDangerous: (T) -> Boolean,
    onClick: (String, T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        commands.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (label, command) ->
                    val dangerous = isDangerous(command)
                    if (dangerous) {
                        Button(
                            onClick = { onClick(label, command) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DemoColors.SurfaceDanger,
                                contentColor = DemoColors.Danger,
                                disabledContainerColor = DemoColors.SurfaceDanger.copy(alpha = 0.55f),
                                disabledContentColor = DemoColors.Danger.copy(alpha = 0.6f),
                            ),
                        ) {
                            Text("! $label")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onClick(label, command) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DemoColors.Outline),
                        ) {
                            Text(label, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun DangerDialog(
    label: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = demoStringResource(R.string.confirm_execution),
                modifier = Modifier.demoTestTag(DemoTestTags.CONSOLE_DANGER_DIALOG_TITLE),
            )
        },
        text = { Text("$label\n\n$message") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.demoTestTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CONFIRM),
            ) {
                Text(demoStringResource(R.string.continue_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.demoTestTag(DemoTestTags.CONSOLE_DANGER_DIALOG_CANCEL),
            ) {
                Text(demoStringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun ConsoleHint(text: String) {
    DemoHintCard(text = text)
}

internal enum class ConsoleBannerTone {
    Success,
    Error,
}

@Composable
internal fun ConsoleFeedbackBanner(
    text: String,
    tone: ConsoleBannerTone,
) {
    val background = when (tone) {
        ConsoleBannerTone.Success -> DemoColors.SurfaceAccent
        ConsoleBannerTone.Error -> DemoColors.SurfaceDanger
    }
    val foreground = when (tone) {
        ConsoleBannerTone.Success -> DemoColors.TextPrimary
        ConsoleBannerTone.Error -> DemoColors.Danger
    }
    DemoFeedbackBanner(
        text = text,
        backgroundColor = background,
        textColor = foreground,
    )
}
