package com.vapps.module_ads.config

import androidx.lifecycle.MutableLiveData

class VioAdsConfig {
    companion object {
        var VARIANT_DEV = true

        const val PROVIDER_ADMOB = 0
        const val PROVIDER_MAX = 1


        const val ENVIRONMENT_DEVELOP = "develop"
        const val ENVIRONMENT_PRODUCTION = "production"

        const val DEFAULT_TOKEN_FACEBOOK_SDK = "client_token"

        /**
         * current total revenue for paid_ad_impression_value_0.01 event
         */
        var currentTotalRevenue001Ad = 0f

        var messageInit = MutableLiveData<String>()

        var maxClickAds: Int = 100
    }
}