package com.vapps.module_ads.control.config

import android.app.Application

class AperoAdConfig {
    /**
     * config number of times can reloads
     */
    var numberOfTimesReloadAds = 0

    /**
     * config ad mediation using for app
     */
    var mediationProvider = PROVIDER_ADMOB
    var isVariantDev = false
        private set

    /**
     * adjustConfig enable adjust and setup adjust token
     */
    var adjustConfig: AdjustConfig? = null

    /**
     * eventNamePurchase push event to adjust when user purchased
     */
    val eventNamePurchase = ""
    var idAdResume: String? = null
        set(idAdResume) {
            field = idAdResume
            isEnableAdResume = true
        }
    var idAdResumeMedium: String? = null
    var idAdResumeHigh: String? = null
    var listDeviceTest: ArrayList<String> = ArrayList()
    var application: Application
        private set
    var isEnableAdResume = false
        private set
    var facebookClientToken = DEFAULT_TOKEN_FACEBOOK_SDK

    /**
     * intervalInterstitialAd: time between two interstitial ad impressions
     * unit: seconds
     */
    var intervalInterstitialAd = 0

    constructor(application: Application) {
        this.application = application
    }

    constructor(application: Application, mediationProvider: Int, environment: String) {
        this.mediationProvider = mediationProvider
        isVariantDev = environment == ENVIRONMENT_DEVELOP
        this.application = application
    }

    @Deprecated("As of release 5.5.0, replaced by {@link #setEnvironment(String)}")
    fun setVariant(isVariantDev: Boolean) {
        this.isVariantDev = isVariantDev
    }

    fun setEnvironment(environment: String) {
        isVariantDev = environment == ENVIRONMENT_DEVELOP
    }

    companion object {
        //switch mediation use for app
        const val PROVIDER_ADMOB = 0
        const val PROVIDER_MAX = 1
        const val ENVIRONMENT_DEVELOP = "develop"
        const val ENVIRONMENT_PRODUCTION = "production"
        const val DEFAULT_TOKEN_FACEBOOK_SDK = "client_token"
    }
}