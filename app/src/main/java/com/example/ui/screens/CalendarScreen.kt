package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.calendar.HijriCalendarHelper
import com.example.data.calendar.HijriDate
import com.example.data.calendar.IslamicOccasion
import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.CityLocation
import com.example.data.prayer.CityPresets
import com.example.data.prayer.JuristicMethod
import com.example.data.prayer.PrayerTimesCalculator
import com.example.data.prayer.PrayerType
import com.example.ui.theme.GoldAccent
import java.util.Calendar

data class MonthDayPrayerSchedule(
    val dayNumber: Int,
    val dayNameAr: String,
    val isToday: Boolean,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    todayHijri: HijriDate,
    todayGregorian: String,
    selectedCity: CityLocation = CityPresets.DEFAULT,
    calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    juristicMethod: JuristicMethod = JuristicMethod.SHAFI,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val occasions = HijriCalendarHelper.ISLAMIC_OCCASIONS
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Precalculate all days in the current month
    val monthPrayerDays = remember(selectedCity, calculationMethod, juristicMethod) {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        (1..maxDays).map { day ->
            val cal = Calendar.getInstance().apply {
                set(year, month, day, 12, 0, 0)
            }
            val schedule = PrayerTimesCalculator.calculatePrayerTimes(
                latitude = selectedCity.latitude,
                longitude = selectedCity.longitude,
                calendar = cal,
                method = calculationMethod,
                juristic = juristicMethod
            )

            val dayNameAr = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "السبت"
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الاثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                else -> ""
            }

            MonthDayPrayerSchedule(
                dayNumber = day,
                dayNameAr = dayNameAr,
                isToday = (day == currentDay),
                fajr = schedule.prayers.find { it.type == PrayerType.FAJR }?.timeFormatted ?: "--:--",
                sunrise = schedule.prayers.find { it.type == PrayerType.SUNRISE }?.timeFormatted ?: "--:--",
                dhuhr = schedule.prayers.find { it.type == PrayerType.DHUHR }?.timeFormatted ?: "--:--",
                asr = schedule.prayers.find { it.type == PrayerType.ASR }?.timeFormatted ?: "--:--",
                maghrib = schedule.prayers.find { it.type == PrayerType.MAGHRIB }?.timeFormatted ?: "--:--",
                isha = schedule.prayers.find { it.type == PrayerType.ISHA }?.timeFormatted ?: "--:--",
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "التقويم ومواقيت الشهر",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("المناسبات والتقويم", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("جدول مواقيت الشهر", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTabIndex == 0) {
                // Occasions and Today Hijri Date
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Today Date Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = todayHijri.dayNameAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${todayHijri.day} ${todayHijri.monthNameAr} ${todayHijri.year} هـ",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "الموافق: $todayGregorian",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Occasions Header
                    item {
                        Text(
                            text = "المناسبات الإسلامية الكبرى",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Occasions Items
                    items(occasions) { occasion ->
                        val monthName = HijriCalendarHelper.HIJRI_MONTHS_AR.getOrElse(occasion.hijriMonth - 1) { "" }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = occasion.nameAr,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = occasion.descriptionAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${occasion.hijriDay} $monthName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Full Month Prayer Times Table (PRD Section 20)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "مواقيت الشهر — ${selectedCity.nameAr}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "جدول كامل لجميع صلوات الشهر مع تمييز اليوم الحالي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthPrayerDays) { dayItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (dayItem.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (dayItem.isToday) CardDefaults.outlinedCardBorder() else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (dayItem.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = dayItem.dayNumber.toString(),
                                                        color = if (dayItem.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = dayItem.dayNameAr,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (dayItem.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (dayItem.isToday) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "اليوم",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Row of 6 prayers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        PrayerMonthMiniCol("الفجر", dayItem.fajr, dayItem.isToday)
                                        PrayerMonthMiniCol("الشروق", dayItem.sunrise, dayItem.isToday)
                                        PrayerMonthMiniCol("الظهر", dayItem.dhuhr, dayItem.isToday)
                                        PrayerMonthMiniCol("العصر", dayItem.asr, dayItem.isToday)
                                        PrayerMonthMiniCol("المغرب", dayItem.maghrib, dayItem.isToday)
                                        PrayerMonthMiniCol("العشاء", dayItem.isha, dayItem.isToday)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerMonthMiniCol(
    label: String,
    time: String,
    isToday: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

