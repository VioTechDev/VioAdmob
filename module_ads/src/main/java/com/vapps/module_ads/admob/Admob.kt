package com.vapps.module_ads.admob

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.applovin.mediation.AppLovinExtras
import com.applovin.mediation.ApplovinAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jirbo.adcolony.AdColonyAdapter
import com.jirbo.adcolony.AdColonyBundleBuilder
import com.vapps.module_ads.R
import com.vapps.module_ads.config.VioAdsConfig
import com.vapps.module_ads.dialog.LoadingDialog
import com.vapps.module_ads.event.VioLogEventManager
import com.vapps.module_ads.listener.AdmobAdsCallback
import com.vapps.module_ads.purchase.AppPurchase
import com.vapps.module_ads.utils.AdType
import com.vapps.module_ads.utils.AdmobHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Admob {
    private val TAG = Admob::class.simpleName
    private var mInterstitialSplash: InterstitialAd? = null
    private var isShowLoadingSplash = false
    @SuppressLint("StaticFieldLeak")
    private lateinit var loadingDialog: LoadingDialog
    private const val isAdcolony = false
    private const val isAppLovin = false
    private var jobDelayShowAds : Job? = null
    private var jobTimeOutRequest : Job? = null
    private var startSplashRequestTime = 0L
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap: Map<String, AdapterStatus> =
                initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status: AdapterStatus? = statusMap[adapterClass]
                Log.d(
                    TAG, java.lang.String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
        }
    }

    private fun getAdRequest(): AdRequest {
        val builder = AdRequest.Builder()
        if (isAdcolony) {
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

    private fun interstitialSplashIsReady(): Boolean {
        return mInterstitialSplash != null
    }

    fun requestLoadInterstitialSplash(
        activity: AppCompatActivity,
        interstitialSplashId: String,
        timeOut: Long = 30000,
        timeDelay: Long = 5000,
        showIfReady: Boolean = false,
        admobAdsCallback: AdmobAdsCallback
    ) {
        if (AppPurchase.isPurchased()) {
            runBlocking {
                delay(timeDelay)
                admobAdsCallback.onNextAction()
            }
            return
        }
        startSplashRequestTime = System.currentTimeMillis()
        getInterstitialAds(activity, interstitialSplashId, object : AdmobAdsCallback {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                interstitialAd.let {
                    mInterstitialSplash = interstitialAd
                    mInterstitialSplash?.onPaidEventListener =
                        OnPaidEventListener { adValue: AdValue ->
                            Log.d(TAG, "OnPaidEvent splash:" + adValue.valueMicros)
                            mInterstitialSplash!!.responseInfo
                                .mediationAdapterClassName?.let {
                                    mInterstitialSplash?.adUnitId?.let { it1 ->
                                        VioLogEventManager.logPaidAdImpression(
                                            activity,
                                            adValue,
                                            it1,
                                            it, AdType.INTERSTITIAL
                                        )
                                    }
                                }
                        }
                    if (showIfReady) {
                        val endRequestTime = System.currentTimeMillis()
                        val requestTime = endRequestTime - startSplashRequestTime
                        val showDelayTime = if ((requestTime) < timeDelay) {
                            timeDelay - requestTime
                        } else {
                            0
                        }
                        jobDelayShowAds = activity.lifecycle.coroutineScope.launch {
                            delay(showDelayTime)
                            jobTimeOutRequest?.cancelAndJoin()
                            Log.e(TAG, "onInterstitialLoad: ", )
                            forceShowInterstitialSplash(
                                activity,
                                admobAdsCallback
                            )
                        }
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplash = false
                admobAdsCallback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                isShowLoadingSplash = false
                admobAdsCallback.onAdFailedToShow(adError)
            }
        })

        jobTimeOutRequest = activity.lifecycle.coroutineScope.launch {
            delay(timeOut)
            jobDelayShowAds?.cancelAndJoin()
            Log.e(TAG, "timeOut12: ")
            if (interstitialSplashIsReady()) {
                Log.e(TAG, "onShowSplash: 2")
                forceShowInterstitialSplash(activity, admobAdsCallback)
            } else {
                Log.e(TAG, "timeOut: ")
                admobAdsCallback.onNextAction()
            }
        }
    }

    suspend fun forceShowInterstitialSplash(
        activity: AppCompatActivity,
        admobAdsCallback: AdmobAdsCallback
    ) {
        if (AppPurchase.isPurchased() || !interstitialSplashIsReady()) {
            admobAdsCallback.onNextAction()
            return
        }
        mInterstitialSplash?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()

            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
            }

            override fun onAdClicked() {
                super.onAdClicked()
            }

            override fun onAdImpression() {
                super.onAdImpression()
            }
        }
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            showDialogLoadingAds(context = activity)
            coroutineScope {
                delay(800)
                hideDialogLoadingAds()
                mInterstitialSplash?.show(activity)
            }
        }
    }

    private fun showDialogLoadingAds(context: Context) {
        if (!this::loadingDialog.isInitialized) {
            loadingDialog = LoadingDialog(context)
        }
        if (loadingDialog.isShowing)
            return
        loadingDialog.show()
    }

    private fun hideDialogLoadingAds(){
        if (this::loadingDialog.isInitialized){
            loadingDialog.dismiss()
        }
    }

    /**
     * Trả về 1 InterstitialAd và request Ads
     *
     * @param context
     * @param id
     * @return
     */
    private fun getInterstitialAds(
        context: Context,
        id: String,
        admobAdsCallback: AdmobAdsCallback?
    ) {
        if (listOf<String>(*context.resources.getStringArray(R.array.list_id_test))
                .contains(id)
        ) {
            showTestIdAlert(context, AdFormat.INTERSTITIAL, id)
        }
        if (AppPurchase.isPurchased() || AdmobHelper.getNumClickAdsPerDay(
                context,
                id
            ) >= VioAdsConfig.maxClickAds
        ) {
            Log.e(TAG, "getInterstitialAds: ")
            admobAdsCallback!!.onInterstitialLoad(null)
            return
        }
        InterstitialAd.load(context, id, getAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    admobAdsCallback?.onInterstitialLoad(interstitialAd)

                    //tracking adjust
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        Log.d(
                            TAG,
                            "OnPaidEvent getInterstitalAds:" + adValue.valueMicros
                        )
                        interstitialAd.responseInfo
                            .mediationAdapterClassName?.let {
                                VioLogEventManager.logPaidAdImpression(
                                    context,
                                    adValue,
                                    interstitialAd.adUnitId,
                                    it, AdType.INTERSTITIAL
                                )
                            }
                    }
                    //onShowSplash(context as AppCompatActivity?, null)
                    Log.i(TAG, "InterstitialAds onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.i(TAG, loadAdError.message)
                    admobAdsCallback?.onAdFailedToLoad(loadAdError)
                }
            })
    }

    private fun showTestIdAlert(context: Context, typeAds: AdFormat, id: String) {
        var content: String = when (typeAds) {
            AdFormat.BANNER -> "Banner Ads: "
            AdFormat.INTERSTITIAL -> "Interstitial Ads: "
            AdFormat.REWARDED -> "Rewarded Ads: "
            AdFormat.NATIVE -> "Native Ads: "
            else -> {
                "Ads: "
            }
        }
        content += id
        val notification: Notification = NotificationCompat.Builder(context, "warning_ads")
            .setContentTitle("Found test ad id")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_warning_ads)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "warning_ads",
                "Warning Ads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(typeAds.ordinal, notification)
        Log.e(TAG, "Found test ad id on debug : " + VioAdsConfig.VARIANT_DEV)
        if (!VioAdsConfig.VARIANT_DEV) {
            Log.e(
                TAG,
                "Found test ad id on environment production. use test id only for develop environment "
            )
            throw RuntimeException("Found test ad id on environment production. Id found: $id")
        }
    }
}

enum class AdFormat {
    APP_OPEN, BANNER, INTERSTITIAL, REWARDED, NATIVE
}