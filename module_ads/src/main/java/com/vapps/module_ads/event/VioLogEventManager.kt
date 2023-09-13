package com.vapps.module_ads.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.applovin.mediation.MaxAd
import com.google.android.gms.ads.AdValue
import com.vapps.module_ads.config.VioAdjust
import com.vapps.module_ads.config.VioAdsConfig
import com.vapps.module_ads.utils.AdType
import com.vapps.module_ads.utils.SharePreferenceUtils
import com.vungle.warren.model.Advertisement


object VioLogEventManager {
    private const val TAG = "VioLogEventManager"
    fun logPaidAdImpression(
        context: Context,
        adValue: AdValue,
        adUnitId: String,
        mediationAdapterClassName: String,
        adType: AdType?
    ) {
        logEventWithAds(
            context,
            adValue.valueMicros.toFloat(),
            adValue.precisionType,
            adUnitId,
            mediationAdapterClassName,
            VioAdsConfig.PROVIDER_ADMOB
        )
        VioAdjust.pushTrackEventAdmob(adValue)
    }

    fun logPaidAdImpression(context: Context, adValue: MaxAd, adType: AdType?) {
        logEventWithAds(
            context,
            adValue.revenue.toFloat(),
            0,
            adValue.adUnitId,
            adValue.networkName,
            VioAdsConfig.PROVIDER_MAX
        )
        VioAdjust.pushTrackEventApplovin(adValue, context)
    }

    private fun logEventWithAds(
        context: Context,
        revenue: Float,
        precision: Int,
        adUnitId: String,
        network: String,
        mediationProvider: Int
    ) {
        Log.d(
            TAG, String.format(
                "Paid event of value %.0f microcents in currency USD of precision %s%n occurred for ad unit %s from ad network %s.mediation provider: %s%n",
                revenue,
                precision,
                adUnitId,
                network, mediationProvider
            )
        )
        val params = Bundle() // Log ad value in micros.
        params.putDouble("valuemicros", revenue.toDouble())
        params.putString("currency", "USD")
        // These values below won’t be used in ROAS recipe.
        // But log for purposes of debugging and future reference.
        params.putInt("precision", precision)
        params.putString("adunitid", adUnitId)
        params.putString("network", network)

        // log revenue this ad
        logPaidAdImpressionValue(
            context,
            revenue / 1000000.0,
            precision,
            adUnitId,
            network,
            mediationProvider
        )
        FirebaseAnalyticsUtil.logEventWithAds(context, params)
        FacebookEventUtils.logEventWithAds(context, params)
        // update current tota
        // l revenue ads
        SharePreferenceUtils.updateCurrentTotalRevenueAd(context, revenue)
        logCurrentTotalRevenueAd(context, "event_current_total_revenue_ad")

        // update current total revenue ads for event paid_ad_impression_value_0.01
        VioAdsConfig.currentTotalRevenue001Ad += revenue
        SharePreferenceUtils.updateCurrentTotalRevenue001Ad(
            context,
            VioAdsConfig.currentTotalRevenue001Ad
        )
        logTotalRevenue001Ad(context)
        logTotalRevenueAdIn3DaysIfNeed(context)
        logTotalRevenueAdIn7DaysIfNeed(context)
    }

    private fun logPaidAdImpressionValue(
        context: Context,
        value: Double,
        precision: Int,
        adUnitId: String,
        network: String,
        mediationProvider: Int
    ) {
        val params = Bundle()
        params.putDouble("value", value)
        params.putString("currency", "USD")
        params.putInt("precision", precision)
        params.putString("adunitid", adUnitId)
        params.putString("network", network)
        VioAdjust.logPaidAdImpressionValue(value, "USD")
        FirebaseAnalyticsUtil.logPaidAdImpressionValue(context, params, mediationProvider)
        FacebookEventUtils.logPaidAdImpressionValue(context, params, mediationProvider)
    }

    fun logClickAdsEvent(context: Context, adUnitId: String?) {
        Log.d(
            TAG, String.format(
                "User click ad for ad unit %s.",
                adUnitId
            )
        )
        val bundle = Bundle()
        bundle.putString("ad_unit_id", adUnitId)
        FirebaseAnalyticsUtil.logClickAdsEvent(context, bundle)
        FacebookEventUtils.logClickAdsEvent(context, bundle)
    }

    fun logCurrentTotalRevenueAd(context: Context, eventName: String) {
        val currentTotalRevenue: Float = SharePreferenceUtils.getCurrentTotalRevenueAd(context)
        val bundle = Bundle()
        bundle.putFloat("value", currentTotalRevenue)
        FirebaseAnalyticsUtil.logCurrentTotalRevenueAd(context, eventName, bundle)
        FacebookEventUtils.logCurrentTotalRevenueAd(context, eventName, bundle)
    }

    fun logTotalRevenue001Ad(context: Context) {
        val revenue: Float = VioAdsConfig.currentTotalRevenue001Ad
        if (revenue / 1000000 >= 0.01) {
            VioAdsConfig.currentTotalRevenue001Ad = 0F
            SharePreferenceUtils.updateCurrentTotalRevenue001Ad(context, 0F)
            val bundle = Bundle()
            bundle.putFloat("value", revenue / 1000000)
            FirebaseAnalyticsUtil.logTotalRevenue001Ad(context, bundle)
            FacebookEventUtils.logTotalRevenue001Ad(context, bundle)
        }
    }

    fun logTotalRevenueAdIn3DaysIfNeed(context: Context) {
        val installTime: Long = SharePreferenceUtils.getInstallTime(context)
        if (!SharePreferenceUtils.isPushRevenue3Day(context) && System.currentTimeMillis() - installTime >= 3L * 24 * 60 * 60 * 1000) {
            Log.d(TAG, "logTotalRevenueAdAt3DaysIfNeed: ")
            logCurrentTotalRevenueAd(context, "event_total_revenue_ad_in_3_days")
            SharePreferenceUtils.setPushedRevenue3Day(context)
        }
    }

    fun logTotalRevenueAdIn7DaysIfNeed(context: Context) {
        val installTime: Long = SharePreferenceUtils.getInstallTime(context)
        if (!SharePreferenceUtils.isPushRevenue7Day(context) && System.currentTimeMillis() - installTime >= 7L * 24 * 60 * 60 * 1000) {
            Log.d(TAG, "logTotalRevenueAdAt7DaysIfNeed: ")
            logCurrentTotalRevenueAd(context, "event_total_revenue_ad_in_7_days")
            SharePreferenceUtils.setPushedRevenue7Day(context)
        }
    }

    fun setEventNamePurchaseAdjust(eventNamePurchase: String?) {
        VioAdjust.setEventNamePurchase(eventNamePurchase)
    }

    fun trackAdRevenue(id: String?) {
        VioAdjust.trackAdRevenue(id)
    }

    fun onTrackEvent(eventName: String?) {
        VioAdjust.onTrackEvent(eventName)
    }

    fun onTrackEvent(eventName: String?, id: String?) {
        VioAdjust.onTrackEvent(eventName, id)
    }

    fun onTrackRevenue(eventName: String?, revenue: Float, currency: String?) {
        VioAdjust.onTrackRevenue(eventName, revenue, currency)
    }

    fun onTrackRevenuePurchase(
        revenue: Float,
        currency: String?,
        idPurchase: String?,
        typeIAP: Int
    ) {
        VioAdjust.onTrackRevenuePurchase(revenue, currency)
    }

    fun pushTrackEventAdmob(adValue: AdValue) {
        VioAdjust.pushTrackEventAdmob(adValue)
    }

    fun pushTrackEventApplovin(ad: MaxAd, context: Context?) {
        VioAdjust.pushTrackEventApplovin(ad, context)
    }
}