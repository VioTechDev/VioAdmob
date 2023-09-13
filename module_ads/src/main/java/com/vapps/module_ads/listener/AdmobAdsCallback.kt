package com.vapps.module_ads.listener

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

interface AdmobAdsCallback {
    fun onNextAction() {}
    fun onAdClosed() {}
    open fun onAdFailedToLoad(i: LoadAdError?) {}
    open fun onAdFailedToShow(adError: AdError?) {}
    fun onAdLeftApplication() {}
    fun onAdLoaded() {}
    fun onAdSplashReady() {}
    open fun onInterstitialLoad(interstitialAd: InterstitialAd?) {}
    fun onAdClicked() {}
    fun onAdImpression() {}
    fun onRewardAdLoaded(rewardedAd: RewardedAd?) {}
    fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {}
    fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {}
    fun onInterstitialShow() {}
    fun onBannerLoaded(adView: AdView?) {}
}