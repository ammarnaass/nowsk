package com.example.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.AdSize

/**
 * AdMobBannerView renders an adaptive banner ad by default,
 * delegating to [AdaptiveBannerAd].
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.TEST_BANNER_AD_UNIT_ID,
    adSize: AdSize? = null
) {
    AdaptiveBannerAd(
        modifier = modifier,
        adUnitId = adUnitId
    )
}
