package com.netumscan.scannersdk.demo

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netumscan.scannersdk.demo.ui.theme.DemoColors

@Composable
fun DemoTopBar(
    title: String,
    context: Context,
    onBack: (() -> Unit)? = null,
    showLogsEntry: Boolean = true,
    subtitle: String? = demoStringResource(R.string.app_name),
    titleTestTag: String? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = DemoColors.Page
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.Page)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(R.drawable.ic_demo_back),
                            contentDescription = demoStringResource(R.string.back),
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(DemoColors.TextPrimary)
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = if (onBack == null) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.demoTestTag(titleTestTag),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DemoColors.TextPrimary
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = DemoColors.TextSecondary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.demoTestTag(DemoTestTags.TOP_BAR_MENU),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_demo_settings),
                        contentDescription = demoStringResource(R.string.settings),
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(DemoColors.TextPrimary)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DemoLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    DemoStrings.fromContext(
                                        context,
                                        R.string.select_language,
                                        DemoLocaleController.languageLabel(language)
                                    )
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                DemoLocaleController.setLanguage(context, language)
                            },
                            enabled = language != DemoLocaleController.currentLanguage
                        )
                    }
                    if (showLogsEntry) {
                        DropdownMenuItem(
                            modifier = Modifier.demoTestTag(DemoTestTags.APP_LOG_MENU_ITEM),
                            text = { Text(demoStringResource(R.string.app_logs)) },
                            onClick = {
                                menuExpanded = false
                                context.startActivity(Intent(context, AppLogActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }
}
