package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager is a centralized helper that coordinates Google Mobile Ads (AdMob) initialization,
 * adaptive banner sizing calculation, request building, and full-screen ad preloading.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob sample test IDs (safe for testing & development)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val isInitializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    val isInitialized: Boolean
        get() = initialized.get()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    /**
     * Initializes Google Mobile Ads SDK on a background thread.
     * Safe to call multiple times; redundant calls are ignored.
     */
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (initialized.get()) {
            onComplete?.invoke()
            return
        }
        if (!isInitializing.compareAndSet(false, true)) {
            return
        }

        try {
            // Configure global test device parameters
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            MobileAds.initialize(context) { status ->
                initialized.set(true)
                isInitializing.set(false)
                Log.d(TAG, "AdMob MobileAds initialized successfully: $status")

                // Preload full-screen formats
                preloadInterstitial(context)
                preloadRewarded(context)

                onComplete?.invoke()
            }
        } catch (e: Exception) {
            isInitializing.set(false)
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Builds a standard AdRequest for banner or full-screen ads.
     */
    fun buildAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Computes the Google AdMob Anchored Adaptive Banner size dynamically
     * based on current device orientation and available width in density-independent pixels (dp).
     */
    fun getAdaptiveBannerAdSize(context: Context, widthInDp: Int): AdSize {
        val safeWidth = widthInDp.coerceAtLeast(320)
        return try {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, safeWidth)
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to standard BANNER size: ${e.message}")
            AdSize.BANNER
        }
    }

    /**
     * Creates and configures an AdView instance for banner ads.
     */
    @MainThread
    fun createAdaptiveBannerAdView(
        context: Context,
        adUnitId: String,
        adSize: AdSize,
        adListener: AdListener? = null
    ): AdView {
        return AdView(context).apply {
            this.setAdSize(adSize)
            this.setAdUnitId(adUnitId)
            adListener?.let { this.adListener = it }
            loadAd(buildAdRequest())
        }
    }

    // ---------------------------------------------------------
    // Interstitial Ad Management
    // ---------------------------------------------------------

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            buildAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            preloadInterstitial(activity)
            onAdDismissed()
        }
    }

    // ---------------------------------------------------------
    // Rewarded Ad Management
    // ---------------------------------------------------------

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            buildAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onUserEarnedReward: (Int, String) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preloadRewarded(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    preloadRewarded(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity) { rewardItem ->
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
        } else {
            preloadRewarded(activity)
            onAdDismissed()
        }
    }
}
