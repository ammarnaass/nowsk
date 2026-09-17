package com.example.ui

import android.app.Application
import android.content.Context
import com.example.NusakkirApplication
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.data.adhkar.DhikrItem
import com.example.data.adhkar.DuaItem
import com.example.data.api.AladhanRepository
import com.example.data.api.model.AladhanCalculationMethod
import com.example.data.calendar.HijriCalendarHelper
import com.example.data.calendar.HijriDate
import com.example.data.local.NusakkirDatabase
import com.example.data.local.entity.AdhkarEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.PrayerTimeEntity
import com.example.data.local.entity.QuranProgressEntity
import com.example.data.local.entity.TasbeehEntity
import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.CityLocation
import com.example.data.prayer.CityPresets
import com.example.data.prayer.JuristicMethod
import com.example.data.prayer.PrayerSchedule
import com.example.data.prayer.PrayerTimesCalculator
import com.example.data.prayer.PrayerType
import com.example.data.qibla.QiblaCalculator
import com.example.data.quran.Ayah
import com.example.data.quran.QuranRepository
import com.example.data.quran.Surah
import com.example.data.repository.NusakkirRepository
import com.example.util.AdhanPlayer
import com.example.util.NusakkirNotificationHelper
import com.example.util.PrayerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class NusakkirUiState(
    // App & Flow State
    val isOnboardingCompleted: Boolean = true,
    val selectedThemeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val quranFontSizeSp: Float = 22f,

    // Location & Calculation
    val selectedCity: CityLocation = CityPresets.DEFAULT,
    val isUsingGps: Boolean = false,
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val juristicMethod: JuristicMethod = JuristicMethod.SHAFI,

    // Prayer Times & Countdown
    val prayerSchedule: PrayerSchedule? = null,
    val countdownFormatted: String = "00:00:00",
    val todayHijri: HijriDate = HijriCalendarHelper.getTodayHijriDate(),
    val todayGregorian: String = HijriCalendarHelper.formatGregorianArabic(Calendar.getInstance()),

    // Daily Progress
    val isMorningAdhkarDone: Boolean = false,
    val isEveningAdhkarDone: Boolean = false,
    val dailyQuranPages: Int = 4,

    // Qibla & Compass
    val compassHeading: Float = 0f,
    val qiblaAngle: Double = 0.0,
    val distanceToMakkahKm: Double = 0.0,
    val isPointedAtQibla: Boolean = false,
    val isCompassAvailable: Boolean = true,

    // Active reading selections
    val currentSurah: Surah = QuranRepository.SURAHS[0],
    val currentAyahs: List<Ayah> = QuranRepository.getAyahs(1),
    val selectedAdhkarCategory: AdhkarCategory = AdhkarRepository.CATEGORIES[0],
    val currentDhikrList: List<DhikrItem> = AdhkarRepository.getItemsForCategory("morning"),

    // Global Search
    val searchQuery: String = "",
    val searchResultsDhikr: List<DhikrItem> = emptyList(),
    val searchResultsAyahs: List<Ayah> = emptyList(),
    val searchResultsSurahs: List<Surah> = emptyList(),

    // Notifications and Adhan sound settings
    val notifyOnPrayer: Boolean = true,
    val notifyBeforePrayer: Boolean = true,
    val notifyMorningAdhkar: Boolean = true,
    val notifyEveningAdhkar: Boolean = true,
    val playAdhanSound: Boolean = true,
    val isAdhanPlaying: Boolean = false,
    val activeAdhanPrayer: String? = null,

    // Aladhan API State
    val isAladhanSyncing: Boolean = false,
    val aladhanSyncSuccess: Boolean? = null,
    val aladhanMethods: List<AladhanCalculationMethod> = emptyList(),
    val selectedAladhanMethodId: Int = 19, // Algeria method default
    val placesList: List<CityLocation> = emptyList(),
    val placeSearchQuery: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository = (application as? NusakkirApplication)?.repository
        ?: NusakkirRepository(NusakkirDatabase.getInstance(application))
    private val database = (application as? NusakkirApplication)?.database
        ?: NusakkirDatabase.getInstance(application)
    private val aladhanRepository = (application as? NusakkirApplication)?.aladhanRepository
        ?: AladhanRepository(database = database)

    private val _uiState = MutableStateFlow(NusakkirUiState())
    val uiState: StateFlow<NusakkirUiState> = _uiState.asStateFlow()

    // Room Streams
    val allFavorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasbeeh: StateFlow<List<TasbeehEntity>> = repository.allTasbeeh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdhkar: StateFlow<List<AdhkarEntity>> = repository.allAdhkar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quranProgress: StateFlow<QuranProgressEntity?> = repository.quranProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sensor Manager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    init {
        _uiState.update { it.copy(isCompassAvailable = (rotationSensor != null)) }
        loadInitialSettings()
        loadAladhanMethods()
        recalculatePrayers()
        syncPrayerTimesFromAladhan()
        startCountdownTicker()
        startSensorListening()
        observeAdhanPlayer()
    }

    private fun observeAdhanPlayer() {
        viewModelScope.launch {
            AdhanPlayer.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isAdhanPlaying = isPlaying) }
            }
        }
        viewModelScope.launch {
            AdhanPlayer.currentPrayerPlaying.collect { prayerName ->
                _uiState.update { it.copy(activeAdhanPrayer = prayerName) }
            }
        }
    }

    private fun loadInitialSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check onboarding
            database.nusakkirDao().getSetting("onboarding_done").collect { value ->
                if (value != null) {
                    _uiState.update { it.copy(isOnboardingCompleted = value == "true") }
                } else {
                    // Default show onboarding once
                    _uiState.update { it.copy(isOnboardingCompleted = false) }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val settingsDao = database.settingsDao()
            val adhanVal = settingsDao.getSettingValue("play_adhan_sound")
            if (adhanVal != null) {
                _uiState.update { it.copy(playAdhanSound = adhanVal.toBooleanStrictOrNull() ?: true) }
            }
            val prayerVal = settingsDao.getSettingValue("notify_prayer")
            if (prayerVal != null) {
                _uiState.update { it.copy(notifyOnPrayer = prayerVal.toBooleanStrictOrNull() ?: true) }
            }
            val beforeVal = settingsDao.getSettingValue("notify_before_prayer")
            if (beforeVal != null) {
                _uiState.update { it.copy(notifyBeforePrayer = beforeVal.toBooleanStrictOrNull() ?: true) }
            }
            val morningVal = settingsDao.getSettingValue("notify_morning_adhkar")
            if (morningVal != null) {
                _uiState.update { it.copy(notifyMorningAdhkar = morningVal.toBooleanStrictOrNull() ?: true) }
            }
            val eveningVal = settingsDao.getSettingValue("notify_evening_adhkar")
            if (eveningVal != null) {
                _uiState.update { it.copy(notifyEveningAdhkar = eveningVal.toBooleanStrictOrNull() ?: true) }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("onboarding_done", "true")
            _uiState.update { it.copy(isOnboardingCompleted = true) }
        }
    }

    fun selectCity(city: CityLocation) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                isUsingGps = false
            )
        }
        recalculatePrayers()
        syncPrayerTimesFromAladhan(force = true)
    }

    fun setGpsLocation(lat: Double, lon: Double, cityName: String = "موقعي الحالي") {
        val gpsCity = CityLocation(cityName, "الموقع التلقائي", lat, lon, 1.0)
        _uiState.update {
            it.copy(
                selectedCity = gpsCity,
                isUsingGps = true
            )
        }
        recalculatePrayers()
        syncPrayerTimesFromAladhan(force = true)
    }

    fun loadAladhanMethods() {
        viewModelScope.launch {
            val result = aladhanRepository.getCalculationMethods()
            val methods = result.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    aladhanMethods = methods,
                    placesList = aladhanRepository.availablePlaces
                )
            }
        }
    }

    fun syncPrayerTimesFromAladhan(force: Boolean = false) {
        val city = _uiState.value.selectedCity
        val methodId = _uiState.value.selectedAladhanMethodId
        val schoolId = if (_uiState.value.juristicMethod == JuristicMethod.HANAFI) 1 else 0

        viewModelScope.launch {
            _uiState.update { it.copy(isAladhanSyncing = true) }
            val result = aladhanRepository.getPrayerTimesByCoordinates(
                latitude = city.latitude,
                longitude = city.longitude,
                cityName = city.nameAr,
                methodId = methodId,
                schoolId = schoolId
            )

            result.fold(
                onSuccess = { schedule ->
                    _uiState.update {
                        it.copy(
                            prayerSchedule = schedule,
                            isAladhanSyncing = false,
                            aladhanSyncSuccess = true
                        )
                    }
                    schedulePrayerAlarms()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isAladhanSyncing = false,
                            aladhanSyncSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun setAladhanMethod(methodId: Int) {
        _uiState.update { it.copy(selectedAladhanMethodId = methodId) }
        syncPrayerTimesFromAladhan(force = true)
    }

    fun filterPlaces(query: String) {
        val filtered = aladhanRepository.searchPlaces(query)
        _uiState.update { it.copy(placeSearchQuery = query, placesList = filtered) }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        _uiState.update { it.copy(calculationMethod = method) }
        recalculatePrayers()
    }

    fun setJuristicMethod(method: JuristicMethod) {
        _uiState.update { it.copy(juristicMethod = method) }
        recalculatePrayers()
    }

    fun setThemeMode(mode: String) {
        _uiState.update { it.copy(selectedThemeMode = mode) }
    }

    fun setQuranFontSize(size: Float) {
        _uiState.update { it.copy(quranFontSizeSp = size.coerceIn(16f, 36f)) }
    }

    fun toggleAdhanSound(enabled: Boolean) {
        _uiState.update { it.copy(playAdhanSound = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("play_adhan_sound", enabled.toString())
        }
    }

    fun playAdhanPreview(prayerName: String = "الظهر") {
        val context = getApplication<Application>()
        AdhanPlayer.playAdhan(context, prayerName)
    }

    fun stopAdhan() {
        AdhanPlayer.stopAdhan()
    }

    fun testPrayerNotification(prayerName: String = "الظهر") {
        val context = getApplication<Application>()
        NusakkirNotificationHelper.showPrayerNotification(
            context = context,
            prayerName = prayerName,
            isPreAlarm = false,
            isTest = true
        )
        if (_uiState.value.playAdhanSound) {
            AdhanPlayer.playAdhan(context, prayerName)
        }
    }

    fun schedulePrayerAlarms() {
        val schedule = _uiState.value.prayerSchedule ?: return
        val context = getApplication<Application>()
        PrayerScheduler.schedulePrayerAlarms(
            context = context,
            schedule = schedule,
            notifyBefore = _uiState.value.notifyBeforePrayer,
            notifyMorningAdhkar = _uiState.value.notifyMorningAdhkar,
            notifyEveningAdhkar = _uiState.value.notifyEveningAdhkar
        )
    }

    fun toggleNotification(type: String, enabled: Boolean) {
        _uiState.update {
            when (type) {
                "prayer" -> it.copy(notifyOnPrayer = enabled)
                "before_prayer" -> it.copy(notifyBeforePrayer = enabled)
                "morning_adhkar" -> it.copy(notifyMorningAdhkar = enabled)
                "evening_adhkar" -> it.copy(notifyEveningAdhkar = enabled)
                else -> it
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val key = when (type) {
                "prayer" -> "notify_prayer"
                "before_prayer" -> "notify_before_prayer"
                "morning_adhkar" -> "notify_morning_adhkar"
                "evening_adhkar" -> "notify_evening_adhkar"
                else -> ""
            }
            if (key.isNotEmpty()) {
                repository.setSetting(key, enabled.toString())
            }
        }
        schedulePrayerAlarms()
    }

    fun selectSurah(surahNumber: Int) {
        val surah = QuranRepository.getSurah(surahNumber) ?: QuranRepository.SURAHS[0]
        val ayahs = QuranRepository.getAyahs(surahNumber)
        _uiState.update {
            it.copy(
                currentSurah = surah,
                currentAyahs = ayahs
            )
        }
        // update progress
        viewModelScope.launch {
            repository.updateQuranProgress(surah.number, surah.nameAr, 1)
        }
    }

    fun selectAdhkarCategory(category: AdhkarCategory) {
        val items = AdhkarRepository.getItemsForCategory(category.id)
        _uiState.update {
            it.copy(
                selectedAdhkarCategory = category,
                currentDhikrList = items
            )
        }
    }

    fun markMorningAdhkarDone(done: Boolean = true) {
        _uiState.update { it.copy(isMorningAdhkarDone = done) }
    }

    fun markEveningAdhkarDone(done: Boolean = true) {
        _uiState.update { it.copy(isEveningAdhkarDone = done) }
    }

    fun performSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    searchResultsDhikr = emptyList(),
                    searchResultsAyahs = emptyList(),
                    searchResultsSurahs = emptyList()
                )
            }
            return
        }

        val dhikrResults = AdhkarRepository.searchAll(q)
        val ayahResults = QuranRepository.searchQuran(q)
        val surahResults = QuranRepository.SURAHS.filter {
            it.nameAr.contains(q) || it.nameEn.lowercase().contains(q.lowercase())
        }

        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResultsDhikr = dhikrResults,
                searchResultsAyahs = ayahResults,
                searchResultsSurahs = surahResults
            )
        }
    }

    // Tasbeeh Operations
    fun incrementTasbeeh(tasbeeh: TasbeehEntity) {
        viewModelScope.launch {
            repository.incrementTasbeeh(tasbeeh.id, tasbeeh.count)
            vibrateDevice(40)
        }
    }

    fun resetTasbeeh(id: Long) {
        viewModelScope.launch {
            repository.resetTasbeeh(id)
        }
    }

    fun addCustomTasbeeh(title: String, target: Int) {
        viewModelScope.launch {
            repository.addTasbeeh(title, target)
        }
    }

    fun deleteTasbeeh(id: Long) {
        viewModelScope.launch {
            repository.deleteTasbeeh(id)
        }
    }

    // Favorites
    fun toggleFavorite(type: String, title: String, content: String, subtitle: String = "") {
        viewModelScope.launch {
            val existing = allFavorites.value.find { it.title == title && it.type == type }
            if (existing != null) {
                repository.removeFavorite(existing.id)
            } else {
                repository.addFavorite(
                    FavoriteEntity(
                        type = type,
                        title = title,
                        subtitle = subtitle,
                        content = content
                    )
                )
            }
        }
    }

    fun isItemFavorite(title: String, type: String): Boolean {
        return allFavorites.value.any { it.title == title && it.type == type }
    }

    fun removeFavorite(id: Long) {
        viewModelScope.launch {
            repository.removeFavorite(id)
        }
    }

    // Prayer Times & Qibla
    private fun recalculatePrayers() {
        val city = _uiState.value.selectedCity
        val schedule = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = city.latitude,
            longitude = city.longitude,
            calendar = Calendar.getInstance(),
            method = _uiState.value.calculationMethod,
            juristic = _uiState.value.juristicMethod
        )

        val qiblaAngle = QiblaCalculator.calculateQiblaAngle(city.latitude, city.longitude)
        val distanceKm = QiblaCalculator.calculateDistanceToKaabaKm(city.latitude, city.longitude)

        _uiState.update {
            it.copy(
                prayerSchedule = schedule,
                qiblaAngle = qiblaAngle,
                distanceToMakkahKm = distanceKm,
                todayHijri = HijriCalendarHelper.getTodayHijriDate(),
                todayGregorian = HijriCalendarHelper.formatGregorianArabic(Calendar.getInstance())
            )
        }

        // Cache prayer times to Room Database
        viewModelScope.launch(Dispatchers.IO) {
            val fajr = schedule.prayers.find { it.type == PrayerType.FAJR }?.timeFormatted ?: ""
            val sunrise = schedule.prayers.find { it.type == PrayerType.SUNRISE }?.timeFormatted ?: ""
            val dhuhr = schedule.prayers.find { it.type == PrayerType.DHUHR }?.timeFormatted ?: ""
            val asr = schedule.prayers.find { it.type == PrayerType.ASR }?.timeFormatted ?: ""
            val maghrib = schedule.prayers.find { it.type == PrayerType.MAGHRIB }?.timeFormatted ?: ""
            val isha = schedule.prayers.find { it.type == PrayerType.ISHA }?.timeFormatted ?: ""

            repository.savePrayerTimes(
                PrayerTimeEntity(
                    date = schedule.dateString,
                    cityName = city.nameAr,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    fajr = fajr,
                    sunrise = sunrise,
                    dhuhr = dhuhr,
                    asr = asr,
                    maghrib = maghrib,
                    isha = isha,
                    calculationMethod = _uiState.value.calculationMethod.name,
                    juristicMethod = _uiState.value.juristicMethod.name
                )
            )
        }

        schedulePrayerAlarms()
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val current = _uiState.value.prayerSchedule
                if (current != null && current.nextPrayer != null) {
                    val now = System.currentTimeMillis()
                    val diff = current.nextPrayer.timestampMillis - now
                    if (diff <= 0) {
                        recalculatePrayers()
                    } else {
                        val hours = (diff / (1000 * 60 * 60)) % 24
                        val minutes = (diff / (1000 * 60)) % 60
                        val seconds = (diff / 1000) % 60
                        val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        _uiState.update { it.copy(countdownFormatted = formatted) }
                    }
                }
                delay(1000)
            }
        }
    }

    // Compass Sensor
    private fun startSensorListening() {
        rotationSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            var azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (azimuthDegrees < 0) azimuthDegrees += 360f

            val qibla = _uiState.value.qiblaAngle.toFloat()
            val diff = kotlin.math.abs(azimuthDegrees - qibla)
            val isAligned = diff < 3.5f || diff > 356.5f

            _uiState.update {
                it.copy(
                    compassHeading = azimuthDegrees,
                    isPointedAtQibla = isAligned
                )
            }
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val heading = event.values[0]
            val qibla = _uiState.value.qiblaAngle.toFloat()
            val diff = kotlin.math.abs(heading - qibla)
            val isAligned = diff < 3.5f || diff > 356.5f

            _uiState.update {
                it.copy(
                    compassHeading = heading,
                    isPointedAtQibla = isAligned
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun vibrateDevice(durationMillis: Long = 50) {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMillis)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration fails or permission is restricted
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
    }
}
