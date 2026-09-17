package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.NusakkirTheme
import com.example.util.NusakkirNotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels with custom Adhan sound
        NusakkirNotificationHelper.createNotificationChannels(this)

        val targetRoute = intent?.getStringExtra("initial_route")

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val quranProgress by viewModel.quranProgress.collectAsStateWithLifecycle()
            val tasbeehList by viewModel.allTasbeeh.collectAsStateWithLifecycle()
            val favoritesList by viewModel.allFavorites.collectAsStateWithLifecycle()

            // Request Notification Permission on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Theme calculation
            val isDarkTheme = when (uiState.selectedThemeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            NusakkirTheme(darkTheme = isDarkTheme) {
                // Ensure authentic Arabic Right-To-Left layout direction throughout the entire application
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (!uiState.isOnboardingCompleted) {
                        OnboardingScreen(
                            onFinishOnboarding = { viewModel.completeOnboarding() }
                        )
                    } else {
                        NusakkirMainApp(
                            viewModel = viewModel,
                            uiState = uiState,
                            quranProgress = quranProgress,
                            tasbeehList = tasbeehList,
                            favoritesList = favoritesList,
                            initialRoute = targetRoute ?: "home"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NusakkirMainApp(
    viewModel: MainViewModel,
    uiState: com.example.ui.NusakkirUiState,
    quranProgress: com.example.data.local.entity.QuranProgressEntity?,
    tasbeehList: List<com.example.data.local.entity.TasbeehEntity>,
    favoritesList: List<com.example.data.local.entity.FavoriteEntity>,
    initialRoute: String = "home"
) {
    var currentRoute by remember { mutableStateOf(initialRoute) }

    // Bottom Navigation Bar is shown only on main tabs
    val isMainTab = currentRoute in listOf("home", "quran", "adhkar", "qibla", "more")

    Scaffold(
        bottomBar = {
            if (isMainTab) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { currentRoute = "home" },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "home") Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "الرئيسية"
                            )
                        },
                        label = { Text("الرئيسية") },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = currentRoute == "quran",
                        onClick = { currentRoute = "quran" },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "quran") Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "القرآن"
                            )
                        },
                        label = { Text("القرآن") },
                        modifier = Modifier.testTag("nav_item_quran")
                    )
                    NavigationBarItem(
                        selected = currentRoute == "adhkar",
                        onClick = { currentRoute = "adhkar" },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "adhkar") Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                                contentDescription = "الأذكار"
                            )
                        },
                        label = { Text("الأذكار") },
                        modifier = Modifier.testTag("nav_item_adhkar")
                    )
                    NavigationBarItem(
                        selected = currentRoute == "qibla",
                        onClick = { currentRoute = "qibla" },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "qibla") Icons.Default.Explore else Icons.Outlined.Explore,
                                contentDescription = "القبلة"
                            )
                        },
                        label = { Text("القبلة") },
                        modifier = Modifier.testTag("nav_item_qibla")
                    )
                    NavigationBarItem(
                        selected = currentRoute == "more",
                        onClick = { currentRoute = "more" },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "more") Icons.Default.MoreHoriz else Icons.Outlined.MoreHoriz,
                                contentDescription = "المزيد"
                            )
                        },
                        label = { Text("المزيد") },
                        modifier = Modifier.testTag("nav_item_more")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentRoute) {
                "home" -> HomeScreen(
                    uiState = uiState,
                    quranProgress = quranProgress,
                    tasbeehList = tasbeehList,
                    onPrayerDetailsClick = { currentRoute = "prayer_details" },
                    onLocationClick = { currentRoute = "settings" },
                    onSearchClick = { currentRoute = "search" },
                    onSettingsClick = { currentRoute = "settings" },
                    onNavigateToQuran = { currentRoute = "quran" },
                    onNavigateToMorningAdhkar = {
                        val morningCat = AdhkarRepository.CATEGORIES.first { it.id == "morning" }
                        viewModel.selectAdhkarCategory(morningCat)
                        currentRoute = "dhikr_detail"
                    },
                    onNavigateToEveningAdhkar = {
                        val eveningCat = AdhkarRepository.CATEGORIES.first { it.id == "evening" }
                        viewModel.selectAdhkarCategory(eveningCat)
                        currentRoute = "dhikr_detail"
                    },
                    onNavigateToQibla = { currentRoute = "qibla" },
                    onNavigateToTasbeeh = { currentRoute = "tasbeeh" },
                    onNavigateToDuas = { currentRoute = "duas" },
                    onToggleMorningDone = { viewModel.markMorningAdhkarDone(!uiState.isMorningAdhkarDone) },
                    onToggleEveningDone = { viewModel.markEveningAdhkarDone(!uiState.isEveningAdhkarDone) },
                    onStopAdhan = { viewModel.stopAdhan() },
                    onRefreshFromApi = { viewModel.syncPrayerTimesFromAladhan(force = true) }
                )

                "prayer_details" -> PrayerTimesDetailScreen(
                    uiState = uiState,
                    onBackClick = { currentRoute = "home" },
                    onOpenSettingsClick = { currentRoute = "settings" },
                    onTogglePreAlarm = { viewModel.toggleNotification("before_prayer", it) },
                    onRefreshFromApi = { viewModel.syncPrayerTimesFromAladhan(force = true) },
                    onTestPrayerWithAdhan = { prayerName -> viewModel.testPrayerNotification(prayerName) },
                    onStopAdhan = { viewModel.stopAdhan() }
                )

                "quran" -> QuranScreen(
                    quranProgress = quranProgress,
                    onSurahSelected = { surahNumber ->
                        viewModel.selectSurah(surahNumber)
                        currentRoute = "quran_reader"
                    },
                    onContinueReadingClick = {
                        val surahNumber = quranProgress?.surahNumber ?: 1
                        viewModel.selectSurah(surahNumber)
                        currentRoute = "quran_reader"
                    }
                )

                "quran_reader" -> QuranReaderScreen(
                    surah = uiState.currentSurah,
                    ayahs = uiState.currentAyahs,
                    fontSizeSp = uiState.quranFontSizeSp,
                    isFavorite = viewModel.isItemFavorite("سورة ${uiState.currentSurah.nameAr}", "quran"),
                    onBackClick = { currentRoute = "quran" },
                    onFontSizeChange = { viewModel.setQuranFontSize(it) },
                    onToggleFavoriteSurah = {
                        viewModel.toggleFavorite(
                            type = "quran",
                            title = "سورة ${uiState.currentSurah.nameAr}",
                            content = "سورة ${uiState.currentSurah.nameAr} (${uiState.currentSurah.typeAr} - ${uiState.currentSurah.versesCount} آية)"
                        )
                    },
                    onBookmarkAyah = { ayahNum ->
                        viewModel.selectSurah(uiState.currentSurah.number)
                    }
                )

                "adhkar" -> AdhkarScreen(
                    onCategorySelected = { category ->
                        viewModel.selectAdhkarCategory(category)
                        currentRoute = "dhikr_detail"
                    }
                )

                "dhikr_detail" -> DhikrDetailScreen(
                    category = uiState.selectedAdhkarCategory,
                    items = uiState.currentDhikrList,
                    isItemFavorite = { title -> viewModel.isItemFavorite(title, "dhikr") },
                    onToggleFavorite = { dhikr ->
                        viewModel.toggleFavorite(
                            type = "dhikr",
                            title = dhikr.title,
                            content = dhikr.text,
                            subtitle = dhikr.virtue
                        )
                    },
                    onVibrate = { viewModel.vibrateDevice(35) },
                    onBackClick = { currentRoute = "adhkar" }
                )

                "tasbeeh" -> TasbeehScreen(
                    tasbeehList = tasbeehList,
                    onIncrement = { viewModel.incrementTasbeeh(it) },
                    onReset = { viewModel.resetTasbeeh(it) },
                    onAddCustom = { title, target -> viewModel.addCustomTasbeeh(title, target) },
                    onDeleteCustom = { viewModel.deleteTasbeeh(it) }
                )

                "qibla" -> QiblaScreen(
                    uiState = uiState,
                    onVibrate = { viewModel.vibrateDevice(50) }
                )

                "more" -> MoreScreen(
                    onNavigateToTasbeeh = { currentRoute = "tasbeeh" },
                    onNavigateToDuas = { currentRoute = "duas" },
                    onNavigateToCalendar = { currentRoute = "calendar" },
                    onNavigateToFavorites = { currentRoute = "favorites" },
                    onNavigateToPrayers = { currentRoute = "prayer_details" },
                    onNavigateToSettings = { currentRoute = "settings" }
                )

                "duas" -> DuasScreen(
                    isItemFavorite = { title -> viewModel.isItemFavorite(title, "dua") },
                    onToggleFavorite = { dua ->
                        viewModel.toggleFavorite(
                            type = "dua",
                            title = dua.title,
                            content = dua.text,
                            subtitle = dua.source
                        )
                    },
                    onBackClick = { currentRoute = "more" }
                )

                "calendar" -> CalendarScreen(
                    todayHijri = uiState.todayHijri,
                    todayGregorian = uiState.todayGregorian,
                    selectedCity = uiState.selectedCity,
                    calculationMethod = uiState.calculationMethod,
                    juristicMethod = uiState.juristicMethod,
                    onBackClick = { currentRoute = "more" }
                )

                "favorites" -> FavoritesScreen(
                    favorites = favoritesList,
                    onRemoveFavorite = { viewModel.removeFavorite(it) },
                    onBackClick = { currentRoute = "more" }
                )

                "search" -> SearchScreen(
                    query = uiState.searchQuery,
                    surahResults = uiState.searchResultsSurahs,
                    ayahResults = uiState.searchResultsAyahs,
                    dhikrResults = uiState.searchResultsDhikr,
                    onQueryChange = { viewModel.performSearch(it) },
                    onSurahClick = { surahNumber ->
                        viewModel.selectSurah(surahNumber)
                        currentRoute = "quran_reader"
                    },
                    onBackClick = { currentRoute = "home" }
                )

                "settings" -> SettingsScreen(
                    uiState = uiState,
                    onCitySelected = { viewModel.selectCity(it) },
                    onGpsLocationDetected = { lat, lon -> viewModel.setGpsLocation(lat, lon) },
                    onMethodSelected = { viewModel.setCalculationMethod(it) },
                    onJuristicSelected = { viewModel.setJuristicMethod(it) },
                    onThemeSelected = { viewModel.setThemeMode(it) },
                    onToggleNotification = { type, enabled -> viewModel.toggleNotification(type, enabled) },
                    onBackClick = { currentRoute = "home" },
                    onAladhanMethodSelected = { viewModel.setAladhanMethod(it) },
                    onSyncAladhanApi = { viewModel.syncPrayerTimesFromAladhan(force = true) },
                    onSearchPlaces = { viewModel.filterPlaces(it) },
                    onToggleAdhanSound = { viewModel.toggleAdhanSound(it) },
                    onPlayAdhanPreview = { viewModel.playAdhanPreview() },
                    onStopAdhan = { viewModel.stopAdhan() },
                    onTestNotification = { viewModel.testPrayerNotification() }
                )
            }
        }
    }
}
