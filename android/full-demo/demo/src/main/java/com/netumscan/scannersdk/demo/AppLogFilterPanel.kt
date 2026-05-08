package com.netumscan.scannersdk.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes

@Composable
internal fun AppLogFilterCard(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedSourceNames: Set<String>,
    onToggleSource: (AppLogSourceFilter) -> Unit,
    levelFilter: AppLogLevelFilter,
    onLevelFilterChange: (AppLogLevelFilter) -> Unit,
    exportFilteredEnabled: Boolean,
    exportAllEnabled: Boolean,
    onExportFiltered: () -> Unit,
    onExportAll: () -> Unit,
    onExportCompatibilityRecord: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.card,
        colors = CardDefaults.cardColors(containerColor = DemoColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = demoStringResource(R.string.global_logs),
                modifier = Modifier.demoTestTag(DemoTestTags.APP_LOG_FILTER_TITLE),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary
            )
            Text(
                text = demoStringResource(R.string.global_logs_description),
                fontSize = 13.sp,
                color = DemoColors.TextSecondary
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .demoTestTag(DemoTestTags.APP_LOG_SEARCH_FIELD),
                label = { Text(demoStringResource(R.string.search_message_text)) },
                singleLine = true
            )
            FilterSection(
                title = demoStringResource(R.string.source),
                labels = AppLogSourceFilter.entries.map(::sourceFilterLabel),
                selectedIndices = AppLogSourceFilter.entries.mapIndexedNotNull { index, filter ->
                    index.takeIf { filter.name in selectedSourceNames }
                }.toSet(),
                testTagForIndex = { index -> DemoTestTags.appLogSourceChip(AppLogSourceFilter.entries[index]) },
                onToggle = { index ->
                    onToggleSource(AppLogSourceFilter.entries[index])
                }
            )
            FilterSection(
                title = demoStringResource(R.string.level),
                labels = AppLogLevelFilter.entries.map(::levelFilterLabel),
                selectedIndices = setOf(levelFilter.ordinal),
                onToggle = { onLevelFilterChange(AppLogLevelFilter.entries[it]) }
            )
            AppLogActionRow(
                exportFilteredEnabled = exportFilteredEnabled,
                exportAllEnabled = exportAllEnabled,
                onExportFiltered = onExportFiltered,
                onExportAll = onExportAll,
                onExportCompatibilityRecord = onExportCompatibilityRecord,
                onClear = onClear
            )
        }
    }
}

@Composable
internal fun AppLogFilterSummaryCard(
    filteredCount: Int,
    totalCount: Int,
    selectedSources: Set<AppLogSourceFilter>,
    levelFilter: AppLogLevelFilter,
    query: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.panel,
        colors = CardDefaults.cardColors(containerColor = DemoColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = demoStringResource(R.string.filter_summary),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary
            )
            Text(
                text = demoStringResource(
                    R.string.filter_summary_format,
                    filteredCount,
                    totalCount,
                    sourceFilterSummary(selectedSources),
                    levelFilterLabel(levelFilter)
                ),
                fontSize = 12.sp,
                color = DemoColors.TextSecondary
            )
            if (query.isNotBlank()) {
                Text(
                    text = demoStringResource(R.string.search_keyword_format, query),
                    fontSize = 12.sp,
                    color = DemoColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AppLogActionRow(
    exportFilteredEnabled: Boolean,
    exportAllEnabled: Boolean,
    onExportFiltered: () -> Unit,
    onExportAll: () -> Unit,
    onExportCompatibilityRecord: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportFiltered,
                enabled = exportFilteredEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(demoStringResource(R.string.export_filtered))
            }
            OutlinedButton(
                onClick = onExportAll,
                enabled = exportAllEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(demoStringResource(R.string.export_all))
            }
        }
        OutlinedButton(
            onClick = onExportCompatibilityRecord,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(demoStringResource(R.string.export_compatibility_record))
        }
        Button(
            onClick = onClear,
            modifier = Modifier
                .fillMaxWidth()
                .demoTestTag(DemoTestTags.APP_LOG_CLEAR_BUTTON)
        ) {
            Text(demoStringResource(R.string.clear_global_logs))
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    labels: List<String>,
    selectedIndices: Set<Int>,
    testTagForIndex: ((Int) -> String)? = null,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DemoColors.TextPrimary
        )
        labels.chunked(3).forEachIndexed { chunkIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { offset, label ->
                    val index = chunkIndex * 3 + offset
                    DemoSelectableChipButton(
                        label = label,
                        selected = index in selectedIndices,
                        onClick = { onToggle(index) },
                        modifier = Modifier.weight(1f),
                        testTag = testTagForIndex?.invoke(index),
                        unselectedBorder = BorderStroke(1.dp, DemoColors.Outline),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
