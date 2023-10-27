package com.vapps.module_ads.control.admob.adsopen

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.vapps.module_ads.R
import com.vapps.module_ads.control.billing.AppPurchase
import com.vapps.module_ads.control.dialog.PrepareLoadingAdsDialog
import com.vapps.module_ads.control.dialog.ResumeLoadingDialog
import com.vapps.module_ads.control.event.AperoLogEventManager.logClickAdsEvent
import com.vapps.module_ads.control.event.AperoLogEventManager.logPaidAdImpression
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.module_ads.control.utils.AdType
import java.util.Arrays
import java.util.Date

class AppOpenManager private constructor() : ActivityLifecycleCallbacks, LifecycleObserver {
    private var appResumeAd: AppOpenAd? = null
    private var appResumeMediumAd: AppOpenAd? = null
    private var appResumeHighAd: AppOpenAd? = null
    private var splashAd: AppOpenAd? = null
    private val splashAdHigh: AppOpenAd? = null
    private val splashAdMedium: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var fullScreenContentCallback: FullScreenContentCallback? = null
    private var appResumeAdId: String? = null
    var appResumeAdMediumId: String? = null
    var appResumeAdHighId: String? = null
    fun setSplashAdId(splashAdId: String?) {
        this.splashAdId = splashAdId
    }

    private var splashAdId: String? = null
    private val splashAdIdHigh: String? = null
    private val splashAdIdMedium: String? = null
    private var currentActivity: Activity? = null
    private var myApplication: Application? = null
    private var appResumeLoadTime: Long = 0
    private var appResumeMediumLoadTime: Long = 0
    private var appResumeHighLoadTime: Long = 0
    private var splashLoadTime: Long = 0
    private var splashTimeout = 0
    var isInitialized = false // on  - off ad resume on app
    private var isAppResumeEnabled = true
    var isInterstitialShowing = false
    private var enableScreenContentCallback =
        false // default =  true when use splash & false after show splash
    private var disableAdResumeByClickAction = false
    private val disabledAppOpenList: MutableList<Class<*>>
    private var splashActivity: Class<*>? = null
    private var isTimeout = false
    private var isLoadingAppResumeHigh = false
    private var isLoadingAppResumeMedium = false
    private var isLoadingAppResumeNormal = false
    private var timeoutHandler: Handler? = null

    /**
     * Init AppOpenManager
     *
     * @param application
     */
    fun init(application: Application?, appOpenAdId: String?) {
        isInitialized = true
        disableAdResumeByClickAction = false
        myApplication = application
        myApplication!!.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appResumeAdId = appOpenAdId
        appResumeAdMediumId = appResumeAdMediumId
        appResumeAdHighId = appResumeAdHighId
        //        if (!Purchase.getInstance().isPurchased(application.getApplicationContext()) &&
//                !isAdAvailable(false) && appOpenAdId != null) {
//            fetchAd(false);
//        }
    }

    fun setEnableScreenContentCallback(enableScreenContentCallback: Boolean) {
        this.enableScreenContentCallback = enableScreenContentCallback
    }

    /**
     * Call disable ad resume when click a button, auto enable ad resume in next start
     */
    fun disableAdResumeByClickAction() {
        disableAdResumeByClickAction = true
    }

    fun setDisableAdResumeByClickAction(disableAdResumeByClickAction: Boolean) {
        this.disableAdResumeByClickAction = disableAdResumeByClickAction
    }

    val isShowingAd: Boolean
        /**
         * Check app open ads is showing
         *
         * @return
         */
        get() = Companion.isShowingAd

    /**
     * Disable app open app on specific activity
     *
     * @param activityClass
     */
    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.name)
        disabledAppOpenList.add(activityClass)
    }

    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.name)
        disabledAppOpenList.remove(activityClass)
    }

    fun disableAppResume() {
        isAppResumeEnabled = false
    }

    fun enableAppResume() {
        isAppResumeEnabled = true
    }

    fun setSplashActivity(splashActivity: Class<*>?, adId: String?, timeoutInMillis: Int) {
        this.splashActivity = splashActivity
        splashAdId = adId
        splashTimeout = timeoutInMillis
    }

    fun setAppResumeAdId(appResumeAdId: String?) {
        this.appResumeAdId = appResumeAdId
    }

    fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        fullScreenContentCallback = callback
    }

    fun removeFullScreenContentCallback() {
        fullScreenContentCallback = null
    }

    /**
     * Request an ad
     */
    fun fetchAd(isSplash: Boolean) {
        Log.d(TAG, "fetchAd: isSplash = $isSplash")
        if (isAdAvailableHighFloor(isSplash)
            && isAdAvailableNormal(isSplash)
            && isAdAvailableMedium(isSplash)
        ) {
            return
        }
        if (!Companion.isShowingAd) {
            loadAppResumeAdSameTime(isSplash)
        }
        if (currentActivity != null) {
            if (AppPurchase.instance!!.isPurchased(currentActivity)) return
            if (appResumeAdHighId != null && !appResumeAdHighId!!.isEmpty()
                && Arrays.asList(*currentActivity!!.resources.getStringArray(R.array.list_id_test))
                    .contains(if (isSplash) splashAdId else appResumeAdHighId)
            ) {
            }
            if (appResumeAdMediumId != null && !appResumeAdMediumId!!.isEmpty()
                && Arrays.asList(*currentActivity!!.resources.getStringArray(R.array.list_id_test))
                    .contains(if (isSplash) splashAdId else appResumeAdMediumId)
            ) {
            }
            if (Arrays.asList(*currentActivity!!.resources.getStringArray(R.array.list_id_test))
                    .contains(if (isSplash) splashAdId else appResumeAdId)
            ) {
            }
        }
    }

    private fun loadAppResumeAdSameTime(isSplash: Boolean) {
        /**
         * Called when an app open ad has loaded.
         *
         * @param ad the loaded app open ad.
         */
        /**
         * Called when an app open ad has failed to load.
         *
         * @param loadAdError the error.
         */
        //                        if (isSplash && fullScreenContentCallback!=null)
        //                            fullScreenContentCallback.onAdDismissedFullScreenContent();
        val loadCallbackAppResumeHighAd: AppOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                isLoadingAppResumeHigh = false
                Log.d(TAG, "onAdLoaded: ads Open Resume High Floor " + ad.adUnitId)
                if (!isSplash) {
                    appResumeHighAd = ad
                    appResumeHighAd!!.onPaidEventListener =
                        OnPaidEventListener { adValue: AdValue? ->
                            logPaidAdImpression(
                                myApplication!!.applicationContext,
                                adValue!!,
                                ad.adUnitId,
                                ad.responseInfo
                                    .mediationAdapterClassName!!, AdType.APP_OPEN
                            )
                        }
                    appResumeHighLoadTime = Date().time
                }
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoadingAppResumeHigh = false
                Log.d(
                    TAG,
                    "onAppOpenAdFailedToLoad: isSplash" + isSplash + " message " + loadAdError.message
                )
                //                        if (isSplash && fullScreenContentCallback!=null)
//                            fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
        }
        val loadCallbackAppResumeMediumAd: AppOpenAdLoadCallback =
            object : AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param ad the loaded app open ad.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoadingAppResumeMedium = false
                    Log.d(TAG, "onAdLoaded: ads Open Resume Medium Floor " + ad.adUnitId)
                    if (!isSplash) {
                        appResumeMediumAd = ad
                        appResumeMediumAd!!.onPaidEventListener =
                            OnPaidEventListener { adValue: AdValue? ->
                                logPaidAdImpression(
                                    myApplication!!.applicationContext,
                                    adValue!!,
                                    ad.adUnitId,
                                    ad.responseInfo
                                        .mediationAdapterClassName!!, AdType.APP_OPEN
                                )
                            }
                        appResumeMediumLoadTime = Date().time
                    }
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppResumeMedium = false
                    Log.d(
                        TAG,
                        "onAppOpenAdFailedToLoad: isSplash" + isSplash + " message " + loadAdError.message
                    )
                }
            }
        loadCallback = object : AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                isLoadingAppResumeNormal = false
                Log.d(TAG, "onAdLoaded: ads Open Resume Normal " + ad.adUnitId)
                if (!isSplash) {
                    appResumeAd = ad
                    appResumeAd!!.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        logPaidAdImpression(
                            myApplication!!.applicationContext,
                            adValue!!,
                            ad.adUnitId,
                            ad.responseInfo
                                .mediationAdapterClassName!!, AdType.APP_OPEN
                        )
                    }
                    appResumeLoadTime = Date().time
                } else {
                    splashAd = ad
                    splashAd!!.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        logPaidAdImpression(
                            myApplication!!.applicationContext,
                            adValue!!,
                            ad.adUnitId,
                            ad.responseInfo
                                .mediationAdapterClassName!!, AdType.APP_OPEN
                        )
                    }
                    splashLoadTime = Date().time
                }
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoadingAppResumeNormal = false
                Log.d(
                    TAG,
                    "onAppOpenAdFailedToLoad: isSplash" + isSplash + " message " + loadAdError.message
                )
                //                        if (isSplash && fullScreenContentCallback!=null)
//                            fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
        }
        val request = adRequest
        if (appResumeAdHighId != null && !appResumeAdHighId!!.isEmpty() && appResumeHighAd == null && !isLoadingAppResumeHigh) {
            isLoadingAppResumeHigh = true
            AppOpenAd.load(
                myApplication!!, (if (isSplash) splashAdId else appResumeAdHighId)!!, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallbackAppResumeHighAd
            )
        }
        if (appResumeAdMediumId != null && !appResumeAdMediumId!!.isEmpty() && appResumeMediumAd == null && !isLoadingAppResumeMedium) {
            isLoadingAppResumeMedium = true
            AppOpenAd.load(
                myApplication!!, (if (isSplash) splashAdId else appResumeAdMediumId)!!, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallbackAppResumeMediumAd
            )
        }
        if (appResumeAd == null && !isLoadingAppResumeNormal) {
            isLoadingAppResumeNormal = true
            AppOpenAd.load(
                myApplication!!, (if (isSplash) splashAdId else appResumeAdId)!!, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
            )
        }
    }

    private val adRequest: AdRequest
        /**
         * Creates and returns ad request.
         */
        private get() = AdRequest.Builder().build()

    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    fun isAdAvailable(isSplash: Boolean): Boolean {
        return if (appResumeHighAd != null) {
            isAdAvailableHighFloor(isSplash)
        } else if (appResumeMediumAd != null) {
            isAdAvailableMedium(isSplash)
        } else {
            isAdAvailableNormal(isSplash)
        }
    }

    fun isAdAvailableHighFloor(isSplash: Boolean): Boolean {
        val loadTime: Long
        loadTime = if (isSplash) splashLoadTime else appResumeHighLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
        Log.d(TAG, "isAdAvailable: $wasLoadTimeLessThanNHoursAgo")
        return ((if (isSplash) splashAd != null else appResumeHighAd != null)
                && wasLoadTimeLessThanNHoursAgo)
    }

    fun isAdAvailableMedium(isSplash: Boolean): Boolean {
        val loadTime: Long
        loadTime = if (isSplash) splashLoadTime else appResumeMediumLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
        Log.d(TAG, "isAdAvailable: $wasLoadTimeLessThanNHoursAgo")
        return ((if (isSplash) splashAd != null else appResumeMediumAd != null)
                && wasLoadTimeLessThanNHoursAgo)
    }

    fun isAdAvailableNormal(isSplash: Boolean): Boolean {
        val loadTime: Long
        loadTime = if (isSplash) splashLoadTime else appResumeLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
        Log.d(TAG, "isAdAvailable: $wasLoadTimeLessThanNHoursAgo")
        return ((if (isSplash) splashAd != null else appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        Log.d(TAG, "onActivityStarted: $currentActivity")
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        Log.d(TAG, "onActivityResumed: $currentActivity")
        if (splashActivity == null) {
            if (activity.javaClass.name != AdActivity::class.java.name) {
                Log.d(TAG, "onActivityResumed 1: with " + activity.javaClass.name)
                fetchAd(false)
            }
        } else {
            if (activity.javaClass.name != splashActivity!!.name && activity.javaClass.name != AdActivity::class.java.name) {
                Log.d(TAG, "onActivityResumed 2: with " + activity.javaClass.name)
                fetchAd(false)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
        Log.d(TAG, "onActivityDestroyed: null")
    }

    fun showAdIfAvailable(isSplash: Boolean) {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (currentActivity == null || AppPurchase.instance!!.isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        Log.d(TAG, "showAdIfAvailable: " + ProcessLifecycleOwner.get().lifecycle.currentState)
        Log.d(TAG, "showAd isSplash: $isSplash")
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, "showAdIfAvailable: return")
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        if (!Companion.isShowingAd && isAdAvailable(isSplash)) {
            Log.d(TAG, "Will show ad isSplash:$isSplash")
            if (isSplash) {
                showAdsWithLoading()
            } else {
                showResumeAds()
            }
        } else {
            Log.d(TAG, "Ad is not ready")
            if (!isSplash) {
                fetchAd(false)
            }
            if (isSplash && Companion.isShowingAd && isAdAvailable(true)) {
                showAdsWithLoading()
            }
        }
    }

    private fun showAdsWithLoading() {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            var dialog: Dialog? = null
            try {
                dialog = PrepareLoadingAdsDialog(currentActivity)
                try {
                    dialog.show()
                } catch (e: Exception) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val finalDialog = dialog
            Handler().postDelayed({
                if (splashAd != null) {
                    splashAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            appResumeAd = null
                            appResumeMediumAd = null
                            appResumeHighAd = null
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                                enableScreenContentCallback = false
                            }
                            Companion.isShowingAd = false
                            fetchAd(true)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(
                                    adError
                                )
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdShowedFullScreenContent()
                            }
                            Companion.isShowingAd = true
                            splashAd = null
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            if (currentActivity != null) {
                                logClickAdsEvent(currentActivity, splashAdId)
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback!!.onAdClicked()
                                }
                            }
                        }
                    }
                    splashAd!!.show(currentActivity!!)
                }
            }, 800)
        }
    }

    var dialog: Dialog? = null
    private fun showResumeAds() {
        if (appResumeAd == null && appResumeMediumAd == null && appResumeHighAd == null || currentActivity == null || AppPurchase.instance!!.isPurchased(
                currentActivity
            )
        ) {
            return
        }
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            try {
                dismissDialogLoading()
                dialog = ResumeLoadingDialog(currentActivity)
                try {
                    dialog?.show()
                } catch (e: Exception) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //            new Handler().postDelayed(() -> {
            if (appResumeHighAd != null) {
                appResumeHighAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appResumeHighAd = null
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                        }
                        Companion.isShowingAd = false
                        dismissDialogLoading()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(adError)
                        }
                        if (currentActivity != null && !currentActivity!!.isDestroyed && dialog != null && dialog!!.isShowing) {
                            Log.d(TAG, "dismiss dialog loading ad open: ")
                            try {
                                dialog!!.dismiss()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        appResumeHighAd = null
                        Companion.isShowingAd = false
                        fetchAd(false)
                    }

                    override fun onAdShowedFullScreenContent() {
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback!!.onAdShowedFullScreenContent()
                        }
                        Companion.isShowingAd = true
                        appResumeHighAd = null
                        Log.d(TAG, "onAdShowedFullScreenContent: High Floor")
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        if (currentActivity != null) {
                            logClickAdsEvent(currentActivity, appResumeAdHighId)
                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback!!.onAdClicked()
                            }
                        }
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        if (currentActivity != null) {
                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback!!.onAdImpression()
                            }
                        }
                    }
                }
                appResumeHighAd!!.show(currentActivity!!)
            } else if (appResumeMediumAd != null) {
                appResumeMediumAd!!.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            appResumeMediumAd = null
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                            }
                            Companion.isShowingAd = false
                            dismissDialogLoading()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(
                                    adError
                                )
                            }
                            if (currentActivity != null && !currentActivity!!.isDestroyed && dialog != null && dialog!!.isShowing) {
                                Log.d(TAG, "dismiss dialog loading ad open: ")
                                try {
                                    dialog!!.dismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            appResumeMediumAd = null
                            Companion.isShowingAd = false
                            fetchAd(false)
                        }

                        override fun onAdShowedFullScreenContent() {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdShowedFullScreenContent()
                            }
                            Companion.isShowingAd = true
                            appResumeMediumAd = null
                            Log.d(TAG, "onAdShowedFullScreenContent: Medium")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            if (currentActivity != null) {
                                logClickAdsEvent(currentActivity, appResumeAdMediumId)
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback!!.onAdClicked()
                                }
                            }
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            if (currentActivity != null) {
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback!!.onAdImpression()
                                }
                            }
                        }
                    }
                appResumeMediumAd!!.show(currentActivity!!)
            } else {
                if (appResumeAd != null) {
                    appResumeAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            appResumeAd = null
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                            }
                            Companion.isShowingAd = false
                            dismissDialogLoading()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(
                                    adError
                                )
                            }
                            if (currentActivity != null && !currentActivity!!.isDestroyed && dialog != null && dialog!!.isShowing) {
                                Log.d(TAG, "dismiss dialog loading ad open: ")
                                try {
                                    dialog!!.dismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            appResumeAd = null
                            Companion.isShowingAd = false
                            fetchAd(false)
                        }

                        override fun onAdShowedFullScreenContent() {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback!!.onAdShowedFullScreenContent()
                            }
                            Companion.isShowingAd = true
                            appResumeAd = null
                            Log.d(TAG, "onAdShowedFullScreenContent: Normal")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            if (currentActivity != null) {
                                logClickAdsEvent(currentActivity, appResumeAdId)
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback!!.onAdClicked()
                                }
                            }
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            if (currentActivity != null) {
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback!!.onAdImpression()
                                }
                            }
                        }
                    }
                    appResumeAd!!.show(currentActivity!!)
                }
            }
            //            }, 1000);
        }
    }

    fun loadAndShowSplashAds(aId: String?) {
        loadAndShowSplashAds(aId, 0)
    }

    fun loadAndShowSplashAds(adId: String?, delay: Long) {
        isTimeout = false
        enableScreenContentCallback = true
        if (currentActivity != null && AppPurchase.instance!!.isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                Handler().postDelayed(
                    { fullScreenContentCallback!!.onAdDismissedFullScreenContent() },
                    delay
                )
            }
            return
        }

//        if (isAdAvailable(true)) {
//            showAdIfAvailable(true);
//            return;
//        }
        loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                Log.d(TAG, "onAppOpenAdLoaded: splash")
                timeoutHandler!!.removeCallbacks(runnableTimeout)
                if (isTimeout) {
                    Log.e(TAG, "onAppOpenAdLoaded: splash timeout")
                    //                            if (fullScreenContentCallback != null) {
//                                fullScreenContentCallback.onAdDismissedFullScreenContent();
//                                enableScreenContentCallback = false;
//                            }
                } else {
                    splashAd = appOpenAd
                    splashLoadTime = Date().time
                    appOpenAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        logPaidAdImpression(
                            myApplication!!.applicationContext,
                            adValue!!,
                            appOpenAd.adUnitId,
                            appOpenAd.responseInfo
                                .mediationAdapterClassName!!, AdType.APP_OPEN
                        )
                    }
                    Handler().postDelayed({ showAdIfAvailable(true) }, delay)
                }
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "onAppOpenAdFailedToLoad: splash " + loadAdError.message)
                if (isTimeout) {
                    Log.e(TAG, "onAdFailedToLoad: splash timeout")
                    return
                }
                if (fullScreenContentCallback != null && enableScreenContentCallback) {
                    Handler().postDelayed(
                        { fullScreenContentCallback!!.onAdDismissedFullScreenContent() },
                        delay
                    )
                    enableScreenContentCallback = false
                }
            }
        }
        val request = adRequest
        AppOpenAd.load(
            myApplication!!, splashAdId!!, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )
        if (splashTimeout > 0) {
            timeoutHandler = Handler()
            timeoutHandler!!.postDelayed(runnableTimeout, splashTimeout.toLong())
        }
    }

    fun loadOpenAppAdSplash(
        context: Context?,
        timeDelay: Long,
        timeOut: Long,
        isShowAdIfReady: Boolean,
        adCallback: AdCallback
    ) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            adCallback.onNextAction()
            return
        }
        val startLoadAd = System.currentTimeMillis()
        val actionTimeOut = Runnable {
            Log.d(TAG, "getAdSplash time out")
            adCallback.onNextAction()
            Companion.isShowingAd = false
        }
        val handleTimeOut = Handler()
        handleTimeOut.postDelayed(actionTimeOut, timeOut)
        val request = adRequest
        AppOpenAd.load(
            context!!,
            splashAdId!!,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    adCallback.onAdFailedToLoad(loadAdError)
                    handleTimeOut.removeCallbacks(actionTimeOut)
                }

                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    super.onAdLoaded(appOpenAd)
                    handleTimeOut.removeCallbacks(actionTimeOut)
                    splashAd = appOpenAd
                    splashAd!!.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        logPaidAdImpression(
                            context,
                            adValue!!,
                            splashAd!!.adUnitId,
                            splashAd!!.responseInfo
                                .mediationAdapterClassName!!, AdType.APP_OPEN
                        )
                    }
                    if (isShowAdIfReady) {
                        val delayTimeLeft = System.currentTimeMillis() - startLoadAd
                        Handler().postDelayed(
                            { showAppOpenSplash(context, adCallback) },
                            if (delayTimeLeft >= timeDelay) 0 else delayTimeLeft
                        )
                    } else {
                        adCallback.onAdSplashReady()
                    }
                }
            }
        )
    }

    fun showAppOpenSplash(
        context: Context?,
        adCallback: AdCallback
    ) {
        if (splashAd == null) {
            adCallback.onNextAction()
            return
        }
        dismissDialogLoading()
        try {
            dialog = PrepareLoadingAdsDialog(context)
            try {
                dialog?.setCancelable(false)
                dialog?.show()
            } catch (e: Exception) {
                adCallback.onNextAction()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val finalDialog = dialog
        Handler().postDelayed({
            if (splashAd != null) {
                splashAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        adCallback.onAdClosed()
                        splashAd = null
                        Companion.isShowingAd = false
                        if (finalDialog != null && !currentActivity!!.isDestroyed) {
                            try {
                                finalDialog.dismiss()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        adCallback.onAdFailedToShow(adError)
                        Companion.isShowingAd = false
                        dismissDialogLoading()
                    }

                    override fun onAdShowedFullScreenContent() {
                        adCallback.onAdImpression()
                        Companion.isShowingAd = true
                        splashAd = null
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        logClickAdsEvent(context, splashAdId)
                        adCallback.onAdClicked()
                    }
                }
                splashAd!!.show(currentActivity!!)
            }
        }, 800)
    }

    fun onCheckShowAppOpenSplashWhenFail(
        activity: AppCompatActivity,
        callback: AdCallback,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed({
            if (splashAd != null && !this.isShowingAd) {
                showAppOpenSplash(activity, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        callback.onNextAction()
                        splashAd = null
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        callback.onAdClosed()
                        splashAd = null
                    }

                    override fun onAdFailedToShow(adError: AdError?) {
                        super.onAdFailedToShow(adError)
                        callback.onAdFailedToShow(adError)
                        splashAd = null
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        callback.onAdImpression()
                        splashAd = null
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        callback.onAdClicked()
                    }
                })
            }
        }, timeDelay.toLong())
    }

    var runnableTimeout = Runnable {
        Log.e(TAG, "timeout load ad ")
        isTimeout = true
        enableScreenContentCallback = false
        if (fullScreenContentCallback != null) {
            fullScreenContentCallback!!.onAdDismissedFullScreenContent()
        }
    }
    //            = new Handler(msg -> {
    //        if (msg.what == TIMEOUT_MSG) {
    //
    //                Log.e(TAG, "timeout load ad ");
    //                isTimeout = true;
    //                enableScreenContentCallback = false;
    //                if (fullScreenContentCallback != null) {
    //                    fullScreenContentCallback.onAdDismissedFullScreenContent();
    //                }
    //
    //        }
    //        return false;
    //    });
    /**
     * Constructor
     */
    init {
        disabledAppOpenList = ArrayList()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() {
        if (!isAppResumeEnabled) {
            Log.d(TAG, "onResume: app resume is disabled")
            return
        }
        if (isInterstitialShowing) {
            Log.d(TAG, "onResume: interstitial is showing")
            return
        }
        if (disableAdResumeByClickAction) {
            Log.d(TAG, "onResume:ad resume disable ad by action")
            disableAdResumeByClickAction = false
            return
        }
        for (activity in disabledAppOpenList) {
            if (activity.name == currentActivity!!.javaClass.name) {
                Log.d(TAG, "onStart: activity is disabled")
                return
            }
        }
        if (splashActivity != null && splashActivity!!.name == currentActivity!!.javaClass.name) {
            val adId = splashAdId
            if (adId == null) {
                Log.e(TAG, "splash ad id must not be null")
            }
            Log.d(TAG, "onStart: load and show splash ads")
            loadAndShowSplashAds(adId)
            return
        }
        Log.d(TAG, "onStart: show resume ads :" + currentActivity!!.javaClass.name)
        showAdIfAvailable(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d(TAG, "onStop: app stop")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.d(TAG, "onPause")
    }

    private fun dismissDialogLoading() {
        if (dialog != null && dialog!!.isShowing) {
            try {
                dialog!!.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "AppOpenManager"
        const val AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/3419835294"

        @Volatile
        private var INSTANCE: AppOpenManager? = null
        private var isShowingAd = false
        private const val TIMEOUT_MSG = 11

        @get:Synchronized
        val instance: AppOpenManager?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = AppOpenManager()
                }
                return INSTANCE
            }
    }
}