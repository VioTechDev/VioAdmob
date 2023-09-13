package com.vapps.module_ads.event

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.vapps.module_ads.config.VioAdsConfig

object FirebaseAnalyticsUtil {
    fun logEventWithAds(context: Context, params: Bundle?) {
        FirebaseAnalytics.getInstance(context).logEvent("paid_ad_impression", params)
    }

    fun logPaidAdImpressionValue(context: Context, bundle: Bundle?, mediationProvider: Int) {
        if (mediationProvider == VioAdsConfig.PROVIDER_MAX) FirebaseAnalytics.getInstance(context)
            .logEvent("max_paid_ad_impression_value", bundle) else FirebaseAnalytics.getInstance(
            context
        ).logEvent("paid_ad_impression_value", bundle)
    }

    fun logClickAdsEvent(context: Context, bundle: Bundle?) {
        FirebaseAnalytics.getInstance(context).logEvent("event_user_click_ads", bundle)
    }

    fun logCurrentTotalRevenueAd(context: Context, eventName: String, bundle: Bundle?) {
        FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
    }

    fun logTotalRevenue001Ad(context: Context, bundle: Bundle?) {
        FirebaseAnalytics.getInstance(context).logEvent("paid_ad_impression_value_001", bundle)
    }
}