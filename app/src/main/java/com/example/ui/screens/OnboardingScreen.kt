package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicSurfaceVariantLight
import kotlinx.coroutines.launch

data class OnboardingStep(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accentTag: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    onGpsDetected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val steps = remember {
        listOf(
            OnboardingStep(
                titleRes = R.string.onboarding_title_1,
                descRes = R.string.onboarding_desc_1,
                icon = Icons.Default.Mosque,
                accentTag = "صلواتك في وقتها"
            ),
            OnboardingStep(
                titleRes = R.string.onboarding_title_2,
                descRes = R.string.onboarding_desc_2,
                icon = Icons.Default.MenuBook,
                accentTag = "نور قلبك بالذكر"
            ),
            OnboardingStep(
                titleRes = R.string.onboarding_title_3,
                descRes = R.string.onboarding_desc_3,
                icon = Icons.Default.Explore,
                accentTag = "اتجاهك وذكرك"
            ),
            OnboardingStep(
                titleRes = R.string.onboarding_title_4,
                descRes = R.string.onboarding_desc_4,
                icon = Icons.Default.Shield,
                accentTag = "تجربة مخصصة لك"
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })

    // Track Permission Statuses
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Permission launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted

        if (hasLocationPermission && onGpsDetected != null) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    onGpsDetected(loc.latitude, loc.longitude)
                }
            } catch (_: SecurityException) {}
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Explicit RTL layout guarantees correct Arabic reading order and icon flips
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("onboarding_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar with Skip Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visual App Brand Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Skip Button on earlier steps
                    if (pagerState.currentPage < steps.size - 1) {
                        TextButton(
                            onClick = onFinishOnboarding,
                            modifier = Modifier.testTag("btn_onboarding_skip")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Spacer to balance layout
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Middle Content: Swipeable Pages
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    val isLastPage = pageIndex == steps.size - 1

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Hero Icon with Islamic Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(GoldAccent, MaterialTheme.colorScheme.primary)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = stringResource(step.titleRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(68.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Category / Tag Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = step.accentTag,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        // Headline Title
                        Text(
                            text = stringResource(step.titleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Description paragraph
                        Text(
                            text = stringResource(step.descRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // If on the permissions page (Page 4), show contextual permission grant cards
                        if (isLastPage) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Location Permission Context Card
                            PermissionContextCard(
                                title = stringResource(R.string.perm_location_title),
                                description = stringResource(R.string.perm_location_desc),
                                icon = Icons.Default.LocationOn,
                                isGranted = hasLocationPermission,
                                testTag = "perm_card_location",
                                onRequestClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Notification Permission Context Card
                            PermissionContextCard(
                                title = stringResource(R.string.perm_notification_title),
                                description = stringResource(R.string.perm_notification_desc),
                                icon = Icons.Default.NotificationsActive,
                                isGranted = hasNotificationPermission,
                                testTag = "perm_card_notification",
                                onRequestClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        hasNotificationPermission = true
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Privacy note
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.perm_privacy_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar: Indicator dots and navigation buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pager Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        repeat(steps.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 26.dp else 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Navigation Action Button
                    val isLastPage = pagerState.currentPage == steps.size - 1

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinishOnboarding()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = tween(durationMillis = 350)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag(if (isLastPage) "btn_onboarding_start" else "btn_onboarding_next"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLastPage) stringResource(R.string.onboarding_start) else stringResource(R.string.onboarding_next),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isLastPage) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionContextCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    testTag: String,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isGranted)
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, EmeraldPrimaryLight)
                )
            )
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isGranted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // Action or Status Pill
            if (isGranted) {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.perm_granted),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onRequestClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = stringResource(R.string.perm_request),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
