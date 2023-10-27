package com.vapps.module_ads.control.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringDef
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEventFailure
import com.adjust.sdk.AdjustEventSuccess
import com.adjust.sdk.AdjustSessionFailure
import com.adjust.sdk.AdjustSessionSuccess
import com.adjust.sdk.LogLevel
import com.adjust.sdk.OnAttributionChangedListener
import com.adjust.sdk.OnEventTrackingFailedListener
import com.adjust.sdk.OnEventTrackingSucceededListener
import com.adjust.sdk.OnSessionTrackingFailedListener
import com.adjust.sdk.OnSessionTrackingSucceededListener
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacer
import com.applovin.mediation.nativeAds.adPlacer.MaxRecyclerAdapter
import com.facebook.FacebookSdk
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.vapps.module_ads.R
import com.vapps.module_ads.control.admob.adsopen.AppOpenManager
import com.vapps.module_ads.control.admob.nativead.AperoAdAdapter
import com.vapps.module_ads.control.admob.nativead.AperoAdPlacer
import com.vapps.module_ads.control.billing.AppPurchase
import com.vapps.module_ads.control.config.AperoAdConfig
import com.vapps.module_ads.control.event.AperoLogEventManager
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.module_ads.control.listener.AperoAdCallback
import com.vapps.module_ads.control.listener.AperoInitCallback
import com.vapps.module_ads.control.listener.RewardCallback
import com.vapps.module_ads.control.model.ApAdError
import com.vapps.module_ads.control.model.ApAdValue
import com.vapps.module_ads.control.model.ApInterstitialAd
import com.vapps.module_ads.control.model.ApNativeAd
import com.vapps.module_ads.control.model.ApRewardAd
import com.vapps.module_ads.control.model.ApRewardItem
import com.vapps.module_ads.control.utils.AppUtil
import com.vapps.module_ads.control.utils.AppUtil.VARIANT_DEV
import com.vapps.module_ads.control.utils.SharePreferenceUtils

class AperoAd {
    private val TAG = AperoAd::class.simpleName
    var adConfig: AperoAdConfig? = null
        private set
    private var initCallback: AperoInitCallback? = null
    private var initAdSuccess = false
    private val messages = StringBuilder("")

    /**
     * Set count click to show ads interstitial when call showInterstitialAdByTimes()
     *
     * @param countClickToShowAds - default = 3
     */
    fun setCountClickToShowAds(countClickToShowAds: Int) {
        Admob.instance!!.setNumToShowAds(countClickToShowAds)
    }

    /**
     * Set count click to show ads interstitial when call showInterstitialAdByTimes()
     *
     * @param countClickToShowAds Default value = 3
     * @param currentClicked      Default value = 0
     */
    fun setCountClickToShowAds(countClickToShowAds: Int, currentClicked: Int) {
        Admob.instance!!.setNumToShowAds(countClickToShowAds, currentClicked)
    }
    /**
     * @param context
     * @param adConfig             AperoAdConfig object used for SDK initialisation
     * @param enableDebugMediation set show Mediation Debugger - use only for Max Mediation
     */
    /**
     * @param context
     * @param adConfig AperoAdConfig object used for SDK initialisation
     */
    @JvmOverloads
    fun init(
        context: Application,
        adConfig: AperoAdConfig?,
        enableDebugMediation: Boolean? = false
    ) {
        if (adConfig == null) {
            throw RuntimeException("cant not set AperoAdConfig null")
        }
        this.adConfig = adConfig
        VARIANT_DEV = adConfig.isVariantDev
        Log.i(TAG, "Config variant dev: " + VARIANT_DEV)
        Log.i(TAG, "init adjust")
        setupAdjust(adConfig.isVariantDev, adConfig.adjustConfig!!.adjustToken)

        Admob.instance!!.init(context, adConfig.listDeviceTest)
        if (adConfig.isEnableAdResume) {
            AppOpenManager.instance!!.init(adConfig.application, adConfig.idAdResume)
            if (adConfig.idAdResumeHigh != null && !adConfig.idAdResumeHigh!!.isEmpty()) {
                AppOpenManager.instance!!.appResumeAdHighId = adConfig.idAdResumeHigh
            }
            if (adConfig.idAdResumeMedium != null && !adConfig.idAdResumeMedium!!.isEmpty()) {
                AppOpenManager.instance!!.appResumeAdMediumId = adConfig.idAdResumeMedium
            }
        }
        initAdSuccess = true
        if (initCallback != null) initCallback!!.initAdSuccess()
        FacebookSdk.sdkInitialize(context)
    }


    val mediationProvider: Int
        get() = adConfig!!.mediationProvider

    fun setInitCallback(initCallback: AperoInitCallback) {
        this.initCallback = initCallback
        if (initAdSuccess) initCallback.initAdSuccess()
    }

    private fun setupAdjust(buildDebug: Boolean, adjustToken: String) {
        val environment: String =
            if (buildDebug) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        Log.i("Application", "setupAdjust: $environment")
        val config = AdjustConfig(adConfig!!.application, adjustToken, environment)

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE)
        config.setPreinstallTrackingEnabled(true)
        config.setOnAttributionChangedListener { attribution ->
            Log.d(TAG_ADJUST, "Attribution callback called!")
            Log.d(TAG_ADJUST, "Attribution: $attribution")
        }

        // Set event success tracking delegate.
        config.setOnEventTrackingSucceededListener { eventSuccessResponseData ->
            Log.d(TAG_ADJUST, "Event success callback called!")
            Log.d(TAG_ADJUST, "Event success data: $eventSuccessResponseData")
            messages.append(eventSuccessResponseData.toString()).append("\n\n")
            AppUtil.messageInit.postValue(messages.toString())
        }
        // Set event failure tracking delegate.
        config.setOnEventTrackingFailedListener { eventFailureResponseData ->
            Log.d(TAG_ADJUST, "Event failure callback called!")
            Log.d(TAG_ADJUST, "Event failure data: $eventFailureResponseData")
        }

        // Set session success tracking delegate.
        config.setOnSessionTrackingSucceededListener { sessionSuccessResponseData ->
            Log.d(TAG_ADJUST, "Session success callback called!")
            val d = Log.d(TAG_ADJUST, "Session success data: $sessionSuccessResponseData")
        }

        // Set session failure tracking delegate.
        config.setOnSessionTrackingFailedListener { sessionFailureResponseData ->
            Log.d(TAG_ADJUST, "Session failure callback called!")
            Log.d(TAG_ADJUST, "Session failure data: $sessionFailureResponseData")
        }
        config.setSendInBackground(true)
        Adjust.onCreate(config)
        adConfig!!.application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
        if (config.isValid) {
            messages.append("init adjust sdk successfully").append("\n\n")
        } else {
            messages.append("init adjust sdk failed").append("\n\n")
        }
        messages.append("Adjust Token : ")
            .append(adjustToken).append("\n\n")
        messages.append("Adjust Environment : ")
            .append(environment).append("\n\n")
    }

    private class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
    }

    fun loadBanner(mActivity: Activity?, id: String?) {
        Admob.instance!!.loadBanner(
            mActivity!!, id
        )
    }

    fun loadBanner(mActivity: Activity?, id: String?, adCallback: AperoAdCallback) {
        mActivity?.let {
            Admob.instance?.loadBanner(
                it,
                id,
                object : AdCallback() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        adCallback.onAdLoaded()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        adCallback.onAdClicked()
                    }

                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        adCallback.onAdFailedToLoad(ApAdError(i))
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        adCallback.onAdImpression()
                    }
                })
        }
    }

    fun loadCollapsibleBanner(
        activity: Activity?,
        id: String?,
        gravity: String?,
        adCallback: AdCallback?
    ) {
        Admob.instance!!.loadCollapsibleBanner(activity!!, id, gravity, adCallback)
    }

    fun loadBannerFragment(mActivity: Activity?, id: String?, rootView: View?) {
        Admob.instance!!.loadBannerFragment(
            mActivity!!, id, rootView!!
        )
    }

    fun loadBannerFragment(
        mActivity: Activity?,
        id: String?,
        rootView: View?,
        adCallback: AdCallback?
    ) {
        mActivity?.let {
            rootView?.let { it1 ->
                Admob.instance?.loadBannerFragment(
                    it,
                    id,
                    it1,
                    adCallback
                )
            }
        }
    }

    fun requestLoadBanner(activity: Activity?, idBannerAd: String, adCallback: AdCallback?) {
        if (adConfig!!.mediationProvider == AperoAdConfig.PROVIDER_ADMOB) {
            Admob.instance!!.requestLoadBanner(
                activity!!,
                idBannerAd,
                adCallback,
                false,
                Admob.BANNER_INLINE_LARGE_STYLE
            )
        } else {
            /*no op*/
        }
    }

    fun populateUnifiedBannerAdView(
        mActivity: Activity?,
        adView: AdView?,
        adContainer: FrameLayout?
    ) {
        if (adConfig!!.mediationProvider == AperoAdConfig.PROVIDER_ADMOB) {
            Admob.instance!!.populateUnifiedBannerAdView(mActivity!!, adView!!, adContainer!!)
        } else {
            /*no op*/
        }
    }

    fun loadCollapsibleBannerFragment(
        mActivity: Activity?,
        id: String?,
        rootView: View?,
        gravity: String?,
        adCallback: AdCallback?
    ) {
        Admob.instance!!.loadCollapsibleBannerFragment(
            mActivity!!,
            id,
            rootView!!,
            gravity,
            adCallback
        )
    }

    //    public void loadBanner(final Activity mActivity, String id, final AperoAdCallback callback) {
//        switch (adConfig.getMediationProvider()) {
//            case AperoAdConfig.PROVIDER_ADMOB:
//                Admob.getInstance().loadBanner(mActivity, id , new AdCallback(){
//                    @Override
//                    public void onAdClicked() {
//                        super.onAdClicked();
//                        callback.onAdClicked();
//                    }
//                });
//                break;
//            case AperoAdConfig.PROVIDER_MAX:
//                AppLovin.getInstance().loadBanner(mActivity, id, new AppLovinCallback(){
//
//                });
//        }
//    }
    fun loadSplashInterstitialAds(
        context: Context?,
        id: String?,
        timeOut: Long,
        timeDelay: Long,
        adListener: AperoAdCallback?
    ) {
        loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener)
    }

    fun loadSplashInterstitialAds(
        context: Context?,
        id: String?,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean,
        adListener: AperoAdCallback?
    ) {
        Admob.instance?.loadSplashInterstitialAds(
            context,
            id,
            timeOut,
            timeDelay,
            showSplashIfReady,
            object : AdCallback() {
                override fun onAdClosed() {
                    super.onAdClosed()
                    adListener?.onAdClosed()
                }

                override fun onNextAction() {
                    super.onNextAction()
                    adListener?.onNextAction()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    adListener?.onAdFailedToLoad(ApAdError(i))
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    adListener?.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adListener?.onAdLoaded()
                }

                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    adListener?.onAdSplashReady()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adListener?.onAdClicked()
                }
            })
    }


    private var isFinishedLoadInter = false
    private var isShowInter = false
    fun loadAppOpenSplashSameTime(
        context: Context?,
        interId: String?,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean,
        adListener: AperoAdCallback?
    ) {
        isFinishedLoadInter = false
        isShowInter = false
        Admob.instance!!.loadSplashInterstitialAds(
            context,
            interId,
            timeOut,
            timeDelay,
            false,
            object : AdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    isFinishedLoadInter = true
                    if (isShowInter) {
                        if (showSplashIfReady) {
                            Admob.instance!!.onShowSplash(
                                context as AppCompatActivity?,
                                object : AdCallback() {
                                    override fun onAdFailedToShow(adError: AdError?) {
                                        super.onAdFailedToShow(adError)
                                        adListener?.onAdFailedToShow(ApAdError(adError))
                                    }

                                    override fun onNextAction() {
                                        super.onNextAction()
                                        adListener?.onNextAction()
                                    }

                                    override fun onAdClosed() {
                                        super.onAdClosed()
                                        adListener?.onAdClosed()
                                    }

                                    override fun onAdImpression() {
                                        super.onAdImpression()
                                        adListener?.onAdImpression()
                                    }

                                    override fun onAdClicked() {
                                        super.onAdClicked()
                                        adListener?.onAdClicked()
                                    }
                                })
                        } else {
                            adListener?.onInterstitialLoad(ApInterstitialAd(Admob.instance!!.getmInterstitialSplash()))
                        }
                    }
                }

                override fun onNextAction() {
                    super.onNextAction()
                    isFinishedLoadInter = true
                    if (isShowInter) {
                        adListener?.onNextAction()
                    }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    isFinishedLoadInter = true
                    if (isShowInter) {
                        adListener?.onAdFailedToLoad(ApAdError(i))
                    }
                }
            })
        AppOpenManager.instance?.loadOpenAppAdSplash(
            context,
            timeDelay,
            timeOut,
            showSplashIfReady,
            object : AdCallback() {
                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    if (isFinishedLoadInter) {
                        if (Admob.instance!!.getmInterstitialSplash() != null) {
                            if (showSplashIfReady) {
                                Admob.instance!!.onShowSplash(
                                    context as AppCompatActivity?,
                                    object : AdCallback() {
                                        override fun onAdFailedToShow(adError: AdError?) {
                                            super.onAdFailedToShow(adError)
                                            adListener?.onAdFailedToShow(ApAdError(adError))
                                        }

                                        override fun onNextAction() {
                                            super.onNextAction()
                                            adListener?.onNextAction()
                                        }

                                        override fun onAdClosed() {
                                            super.onAdClosed()
                                            adListener?.onAdClosed()
                                        }

                                        override fun onAdImpression() {
                                            super.onAdImpression()
                                            adListener?.onAdImpression()
                                        }

                                        override fun onAdClicked() {
                                            super.onAdClicked()
                                            adListener?.onAdClicked()
                                        }
                                    })
                            } else {
                                adListener?.onInterstitialLoad(ApInterstitialAd(Admob.instance!!.getmInterstitialSplash()))
                            }
                        } else {
                            adListener?.onAdFailedToLoad(ApAdError(i))
                        }
                    } else {
                        isShowInter = true
                    }
                }

                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    adListener?.onAdSplashReady()
                }

                override fun onNextAction() {
                    super.onNextAction()
                    adListener?.onNextAction()
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    adListener?.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adListener?.onAdClicked()
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adListener?.onAdImpression()
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    adListener?.onAdClosed()
                }
            })
    }

    fun onShowSplash(activity: AppCompatActivity?, adListener: AperoAdCallback) {
        Admob.instance!!.onShowSplash(
            activity,
            object : AdCallback() {
                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    adListener.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    adListener.onAdClosed()
                }

                override fun onNextAction() {
                    super.onNextAction()
                    adListener.onNextAction()
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adListener.onAdImpression()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adListener.onAdClicked()
                }
            }
        )
    }


    /**
     * Called  on Resume - SplashActivity
     * It call reshow ad splash when ad splash show fail in background
     *
     * @param activity
     * @param callback
     * @param timeDelay time delay before call show ad splash (ms)
     */
    fun onCheckShowSplashWhenFail(
        activity: AppCompatActivity?, callback: AperoAdCallback,
        timeDelay: Int
    ) {
        Admob.instance!!.onCheckShowSplashWhenFail(
            activity!!, object : AdCallback() {
                override fun onNextAction() {
                    super.onAdClosed()
                    callback.onNextAction()
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    callback.onAdFailedToLoad(ApAdError(i))
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    callback.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    callback.onAdClosed()
                }
            }, timeDelay
        )
    }


    /**
     * Result a ApInterstitialAd in onInterstitialLoad
     *
     * @param context
     * @param id         admob or max mediation
     * @param adListener
     */
    fun getInterstitialAds(
        context: Context?,
        id: String?,
        adListener: AperoAdCallback
    ): ApInterstitialAd {
        val apInterstitialAd = ApInterstitialAd()
        Admob.instance!!.getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.d(TAG, "Admob onInterstitialLoad")
                apInterstitialAd.setInterstitialAd(interstitialAd)
                adListener.onInterstitialLoad(apInterstitialAd)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener.onAdFailedToLoad(ApAdError(i))
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                adListener.onAdFailedToShow(ApAdError(adError))
            }
        })
        return apInterstitialAd
    }

    /**
     * Result a ApInterstitialAd in onInterstitialLoad
     *
     * @param context
     * @param id      admob or max mediation
     */
    fun getInterstitialAds(context: Context?, id: String?): ApInterstitialAd {
        val apInterstitialAd = ApInterstitialAd()
        Admob.instance!!.getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.d(TAG, "Admob onInterstitialLoad: ")
                apInterstitialAd.setInterstitialAd(interstitialAd)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
            }
        })
        return apInterstitialAd
    }

    /**
     * Called force show ApInterstitialAd when ready
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    fun forceShowInterstitial(
        context: Context, mInterstitialAd: ApInterstitialAd?,
        callback: AperoAdCallback
    ) {
        forceShowInterstitial(context, mInterstitialAd, callback, false)
    }

    /**
     * Called force show ApInterstitialAd when ready
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     * @param shouldReloadAds auto reload ad when ad close
     */
    fun forceShowInterstitial(
        context: Context, mInterstitialAd: ApInterstitialAd?,
        callback: AperoAdCallback, shouldReloadAds: Boolean
    ) {
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(
                context
            )
            < instance!!.adConfig!!.intervalInterstitialAd * 1000L
        ) {
            Log.i(TAG, "forceShowInterstitial: ignore by interval impression interstitial time")
            callback.onNextAction()
            return
        }
        if (mInterstitialAd == null || mInterstitialAd.isNotReady) {
            Log.e(TAG, "forceShowInterstitial: ApInterstitialAd is not ready")
            callback.onNextAction()
            return
        }
        val adCallback: AdCallback = object : AdCallback() {
            override fun onAdClosed() {
                super.onAdClosed()
                Log.d(TAG, "onAdClosed: ")
                callback.onAdClosed()
                if (shouldReloadAds) {
                    Admob.instance!!.getInterstitialAds(
                        context,
                        mInterstitialAd.getInterstitialAd()?.adUnitId,
                        object : AdCallback() {
                            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                                super.onInterstitialLoad(interstitialAd)
                                Log.d(TAG, "Admob shouldReloadAds success")
                                mInterstitialAd.setInterstitialAd(interstitialAd)
                                callback.onInterstitialLoad(mInterstitialAd)
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                mInterstitialAd.setInterstitialAd(null)
                                callback.onAdFailedToLoad(ApAdError(i))
                            }

                            override fun onAdFailedToShow(adError: AdError?) {
                                super.onAdFailedToShow(adError)
                                callback.onAdFailedToShow(ApAdError(adError))
                            }
                        })
                } else {
                    mInterstitialAd.setInterstitialAd(null)
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                Log.d(TAG, "onNextAction: ")
                callback.onNextAction()
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                Log.d(TAG, "onAdFailedToShow: ")
                callback.onAdFailedToShow(ApAdError(adError))
                if (shouldReloadAds) Admob.instance!!.getInterstitialAds(
                    context,
                    mInterstitialAd.getInterstitialAd()?.getAdUnitId(),
                    object : AdCallback() {
                        override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                            super.onInterstitialLoad(interstitialAd)
                            Log.d(TAG, "Admob shouldReloadAds success")
                            mInterstitialAd.setInterstitialAd(interstitialAd)
                            callback.onInterstitialLoad(mInterstitialAd)
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            callback.onAdFailedToLoad(ApAdError(i))
                        }

                        override fun onAdFailedToShow(adError: AdError?) {
                            super.onAdFailedToShow(adError)
                            callback.onAdFailedToShow(ApAdError(adError))
                        }
                    }) else {
                    mInterstitialAd.setInterstitialAd(null)
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                callback.onInterstitialShow()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }
        }
        Admob.instance!!.forceShowInterstitial(
            context,
            mInterstitialAd.getInterstitialAd(),
            adCallback
        )
    }

    /**
     * Called force show ApInterstitialAd when reach the number of clicks show ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     * @param shouldReloadAds auto reload ad when ad close
     */
    fun showInterstitialAdByTimes(
        context: Context?, mInterstitialAd: ApInterstitialAd,
        callback: AperoAdCallback?, shouldReloadAds: Boolean
    ) {
        if (mInterstitialAd.isNotReady) {
            Log.e(TAG, "forceShowInterstitial: ApInterstitialAd is not ready")
            callback?.onAdFailedToShow(ApAdError("ApInterstitialAd is not ready"))
            return
        }
        val adCallback: AdCallback = object : AdCallback() {
            override fun onAdClosed() {
                super.onAdClosed()
                Log.d(TAG, "onAdClosed: ")
                callback?.onAdClosed()
                if (shouldReloadAds) {
                    Admob.instance!!.getInterstitialAds(
                        context,
                        mInterstitialAd.getInterstitialAd()?.adUnitId,
                        object : AdCallback() {
                            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                                super.onInterstitialLoad(interstitialAd)
                                Log.d(TAG, "Admob shouldReloadAds success")
                                mInterstitialAd.setInterstitialAd(interstitialAd)
                                callback?.onInterstitialLoad(mInterstitialAd)
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                mInterstitialAd.setInterstitialAd(null)
                                callback?.onAdFailedToLoad(ApAdError(i))
                            }

                            override fun onAdFailedToShow(adError: AdError?) {
                                super.onAdFailedToShow(adError)
                                callback?.onAdFailedToShow(ApAdError(adError))
                            }
                        })
                } else {
                    mInterstitialAd.setInterstitialAd(null)
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                Log.d(TAG, "onNextAction: ")
                callback?.onNextAction()
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                Log.d(TAG, "onAdFailedToShow: ")
                callback?.onAdFailedToShow(ApAdError(adError))
                if (shouldReloadAds) {
                    Admob.instance!!.getInterstitialAds(
                        context,
                        mInterstitialAd.getInterstitialAd()?.adUnitId,
                        object : AdCallback() {
                            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                                super.onInterstitialLoad(interstitialAd)
                                Log.d(TAG, "Admob shouldReloadAds success")
                                mInterstitialAd.setInterstitialAd(interstitialAd)
                                callback?.onInterstitialLoad(mInterstitialAd)
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                callback?.onAdFailedToLoad(ApAdError(i))
                            }

                            override fun onAdFailedToShow(adError: AdError?) {
                                super.onAdFailedToShow(adError)
                                callback?.onAdFailedToShow(ApAdError(adError))
                            }
                        })
                } else {
                    mInterstitialAd.setInterstitialAd(null)
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback?.onAdClicked()
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                callback?.onInterstitialShow()
            }
        }
        Admob.instance!!.showInterstitialAdByTimes(
            context!!,
            mInterstitialAd.getInterstitialAd(),
            adCallback
        )
    }

    /**
     * Load native ad and auto populate ad to view in activity
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     */
    fun loadNativeAd(
        activity: Activity, id: String?,
        layoutCustomNative: Int
    ) {
        val adPlaceHolder = activity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmerLoading =
            activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        if (AppPurchase.instance?.isPurchased == true) {
            if (containerShimmerLoading != null) {
                containerShimmerLoading.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
            }
            return
        }
        Admob.instance!!.loadNativeAd(
            activity as Context, id, object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    populateNativeAdView(
                        activity,
                        ApNativeAd(layoutCustomNative, unifiedNativeAd),
                        adPlaceHolder,
                        containerShimmerLoading
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e(TAG, "onAdFailedToLoad : NativeAd")
                }
            })
    }

    /**
     * Load native ad and auto populate ad to adPlaceHolder and hide containerShimmerLoading
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    fun loadNativeAd(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?
    ) {
        Admob.instance!!.loadNativeAd(
            activity as Context?, id, object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    populateNativeAdView(
                        activity,
                        ApNativeAd(layoutCustomNative, unifiedNativeAd),
                        adPlaceHolder,
                        containerShimmerLoading
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e(TAG, "onAdFailedToLoad : NativeAd")
                }
            })
    }

    /**
     * Load native ad and auto populate ad to adPlaceHolder and hide containerShimmerLoading
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    fun loadNativeAd(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        callback: AperoAdCallback
    ) {
        Admob.instance!!.loadNativeAd(
            activity as Context?, id, object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    callback.onNativeAdLoaded(ApNativeAd(layoutCustomNative, unifiedNativeAd))
                    populateNativeAdView(
                        activity,
                        ApNativeAd(layoutCustomNative, unifiedNativeAd),
                        adPlaceHolder,
                        containerShimmerLoading
                    )
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    callback.onAdImpression()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    callback.onAdFailedToLoad(ApAdError(i))
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    callback.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    callback.onAdClicked()
                }
            })

    }

    /**
     * Result a ApNativeAd in onUnifiedNativeAdLoaded when native ad loaded
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param callback
     */
    fun loadNativeAdResultCallback(
        activity: Activity?, id: String?,
        layoutCustomNative: Int, callback: AperoAdCallback
    ) {
        Admob.instance!!.loadNativeAd(
            activity as Context?, id, object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    callback.onNativeAdLoaded(ApNativeAd(layoutCustomNative, unifiedNativeAd))
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    callback.onAdFailedToLoad(ApAdError(i))
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    callback.onAdFailedToShow(ApAdError(adError))
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    callback.onAdClicked()
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    callback.onAdImpression()
                }
            })
    }

    /**
     * Populate Unified Native Ad to View
     *
     * @param activity
     * @param apNativeAd
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    fun populateNativeAdView(
        activity: Activity?,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?
    ) {
        if (apNativeAd.getAdmobNativeAd() == null && apNativeAd.nativeView == null) {
            containerShimmerLoading!!.visibility = View.GONE
            Log.e(TAG, "populateNativeAdView failed : native is not loaded ")
            return
        }
        when (adConfig!!.mediationProvider) {
            AperoAdConfig.PROVIDER_ADMOB -> {
                @SuppressLint("InflateParams") val adView = LayoutInflater.from(activity)
                    .inflate(apNativeAd.layoutCustomNative, null) as NativeAdView
                containerShimmerLoading!!.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
                adPlaceHolder.visibility = View.VISIBLE
                apNativeAd.getAdmobNativeAd()
                    ?.let { Admob.instance?.populateUnifiedNativeAdView(it, adView) }
                adPlaceHolder.removeAllViews()
                adPlaceHolder.addView(adView)
            }

            AperoAdConfig.PROVIDER_MAX -> {
                containerShimmerLoading!!.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
                adPlaceHolder.visibility = View.VISIBLE
                adPlaceHolder.removeAllViews()
                if (apNativeAd.nativeView?.parent != null) {
                    (apNativeAd.nativeView?.parent as ViewGroup).removeAllViews()
                }
                adPlaceHolder.addView(apNativeAd.nativeView)
            }
        }
    }

    fun getRewardAd(activity: Activity?, id: String?): ApRewardAd {
        val apRewardAd = ApRewardAd()
        Admob.instance!!.initRewardAds(
            activity,
            id,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    Log.i(TAG, "getRewardAd AdLoaded: ")
                    apRewardAd.setAdmobReward(rewardedAd)
                }
            })
        return apRewardAd
    }

    fun getRewardAdInterstitial(activity: Activity?, id: String?): ApRewardAd {
        val apRewardAd = ApRewardAd()
        Admob.instance!!.getRewardInterstitial(
            activity,
            id,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    Log.i(TAG, "getRewardAdInterstitial AdLoaded: ")
                    apRewardAd.setAdmobReward(rewardedAd)
                }
            })
        return apRewardAd
    }

    fun getRewardAd(activity: Activity?, id: String?, callback: AperoAdCallback): ApRewardAd {
        val apRewardAd = ApRewardAd()
        Admob.instance!!.initRewardAds(activity, id, object : AdCallback() {
            override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                super.onRewardAdLoaded(rewardedAd)
                apRewardAd.setAdmobReward(rewardedAd)
                callback.onAdLoaded()
            }
        })
        return apRewardAd
    }

    fun getRewardInterstitialAd(
        activity: Activity?,
        id: String?,
        callback: AperoAdCallback
    ): ApRewardAd {
        val apRewardAd = ApRewardAd()
        Admob.instance!!.getRewardInterstitial(activity, id, object : AdCallback() {
            override fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {
                super.onRewardAdLoaded(rewardedAd)
                apRewardAd.setAdmobReward(rewardedAd)
                callback.onAdLoaded()
            }
        })
        return apRewardAd
        return apRewardAd
    }

    fun forceShowRewardAd(activity: Activity?, apRewardAd: ApRewardAd, callback: AperoAdCallback?) {
        if (!apRewardAd.isReady) {
            Log.e(TAG, "forceShowRewardAd fail: reward ad not ready")
            callback?.onNextAction()
            return
        }
        if (apRewardAd.isRewardInterstitial) {
            Admob.instance!!.showRewardInterstitial(
                activity,
                apRewardAd.admobRewardInter,
                object : RewardCallback {
                    override fun onUserEarnedReward(var1: RewardItem?) {
                        var1?.let { ApRewardItem(it) }?.let { callback?.onUserEarnedReward(it) }
                    }

                    override fun onRewardedAdClosed() {
                        apRewardAd.clean()
                        callback?.onNextAction()
                    }

                    override fun onRewardedAdFailedToShow(codeError: Int) {
                        apRewardAd.clean()
                        callback?.onAdFailedToShow(
                            ApAdError(
                                AdError(
                                    codeError,
                                    "note msg",
                                    "Reward"
                                )
                            )
                        )
                    }

                    override fun onAdClicked() {
                        callback?.onAdClicked()
                    }
                })
        } else {
            Admob.instance!!.showRewardAds(
                activity,
                apRewardAd.getAdmobReward(),
                object : RewardCallback {
                    override fun onUserEarnedReward(var1: RewardItem?) {
                        var1?.let { ApRewardItem(it) }?.let { callback?.onUserEarnedReward(it) }
                    }

                    override fun onRewardedAdClosed() {
                        apRewardAd.clean()
                        callback?.onNextAction()
                    }

                    override fun onRewardedAdFailedToShow(codeError: Int) {
                        apRewardAd.clean()
                        callback?.onAdFailedToShow(
                            ApAdError(
                                AdError(
                                    codeError,
                                    "note msg",
                                    "Reward"
                                )
                            )
                        )
                    }

                    override fun onAdClicked() {
                        callback?.onAdClicked()
                    }
                })
        }
    }

    /**
     * Result a AperoAdAdapter with ad native repeating interval
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param layoutAdPlaceHolder
     * @param originalAdapter
     * @param listener
     * @param repeatingInterval
     * @return
     */
    fun getNativeRepeatAdapter(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        layoutAdPlaceHolder: Int,
        originalAdapter: RecyclerView.Adapter<*>?,
        listener: AperoAdPlacer.Listener,
        repeatingInterval: Int
    ): AperoAdAdapter {
        return AperoAdAdapter(
            Admob.instance!!.getNativeRepeatAdapter(
                activity, id, layoutCustomNative, layoutAdPlaceHolder,
                originalAdapter, listener, repeatingInterval
            )
        )
    }

    /**
     * Result a AperoAdAdapter with ad native fixed in position
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param layoutAdPlaceHolder
     * @param originalAdapter
     * @param listener
     * @param position
     * @return
     */
    fun getNativeFixedPositionAdapter(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        layoutAdPlaceHolder: Int,
        originalAdapter: RecyclerView.Adapter<*>?,
        listener: AperoAdPlacer.Listener,
        position: Int
    ): AperoAdAdapter {
        return AperoAdAdapter(
            Admob.instance!!.getNativeFixedPositionAdapter(
                activity, id, layoutCustomNative, layoutAdPlaceHolder,
                originalAdapter, listener, position
            )
        )
    }


    companion object {
        const val TAG_ADJUST = "AperoAdjust"
        const val TAG = "AperoAd"

        @Volatile
        private var INSTANCE: AperoAd? = null

        @get:Synchronized
        val instance: AperoAd?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = AperoAd()
                }
                return INSTANCE
            }
    }
}