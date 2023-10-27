package com.vapps.module_ads.control.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.applovin.mediation.AppLovinExtras
import com.applovin.mediation.ApplovinAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.jirbo.adcolony.AdColonyAdapter
import com.jirbo.adcolony.AdColonyBundleBuilder
import com.vapps.module_ads.R
import com.vapps.module_ads.control.admob.adsopen.AppOpenManager
import com.vapps.module_ads.control.admob.nativead.AdmobRecyclerAdapter
import com.vapps.module_ads.control.admob.nativead.AperoAdPlacer
import com.vapps.module_ads.control.admob.nativead.AperoAdPlacerSettings
import com.vapps.module_ads.control.billing.AppPurchase
import com.vapps.module_ads.control.dialog.PrepareLoadingAdsDialog
import com.vapps.module_ads.control.event.AperoLogEventManager.logClickAdsEvent
import com.vapps.module_ads.control.event.AperoLogEventManager.logPaidAdImpression
import com.vapps.module_ads.control.helper.AdmodHelper.getNumClickAdsPerDay
import com.vapps.module_ads.control.helper.AdmodHelper.setupAdmodData
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.module_ads.control.listener.RewardCallback
import com.vapps.module_ads.control.utils.AdType
import com.vapps.module_ads.control.utils.AppUtil.VARIANT_DEV
import com.vapps.module_ads.control.utils.SharePreferenceUtils.setLastImpressionInterstitialTime
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Calendar
import java.util.Locale
import java.util.Objects

class Admob private constructor() {
    private var currentClicked = 0
    private var nativeId: String? = null
    private var numShowAds = 3
    private var maxClickAds = 100
    private var handlerTimeout: Handler? = null
    private var rdTimeout: Runnable? = null
    private val handlerTimeoutPriority: Handler? = null
    private val rdTimeoutPriority: Runnable? = null
    private var dialog: PrepareLoadingAdsDialog? = null
    private var isTimeout = false // xử lý timeout show ads
    private val isTimeoutPriority = false
    private var disableAdResumeWhenClickAds = false
    var isShowLoadingSplash =
        false //kiểm tra trạng thái ad splash, ko cho load, show khi đang show loading ads splash
        private set
    private var isFan = false
    private var isAdcolony = false
    private var isAppLovin = false
    var isTimeDelay = false //xử lý delay time show ads, = true mới show ads
    var isTimeDelayPriority = false //xử lý delay time show ads, = true mới show ads
    private var openActivityAfterShowInterAds = false
    private var context: Context? = null
    private val MAX_SMALL_INLINE_BANNER_HEIGHT = 50
    var mInterstitialSplash: InterstitialAd? = null
    var mInterstitialSplashPriority: InterstitialAd? = null
    var interstitialAd: InterstitialAd? = null
    private var isShowInterstitialSplashSuccess = false
    fun setFan(fan: Boolean) {
        isFan = fan
    }

    fun setColony(adcolony: Boolean) {
        isAdcolony = adcolony
    }

    fun setAppLovin(appLovin: Boolean) {
        isAppLovin = appLovin
    }

    /**
     * Giới hạn số lần click trên 1 admod tren 1 ngay
     *
     * @param maxClickAds
     */
    fun setMaxClickAdsPerDay(maxClickAds: Int) {
        this.maxClickAds = maxClickAds
    }

    fun setNumToShowAds(numShowAds: Int) {
        this.numShowAds = numShowAds
    }

    fun setNumToShowAds(numShowAds: Int, currentClicked: Int) {
        this.numShowAds = numShowAds
        this.currentClicked = currentClicked
    }

    /**
     * Disable ad resume when user click ads and back to app
     *
     * @param disableAdResumeWhenClickAds
     */
    fun setDisableAdResumeWhenClickAds(disableAdResumeWhenClickAds: Boolean) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds
    }

    /**
     * khởi tạo admod
     *
     * @param context
     */
    fun init(context: Context, testDeviceList: List<String?>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        MobileAds.initialize(context) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    TAG, String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
        }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceList).build()
        )
        this.context = context
    }

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        MobileAds.initialize(context) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    TAG, String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
        }
        this.context = context
    }

    private fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    /**
     * If true -> callback onNextAction() is called right after Ad Interstitial showed
     * It help remove delay when user click close Ad and onAdClosed called
     *
     * @param openActivityAfterShowInterAds
     */
    fun setOpenActivityAfterShowInterAds(openActivityAfterShowInterAds: Boolean) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds
    }

    val adRequest: AdRequest
        get() {
            val builder = AdRequest.Builder()
            // no need from facebook sdk ver 6.12.0.0
            /*if (isFan) {
            Bundle extras = new FacebookExtras()
                    .setNativeBanner(true)
                    .build();

            builder.addNetworkExtrasBundle(FacebookAdapter.class, extras);
        }*/if (isAdcolony) {
                AdColonyBundleBuilder.setShowPrePopup(true)
                AdColonyBundleBuilder.setShowPostPopup(true)
                builder.addNetworkExtrasBundle(
                    AdColonyAdapter::class.java,
                    AdColonyBundleBuilder.build()
                )
            }
            if (isAppLovin) {
                val extras = AppLovinExtras.Builder()
                    .setMuteAudio(true)
                    .build()
                builder.addNetworkExtrasBundle(ApplovinAdapter::class.java, extras)
            }
            //        builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
            return builder.build()
        }

    private fun requestInterstitialAds(
        mInterstitialAd: InterstitialAd?,
        id: String,
        callback: InterstitialAdLoadCallback
    ) {
        if (mInterstitialAd == null) {
        }
    }

    fun interstitialSplashLoaded(): Boolean {
        return mInterstitialSplash != null
    }

    fun getmInterstitialSplash(): InterstitialAd? {
        return mInterstitialSplash
    }

    /**
     * Load quảng cáo Full tại màn SplashActivity
     * Sau khoảng thời gian timeout thì load ads và callback về cho View
     *
     * @param context
     * @param id
     * @param timeOut    : thời gian chờ ads, timeout <= 0 tương đương với việc bỏ timeout
     * @param timeDelay  : thời gian chờ show ad từ lúc load ads
     * @param adListener
     */
    fun loadSplashInterstitialAds(
        context: Context?,
        id: String?,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelay = false
        isTimeout = false
        Log.i(TAG, "loadSplashInterstitialAds: ")
        Log.i(
            TAG,
            "loadSplashInterstitalAds  start time loading:" + Calendar.getInstance().timeInMillis + "    ShowLoadingSplash:" + isShowLoadingSplash
        )
        if (AppPurchase.instance!!.isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed(Runnable { //check delay show ad splash
            if (mInterstitialSplash != null) {
                Log.i(TAG, "loadSplashInterstitalAds:show ad on delay ")
                onShowSplash(context as AppCompatActivity?, adListener)
                return@Runnable
            }
            Log.i(TAG, "loadSplashInterstitalAds: delay validate")
            isTimeDelay = true
        }, timeDelay)
        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                Log.e(TAG, "loadSplashInterstitalAds: on timeout")
                isTimeout = true
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitalAds:show ad on timeout ")
                    onShowSplash(context as AppCompatActivity?, adListener)
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplash = false
                }
            }
            handlerTimeout!!.postDelayed(rdTimeout!!, timeOut)
        }
        isShowLoadingSplash = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.e(
                    TAG,
                    "loadSplashInterstitalAds  end time loading success:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd
                    mInterstitialSplash!!.onPaidEventListener =
                        OnPaidEventListener { adValue: AdValue ->
                            Log.d(TAG, "OnPaidEvent splash:" + adValue.valueMicros)
                            logPaidAdImpression(
                                context!!,
                                adValue,
                                mInterstitialSplash!!.adUnitId,
                                mInterstitialSplash!!.responseInfo
                                    .mediationAdapterClassName!!, AdType.INTERSTITIAL
                            )
                        }
                    if (isTimeDelay) {
                        onShowSplash(context as AppCompatActivity?, adListener)
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on loaded ")
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplash = false
                Log.e(
                    TAG,
                    "loadSplashInterstitalAds  end time loading error:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout!!.removeCallbacks(rdTimeout!!)
                    }
                    if (i != null) Log.e(TAG, "loadSplashInterstitalAds: load fail " + i.message)
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError)
                    adListener.onNextAction()
                }
            }
        })
    }

    /**
     * Load quảng cáo Full tại màn SplashActivity
     * Sau khoảng thời gian timeout thì load ads và callback về cho View
     *
     * @param context
     * @param id
     * @param timeOut           : thời gian chờ ads, timeout <= 0 tương đương với việc bỏ timeout
     * @param timeDelay         : thời gian chờ show ad từ lúc load ads
     * @param showSplashIfReady : auto show ad splash if ready
     * @param adListener
     */
    fun loadSplashInterstitialAds(
        context: Context?,
        id: String?,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean,
        adListener: AdCallback?
    ) {
        isTimeDelay = false
        isTimeout = false
        Log.i(
            TAG,
            "loadSplashInterstitalAds  start time loading:" + Calendar.getInstance().timeInMillis + "    ShowLoadingSplash:" + isShowLoadingSplash
        )
        if (AppPurchase.instance!!.isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed(Runnable { //check delay show ad splash
            if (mInterstitialSplash != null) {
                Log.i(TAG, "loadSplashInterstitalAds:show ad on delay ")
                if (showSplashIfReady) onShowSplash(
                    context as AppCompatActivity?,
                    adListener
                ) else adListener!!.onAdSplashReady()
                return@Runnable
            }
            Log.i(TAG, "loadSplashInterstitalAds: delay validate")
            isTimeDelay = true
        }, timeDelay)
        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                Log.e(TAG, "loadSplashInterstitalAds: on timeout")
                isTimeout = true
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitalAds:show ad on timeout ")
                    if (showSplashIfReady) onShowSplash(
                        context as AppCompatActivity?,
                        adListener
                    ) else adListener!!.onAdSplashReady()
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplash = false
                }
            }
            handlerTimeout!!.postDelayed(rdTimeout!!, timeOut)
        }

//        if (isShowLoadingSplash)
//            return;
        isShowLoadingSplash = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.e(
                    TAG,
                    "loadSplashInterstitalAds  end time loading success:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd
                    if (isTimeDelay) {
                        if (showSplashIfReady) onShowSplash(
                            context as AppCompatActivity?,
                            adListener
                        ) else adListener!!.onAdSplashReady()
                        Log.i(TAG, "loadSplashInterstitalAds:show ad on loaded ")
                    }
                }
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError)
                    adListener.onNextAction()
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                Log.e(
                    TAG,
                    "loadSplashInterstitalAds  end time loading error:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (adListener != null) {
                    adListener.onNextAction()
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout!!.removeCallbacks(rdTimeout!!)
                    }
                    if (i != null) Log.e(TAG, "loadSplashInterstitalAds: load fail " + i.message)
                    adListener.onAdFailedToLoad(i)
                }
            }
        })
    }

    fun onShowSplash(activity: AppCompatActivity?, adListener: AdCallback?) {
        isShowLoadingSplash = true
        Log.d(TAG, "onShowSplash: ")
        if (mInterstitialSplash == null) {
            adListener!!.onNextAction()
            return
        }
        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout!!.removeCallbacks(rdTimeout!!)
        }
        adListener?.onAdLoaded()
        mInterstitialSplash!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, " Splash:onAdShowedFullScreenContent ")
                isShowInterstitialSplashSuccess = true
                AppOpenManager.instance!!.isInterstitialShowing = true
                isShowLoadingSplash = false
                mInterstitialSplash = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, " Splash:onAdDismissedFullScreenContent ")
                AppOpenManager.instance!!.isInterstitialShowing = false
                mInterstitialSplash = null
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction()
                    }
                    adListener.onAdClosed()
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                }
                isShowLoadingSplash = false
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Splash onAdFailedToShowFullScreenContent: " + adError.message)
                mInterstitialSplash = null
                isShowLoadingSplash = false
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError)
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction()
                    }
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adListener?.onAdClicked()
                if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                logClickAdsEvent(context, mInterstitialSplash!!.adUnitId)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                adListener?.onAdImpression()
            }
        }
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
                dialog = PrepareLoadingAdsDialog(activity)
                try {
                    dialog!!.show()
                } catch (e: Exception) {
                    adListener!!.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialog = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction()
                        Handler().postDelayed(
                            { if (dialog != null && dialog!!.isShowing && !activity.isDestroyed) dialog!!.dismiss() },
                            1500
                        )
                    }
                    if (mInterstitialSplash != null) {
                        Log.i(
                            TAG,
                            "start show InterstitialAd " + activity.lifecycle.currentState.name + "/" + ProcessLifecycleOwner.get().lifecycle.currentState.name
                        )
                        mInterstitialSplash!!.show(activity)
                        isShowLoadingSplash = false
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog!!.dismiss()
                        }
                        adListener.onNextAction()
                        isShowLoadingSplash = false
                    }
                } else {
                    if (dialog != null && dialog!!.isShowing && !activity!!.isDestroyed) dialog!!.dismiss()
                    isShowLoadingSplash = false
                    Log.e(TAG, "onShowSplash:   show fail in background after show loading ad")
                    adListener!!.onAdFailedToShow(
                        AdError(
                            0,
                            " show fail in background after show loading ad",
                            "AperoAd"
                        )
                    )
                }
            }, 800)
        } else {
            adListener!!.onAdFailedToShow(
                AdError(
                    0,
                    " show fail in background after show loading ad",
                    "AperoAd"
                )
            )
            Log.e(TAG, "onShowSplash: fail on background")
            isShowLoadingSplash = false
        }
    }

    fun onCheckShowSplashWhenFail(
        activity: AppCompatActivity,
        callback: AdCallback?,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed(object : Runnable {
            override fun run() {
                if (interstitialSplashLoaded() && !isShowLoadingSplash) {
                    Log.i(TAG, "show ad splash when show fail in background")
                    instance!!.onShowSplash(activity, callback)
                }
            }
        }, timeDelay.toLong())
    }

    fun loadInterstitialAds(
        context: Context?,
        id: String?,
        timeOut: Long,
        adListener: AdCallback?
    ) {
        isTimeout = false
        if (AppPurchase.instance!!.isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        interstitialAd = null
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                this@Admob.interstitialAd = interstitialAd
                if (interstitialAd == null) {
                    adListener?.onAdFailedToLoad(null)
                    return
                }
                if (handlerTimeout != null && rdTimeout != null) {
                    handlerTimeout!!.removeCallbacks(rdTimeout!!)
                }
                if (isTimeout) {
                    return
                }
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout!!.removeCallbacks(rdTimeout!!)
                    }
                    adListener.onInterstitialLoad(interstitialAd)
                }
                if (interstitialAd != null) {
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent loadInterstitialAds:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context!!,
                            adValue,
                            interstitialAd.adUnitId,
                            interstitialAd.responseInfo
                                .mediationAdapterClassName!!, AdType.INTERSTITIAL
                        )
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout!!.removeCallbacks(rdTimeout!!)
                    }
                    adListener.onAdFailedToLoad(i)
                }
            }
        })
        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                isTimeout = true
                if (interstitialAd != null) {
                    adListener!!.onInterstitialLoad(interstitialAd)
                    return@Runnable
                }
                adListener?.onNextAction()
            }
            handlerTimeout!!.postDelayed(rdTimeout!!, timeOut)
        }
    }

    /**
     * Trả về 1 InterstitialAd và request Ads
     *
     * @param context
     * @param id
     * @return
     */
    fun getInterstitialAds(context: Context?, id: String?, adCallback: AdCallback?) {
        if (AppPurchase.instance!!.isPurchased(context) || getNumClickAdsPerDay(
                context!!,
                id
            ) >= maxClickAds
        ) {
            adCallback!!.onInterstitialLoad(null)
            return
        }
        InterstitialAd.load(
            context!!, id!!, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adCallback?.onInterstitialLoad(interstitialAd)

                    //tracking adjust
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent getInterstitalAds:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context,
                            adValue,
                            interstitialAd.adUnitId,
                            interstitialAd.responseInfo
                                .mediationAdapterClassName!!, AdType.INTERSTITIAL
                        )
                    }
                    Log.i(TAG, "InterstitialAds onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.i(TAG, loadAdError.message)
                    adCallback?.onAdFailedToLoad(loadAdError)
                }
            })
    }

    /**
     * Hiển thị ads  timeout
     * Sử dụng khi reopen app in splash
     *
     * @param context
     * @param mInterstitialAd
     * @param timeDelay
     */
    fun showInterstitialAdByTimes(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?,
        timeDelay: Long
    ) {
        if (timeDelay > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable { forceShowInterstitial(context, mInterstitialAd, callback) }
            handlerTimeout!!.postDelayed(rdTimeout!!, timeDelay)
        } else {
            forceShowInterstitial(context, mInterstitialAd, callback)
        }
    }

    /**
     * Hiển thị ads theo số lần được xác định trước và callback result
     * vd: click vào 3 lần thì show ads full.
     * AdmodHelper.setupAdmodData(context) -> kiểm tra xem app đc hoạt động đc 1 ngày chưa nếu YES thì reset lại số lần click vào ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    fun showInterstitialAdByTimes(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        setupAdmodData(context)
        if (AppPurchase.instance!!.isPurchased(context)) {
            callback!!.onNextAction()
            return
        }
        if (mInterstitialAd == null) {
            callback?.onNextAction()
            return
        }
        mInterstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                // Called when fullscreen content is dismissed.
                AppOpenManager.instance!!.isInterstitialShowing = false
                if (callback != null) {
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction()
                    }
                    callback.onAdClosed()
                }
                if (dialog != null) {
                    dialog!!.dismiss()
                }
                Log.e(TAG, "onAdDismissedFullScreenContent")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                // Called when fullscreen content failed to show.
                if (callback != null) {
                    callback.onAdFailedToShow(adError)
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction()
                    }
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                Log.e(TAG, "onAdShowedFullScreenContent ")
                setLastImpressionInterstitialTime(context)
                AppOpenManager.instance!!.isInterstitialShowing = true
                // Called when fullscreen content is shown.
            }

            override fun onAdClicked() {
                super.onAdClicked()
                if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                callback?.onAdClicked()
                logClickAdsEvent(context, mInterstitialAd.adUnitId)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback?.onAdImpression()
            }
        }
        if (getNumClickAdsPerDay(context, mInterstitialAd.adUnitId) < maxClickAds) {
            showInterstitialAd(context, mInterstitialAd, callback)
            return
        }
        callback?.onNextAction()
    }

    /**
     * Bắt buộc hiển thị  ads full và callback result
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    fun forceShowInterstitial(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        currentClicked = numShowAds
        showInterstitialAdByTimes(context, mInterstitialAd, callback)
    }

    /**
     * Kiểm tra và hiện thị ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    private fun showInterstitialAd(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        currentClicked++
        if (currentClicked >= numShowAds && mInterstitialAd != null) {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                try {
                    if (dialog != null && dialog!!.isShowing) dialog!!.dismiss()
                    dialog = PrepareLoadingAdsDialog(context)
                    dialog!!.setCancelable(false)
                    try {
                        callback!!.onInterstitialShow()
                        dialog!!.show()
                    } catch (e: Exception) {
                        callback!!.onNextAction()
                        return
                    }
                } catch (e: Exception) {
                    dialog = null
                    e.printStackTrace()
                }
                Handler().postDelayed({
                    if ((context as AppCompatActivity).lifecycle.currentState.isAtLeast(
                            Lifecycle.State.RESUMED
                        )
                    ) {
                        if (openActivityAfterShowInterAds && callback != null) {
                            callback.onNextAction()
                            Handler().postDelayed(
                                { if (dialog != null && dialog!!.isShowing && !(context as Activity).isDestroyed) dialog!!.dismiss() },
                                1500
                            )
                        }
                        Log.i(
                            TAG,
                            "start show InterstitialAd " + context.lifecycle.currentState.name + "/" + ProcessLifecycleOwner.get().lifecycle.currentState.name
                        )
                        mInterstitialAd.show((context as Activity))
                    } else {
                        if (dialog != null && dialog!!.isShowing && !(context as Activity).isDestroyed) dialog!!.dismiss()
                        Log.e(
                            TAG,
                            "showInterstitialAd:   show fail in background after show loading ad"
                        )
                        callback!!.onAdFailedToShow(
                            AdError(
                                0,
                                " show fail in background after show loading ad",
                                "AperoAd"
                            )
                        )
                    }
                }, 800)
            }
            currentClicked = 0
        } else if (callback != null) {
            if (dialog != null) {
                dialog!!.dismiss()
            }
            callback.onNextAction()
        }
    }

    /**
     * Load quảng cáo Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    fun loadBanner(mActivity: Activity, id: String?) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            null,
            false,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load quảng cáo Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    fun loadBanner(mActivity: Activity, id: String?, callback: AdCallback?) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            false,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     */
    @Deprecated("Using loadInlineBanner()")
    fun loadBanner(mActivity: Activity, id: String?, useInlineAdaptive: Boolean) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            null,
            useInlineAdaptive,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param inlineStyle
     */
    fun loadInlineBanner(activity: Activity, id: String?, inlineStyle: String) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle)
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param callback
     * @param useInlineAdaptive
     */
    @Deprecated("Using loadInlineBanner() with callback")
    fun loadBanner(
        mActivity: Activity,
        id: String?,
        callback: AdCallback?,
        useInlineAdaptive: Boolean
    ) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param inlineStyle
     * @param callback
     */
    fun loadInlineBanner(
        activity: Activity,
        id: String?,
        inlineStyle: String,
        callback: AdCallback?
    ) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle)
    }

    /**
     * Load quảng cáo Collapsible Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    fun loadCollapsibleBanner(
        mActivity: Activity,
        id: String?,
        gravity: String?,
        callback: AdCallback?
    ) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback)
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     */
    fun loadBannerFragment(mActivity: Activity, id: String?, rootView: View) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            null,
            false,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     */
    fun loadBannerFragment(
        mActivity: Activity,
        id: String?,
        rootView: View,
        callback: AdCallback?
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            false,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param rootView
     */
    @Deprecated("Using loadInlineBannerFragment()")
    fun loadBannerFragment(
        mActivity: Activity,
        id: String?,
        rootView: View,
        useInlineAdaptive: Boolean
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            null,
            useInlineAdaptive,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param rootView
     * @param inlineStyle
     */
    fun loadInlineBannerFragment(
        activity: Activity,
        id: String?,
        rootView: View,
        inlineStyle: String
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle)
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param rootView
     * @param callback
     * @param useInlineAdaptive
     */
    @Deprecated("Using loadInlineBannerFragment() with callback")
    fun loadBannerFragment(
        mActivity: Activity,
        id: String?,
        rootView: View,
        callback: AdCallback?,
        useInlineAdaptive: Boolean
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param rootView
     * @param inlineStyle
     * @param callback
     */
    fun loadInlineBannerFragment(
        activity: Activity,
        id: String?,
        rootView: View,
        inlineStyle: String,
        callback: AdCallback?
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle)
    }

    /**
     * Load quảng cáo Collapsible Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     * @param gravity
     * @param callback
     */
    fun loadCollapsibleBannerFragment(
        mActivity: Activity,
        id: String?,
        rootView: View,
        gravity: String?,
        callback: AdCallback?
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback)
    }

    fun loadBanner(
        mActivity: Activity, id: String?,
        adContainer: FrameLayout, containerShimmer: ShimmerFrameLayout,
        callback: AdCallback?, useInlineAdaptive: Boolean, inlineStyle: String
    ) {
        if (AppPurchase.instance!!.isPurchased(mActivity)) {
            containerShimmer.visibility = View.GONE
            return
        }
        containerShimmer.visibility = View.VISIBLE
        containerShimmer.startShimmer()
        try {
            val adView = AdView(mActivity)
            adView.adUnitId = id!!
            adContainer.addView(adView)
            val adSize = getAdSize(mActivity, useInlineAdaptive, inlineStyle)
            val adHeight: Int
            adHeight = if (useInlineAdaptive && inlineStyle.equals(
                    BANNER_INLINE_SMALL_STYLE,
                    ignoreCase = true
                )
            ) {
                MAX_SMALL_INLINE_BANNER_HEIGHT
            } else {
                adSize.height
            }
            containerShimmer.layoutParams.height =
                (adHeight * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    containerShimmer.stopShimmer()
                    adContainer.visibility = View.GONE
                    containerShimmer.visibility = View.GONE
                    callback?.onAdFailedToLoad(loadAdError)
                }

                override fun onAdLoaded() {
                    Log.d(
                        TAG, "Banner adapter class name: " + adView.responseInfo!!
                            .mediationAdapterClassName
                    )
                    containerShimmer.stopShimmer()
                    containerShimmer.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                    adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent banner:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context!!,
                            adValue,
                            adView.adUnitId,
                            adView.responseInfo!!
                                .mediationAdapterClassName!!, AdType.BANNER
                        )
                    }
                    callback?.onAdLoaded()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    callback?.onAdImpression()
                }
            }
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun populateUnifiedBannerAdView(mActivity: Activity, adView: AdView, adContainer: FrameLayout) {
        if (Arrays.asList(*mActivity.resources.getStringArray(R.array.list_id_test))
                .contains(adView.adUnitId)
        ) {
            //showTestIdAlert(mActivity, BANNER_ADS, adView.getAdUnitId());
        }
        try {
            adContainer.addView(adView)
            adContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun requestLoadBanner(
        mActivity: Activity,
        id: String?,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ) {
        if (AppPurchase.instance!!.isPurchased(mActivity)) {
            callback!!.onAdFailedToLoad(LoadAdError(1999, "App isPurchased", "", null, null))
            return
        }
        try {
            val adView = AdView(mActivity)
            adView.adUnitId = id!!
            val adSize = getAdSize(mActivity, useInlineAdaptive, inlineStyle)
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    callback!!.onAdFailedToLoad(loadAdError)
                }

                override fun onAdLoaded() {
                    Log.d(
                        TAG, "Banner adapter class name: " + adView.responseInfo!!
                            .mediationAdapterClassName
                    )
                    callback!!.onBannerLoaded(adView)
                    adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent banner:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context!!,
                            adValue,
                            adView.adUnitId,
                            adView.responseInfo!!
                                .mediationAdapterClassName!!, AdType.BANNER
                        )
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    callback?.onAdImpression()
                }
            }
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCollapsibleBanner(
        mActivity: Activity, id: String?, gravity: String?, adContainer: FrameLayout,
        containerShimmer: ShimmerFrameLayout, callback: AdCallback?
    ) {
        if (AppPurchase.instance!!.isPurchased(mActivity)) {
            containerShimmer.visibility = View.GONE
            return
        }
        containerShimmer.visibility = View.VISIBLE
        containerShimmer.startShimmer()
        try {
            val adView = AdView(mActivity)
            adView.adUnitId = id!!
            adContainer.addView(adView)
            val adSize = getAdSize(mActivity, false, "")
            containerShimmer.layoutParams.height =
                (adSize.height * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity))
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    containerShimmer.stopShimmer()
                    adContainer.visibility = View.GONE
                    containerShimmer.visibility = View.GONE
                    callback?.onAdFailedToLoad(loadAdError)
                }

                override fun onAdLoaded() {
                    Log.d(
                        TAG, "Banner adapter class name: " + adView.responseInfo!!
                            .mediationAdapterClassName
                    )
                    containerShimmer.stopShimmer()
                    containerShimmer.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                    adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent banner:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context!!,
                            adValue,
                            adView.adUnitId,
                            adView.responseInfo!!
                                .mediationAdapterClassName!!, AdType.BANNER
                        )
                    }
                    callback?.onAdLoaded()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    logClickAdsEvent(context, id)
                    callback?.onAdClicked()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAdSize(
        mActivity: Activity,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ): AdSize {

        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = mActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return if (useInlineAdaptive) {
            if (inlineStyle.equals(
                    BANNER_INLINE_LARGE_STYLE,
                    ignoreCase = true
                )
            ) {
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                    mActivity,
                    adWidth
                )
            } else {
                AdSize.getInlineAdaptiveBannerAdSize(
                    adWidth,
                    MAX_SMALL_INLINE_BANNER_HEIGHT
                )
            }
        } else AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            mActivity,
            adWidth
        )
    }

    private fun getAdRequestForCollapsibleBanner(gravity: String?): AdRequest {
        val builder = AdRequest.Builder()
        val admobExtras = Bundle()
        admobExtras.putString("collapsible", gravity)
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, admobExtras)
        // no need from facebook sdk ver 6.12.0.0
        /*if (isFan) {
            Bundle extras = new FacebookExtras()
                    .setNativeBanner(true)
                    .build();

            builder.addNetworkExtrasBundle(FacebookAdapter.class, extras);
        }*/if (isAdcolony) {
            AdColonyBundleBuilder.setShowPrePopup(true)
            AdColonyBundleBuilder.setShowPostPopup(true)
            builder.addNetworkExtrasBundle(
                AdColonyAdapter::class.java,
                AdColonyBundleBuilder.build()
            )
        }
        if (isAppLovin) {
            val extras = AppLovinExtras.Builder()
                .setMuteAudio(true)
                .build()
            builder.addNetworkExtrasBundle(ApplovinAdapter::class.java, extras)
        }
        return builder.build()
    }

    /**
     * load quảng cáo big native
     *
     * @param mActivity
     * @param id
     */
    fun loadNative(mActivity: Activity, id: String) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(
            mActivity,
            containerShimmer,
            frameLayout,
            id,
            R.layout.custom_native_admob_free_size
        )
    }

    fun loadNativeFragment(mActivity: Activity, id: String, parent: View) {
        val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(
            mActivity,
            containerShimmer,
            frameLayout,
            id,
            R.layout.custom_native_admob_free_size
        )
    }

    fun loadSmallNative(mActivity: Activity, adUnitId: String) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(
            mActivity,
            containerShimmer,
            frameLayout,
            adUnitId,
            R.layout.custom_native_admod_medium
        )
    }

    fun loadSmallNativeFragment(mActivity: Activity, adUnitId: String, parent: View) {
        val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(
            mActivity,
            containerShimmer,
            frameLayout,
            adUnitId,
            R.layout.custom_native_admod_medium
        )
    }

    fun loadNativeAd(context: Context?, id: String?, callback: AdCallback?) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context!!, id!!)
            .forNativeAd { nativeAd ->
                callback!!.onUnifiedNativeAdLoaded(nativeAd)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    Log.d(TAG, "OnPaidEvent getInterstitalAds:" + adValue.valueMicros)
                    logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "NativeAd onAdFailedToLoad: " + error.message)
                    callback!!.onAdFailedToLoad(error)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "native onAdImpression")
                    callback?.onAdImpression()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(adRequest)
    }

    fun loadNativeFullScreen(context: Context?, id: String?, callback: AdCallback?) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(false)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(MediaAspectRatio.PORTRAIT)
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context!!, id!!)
            .forNativeAd { nativeAd ->
                callback!!.onUnifiedNativeAdLoaded(nativeAd)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    Log.d(
                        TAG,
                        "loadNativeFullScreen OnPaidEvent getInterstitalAds:" + adValue.valueMicros
                    )
                    logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "loadNativeFullScreen onAdFailedToLoad: " + error.message)
                    callback!!.onAdFailedToLoad(error)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "loadNativeFullScreen onAdImpression")
                    callback?.onAdImpression()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "loadNativeFullScreen onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(adRequest)
    }

    fun loadNativeAds(context: Context?, id: String?, callback: AdCallback?, countAd: Int) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            callback!!.onAdClosed()
            return
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context!!, id!!)
            .forNativeAd { nativeAd ->
                callback!!.onUnifiedNativeAdLoaded(nativeAd)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    Log.d(TAG, "OnPaidEvent getInterstitalAds:" + adValue.valueMicros)
                    logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "NativeAd onAdFailedToLoad: " + error.message)
                    callback!!.onAdFailedToLoad(error)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAds(adRequest, countAd)
    }

    private fun loadNative(
        context: Context,
        containerShimmer: ShimmerFrameLayout,
        frameLayout: FrameLayout,
        id: String,
        layout: Int
    ) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            containerShimmer.visibility = View.GONE
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        containerShimmer.visibility = View.VISIBLE
        containerShimmer.startShimmer()
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context, id)
            .forNativeAd { nativeAd ->
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
                @SuppressLint("InflateParams") val adView = LayoutInflater.from(context)
                    .inflate(layout, null) as NativeAdView
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    Log.d(TAG, "OnPaidEvent native:" + adValue.valueMicros)
                    logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
                populateUnifiedNativeAdView(nativeAd, adView)
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad: " + error.message)
                    containerShimmer.stopShimmer()
                    containerShimmer.visibility = View.GONE
                    frameLayout.visibility = View.GONE
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    logClickAdsEvent(context, id)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(adRequest)
    }

    private fun loadNative(
        context: Context,
        containerShimmer: ShimmerFrameLayout,
        frameLayout: FrameLayout,
        id: String,
        layout: Int,
        callback: AdCallback?
    ) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            containerShimmer.visibility = View.GONE
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        containerShimmer.visibility = View.VISIBLE
        containerShimmer.startShimmer()
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context, id)
            .forNativeAd { nativeAd ->
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
                @SuppressLint("InflateParams") val adView = LayoutInflater.from(context)
                    .inflate(layout, null) as NativeAdView
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    Log.d(TAG, "OnPaidEvent Native:" + adValue.valueMicros)
                    logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
                populateUnifiedNativeAdView(nativeAd, adView)
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad: " + error.message)
                    containerShimmer.stopShimmer()
                    containerShimmer.visibility = View.GONE
                    frameLayout.visibility = View.GONE
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    if (callback != null) {
                        callback.onAdClicked()
                        Log.d(TAG, "onAdClicked")
                    }
                    logClickAdsEvent(context, id)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(adRequest)
    }

    fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        if (adView.mediaView != null) {
            adView.mediaView!!.postDelayed({
                if (context != null && VARIANT_DEV) {
                    val sizeMin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        120f,
                        context!!.resources.displayMetrics
                    )
                    Log.e(TAG, "Native sizeMin: $sizeMin")
                    Log.e(
                        TAG, "Native w/h media : " + adView.mediaView!!
                            .width + "/" + adView.mediaView!!.height
                    )
                    if (adView.mediaView!!.width < sizeMin || adView.mediaView!!.height < sizeMin) {
                        Toast.makeText(context, "Size media native not valid", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }, 1000)
        }
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        //        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline is guaranteed to be in every UnifiedNativeAd.
        try {
            (adView.headlineView as TextView?)!!.text = nativeAd.headline
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        try {
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView?)!!.text = nativeAd.body
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (nativeAd.callToAction == null) {
                Objects.requireNonNull(adView.callToActionView)?.visibility = View.INVISIBLE
            } else {
                Objects.requireNonNull(adView.callToActionView)?.visibility = View.VISIBLE
                (adView.callToActionView as TextView?)!!.text = nativeAd.callToAction
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (nativeAd.icon == null) {
                Objects.requireNonNull(adView.iconView)?.visibility = View.GONE
            } else {
                (adView.iconView as ImageView?)!!.setImageDrawable(
                    nativeAd.icon!!.drawable
                )
                adView.iconView!!.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (nativeAd.price == null) {
                Objects.requireNonNull(adView.priceView)?.visibility = View.INVISIBLE
            } else {
                Objects.requireNonNull(adView.priceView)?.visibility = View.VISIBLE
                (adView.priceView as TextView?)!!.text = nativeAd.price
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //
//        try {
//            if (nativeAd.getStore() == null) {
//                Objects.requireNonNull(adView.getStoreView()).setVisibility(View.INVISIBLE);
//            } else {
//                Objects.requireNonNull(adView.getStoreView()).setVisibility(View.VISIBLE);
//                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
        try {
            if (nativeAd.starRating == null) {
                Objects.requireNonNull(adView.starRatingView)?.visibility = View.INVISIBLE
            } else {
                (Objects.requireNonNull(adView.starRatingView) as RatingBar).rating =
                    nativeAd.starRating!!.toFloat()
                adView.starRatingView!!.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (nativeAd.advertiser == null) {
                adView.advertiserView!!.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView?)!!.text = nativeAd.advertiser
                adView.advertiserView!!.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad. The SDK will populate the adView's MediaView
        // with the media content from this native ad.
        adView.setNativeAd(nativeAd)
    }

    var rewardedAd: RewardedAd? = null
        private set

    /**
     * Khởi tạo quảng cáo reward
     *
     * @param context
     * @param id
     */
    fun initRewardAds(context: Context?, id: String?) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        nativeId = id
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        RewardedAd.load(context!!, id!!, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                this@Admob.rewardedAd = rewardedAd
                this@Admob.rewardedAd!!.onPaidEventListener =
                    OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent Reward:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context,
                            adValue,
                            rewardedAd.adUnitId,
                            this@Admob.rewardedAd!!.responseInfo.mediationAdapterClassName!!,
                            AdType.REWARDED
                        )
                    }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.message)
            }
        })
    }

    /**
     * Load ad Reward
     *
     * @param context
     * @param id
     */
    fun initRewardAds(context: Context?, id: String?, callback: AdCallback) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        nativeId = id
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        RewardedAd.load(context!!, id!!, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                callback.onRewardAdLoaded(rewardedAd)
                this@Admob.rewardedAd = rewardedAd
                this@Admob.rewardedAd!!.onPaidEventListener =
                    OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent Reward:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context,
                            adValue,
                            rewardedAd.adUnitId,
                            this@Admob.rewardedAd!!.responseInfo.mediationAdapterClassName!!,
                            AdType.REWARDED
                        )
                    }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                callback.onAdFailedToLoad(loadAdError)
                rewardedAd = null
                Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.message)
            }
        })
    }

    /**
     * Load ad Reward Interstitial
     *
     * @param context
     * @param id
     */
    fun getRewardInterstitial(context: Context?, id: String?, callback: AdCallback) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        nativeId = id
        if (AppPurchase.instance!!.isPurchased(context)) {
            return
        }
        RewardedInterstitialAd.load(
            context!!,
            id!!,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    callback.onRewardAdLoaded(rewardedAd)
                    Log.i(TAG, "RewardInterstitial onAdLoaded ")
                    rewardedAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(TAG, "OnPaidEvent Reward:" + adValue.valueMicros)
                        logPaidAdImpression(
                            context,
                            adValue,
                            rewardedAd.adUnitId,
                            rewardedAd.responseInfo.mediationAdapterClassName!!, AdType.REWARDED
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    callback.onAdFailedToLoad(loadAdError)
                    Log.e(TAG, "RewardInterstitial onAdFailedToLoad: " + loadAdError.message)
                }
            })
    }

    /**
     * Show Reward and callback
     *
     * @param context
     * @param adCallback
     */
    fun showRewardAds(context: Activity?, adCallback: RewardCallback?) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            adCallback!!.onUserEarnedReward(null)
            return
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId)
            adCallback!!.onRewardedAdFailedToShow(0)
            return
        } else {
            rewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    adCallback?.onRewardedAdClosed()
                    AppOpenManager.instance!!.isInterstitialShowing = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    adCallback?.onRewardedAdFailedToShow(adError.code)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    AppOpenManager.instance!!.isInterstitialShowing = true
                    rewardedAd = null
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    logClickAdsEvent(context, rewardedAd!!.adUnitId)
                }
            }
            rewardedAd!!.show(context!!) { rewardItem -> adCallback?.onUserEarnedReward(rewardItem) }
        }
    }

    /**
     * Show Reward Interstitial and callback
     *
     * @param activity
     * @param rewardedInterstitialAd
     * @param adCallback
     */
    fun showRewardInterstitial(
        activity: Activity?,
        rewardedInterstitialAd: RewardedInterstitialAd?,
        adCallback: RewardCallback?
    ) {
        if (AppPurchase.instance!!.isPurchased(activity)) {
            adCallback!!.onUserEarnedReward(null)
            return
        }
        if (rewardedInterstitialAd == null) {
            initRewardAds(activity, nativeId)
            adCallback!!.onRewardedAdFailedToShow(0)
            return
        } else {
            rewardedInterstitialAd.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        adCallback?.onRewardedAdClosed()
                        AppOpenManager.instance!!.isInterstitialShowing = false
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        super.onAdFailedToShowFullScreenContent(adError)
                        adCallback?.onRewardedAdFailedToShow(adError.code)
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        AppOpenManager.instance!!.isInterstitialShowing = true
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        logClickAdsEvent(activity, rewardedAd!!.adUnitId)
                        if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    }
                }
            rewardedInterstitialAd.show(
                activity!!,
                OnUserEarnedRewardListener { rewardItem -> adCallback?.onUserEarnedReward(rewardItem) })
        }
    }

    /**
     * Show quảng cáo reward và nhận kết quả trả về
     *
     * @param context
     * @param adCallback
     */
    fun showRewardAds(context: Activity?, rewardedAd: RewardedAd?, adCallback: RewardCallback?) {
        if (AppPurchase.instance!!.isPurchased(context)) {
            adCallback!!.onUserEarnedReward(null)
            return
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId)
            adCallback!!.onRewardedAdFailedToShow(0)
            return
        } else {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    adCallback?.onRewardedAdClosed()
                    AppOpenManager.instance!!.isInterstitialShowing = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    adCallback?.onRewardedAdFailedToShow(adError.code)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    AppOpenManager.instance!!.isInterstitialShowing = true
                    initRewardAds(context, nativeId)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.instance!!.disableAdResumeByClickAction()
                    adCallback?.onAdClicked()
                    logClickAdsEvent(context, rewardedAd.adUnitId)
                }
            }
            rewardedAd.show(
                context!!,
                OnUserEarnedRewardListener { rewardItem -> adCallback?.onUserEarnedReward(rewardItem) })
        }
    }

    fun getNativeRepeatAdapter(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        layoutAdPlaceHolder: Int,
        originalAdapter: RecyclerView.Adapter<*>?,
        listener: AperoAdPlacer.Listener?,
        repeatingInterval: Int
    ): AdmobRecyclerAdapter {
        val settings =
            AperoAdPlacerSettings(layoutCustomNative, layoutAdPlaceHolder)
        settings.adUnitId = id
        settings.listener = listener
        settings.setRepeatingInterval(repeatingInterval)
        return AdmobRecyclerAdapter(settings, originalAdapter, activity!!)
    }

    fun getNativeFixedPositionAdapter(
        activity: Activity?,
        id: String?,
        layoutCustomNative: Int,
        layoutAdPlaceHolder: Int,
        originalAdapter: RecyclerView.Adapter<*>?,
        listener: AperoAdPlacer.Listener?,
        position: Int
    ): AdmobRecyclerAdapter {
        val settings =
            AperoAdPlacerSettings(layoutCustomNative, layoutAdPlaceHolder)
        settings.adUnitId = id
        settings.listener = listener
        settings.setFixedPosition(position)
        return AdmobRecyclerAdapter(settings, originalAdapter, activity!!)
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(activity: Activity): String {
        val android_id = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return md5(android_id).uppercase(Locale.getDefault())
    }

    private fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest
                .getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) {
                var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
        }
        return ""
    }


    internal enum class LoadAdsStatus {
        LOADING, FAIL, SUCCESS, SHOWING
    }

    companion object {
        private const val TAG = "AperoAdmob"
        @JvmStatic
        var instance: Admob? = null
            get() {
                if (field == null) {
                    field = Admob()
                    field!!.isShowLoadingSplash = false
                }
                return field
            }
            private set

        //    private AppOpenAd appOpenAd = null;
        const val BANNER_INLINE_SMALL_STYLE = "BANNER_INLINE_SMALL_STYLE"
        const val BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE"
        const val SPLASH_ADS = 0
        const val RESUME_ADS = 1
        private const val BANNER_ADS = 2
        private const val INTERS_ADS = 3
        private const val REWARD_ADS = 4
        private const val NATIVE_ADS = 5
    }
}