package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.NusakkirUiState
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    uiState: NusakkirUiState,
    onVibrate: () -> Unit,
    modifier: Modifier = Modifier,
    onGpsLocationDetected: ((Double, Double) -> Unit)? = null,
    onStartCompass: (() -> Unit)? = null,
    onStopCompass: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var showCalibrationSheet by remember { mutableStateOf(false) }

    // Start/stop sensors along with screen lifecycle
    DisposableEffect(Unit) {
        onStartCompass?.invoke()
        onDispose {
            onStopCompass?.invoke()
        }
    }

    // Shortest-path continuous angular interpolation to prevent 360-degree back-spinning
    var unwrappedHeading by remember { mutableFloatStateOf(uiState.compassHeading) }
    var lastRawHeading by remember { mutableFloatStateOf(uiState.compassHeading) }

    LaunchedEffect(uiState.compassHeading) {
        var diff = (uiState.compassHeading - lastRawHeading) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        unwrappedHeading += diff
        lastRawHeading = uiState.compassHeading
    }

    val animatedHeading by animateFloatAsState(
        targetValue = unwrappedHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "heading"
    )

    val qiblaAngle = uiState.qiblaAngle.toFloat()

    // Trigger haptic vibration when perfectly aligned with Kaaba
    LaunchedEffect(uiState.isPointedAtQibla) {
        if (uiState.isPointedAtQibla) {
            onVibrate()
        }
    }

    // Permission launcher for GPS calibration
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
                    onGpsLocationDetected?.invoke(location.latitude, location.longitude)
                }
            } catch (_: SecurityException) {}
        }
    }

    fun requestGpsUpdate() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location != null) {
                    onGpsLocationDetected?.invoke(location.latitude, location.longitude)
                }
            } catch (_: SecurityException) {}
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Handle system back gesture
    if (onBackClick != null) {
        BackHandler {
            if (showCalibrationSheet) {
                showCalibrationSheet = false
            } else {
                onBackClick()
            }
        }
    }

    val statusColor by animateColorAsState(
        targetValue = if (uiState.isPointedAtQibla) SuccessGreen else GoldAccent,
        label = "statusColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اتجاه القبلة الشريفة",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    }
                },
                actions = {
                    // Calibration and Sensor Diagnostics dialog
                    IconButton(onClick = { showCalibrationSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "معايرة البوصلة والجيروسكوب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // GPS Location Refresh button
                    IconButton(onClick = { requestGpsUpdate() }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "تحديث الموقع عبر GPS",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Location & Target Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.selectedCity.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "زاوية القبلة: ${qiblaAngle.roundToInt()}° من الشمال الحقيقي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "المسافة إلى مكة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.distanceToMakkahKm.roundToInt()} كم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Sensor Accuracy & Calibration Status Badge (Clickable)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCalibrationSheet = true },
                color = when (uiState.compassAccuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SuccessGreen.copy(alpha = 0.12f)
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> GoldAccent.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.compassAccuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Icons.Default.Verified
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Icons.Default.CheckCircle
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Icons.Default.Warning
                            else -> Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = when (uiState.compassAccuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SuccessGreen
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> GoldAccent
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (uiState.compassAccuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "دقة البوصلة: ممتازة (معايرة)"
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "دقة البوصلة: جيدة"
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "دقة منخفضة • اضغط للمعايرة"
                            else -> "البوصلة بحاجة لمعايرة (رقم 8) • اضغط هنا"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (uiState.compassAccuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SuccessGreen
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> GoldAccent
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // 3. Central Compass & Rotating Dial
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = if (uiState.isPointedAtQibla) 3.dp else 1.dp,
                        color = if (uiState.isPointedAtQibla) SuccessGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating dial based on phone heading
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-animatedHeading)
                ) {
                    drawCompassDial(this, qiblaAngle, statusColor, isDark)
                }

                // Center Kaaba Emblem with subtle pulse when locked on
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isPointedAtQibla) SuccessGreen else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕋",
                        fontSize = 28.sp
                    )
                }
            }

            // 4. Direction & Alignment Status Message
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isCompassAvailable) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "لا تتوفر مستشعرات البوصلة على هذا الجهاز.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                } else if (uiState.isPointedAtQibla) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SuccessGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أنت الآن في الاتجاه الصحيح للقبلة 🕋",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                } else {
                    val currentH = ((animatedHeading % 360f + 360f) % 360f).roundToInt()
                    Text(
                        text = "الاتجاه الحالي: $currentH° • القبلة: ${qiblaAngle.roundToInt()}°",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Horizontal Phone Flatness Reminder
                if (!uiState.isFlatOnSurface && uiState.isCompassAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldAccent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ضع الهاتف بشكل أفقي مسطح لزيادة دقة القبلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. Sensor Calibration and Diagnostics Modal Bottom Sheet
    if (showCalibrationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCalibrationSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "معايرة البوصلة ومستشعرات الحركة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "دليل ضبط الجيروسكوب ومستشعرات تحديد القبلة بدقة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Figure-8 Animation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "♾️",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "حرّك الهاتف على شكل الرقم 8 (∞)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أمسك هاتفك وقم بتحريكه في الهواء برسم الرقم 8 ثلاث مرات بعيداً عن أية مصادر مغناطيسية أو معادن لإعادة ضبط المستشعر فورياً.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostics List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "حالة مستشعرات الجهاز (Hardware Diagnostics)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Gyroscope status
                        DiagnosticRow(
                            title = "مستشعر الجيروسكوب (Gyroscope)",
                            value = if (uiState.hasGyroscope) "مدعوم ومفعل بالكامل" else "غير متاح (يتم الاعتماد على التسارع والمغناطيسية)",
                            isSuccess = uiState.hasGyroscope
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sensor Fusion Mode
                        DiagnosticRow(
                            title = "نوع التتبع المستخدم",
                            value = uiState.sensorTypeUsed,
                            isSuccess = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Declination
                        DiagnosticRow(
                            title = "الانحراف المغناطيسي التلقائي",
                            value = "${String.format("%.1f", uiState.magneticDeclination)}° (مصحح للشمال الحقيقي)",
                            isSuccess = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Device Tilt
                        DiagnosticRow(
                            title = "استواء الهاتف",
                            value = if (uiState.isFlatOnSurface) "مستوٍ (مثالي)" else "مائل بزاوية ${uiState.pitchDegrees.roundToInt()}°",
                            isSuccess = uiState.isFlatOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Button(
                    onClick = {
                        requestGpsUpdate()
                        showCalibrationSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تحديث الموقع عبر GPS لدقة أعلى")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showCalibrationSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "تم وفهمت ذلك")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    value: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSuccess) SuccessGreen else GoldAccent
        )
    }
}

private fun drawCompassDial(
    drawScope: DrawScope,
    qiblaAngle: Float,
    statusColor: Color,
    isDarkTheme: Boolean
) {
    val center = drawScope.center
    val radius = drawScope.size.minDimension / 2f - 24f

    // Outer subtle dial circle
    drawScope.drawCircle(
        color = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
        radius = radius,
        center = center,
        style = Stroke(width = 3f)
    )

    // Tick marks around circle (every 15 and 30 degrees)
    for (i in 0 until 360 step 15) {
        val angleRad = Math.toRadians((i - 90).toDouble())
        val isMajor = i % 90 == 0
        val isMedium = i % 30 == 0
        val tickLength = if (isMajor) 22f else if (isMedium) 14f else 8f
        val startX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
        val startY = center.y + (radius - tickLength) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y + radius * sin(angleRad).toFloat()

        drawScope.drawLine(
            color = when {
                i == 0 -> Color(0xFFE53935) // North Red
                isMajor -> if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                isMedium -> if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Gray
                else -> if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.LightGray
            },
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) 4f else if (isMedium) 2.5f else 1.5f,
            cap = StrokeCap.Round
        )
    }

    // North Indicator Dot & Triangle (Red)
    val northAngleRad = Math.toRadians(-90.0)
    val northX = center.x + (radius - 36f) * cos(northAngleRad).toFloat()
    val northY = center.y + (radius - 36f) * sin(northAngleRad).toFloat()
    drawScope.drawCircle(
        color = Color(0xFFE53935),
        radius = 7f,
        center = Offset(northX, northY)
    )

    // Qibla Pointer Line & Needle
    val qiblaAngleRad = Math.toRadians((qiblaAngle - 90).toDouble())
    val qiblaTipX = center.x + (radius - 12f) * cos(qiblaAngleRad).toFloat()
    val qiblaTipY = center.y + (radius - 12f) * sin(qiblaAngleRad).toFloat()

    // Qibla Guide Line
    drawScope.drawLine(
        color = statusColor.copy(alpha = 0.9f),
        start = center,
        end = Offset(qiblaTipX, qiblaTipY),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )

    // Qibla Target Beacon Dot
    drawScope.drawCircle(
        color = statusColor,
        radius = 12f,
        center = Offset(qiblaTipX, qiblaTipY)
    )
    drawScope.drawCircle(
        color = Color.White,
        radius = 4f,
        center = Offset(qiblaTipX, qiblaTipY)
    )
}
