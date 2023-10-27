package com.vapps.module_ads.control.model

import com.applovin.mediation.ads.MaxRewardedAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.vapps.module_ads.control.utils.StatusAd

class ApRewardAd : ApAdBase {
    private var admobReward: RewardedAd? = null
    var admobRewardInter: RewardedInterstitialAd? = null
        private set
    private var maxReward: MaxRewardedAd? = null

    constructor()
    constructor(status: StatusAd?) : super(status!!)

    fun setAdmobReward(admobReward: RewardedAd?) {
        this.admobReward = admobReward
        status = StatusAd.AD_LOADED
    }

    fun setAdmobReward(admobRewardInter: RewardedInterstitialAd?) {
        this.admobRewardInter = admobRewardInter
    }

    fun setMaxReward(maxReward: MaxRewardedAd?) {
        this.maxReward = maxReward
        status = StatusAd.AD_LOADED
    }

    constructor(maxReward: MaxRewardedAd?) {
        this.maxReward = maxReward
        status = StatusAd.AD_LOADED
    }

    constructor(admobRewardInter: RewardedInterstitialAd?) {
        this.admobRewardInter = admobRewardInter
        status = StatusAd.AD_LOADED
    }

    constructor(admobReward: RewardedAd?) {
        this.admobReward = admobReward
        status = StatusAd.AD_LOADED
    }

    fun getAdmobReward(): RewardedAd? {
        return admobReward
    }

    fun getMaxReward(): MaxRewardedAd? {
        return maxReward
    }

    /**
     * Clean reward when shown
     */
    fun clean() {
        maxReward = null
        admobReward = null
        admobRewardInter = null
    }

    override val isReady: Boolean
        get() = admobReward != null || admobRewardInter != null || maxReward != null && maxReward!!.isReady
    val isRewardInterstitial: Boolean
        get() = admobRewardInter != null
}