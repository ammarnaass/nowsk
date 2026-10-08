package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prayer.PrayerSchedule
import com.example.data.prayer.PrayerType
import com.example.ui.theme.*

/**
 * Islamic Andalusian Ornamental Divider with 8-pointed star (۞) and gold hairline gradients
 */
@Composable
fun IslamicMotifDivider(
    modifier: Modifier = Modifier,
    starSymbol: String = "۞"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, GoldAccent.copy(alpha = 0.5f))
                    )
                )
        )
        Text(
            text = " ❖ $starSymbol ❖ ",
            color = GoldAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GoldAccent.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Modern Islamic App Header with Regal Emerald, Gold Location Chip, and Hijri Crescent Badge
 */
@Composable
fun AppHeader(
    cityName: String,
    hijriDateText: String,
    gregorianDateText: String,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand and Location
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLocationClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "نُسُك",
                            style = IslamicDisplayTitle.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الإسلامي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        // Gilded Location Chip
                        Surface(
                            color = GoldAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "الموقع",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cityName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "تغيير المدينة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date Pill with Hijri Moon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌙 $hijriDateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  $gregorianDateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Action Buttons (Search & Settings)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_header_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_header_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Architectural Mihrab Hero Card for the Next Prayer and Live Countdown
 */
@Composable
fun NextPrayerCountdownCard(
    schedule: PrayerSchedule?,
    countdownString: String,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextPrayer = schedule?.nextPrayer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_next_prayer"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    GoldAccent.copy(alpha = 0.6f),
                    GoldAccent.copy(alpha = 0.15f),
                    GoldAccent.copy(alpha = 0.5f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MihrabGreenTop,
                            MihrabGreenBottom
                        )
                    )
                )
                .clickable { onDetailsClick() }
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header inside Mihrab Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "۞  الصلاة القادمة",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccentBright,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nextPrayer?.timeFormatted ?: "--:--",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Prayer Name in Regal Arabic Calligraphy
                Text(
                    text = nextPrayer?.type?.arabicName ?: "جاري الحساب...",
                    style = IslamicDisplayTitle.copy(fontSize = 38.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "متبقي على رفع الأذان المبارك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGold.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Glassmorphic Countdown Box
                Surface(
                    color = Color(0xFF041C14).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = countdownString,
                        style = PrayerCountdownDigits,
                        color = GoldAccentBright,
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5-Prayers Daily Mini-Timeline Bar
                if (schedule != null && schedule.prayers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        schedule.prayers.forEach { prayer ->
                            val isCurrentNext = prayer.isNext
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCurrentNext) {
                                        Text(
                                            text = "•",
                                            color = GoldAccentBright,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = prayer.type.arabicName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCurrentNext) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isCurrentNext) GoldAccentBright else Color.White.copy(alpha = 0.65f)
                                    )
                                }
                                Text(
                                    text = prayer.timeFormatted,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isCurrentNext) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentNext) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Islamic Prayer Niches Card (خمس كوات محرابية لمواقيت الصلوات اليومية)
 */
@Composable
fun PrayerTimeRowCard(
    schedule: PrayerSchedule?,
    modifier: Modifier = Modifier
) {
    if (schedule == null) return
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مواقيت صلوات اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "التوقيت المحلي الرسمي",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Arched Niches side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                schedule.prayers.forEach { prayer ->
                    val isNext = prayer.isNext
                    val nicheBg = if (isNext) {
                        MihrabGreenTop
                    } else if (isDark) {
                        PrayerNicheBgDark
                    } else {
                        PrayerNicheBgLight
                    }

                    val textColor = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface
                    val labelColor = if (isNext) GoldAccentBright else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderStroke = if (isNext) {
                        BorderStroke(1.5.dp, GoldAccent)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                        color = nicheBg,
                        border = borderStroke,
                        shadowElevation = if (isNext) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isNext) {
                                Text(
                                    text = "۞",
                                    fontSize = 10.sp,
                                    color = GoldAccentBright,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Text(
                                text = prayer.type.arabicName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                                color = labelColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = prayer.timeFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Islamic Quick Action Squircle Button with Gilded Rim
 */
@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Daily Spiritual Progress Card with Emerald and Gold Indicators
 */
@Composable
fun DailyProgressCard(
    isMorningDone: Boolean,
    isEveningDone: Boolean,
    tasbeehCount: Int,
    tasbeehTarget: Int,
    quranPages: Int,
    onMorningClick: () -> Unit,
    onEveningClick: () -> Unit,
    onQuranClick: () -> Unit,
    onTasbeehClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "وردك اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "متابعة العبادات والسنن",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Morning & Evening Adhkar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Morning Adhkar
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMorningClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isMorningDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(
                        1.dp,
                        if (isMorningDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMorningDone) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isMorningDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "أذكار الصباح",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isMorningDone) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMorningDone) "تمت بحمد الله" else "اضغط للإتمام",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isMorningDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Evening Adhkar
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEveningClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isEveningDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(
                        1.dp,
                        if (isEveningDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEveningDone) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isEveningDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "أذكار المساء",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isEveningDone) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEveningDone) "تمت بحمد الله" else "اضغط للإتمام",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isEveningDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quran & Tasbeeh Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quran Pages
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onQuranClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ورد القرآن",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$quranPages صفحات اليوم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Tasbeeh
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTasbeehClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "التسبيح",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$tasbeehCount / $tasbeehTarget",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccentDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Islamic Calligraphy Spiritual Ayah Card
 */
@Composable
fun SpiritualAyahCard(
    modifier: Modifier = Modifier,
    ayahText: String = "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾",
    surahInfo: String = "سورة الرعد: الآية 28"
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF14241B) else SoftGoldParchment

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "۞ قبس إيماني ۞",
                style = MaterialTheme.typography.labelSmall,
                color = GoldAccentDark,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = ayahText,
                style = IslamicAyahQuote.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surahInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class IslamicNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

/**
 * Modern Floating Luxury Islamic Dock Navigation Bar
 */
@Composable
fun IslamicBottomNavigationBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val tabs = listOf(
        IslamicNavTab("home", "الرئيسية", Icons.Default.Home, Icons.Outlined.Home, "nav_item_home"),
        IslamicNavTab("quran", "القرآن", Icons.Default.MenuBook, Icons.Outlined.MenuBook, "nav_item_quran"),
        IslamicNavTab("adhkar", "الأذكار", Icons.Default.WbSunny, Icons.Outlined.WbSunny, "nav_item_adhkar"),
        IslamicNavTab("qibla", "القبلة", Icons.Default.Explore, Icons.Outlined.Explore, "nav_item_qibla"),
        IslamicNavTab("more", "المزيد", Icons.Default.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_item_more")
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("bottom_nav_bar"),
        shape = RoundedCornerShape(28.dp),
        color = if (isDark) Color(0xFF101E17) else Color.White,
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    GoldAccent.copy(alpha = 0.65f),
                    GoldAccent.copy(alpha = 0.2f),
                    GoldAccent.copy(alpha = 0.65f)
                )
            )
        ),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onTabSelected(tab.route) }
                        .testTag(tab.tag)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MihrabGreenTop,
                                                MihrabGreenBottom
                                            )
                                        )
                                    )
                                    .border(
                                        BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                        RoundedCornerShape(18.dp)
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = if (isSelected) GoldAccentBright else if (isDark) Color(0xFFBDD2C6) else Color(0xFF1B382B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSelected) Color.White else if (isDark) Color(0xFFBDD2C6) else Color(0xFF1B382B)
                        )
                    }
                }
            }
        }
    }
}

