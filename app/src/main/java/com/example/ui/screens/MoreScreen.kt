package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToTasbeeh: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPrayers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المزيد", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        MoreMenuItem(
                            title = "المسبحة الإلكترونية",
                            subtitle = "عداد التسبيح والأذكار المخصصة",
                            icon = Icons.Default.Fingerprint,
                            onClick = onNavigateToTasbeeh
                        )
                        HorizontalDivider()
                        MoreMenuItem(
                            title = "الأدعية المأثورة",
                            subtitle = "أدعية من القرآن والسنة النبوية",
                            icon = Icons.Default.FavoriteBorder,
                            onClick = onNavigateToDuas
                        )
                        HorizontalDivider()
                        MoreMenuItem(
                            title = "التقويم الهجري والمناسبات",
                            subtitle = "المناسبات الإسلامية والتحويل التاريخي",
                            icon = Icons.Default.CalendarMonth,
                            onClick = onNavigateToCalendar
                        )
                        HorizontalDivider()
                        MoreMenuItem(
                            title = "المفضلة",
                            subtitle = "الآيات والأذكار والأدعية المحفوظة",
                            icon = Icons.Default.BookmarkBorder,
                            onClick = onNavigateToFavorites
                        )
                        HorizontalDivider()
                        MoreMenuItem(
                            title = "تفاصيل مواقيت الصلاة",
                            subtitle = "جدول الأوقات وتنبيهات الأذان",
                            icon = Icons.Default.AccessTime,
                            onClick = onNavigateToPrayers
                        )
                        HorizontalDivider()
                        MoreMenuItem(
                            title = "الإعدادات العامة",
                            subtitle = "الموقع، طرق الحساب، التنبيهات، والمظهر",
                            icon = Icons.Default.Settings,
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }

            // AdMob Banner Placement (إعلان البانر في شاشة المزيد)
            item {
                com.example.ads.AdMobBannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
