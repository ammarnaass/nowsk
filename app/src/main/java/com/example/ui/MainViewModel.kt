package com.example.ui

import android.app.Application
import android.content.Context
import com.example.NusakkirApplication
import android.hardware.GeomagneticField
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
    val calculationMethod: CalculationMethod = CalculationMethod.ALGERIA,
    val juristicMethod: JuristicMethod = JuristicMethod.SHAFI,

    // Prayer Times & Countdown
    val prayerSchedule: PrayerSchedule? = null,
    val countdownFormatted: String = "00:00:00",
    val todayHijri: HijriDate = HijriCalendarHelper.getTodayHijriDate(),
    val todayGregorian: String = HijriCalendarHelper.formatGregorianArabic(Calendar.getInstance()),

    // Manual Prayer Minute Calibration (+/- minutes)
    val fajrOffsetMinutes: Int = 0,
    val sunriseOffsetMinutes: Int = 0,
    val dhuhrOffsetMinutes: Int = 0,
    val asrOffsetMinutes: Int = 0,
    val maghribOffsetMinutes: Int = 0,
    val ishaOffsetMinutes: Int = 0,

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
    val compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val sensorTypeUsed: String = "مستشعر الدوران المتجه (جيروسكوب + تسارع + مغناطيسية)",
    val hasGyroscope: Boolean = true,
    val magneticDeclination: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isFlatOnSurface: Boolean = true,

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

    // Multi-tier Sensor Manager & Fallbacks (Gyroscope, Geomagnetic, Accelerometer + Magnetometer)
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val geomagneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    @Suppress("DEPRECATION")
    private val orientationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var smoothedAzimuth = 0f
    private var hasInitialHeading = false
    private var isSensorListening = false

    init {
        val hasGyro = rotationVectorSensor != null || sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val isCompassAvail = rotationVectorSensor != null ||
                geomagneticSensor != null ||
                (accelerometerSensor != null && magnetometerSensor != null) ||
                orientationSensor != null

        val sensorTypeDescription = when {
            rotationVectorSensor != null -> "مستشعر الدوران المتجه (جيروسكوب + تسارع + مغناطيسية)"
            geomagneticSensor != null -> "مستشعر الدوران المغناطيسي الأرضي (Geomagnetic)"
            accelerometerSensor != null && magnetometerSensor != null -> "دمج مقياس التسارع والمجال المغناطيسي"
            orientationSensor != null -> "مستشعر التوجيه الكلاسيكي (Orientation)"
            else -> "لا توجد مستشعرات بوصلة مدعومة"
        }

        _uiState.update {
            it.copy(
                isCompassAvailable = isCompassAvail,
                hasGyroscope = hasGyro,
                sensorTypeUsed = sensorTypeDescription
            )
        }
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

            // Restore Calculation Method & Juristic Method
            val calcMethodVal = settingsDao.getSettingValue("calculation_method")
            if (calcMethodVal != null) {
                try {
                    val method = CalculationMethod.valueOf(calcMethodVal)
                    _uiState.update { it.copy(calculationMethod = method) }
                } catch (_: Exception) {}
            }
            val juristicVal = settingsDao.getSettingValue("juristic_method")
            if (juristicVal != null) {
                try {
                    val juristic = JuristicMethod.valueOf(juristicVal)
                    _uiState.update { it.copy(juristicMethod = juristic) }
                } catch (_: Exception) {}
            }

            // Restore Selected City
            val savedCityName = settingsDao.getSettingValue("selected_city_name")
            val savedLat = settingsDao.getSettingValue("selected_city_lat")?.toDoubleOrNull()
            val savedLon = settingsDao.getSettingValue("selected_city_lon")?.toDoubleOrNull()
            val savedCountry = settingsDao.getSettingValue("selected_city_country") ?: "الجزائر"
            val savedTz = settingsDao.getSettingValue("selected_city_tz")?.toDoubleOrNull() ?: 1.0
            val savedEn = settingsDao.getSettingValue("selected_city_en") ?: ""
            val savedCode = settingsDao.getSettingValue("selected_city_code")?.toIntOrNull() ?: 0
            val isUsingGps = settingsDao.getSettingValue("is_using_gps") == "true"

            // Restore Manual Prayer Offsets
            val fajrOff = settingsDao.getSettingValue("offset_fajr")?.toIntOrNull() ?: 0
            val sunriseOff = settingsDao.getSettingValue("offset_sunrise")?.toIntOrNull() ?: 0
            val dhuhrOff = settingsDao.getSettingValue("offset_dhuhr")?.toIntOrNull() ?: 0
            val asrOff = settingsDao.getSettingValue("offset_asr")?.toIntOrNull() ?: 0
            val maghribOff = settingsDao.getSettingValue("offset_maghrib")?.toIntOrNull() ?: 0
            val ishaOff = settingsDao.getSettingValue("offset_isha")?.toIntOrNull() ?: 0

            _uiState.update {
                it.copy(
                    fajrOffsetMinutes = fajrOff,
                    sunriseOffsetMinutes = sunriseOff,
                    dhuhrOffsetMinutes = dhuhrOff,
                    asrOffsetMinutes = asrOff,
                    maghribOffsetMinutes = maghribOff,
                    ishaOffsetMinutes = ishaOff
                )
            }

            if (savedCityName != null && savedLat != null && savedLon != null) {
                val matchedCity = CityPresets.CITIES.find { it.nameAr == savedCityName || (savedCode > 0 && it.wilayaCode == savedCode) }
                val restoredCity = matchedCity ?: CityLocation(savedCityName, savedCountry, savedLat, savedLon, savedTz, savedEn, savedCode)
                _uiState.update {
                    it.copy(
                        selectedCity = restoredCity,
                        isUsingGps = isUsingGps
                    )
                }
            }
            recalculatePrayers()
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("selected_city_name", city.nameAr)
            repository.setSetting("selected_city_country", city.countryAr)
            repository.setSetting("selected_city_lat", city.latitude.toString())
            repository.setSetting("selected_city_lon", city.longitude.toString())
            repository.setSetting("selected_city_tz", city.timeZoneOffsetHours.toString())
            repository.setSetting("selected_city_en", city.nameEn)
            repository.setSetting("selected_city_code", city.wilayaCode.toString())
            repository.setSetting("is_using_gps", "false")
        }
        recalculatePrayers()
        syncPrayerTimesFromAladhan(force = true)
    }

    fun setGpsLocation(lat: Double, lon: Double, cityName: String = "موقعي الحالي") {
        val nearest = CityPresets.findNearestCity(lat, lon)
        val resolvedName = if (cityName == "موقعي الحالي" && nearest != null) {
            "موقعي الحالي (${nearest.nameAr})"
        } else {
            cityName
        }
        val gpsCity = CityLocation(
            nameAr = resolvedName,
            countryAr = "الموقع التلقائي",
            latitude = lat,
            longitude = lon,
            timeZoneOffsetHours = 1.0,
            nameEn = nearest?.nameEn ?: "",
            wilayaCode = nearest?.wilayaCode ?: 0
        )
        _uiState.update {
            it.copy(
                selectedCity = gpsCity,
                isUsingGps = true
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("selected_city_name", resolvedName)
            repository.setSetting("selected_city_country", "الموقع التلقائي")
            repository.setSetting("selected_city_lat", lat.toString())
            repository.setSetting("selected_city_lon", lon.toString())
            repository.setSetting("selected_city_tz", "1.0")
            repository.setSetting("selected_city_en", nearest?.nameEn ?: "")
            repository.setSetting("selected_city_code", (nearest?.wilayaCode ?: 0).toString())
            repository.setSetting("is_using_gps", "true")
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
                onSuccess = { apiSchedule ->
                    val manualOffsets = mapOf(
                        PrayerType.FAJR to _uiState.value.fajrOffsetMinutes,
                        PrayerType.SUNRISE to _uiState.value.sunriseOffsetMinutes,
                        PrayerType.DHUHR to _uiState.value.dhuhrOffsetMinutes,
                        PrayerType.ASR to _uiState.value.asrOffsetMinutes,
                        PrayerType.MAGHRIB to _uiState.value.maghribOffsetMinutes,
                        PrayerType.ISHA to _uiState.value.ishaOffsetMinutes
                    )
                    val finalSchedule = if (manualOffsets.values.any { it != 0 }) {
                        PrayerTimesCalculator.calculatePrayerTimes(
                            latitude = city.latitude,
                            longitude = city.longitude,
                            calendar = Calendar.getInstance(),
                            method = _uiState.value.calculationMethod,
                            juristic = _uiState.value.juristicMethod,
                            manualOffsets = manualOffsets
                        )
                    } else {
                        apiSchedule
                    }

                    _uiState.update {
                        it.copy(
                            prayerSchedule = finalSchedule,
                            isAladhanSyncing = false,
                            aladhanSyncSuccess = true
                        )
                    }
                    schedulePrayerAlarms()
                    preCacheMonthlyPrayers()
                },
                onFailure = {
                    // Resilient offline fallback: keep local calculations completely active and reliable!
                    _uiState.update {
                        it.copy(
                            isAladhanSyncing = false,
                            aladhanSyncSuccess = null
                        )
                    }
                    recalculatePrayers()
                    preCacheMonthlyPrayers()
                }
            )
        }
    }

    fun setPrayerOffset(prayerType: PrayerType, offsetMinutes: Int) {
        val clamped = offsetMinutes.coerceIn(-30, 30)
        when (prayerType) {
            PrayerType.FAJR -> {
                _uiState.update { it.copy(fajrOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_fajr", clamped.toString()) }
            }
            PrayerType.SUNRISE -> {
                _uiState.update { it.copy(sunriseOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_sunrise", clamped.toString()) }
            }
            PrayerType.DHUHR -> {
                _uiState.update { it.copy(dhuhrOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_dhuhr", clamped.toString()) }
            }
            PrayerType.ASR -> {
                _uiState.update { it.copy(asrOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_asr", clamped.toString()) }
            }
            PrayerType.MAGHRIB -> {
                _uiState.update { it.copy(maghribOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_maghrib", clamped.toString()) }
            }
            PrayerType.ISHA -> {
                _uiState.update { it.copy(ishaOffsetMinutes = clamped) }
                viewModelScope.launch(Dispatchers.IO) { repository.setSetting("offset_isha", clamped.toString()) }
            }
        }
        recalculatePrayers()
        preCacheMonthlyPrayers()
    }

    fun resetPrayerOffsets() {
        _uiState.update {
            it.copy(
                fajrOffsetMinutes = 0,
                sunriseOffsetMinutes = 0,
                dhuhrOffsetMinutes = 0,
                asrOffsetMinutes = 0,
                maghribOffsetMinutes = 0,
                ishaOffsetMinutes = 0
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("offset_fajr", "0")
            repository.setSetting("offset_sunrise", "0")
            repository.setSetting("offset_dhuhr", "0")
            repository.setSetting("offset_asr", "0")
            repository.setSetting("offset_maghrib", "0")
            repository.setSetting("offset_isha", "0")
        }
        recalculatePrayers()
        preCacheMonthlyPrayers()
    }

    fun preCacheMonthlyPrayers() {
        viewModelScope.launch(Dispatchers.IO) {
            val city = _uiState.value.selectedCity
            val method = _uiState.value.calculationMethod
            val juristic = _uiState.value.juristicMethod
            val manualOffsets = mapOf(
                PrayerType.FAJR to _uiState.value.fajrOffsetMinutes,
                PrayerType.SUNRISE to _uiState.value.sunriseOffsetMinutes,
                PrayerType.DHUHR to _uiState.value.dhuhrOffsetMinutes,
                PrayerType.ASR to _uiState.value.asrOffsetMinutes,
                PrayerType.MAGHRIB to _uiState.value.maghribOffsetMinutes,
                PrayerType.ISHA to _uiState.value.ishaOffsetMinutes
            )
            val now = Calendar.getInstance()
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH) + 1
            val schedules = PrayerTimesCalculator.calculateMonthlyPrayerTimes(
                latitude = city.latitude,
                longitude = city.longitude,
                year = year,
                month = month,
                method = method,
                juristic = juristic,
                manualOffsets = manualOffsets
            )
            val entities = schedules.map { sched ->
                PrayerTimeEntity(
                    date = sched.dateString,
                    cityName = city.nameAr,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    fajr = sched.prayers.find { it.type == PrayerType.FAJR }?.timeFormatted ?: "",
                    sunrise = sched.prayers.find { it.type == PrayerType.SUNRISE }?.timeFormatted ?: "",
                    dhuhr = sched.prayers.find { it.type == PrayerType.DHUHR }?.timeFormatted ?: "",
                    asr = sched.prayers.find { it.type == PrayerType.ASR }?.timeFormatted ?: "",
                    maghrib = sched.prayers.find { it.type == PrayerType.MAGHRIB }?.timeFormatted ?: "",
                    isha = sched.prayers.find { it.type == PrayerType.ISHA }?.timeFormatted ?: "",
                    imsak = sched.imsakTimeFormatted ?: "",
                    midnight = sched.midnightTimeFormatted ?: "",
                    calculationMethod = method.name,
                    juristicMethod = juristic.name,
                    dateHijri = ""
                )
            }
            database.prayerTimesDao().insertAllPrayerTimes(entities)
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("calculation_method", method.name)
        }
        recalculatePrayers()
    }

    fun setJuristicMethod(method: JuristicMethod) {
        _uiState.update { it.copy(juristicMethod = method) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("juristic_method", method.name)
        }
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

    fun saveMushafProgress(pageNumber: Int, surahName: String, surahNumber: Int) {
        viewModelScope.launch {
            repository.updateQuranProgress(surahNumber, surahName, pageNumber)
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
        val manualOffsets = mapOf(
            PrayerType.FAJR to _uiState.value.fajrOffsetMinutes,
            PrayerType.SUNRISE to _uiState.value.sunriseOffsetMinutes,
            PrayerType.DHUHR to _uiState.value.dhuhrOffsetMinutes,
            PrayerType.ASR to _uiState.value.asrOffsetMinutes,
            PrayerType.MAGHRIB to _uiState.value.maghribOffsetMinutes,
            PrayerType.ISHA to _uiState.value.ishaOffsetMinutes
        )
        val schedule = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = city.latitude,
            longitude = city.longitude,
            calendar = Calendar.getInstance(),
            method = _uiState.value.calculationMethod,
            juristic = _uiState.value.juristicMethod,
            manualOffsets = manualOffsets
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

    // Compass Multi-Sensor Management
    fun startSensorListening() {
        if (isSensorListening || sensorManager == null) return
        isSensorListening = true

        when {
            rotationVectorSensor != null -> {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            }
            geomagneticSensor != null -> {
                sensorManager.registerListener(this, geomagneticSensor, SensorManager.SENSOR_DELAY_UI)
            }
            accelerometerSensor != null && magnetometerSensor != null -> {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_UI)
            }
            orientationSensor != null -> {
                @Suppress("DEPRECATION")
                sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopSensorListening() {
        if (!isSensorListening || sensorManager == null) return
        isSensorListening = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawAzimuth: Float? = null
        var pitch = 0f
        var roll = 0f

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                var deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (deg < 0) deg += 360f
                rawAzimuth = deg
                pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, 3)
                lastAccelerometerSet = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, 3)
                lastMagnetometerSet = true
            }
            @Suppress("DEPRECATION")
            Sensor.TYPE_ORIENTATION -> {
                var deg = event.values[0]
                if (deg < 0) deg += 360f
                rawAzimuth = deg
            }
        }

        if (rawAzimuth == null && lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, lastAccelerometer, lastMagnetometer)) {
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(r, orientationAngles)
                var deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (deg < 0) deg += 360f
                rawAzimuth = deg
                pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }
        }

        if (rawAzimuth != null) {
            val city = _uiState.value.selectedCity
            val declination = try {
                val geoField = GeomagneticField(
                    city.latitude.toFloat(),
                    city.longitude.toFloat(),
                    0f,
                    System.currentTimeMillis()
                )
                geoField.declination
            } catch (_: Exception) {
                0f
            }

            // Adjust heading to True North
            val trueNorthAzimuth = (rawAzimuth + declination + 360f) % 360f
            val smoothed = smoothHeading(trueNorthAzimuth)

            val qibla = _uiState.value.qiblaAngle.toFloat()
            var diff = kotlin.math.abs(smoothed - qibla) % 360f
            if (diff > 180f) diff = 360f - diff
            val isAligned = diff <= 4.0f

            val isFlat = kotlin.math.abs(pitch) < 35f && kotlin.math.abs(roll) < 35f

            _uiState.update {
                it.copy(
                    compassHeading = smoothed,
                    isPointedAtQibla = isAligned,
                    magneticDeclination = declination,
                    pitchDegrees = pitch,
                    rollDegrees = roll,
                    isFlatOnSurface = isFlat
                )
            }
        }
    }

    private fun smoothHeading(target: Float): Float {
        if (!hasInitialHeading) {
            smoothedAzimuth = target
            hasInitialHeading = true
            return target
        }
        var diff = (target - smoothedAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        val alpha = 0.18f
        smoothedAzimuth = ((smoothedAzimuth + diff * alpha) + 360f) % 360f
        return smoothedAzimuth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _uiState.update { it.copy(compassAccuracy = accuracy) }
    }

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
