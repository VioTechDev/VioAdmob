package com.vapps.module_ads.control.listener

import com.vapps.module_ads.control.model.ApAdError
import com.vapps.module_ads.control.model.ApInterstitialAd
import com.vapps.module_ads.control.model.ApNativeAd
import com.vapps.module_ads.control.model.ApRewardItem


open class AperoAdCallback {
    open fun onNextAction() {}
    open fun onAdClosed() {}
    open fun onAdFailedToLoad(adError: ApAdError?) {}
    open fun onAdFailedToShow(adError: ApAdError?) {}
    open fun onAdLeftApplication() {}
    open fun onAdLoaded() {}

    // ad splash loaded when showSplashIfReady = false
    open fun onAdSplashReady() {}
    open fun onInterstitialLoad(interstitialAd: ApInterstitialAd?) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    open fun onNativeAdLoaded(nativeAd: ApNativeAd) {}
    open fun onUserEarnedReward(rewardItem: ApRewardItem) {}
    open fun onInterstitialShow() {}
}