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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * AdMobBannerView safely embeds a Google AdMob Banner Ad in Jetpack Compose,
 * handling loading errors gracefully and maintaining Material 3 spacing and visual cleanliness.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.TEST_BANNER_AD_UNIT_ID,
    adSize: AdSize = AdSize.BANNER
) {
    val isInspectionMode = LocalInspectionMode.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasLoadFailed by remember { mutableStateOf(false) }

    if (isInspectionMode) {
        // Compose Preview fallback
        AdPlaceholderBox(modifier = modifier, text = "إعلان تجريبي (AdMob Banner Preview)")
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .wrapContentSize()
                .padding(vertical = 4.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(adSize)
                    setAdUnitId(adUnitId)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            hasLoadFailed = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            hasLoadFailed = true
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )

        // Subtle placeholder shown only if the ad failed to load (offline or test environment)
        if (hasLoadFailed) {
            AdPlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                text = "مساحة إعلانية (Google AdMob)"
            )
        }
    }
}

@Composable
private fun AdPlaceholderBox(
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
