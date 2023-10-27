package com.vapps.module_ads.control.model

import com.google.android.gms.ads.interstitial.InterstitialAd
import com.vapps.module_ads.control.utils.StatusAd

class ApInterstitialAd : ApAdBase {
    private var interstitialAd: InterstitialAd? = null

    constructor(status: StatusAd?) : super(status!!)
    constructor()
    constructor(interstitialAd: InterstitialAd?) {
        this.interstitialAd = interstitialAd
        status = StatusAd.AD_LOADED
    }

    fun setInterstitialAd(interstitialAd: InterstitialAd?) {
        this.interstitialAd = interstitialAd
        status = StatusAd.AD_LOADED
    }

    override val isReady: Boolean
        get() = interstitialAd != null

    fun getInterstitialAd(): InterstitialAd? {
        return interstitialAd
    }
}