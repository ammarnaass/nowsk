package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val animatedHeading by animateFloatAsState(targetValue = uiState.compassHeading, label = "heading")
    val qiblaAngle = uiState.qiblaAngle.toFloat()

    // When aligned, trigger haptic once
    LaunchedEffect(uiState.isPointedAtQibla) {
        if (uiState.isPointedAtQibla) {
            onVibrate()
        }
    }

    val statusColor by animateColorAsState(
        targetValue = if (uiState.isPointedAtQibla) SuccessGreen else MaterialTheme.colorScheme.primary,
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
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Location & Target Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.selectedCity.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "زاوية القبلة: ${qiblaAngle.roundToInt()}° من الشمال",
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

            // Central Compass Canvas
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                // Rotating dial based on heading
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-animatedHeading)
                ) {
                    drawCompassDial(this, qiblaAngle, statusColor)
                }

                // Center Kaaba Emblem
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isPointedAtQibla) SuccessGreen else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕋",
                        fontSize = 26.sp
                    )
                }
            }

            // Direction & Alignment Status Message
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
                            text = "لا يمكن استخدام البوصلة على هذا الجهاز.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                } else if (uiState.isPointedAtQibla) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أنت الآن في الاتجاه الصحيح للقبلة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                } else {
                    Text(
                        text = "الاتجاه الحالي: ${animatedHeading.roundToInt()}° (وجّه الهاتف نحو ${qiblaAngle.roundToInt()}°)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calibration note (PRD Section 38)
                if (uiState.isCompassAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قم بتحريك الهاتف على شكل رقم 8 لمعايرة البوصلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun drawCompassDial(
    drawScope: DrawScope,
    qiblaAngle: Float,
    statusColor: Color
) {
    val center = drawScope.center
    val radius = drawScope.size.minDimension / 2f - 20f

    // Outer circle
    drawScope.drawCircle(
        color = Color.LightGray.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = 4f)
    )

    // Tick marks around circle
    for (i in 0 until 360 step 15) {
        val angleRad = Math.toRadians((i - 90).toDouble())
        val isMajor = i % 90 == 0
        val tickLength = if (isMajor) 24f else 12f
        val startX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
        val startY = center.y + (radius - tickLength) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y + radius * sin(angleRad).toFloat()

        drawScope.drawLine(
            color = if (isMajor) Color.Gray else Color.LightGray,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) 4f else 2f
        )
    }

    // North Indicator (Red)
    val northAngleRad = Math.toRadians(-90.0)
    val northX = center.x + (radius - 35f) * cos(northAngleRad).toFloat()
    val northY = center.y + (radius - 35f) * sin(northAngleRad).toFloat()
    drawScope.drawCircle(
        color = Color(0xFFD32F2F),
        radius = 8f,
        center = Offset(northX, northY)
    )

    // Qibla Pointer Line & Needle
    val qiblaAngleRad = Math.toRadians((qiblaAngle - 90).toDouble())
    val qiblaTipX = center.x + (radius - 10f) * cos(qiblaAngleRad).toFloat()
    val qiblaTipY = center.y + (radius - 10f) * sin(qiblaAngleRad).toFloat()

    drawScope.drawLine(
        color = statusColor,
        start = center,
        end = Offset(qiblaTipX, qiblaTipY),
        strokeWidth = 6f
    )

    drawScope.drawCircle(
        color = statusColor,
        radius = 12f,
        center = Offset(qiblaTipX, qiblaTipY)
    )
}
