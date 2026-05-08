package com.netumscan.scannersdk.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes
import kotlinx.coroutines.launch

@Composable
internal fun AppLogRoute(
    onBack: () -> Unit,
    autoScrollToLatest: Boolean = true,
) {
    val allEvents by AppLogStore.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentLanguage = DemoLocaleController.currentLanguage
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    var selectedSourceNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    var levelFilter by rememberSaveable { mutableStateOf(AppLogLevelFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }

    val selectedSources = AppLogSourceFilter.entries.filter { it.name in selectedSourceNames }.toSet()
    val filteredEvents = remember(allEvents, selectedSourceNames, levelFilter, query, currentLanguage) {
        val trimmedQuery = query.trim()
        allEvents.filter { event ->
            matchesSource(event, selectedSources) &&
                matchesLevel(event, levelFilter) &&
                (
                    trimmedQuery.isBlank() ||
                        event.message.contains(trimmedQuery, ignoreCase = true) ||
                        event.source.name.contains(trimmedQuery, ignoreCase = true) ||
                        event.level.name.contains(trimmedQuery, ignoreCase = true) ||
                        sourceLabel(event.source).contains(trimmedQuery, ignoreCase = true) ||
                        levelLabel(event.level).contains(trimmedQuery, ignoreCase = true)
                    )
        }
    }

    LaunchedEffect(filteredEvents.size, autoScrollToLatest) {
        if (autoScrollToLatest && filteredEvents.isNotEmpty()) {
            listState.animateScrollToItem(filteredEvents.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.Page)
    ) {
        DemoTopBar(
            title = demoStringResource(R.string.app_logs),
            context = context,
            onBack = onBack,
            showLogsEntry = false,
            titleTestTag = DemoTestTags.APP_LOG_TOP_BAR_TITLE,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .demoTestTag(DemoTestTags.APP_LOG_LIST)
                    .navigationBarsPadding(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppLogFilterCard(
                        query = query,
                        onQueryChange = { query = it },
                        selectedSourceNames = selectedSourceNames,
                        onToggleSource = { filter ->
                            selectedSourceNames = selectedSourceNames.toggle(filter.name)
                        },
                        levelFilter = levelFilter,
                        onLevelFilterChange = { levelFilter = it },
                        exportFilteredEnabled = filteredEvents.isNotEmpty(),
                        exportAllEnabled = allEvents.isNotEmpty(),
                        onExportFiltered = {
                            shareDebugEvents(
                                context = context,
                                title = DemoStrings.fromContext(context, R.string.app_logs),
                                summaryLines = listOf(
                                    "${DemoStrings.fromContext(context, R.string.source_filter)}: ${sourceFilterSummary(selectedSources)}",
                                    "${DemoStrings.fromContext(context, R.string.level_filter)}: ${levelFilterLabel(levelFilter)}",
                                    "${DemoStrings.fromContext(context, R.string.search)}: ${query.ifBlank { DemoStrings.fromContext(context, R.string.none) }}",
                                    "${DemoStrings.fromContext(context, R.string.event_count)}: ${filteredEvents.size}",
                                ),
                                events = filteredEvents
                            )
                        },
                        onExportAll = {
                            shareDebugEvents(
                                context = context,
                                title = DemoStrings.fromContext(context, R.string.app_logs),
                                summaryLines = listOf(
                                    "${DemoStrings.fromContext(context, R.string.export_scope)}: ${DemoStrings.fromContext(context, R.string.all_logs)}",
                                    "${DemoStrings.fromContext(context, R.string.event_count)}: ${allEvents.size}",
                                ),
                                events = allEvents
                            )
                        },
                        onExportCompatibilityRecord = {
                            shareTextPayload(
                                context = context,
                                title = DemoStrings.fromContext(context, R.string.compatibility_record),
                                payload = buildString {
                                    appendLine(DemoStrings.fromContext(context, R.string.compatibility_record))
                                    appendLine("${DemoStrings.fromContext(context, R.string.exported_at)}: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}")
                                    appendLine()
                                    append(DemoCompatibilityRecord.format())
                                },
                            )
                        },
                        onClear = { AppLogStore.clear() }
                    )
                }
                item {
                    AppLogFilterSummaryCard(
                        filteredCount = filteredEvents.size,
                        totalCount = allEvents.size,
                        selectedSources = selectedSources,
                        levelFilter = levelFilter,
                        query = query
                    )
                }
                if (filteredEvents.isEmpty()) {
                    item {
                        AppLogEmptyStateCard()
                    }
                } else {
                    itemsIndexed(
                        items = filteredEvents,
                        key = { index, event -> "${event.timestampMs}-${event.source}-${event.level}-$index" }
                    ) { _, event ->
                        AppLogEventRow(event)
                    }
                }
            }

            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .size(width = 96.dp, height = 52.dp),
                    containerColor = DemoColors.SurfaceBrand,
                    contentColor = Color.White,
                    shape = DemoShapes.panel
                ) {
                    Text(
                        text = demoStringResource(R.string.top),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
