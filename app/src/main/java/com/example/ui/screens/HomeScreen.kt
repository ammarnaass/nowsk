package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.QuranProgressEntity
import com.example.data.local.entity.TasbeehEntity
import com.example.ui.NusakkirUiState
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SoftGold

@Composable
fun HomeScreen(
    uiState: NusakkirUiState,
    quranProgress: QuranProgressEntity?,
    tasbeehList: List<TasbeehEntity>,
    onPrayerDetailsClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToQuran: () -> Unit,
    onNavigateToMorningAdhkar: () -> Unit,
    onNavigateToEveningAdhkar: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToTasbeeh: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onToggleMorningDone: () -> Unit,
    onToggleEveningDone: () -> Unit,
    onStopAdhan: () -> Unit = {},
    onRefreshFromApi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hijriFullText = "${uiState.todayHijri.dayNameAr} ${uiState.todayHijri.day} ${uiState.todayHijri.monthNameAr} ${uiState.todayHijri.year} هـ"

    val totalTasbeeh = tasbeehList.sumOf { it.count }
    val targetTasbeeh = if (tasbeehList.isNotEmpty()) tasbeehList.sumOf { it.target } else 300

    Scaffold(
        topBar = {
            AppHeader(
                cityName = uiState.selectedCity.nameAr,
                hijriDateText = hijriFullText,
                gregorianDateText = uiState.todayGregorian,
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onLocationClick = onLocationClick
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline banner (PRD Section 21)
            if (uiState.aladhanSyncSuccess == false) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "أنت الآن بدون اتصال",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "نعرض آخر البيانات المحفوظة بدقة.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onRefreshFromApi,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إعادة المحاولة", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Active Adhan Audio Alert Banner
            if (uiState.isAdhanPlaying) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "صوت الأذان يرفع الآن",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = uiState.activeAdhanPrayer?.let { "أذان $it" } ?: "حيّ على الصلاة، حيّ على الفلاح",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Button(
                                onClick = onStopAdhan,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إيقاف")
                            }
                        }
                    }
                }
            }

            // 1. Next Prayer Countdown Card
            item {
                NextPrayerCountdownCard(
                    schedule = uiState.prayerSchedule,
                    countdownString = uiState.countdownFormatted,
                    onDetailsClick = onPrayerDetailsClick
                )
            }

            // 2. All Daily Prayer Times Row
            item {
                PrayerTimeRowCard(
                    schedule = uiState.prayerSchedule
                )
            }

            // 3. Quick Actions Header & Grid (6 primary shortcuts)
            item {
                Column {
                    Text(
                        text = "الوصول السريع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "القرآن الكريم",
                            icon = Icons.Default.MenuBook,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToQuran,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "أذكار الصباح",
                            icon = Icons.Default.WbSunny,
                            containerColor = SoftGold,
                            contentColor = GoldAccent,
                            onClick = onNavigateToMorningAdhkar,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "أذكار المساء",
                            icon = Icons.Default.NightsStay,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onNavigateToEveningAdhkar,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "اتجاه القبلة",
                            icon = Icons.Default.Explore,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToQibla,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "المسبحة",
                            icon = Icons.Default.Fingerprint,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onNavigateToTasbeeh,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "الأدعية",
                            icon = Icons.Default.FavoriteBorder,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToDuas,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Daily Progress ("وردك اليوم")
            item {
                DailyProgressCard(
                    isMorningDone = uiState.isMorningAdhkarDone,
                    isEveningDone = uiState.isEveningAdhkarDone,
                    tasbeehCount = totalTasbeeh,
                    tasbeehTarget = targetTasbeeh,
                    quranPages = uiState.dailyQuranPages,
                    onMorningClick = onToggleMorningDone,
                    onEveningClick = onToggleEveningDone,
                    onQuranClick = onNavigateToQuran,
                    onTasbeehClick = onNavigateToTasbeeh
                )
            }

            // 5. Quran Last Read Shortcut Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToQuran() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "متابعة القراءة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سورة ${quranProgress?.surahName ?: "الفاتحة"} — الآية ${quranProgress?.ayahNumber ?: 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "قراءة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 6. Islamic Spiritual Quote
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "سورة الرعد: الآية 28",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7. AdMob Banner Placement (إعلان البانر في أسفل الشاشة الرئيسية)
            item {
                com.example.ads.AdMobBannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
