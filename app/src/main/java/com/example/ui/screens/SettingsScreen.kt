package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.CityLocation
import com.example.data.prayer.CityPresets
import com.example.data.prayer.JuristicMethod
import com.example.ui.NusakkirUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: NusakkirUiState,
    onCitySelected: (CityLocation) -> Unit,
    onGpsLocationDetected: (Double, Double) -> Unit,
    onMethodSelected: (CalculationMethod) -> Unit,
    onJuristicSelected: (JuristicMethod) -> Unit,
    onThemeSelected: (String) -> Unit,
    onToggleNotification: (String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onAladhanMethodSelected: (Int) -> Unit = {},
    onSyncAladhanApi: () -> Unit = {},
    onSearchPlaces: (String) -> Unit = {},
    onToggleAdhanSound: (Boolean) -> Unit = {},
    onPlayAdhanPreview: () -> Unit = {},
    onStopAdhan: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showJuristicDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // GPS location launcher using native Android LocationManager
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (location != null) {
                    onGpsLocationDetected(location.latitude, location.longitude)
                    snackbarMessage = "تم تحديد موقعك التلقائي بنجاح"
                } else {
                    snackbarMessage = "تعذر قراءة الموقع، يرجى اختيار المدينة يدوياً"
                }
            } catch (_: SecurityException) {
                snackbarMessage = "إذن الموقع غير متاح"
            }
        } else {
            snackbarMessage = "تم رفض إذن الوصول للموقع"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("حسناً")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
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
            // Section 1: Location & Prayer calculation
            item {
                Text(
                    text = "الموقع ومواقيت الصلاة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // City Picker
                        ListItem(
                            headlineContent = { Text("المدينة الحالية") },
                            supportingContent = { Text("${uiState.selectedCity.nameAr} (${uiState.selectedCity.countryAr})") },
                            leadingContent = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                            modifier = Modifier.clickable { showCityDialog = true }
                        )

                        HorizontalDivider()

                        // GPS Auto detect
                        ListItem(
                            headlineContent = { Text("تحديد موقعي التلقائي عبر GPS") },
                            supportingContent = { Text(if (uiState.isUsingGps) "مفعل حالياً" else "استخدام مستشعر الهاتف") },
                            leadingContent = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.GpsFixed, contentDescription = null) },
                            modifier = Modifier.clickable {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )

                        HorizontalDivider()

                        // Calculation Method
                        ListItem(
                            headlineContent = { Text("طريقة حساب أوقات الصلاة") },
                            supportingContent = {
                                val activeMethodName = uiState.aladhanMethods.find { it.id == uiState.selectedAladhanMethodId }?.nameAr
                                    ?: uiState.calculationMethod.displayNameAr
                                Text(activeMethodName)
                            },
                            leadingContent = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                            modifier = Modifier.clickable { showMethodDialog = true }
                        )

                        HorizontalDivider()

                        // Juristic Method
                        ListItem(
                            headlineContent = { Text("المذهب الفقهي (صلاة العصر)") },
                            supportingContent = { Text(uiState.juristicMethod.displayNameAr) },
                            leadingContent = { Icon(Icons.Default.Balance, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                            modifier = Modifier.clickable { showJuristicDialog = true }
                        )

                        HorizontalDivider()

                        // Aladhan API Sync
                        ListItem(
                            headlineContent = { Text("مزامنة مواقيت Aladhan API") },
                            supportingContent = {
                                Text(
                                    when {
                                        uiState.isAladhanSyncing -> "جاري التحديث عبر الإنترنت..."
                                        uiState.aladhanSyncSuccess == true -> "تم التحديث بنجاح من Aladhan API (محفوظ ومخزن في قاعدة البيانات)"
                                        uiState.aladhanSyncSuccess == false -> "تعذّر الاتصال، يتم الاعتماد على الحساب الفلكي المخزن دون إنترنت"
                                        else -> "انقر للمزامنة المباشرة مع Aladhan API"
                                    }
                                )
                            },
                            leadingContent = {
                                if (uiState.isAladhanSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (uiState.aladhanSyncSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onSyncAladhanApi() },
                                    enabled = !uiState.isAladhanSyncing
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "تحديث الآن")
                                }
                            },
                            modifier = Modifier.clickable {
                                if (!uiState.isAladhanSyncing) {
                                    onSyncAladhanApi()
                                }
                            }
                        )
                    }
                }
            }

            // Section 2: Notifications & Reminders
            item {
                Text(
                    text = "التنبيهات والأذان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Main Adhan sound toggle
                        ListItem(
                            headlineContent = { Text("صوت الأذان عند دخول الوقت") },
                            supportingContent = { Text("تشغيل صوت الأذان كاملاً تلقائياً مع الإشعار") },
                            leadingContent = { Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.playAdhanSound,
                                    onCheckedChange = { onToggleAdhanSound(it) }
                                )
                            }
                        )
                        HorizontalDivider()

                        // Adhan audio preview & stop row
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = if (uiState.isAdhanPlaying) "الأذان قيد التشغيل..." else "معاينة صوت الأذان",
                                    fontWeight = if (uiState.isAdhanPlaying) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.isAdhanPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = { Text("أذان الحرم المكي الشريف بصوت عذب ومتقن") },
                            leadingContent = {
                                Icon(
                                    imageVector = if (uiState.isAdhanPlaying) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (uiState.isAdhanPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            },
                            trailingContent = {
                                if (uiState.isAdhanPlaying) {
                                    Button(
                                        onClick = onStopAdhan,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إيقاف")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = onPlayAdhanPreview
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("استماع")
                                    }
                                }
                            }
                        )
                        HorizontalDivider()

                        // Test notification button
                        ListItem(
                            headlineContent = { Text("إرسال إشعار تجريبي الآن") },
                            supportingContent = { Text("تجربة ظهور الإشعار في شريط الحالة مع صوت الأذان") },
                            leadingContent = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = {
                                        onTestNotification()
                                        snackbarMessage = "تم إرسال إشعار تجريبي مع صوت الأذان"
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تجربة")
                                }
                            }
                        )
                        HorizontalDivider()

                        // Notifications toggle
                        ListItem(
                            headlineContent = { Text("إشعارات أوقات الصلوات") },
                            leadingContent = { Icon(Icons.Default.Alarm, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.notifyOnPrayer,
                                    onCheckedChange = { onToggleNotification("prayer", it) }
                                )
                            }
                        )
                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("تنبيه قبل الصلاة بـ 15 دقيقة") },
                            leadingContent = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.notifyBeforePrayer,
                                    onCheckedChange = { onToggleNotification("before_prayer", it) }
                                )
                            }
                        )
                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("تذكير أذكار الصباح") },
                            leadingContent = { Icon(Icons.Default.WbSunny, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.notifyMorningAdhkar,
                                    onCheckedChange = { onToggleNotification("morning_adhkar", it) }
                                )
                            }
                        )
                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("تذكير أذكار المساء") },
                            leadingContent = { Icon(Icons.Default.NightsStay, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.notifyEveningAdhkar,
                                    onCheckedChange = { onToggleNotification("evening_adhkar", it) }
                                )
                            }
                        )
                    }
                }
            }

            // Section 3: Appearance & Theme
            item {
                Text(
                    text = "المظهر والتخصيص",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("مظهر التطبيق") },
                            supportingContent = {
                                Text(
                                    when (uiState.selectedThemeMode) {
                                        "LIGHT" -> "الوضع الفاتح"
                                        "DARK" -> "الوضع الداكن"
                                        else -> "تلقائي (حسب النظام)"
                                    }
                                )
                            },
                            leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                            modifier = Modifier.clickable { showThemeDialog = true }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("عن تطبيق نسكّر") },
                            supportingContent = { Text("الإصدار 1.0.0 — يعمل دون اتصال بالإنترنت") },
                            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                            modifier = Modifier.clickable { showAboutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // City Selection Dialog
    if (showCityDialog) {
        val allPlaces = if (uiState.placesList.isNotEmpty()) uiState.placesList else CityPresets.CITIES
        val filteredPlaces = remember(allPlaces, citySearchQuery) {
            if (citySearchQuery.isBlank()) {
                allPlaces
            } else {
                allPlaces.filter {
                    it.nameAr.contains(citySearchQuery, ignoreCase = true) ||
                    it.countryAr.contains(citySearchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showCityDialog = false
                citySearchQuery = ""
            },
            title = { Text("اختر المدينة أو الولاية") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = {
                            citySearchQuery = it
                            onSearchPlaces(it)
                        },
                        placeholder = { Text("ابحث عن مدينة أو ولاية...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (citySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { citySearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredPlaces) { city ->
                            val isSelected = city.nameAr == uiState.selectedCity.nameAr
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "${city.nameAr} - ${city.countryAr}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onCitySelected(city)
                                    showCityDialog = false
                                    citySearchQuery = ""
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCityDialog = false
                    citySearchQuery = ""
                }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Calculation Method Dialog
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("طريقة حساب المواقيت (الدوال)") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    if (uiState.aladhanMethods.isNotEmpty()) {
                        items(uiState.aladhanMethods) { method ->
                            val isSelected = method.id == uiState.selectedAladhanMethodId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAladhanMethodSelected(method.id)
                                        showMethodDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onAladhanMethodSelected(method.id)
                                        showMethodDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = method.nameAr,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = method.nameEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    } else {
                        items(CalculationMethod.values().toList()) { method ->
                            val isSelected = method == uiState.calculationMethod
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMethodSelected(method)
                                        showMethodDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onMethodSelected(method)
                                        showMethodDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = method.displayNameAr,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Juristic Method Dialog
    if (showJuristicDialog) {
        AlertDialog(
            onDismissRequest = { showJuristicDialog = false },
            title = { Text("المذهب الفقهي لصلاة العصر") },
            text = {
                Column {
                    JuristicMethod.values().forEach { juristic ->
                        val isSelected = juristic == uiState.juristicMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onJuristicSelected(juristic)
                                    showJuristicDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onJuristicSelected(juristic)
                                    showJuristicDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = juristic.displayNameAr,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJuristicDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Theme Mode Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("مظهر التطبيق") },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to "تلقائي (حسب النظام)",
                        "LIGHT" to "الوضع الفاتح",
                        "DARK" to "الوضع الداكن"
                    ).forEach { (mode, label) ->
                        val isSelected = uiState.selectedThemeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeSelected(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onThemeSelected(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("عن تطبيق نسكّر") },
            text = {
                Column {
                    Text(
                        text = "تطبيق إسلامي شامل وحديث يساعد المسلم على متابعة عباداته اليومية من مكان واحد بكل سكينة وسهولة وبدون إعلانات مزعجة.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• مواقيت دقيقة بدون إنترنت\n• قرآن كريم وأذكار حصن المسلم\n• بوصلة القبلة ومسبحة إلكترونية\n• حماية تامة للخصوصية بدون الحاجة لحساب",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}
