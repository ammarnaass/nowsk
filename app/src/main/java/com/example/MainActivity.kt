package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.data.quran.MushafRepository
import com.example.ui.MainViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.IslamicBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.GoldAccent
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
                            onFinishOnboarding = { viewModel.completeOnboarding() },
                            onGpsDetected = { lat, lon -> viewModel.setGpsLocation(lat, lon) }
                        )
                    } else {
                        NusakkirMainApp(
                            viewModel = viewModel,
                            uiState = uiState,
                            quranProgress = quranProgress,
                            tasbeehList = tasbeehList,
                            favoritesList = favoritesList,
                            isDarkTheme = isDarkTheme,
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
    isDarkTheme: Boolean = false,
    initialRoute: String = "home"
) {
    var currentRoute by remember { mutableStateOf(initialRoute) }
    val backStack = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var selectedMushafPage by remember {
        mutableIntStateOf(quranProgress?.ayahNumber?.coerceIn(1, 604) ?: 1)
    }

    // Comprehensive navigation handlers
    val navigateTo: (String) -> Unit = { route ->
        if (currentRoute != route) {
            // Main bottom tabs clear the stack to avoid deep tab loops
            if (route in listOf("home", "quran", "adhkar", "qibla", "more")) {
                backStack.clear()
            } else {
                backStack.add(currentRoute)
            }
            currentRoute = route
        }
    }

    val navigateBack: () -> Unit = {
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.removeAt(backStack.size - 1)
        } else if (currentRoute != "home") {
            currentRoute = "home"
        } else {
            // On home screen: prompt user with double back to prevent accidental app close
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = now
                android.widget.Toast.makeText(context, "اضغط مرة أخرى للخروج من التطبيق", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Intercept hardware/gesture Android Back button across the entire app
    BackHandler(enabled = true) {
        navigateBack()
    }

    // Bottom Navigation Bar is shown only on main tabs
    val isMainTab = currentRoute in listOf("home", "quran", "adhkar", "qibla", "more")

    Scaffold(
        bottomBar = {
            if (isMainTab) {
                IslamicBottomNavigationBar(
                    currentRoute = currentRoute,
                    onTabSelected = navigateTo
                )
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
                    onPrayerDetailsClick = { navigateTo("prayer_details") },
                    onLocationClick = { navigateTo("settings") },
                    onSearchClick = { navigateTo("search") },
                    onSettingsClick = { navigateTo("settings") },
                    onNavigateToQuran = { navigateTo("quran") },
                    onNavigateToMorningAdhkar = {
                        val morningCat = AdhkarRepository.CATEGORIES.first { it.id == "morning" }
                        viewModel.selectAdhkarCategory(morningCat)
                        navigateTo("dhikr_detail")
                    },
                    onNavigateToEveningAdhkar = {
                        val eveningCat = AdhkarRepository.CATEGORIES.first { it.id == "evening" }
                        viewModel.selectAdhkarCategory(eveningCat)
                        navigateTo("dhikr_detail")
                    },
                    onNavigateToQibla = { navigateTo("qibla") },
                    onNavigateToTasbeeh = { navigateTo("tasbeeh") },
                    onNavigateToDuas = { navigateTo("duas") },
                    onToggleMorningDone = { viewModel.markMorningAdhkarDone(!uiState.isMorningAdhkarDone) },
                    onToggleEveningDone = { viewModel.markEveningAdhkarDone(!uiState.isEveningAdhkarDone) },
                    onStopAdhan = { viewModel.stopAdhan() },
                    onRefreshFromApi = { viewModel.syncPrayerTimesFromAladhan(force = true) }
                )

                "prayer_details" -> PrayerTimesDetailScreen(
                    uiState = uiState,
                    onBackClick = navigateBack,
                    onOpenSettingsClick = { navigateTo("settings") },
                    onTogglePreAlarm = { viewModel.toggleNotification("before_prayer", it) },
                    onRefreshFromApi = { viewModel.syncPrayerTimesFromAladhan(force = true) },
                    onTestPrayerWithAdhan = { prayerName -> viewModel.testPrayerNotification(prayerName) },
                    onStopAdhan = { viewModel.stopAdhan() }
                )

                "quran" -> QuranScreen(
                    quranProgress = quranProgress,
                    onSurahSelected = { surahNumber ->
                        val page = MushafRepository.getPageForSurah(surahNumber)
                        selectedMushafPage = page
                        navigateTo("mushaf_reader")
                    },
                    onPageSelected = { pageNumber ->
                        selectedMushafPage = pageNumber
                        navigateTo("mushaf_reader")
                    },
                    onContinueReadingClick = {
                        val page = quranProgress?.ayahNumber?.coerceIn(1, 604) ?: 1
                        selectedMushafPage = page
                        navigateTo("mushaf_reader")
                    }
                )

                "mushaf_reader" -> MushafReaderScreen(
                    initialPage = selectedMushafPage,
                    isDarkTheme = isDarkTheme,
                    onBackClick = navigateBack,
                    onSaveBookmark = { page, surahName, surahNumber ->
                        viewModel.saveMushafProgress(page, surahName, surahNumber)
                    }
                )

                "quran_reader" -> QuranReaderScreen(
                    surah = uiState.currentSurah,
                    ayahs = uiState.currentAyahs,
                    fontSizeSp = uiState.quranFontSizeSp,
                    isFavorite = viewModel.isItemFavorite("سورة ${uiState.currentSurah.nameAr}", "quran"),
                    onBackClick = navigateBack,
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
                        navigateTo("dhikr_detail")
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
                    onBackClick = navigateBack
                )

                "tasbeeh" -> TasbeehScreen(
                    tasbeehList = tasbeehList,
                    onIncrement = { viewModel.incrementTasbeeh(it) },
                    onReset = { viewModel.resetTasbeeh(it) },
                    onAddCustom = { title, target -> viewModel.addCustomTasbeeh(title, target) },
                    onDeleteCustom = { viewModel.deleteTasbeeh(it) },
                    onBackClick = navigateBack
                )

                "qibla" -> QiblaScreen(
                    uiState = uiState,
                    onVibrate = { viewModel.vibrateDevice(50) },
                    onGpsLocationDetected = { lat, lon -> viewModel.setGpsLocation(lat, lon) },
                    onStartCompass = { viewModel.startSensorListening() },
                    onStopCompass = { viewModel.stopSensorListening() },
                    onBackClick = navigateBack
                )

                "more" -> MoreScreen(
                    onNavigateToTasbeeh = { navigateTo("tasbeeh") },
                    onNavigateToDuas = { navigateTo("duas") },
                    onNavigateToCalendar = { navigateTo("calendar") },
                    onNavigateToFavorites = { navigateTo("favorites") },
                    onNavigateToPrayers = { navigateTo("prayer_details") },
                    onNavigateToSettings = { navigateTo("settings") }
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
                    onBackClick = navigateBack
                )

                "calendar" -> CalendarScreen(
                    todayHijri = uiState.todayHijri,
                    todayGregorian = uiState.todayGregorian,
                    selectedCity = uiState.selectedCity,
                    calculationMethod = uiState.calculationMethod,
                    juristicMethod = uiState.juristicMethod,
                    onBackClick = navigateBack
                )

                "favorites" -> FavoritesScreen(
                    favorites = favoritesList,
                    onRemoveFavorite = { viewModel.removeFavorite(it) },
                    onBackClick = navigateBack
                )

                "search" -> SearchScreen(
                    query = uiState.searchQuery,
                    surahResults = uiState.searchResultsSurahs,
                    ayahResults = uiState.searchResultsAyahs,
                    dhikrResults = uiState.searchResultsDhikr,
                    onQueryChange = { viewModel.performSearch(it) },
                    onSurahClick = { surahNumber ->
                        val page = MushafRepository.getPageForSurah(surahNumber)
                        selectedMushafPage = page
                        navigateTo("mushaf_reader")
                    },
                    onPageClick = { pageNumber ->
                        selectedMushafPage = pageNumber
                        navigateTo("mushaf_reader")
                    },
                    onBackClick = navigateBack
                )

                "settings" -> SettingsScreen(
                    uiState = uiState,
                    onCitySelected = { viewModel.selectCity(it) },
                    onGpsLocationDetected = { lat, lon -> viewModel.setGpsLocation(lat, lon) },
                    onMethodSelected = { viewModel.setCalculationMethod(it) },
                    onJuristicSelected = { viewModel.setJuristicMethod(it) },
                    onThemeSelected = { viewModel.setThemeMode(it) },
                    onToggleNotification = { type, enabled -> viewModel.toggleNotification(type, enabled) },
                    onBackClick = navigateBack,
                    onAladhanMethodSelected = { viewModel.setAladhanMethod(it) },
                    onSyncAladhanApi = { viewModel.syncPrayerTimesFromAladhan(force = true) },
                    onSearchPlaces = { viewModel.filterPlaces(it) },
                    onToggleAdhanSound = { viewModel.toggleAdhanSound(it) },
                    onPlayAdhanPreview = { viewModel.playAdhanPreview() },
                    onStopAdhan = { viewModel.stopAdhan() },
                    onTestNotification = { viewModel.testPrayerNotification() },
                    onSetPrayerOffset = { prayerType, offset -> viewModel.setPrayerOffset(prayerType, offset) },
                    onResetPrayerOffsets = { viewModel.resetPrayerOffsets() }
                )
            }
        }
    }
}
