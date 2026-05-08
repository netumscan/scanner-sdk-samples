package com.netumscan.scannersdk.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors
import com.netumscan.scannersdk.demo.ui.theme.DemoShapes

@Composable
internal fun AppLogEmptyStateCard() {
    DemoHintCard(
        text = demoStringResource(R.string.no_logs_match),
        modifier = Modifier.demoTestTag(DemoTestTags.APP_LOG_EMPTY_STATE),
        shape = DemoShapes.panel,
        showBorder = false,
    )
}

@Composable
internal fun AppLogEventRow(event: DebugEvent) {
    val sourceColor = when (event.source) {
        DebugEventSource.UI -> DemoColors.Accent
        DebugEventSource.SDK -> DemoColors.TextSecondary
        DebugEventSource.CORE -> Color(0xFF3D6B4F)
        DebugEventSource.BLE -> Color(0xFF2F5D7C)
        DebugEventSource.SESSION -> Color(0xFF7A5C2E)
        DebugEventSource.SCAN -> Color(0xFF7C4D2F)
        DebugEventSource.COMMAND -> Color(0xFF5A3E7A)
    }
    val levelColor = when (event.level) {
        DebugEventLevel.Debug -> DemoColors.TextTertiary
        DebugEventLevel.Info -> DemoColors.TextSecondary
        DebugEventLevel.Warn -> Color(0xFF8A6A00)
        DebugEventLevel.Error -> DemoColors.Danger
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DemoShapes.panel,
        colors = CardDefaults.cardColors(containerColor = DemoColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sourceLabel(event.source),
                    modifier = Modifier
                        .background(sourceColor, DemoShapes.chip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = Color.White
                )
                Text(
                    text = levelLabel(event.level),
                    fontSize = 10.sp,
                    color = levelColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(text = event.message, fontSize = 13.sp, color = DemoColors.TextPrimary)
        }
    }
}
