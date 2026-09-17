package com.example.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Renders a Google AdMob Anchored Adaptive Banner Ad in Jetpack Compose.
 *
 * It dynamically computes the adaptive banner height based on available width,
 * observes the Android Lifecycle (handling pause, resume, destroy),
 * and provides testTag and preview fallbacks.
 */
@Composable
fun AdaptiveBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.TEST_BANNER_AD_UNIT_ID,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null
) {
    val isInspectionMode = LocalInspectionMode.current

    if (isInspectionMode) {
        AdaptiveBannerPlaceholder(
            modifier = modifier,
            text = "إعلان تكيفي تجريبي (Adaptive Banner Preview)"
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adaptive_banner_ad_container"),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val containerWidthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adaptiveSize = remember(containerWidthDp) {
            AdManager.getAdaptiveBannerAdSize(context, containerWidthDp)
        }

        var isLoaded by remember { mutableStateOf(false) }
        var loadFailed by remember { mutableStateOf(false) }

        // Track AdView instance to properly hook into lifecycle
        var adViewInstance by remember { mutableStateOf<AdView?>(null) }

        DisposableEffect(lifecycleOwner, adViewInstance) {
            val adView = adViewInstance
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView?.resume()
                    Lifecycle.Event.ON_PAUSE -> adView?.pause()
                    Lifecycle.Event.ON_DESTROY -> adView?.destroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView?.destroy()
            }
        }

        AndroidView(
            modifier = Modifier
                .wrapContentSize()
                .padding(vertical = 4.dp)
                .testTag("adaptive_banner_ad_view"),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adaptiveSize)
                    setAdUnitId(adUnitId)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isLoaded = true
                            loadFailed = false
                            onAdLoaded?.invoke()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoaded = false
                            loadFailed = true
                            onAdFailedToLoad?.invoke(error)
                        }
                    }
                    adViewInstance = this
                    loadAd(AdManager.buildAdRequest())
                }
            },
            update = { view ->
                // If the adaptive size changed significantly, update ad size
                if (view.adSize != adaptiveSize) {
                    view.setAdSize(adaptiveSize)
                }
            }
        )

        // Clean fallback placeholder when offline or test ad fails to load
        if (loadFailed) {
            AdaptiveBannerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                text = "مساحة إعلانية تكيفية (Google AdMob Adaptive Banner)"
            )
        }
    }
}

@Composable
private fun AdaptiveBannerPlaceholder(
    modifier: Modifier = Modifier,
    text: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
