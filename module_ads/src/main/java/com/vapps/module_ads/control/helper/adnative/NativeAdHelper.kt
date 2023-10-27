package com.vapps.module_ads.control.helper.adnative

import android.app.Activity
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

import com.vapps.module_ads.control.helper.AdsHelper
import com.facebook.shimmer.ShimmerFrameLayout
import com.vapps.module_ads.control.helper.adnative.params.AdNativeState
import com.vapps.module_ads.control.helper.adnative.params.NativeAdParam
import com.vapps.module_ads.control.admob.AperoAd
import com.vapps.module_ads.control.listener.AperoAdCallback
import com.vapps.module_ads.control.model.ApAdError
import com.vapps.module_ads.control.model.ApInterstitialAd
import com.vapps.module_ads.control.model.ApNativeAd
import com.vapps.module_ads.control.model.ApRewardItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by KO Huyn on 09/10/2023.
 */
class NativeAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: NativeAdConfig
) : AdsHelper<NativeAdConfig, NativeAdParam>(activity, lifecycleOwner, config) {
    private val adNativeState: MutableStateFlow<AdNativeState> =
        MutableStateFlow(if (canRequestAds()) AdNativeState.None else AdNativeState.Fail)
    private val resumeCount: AtomicInteger = AtomicInteger(0)
    private val listAdCallback: CopyOnWriteArrayList<AperoAdCallback> = CopyOnWriteArrayList()
    private var flagEnableReload = config.canReloadAds
    private var shimmerLayoutView: ShimmerFrameLayout? = null
    private var nativeContentView: FrameLayout? = null
    var nativeAd: ApNativeAd? = null
        private set

    init {
        registerAdListener(getDefaultCallback())
        lifecycleEventState.onEach {
            if (it == Lifecycle.Event.ON_CREATE) {
                if (!canRequestAds()) {
                    nativeContentView?.isVisible = false
                    shimmerLayoutView?.isVisible = false
                }
            }
            if (it == Lifecycle.Event.ON_RESUME) {
                if (!canShowAds() && isActiveState()) {
                    cancel()
                }
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //Request when resume
        lifecycleEventState.debounce(300).onEach { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount.incrementAndGet()
                logZ("Resume repeat ${resumeCount.get()} times")
            }
            if (event == Lifecycle.Event.ON_RESUME && resumeCount.get() > 1 && nativeAd != null && canRequestAds() && canReloadAd() && isActiveState()) {
                requestAds(NativeAdParam.Request)
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //for action resume or init
        adNativeState
            .onEach { logZ("adNativeState(${it::class.java.simpleName})") }
            .launchIn(lifecycleOwner.lifecycleScope)
        adNativeState.onEach { adsParam ->
            handleShowAds(adsParam)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }

    fun setShimmerLayoutView(shimmerLayoutView: ShimmerFrameLayout) = apply {
        kotlin.runCatching {
            this.shimmerLayoutView = shimmerLayoutView
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    shimmerLayoutView.isVisible = false
                }
            }
        }
    }

    fun setNativeContentView(nativeContentView: FrameLayout) = apply {
        kotlin.runCatching {
            this.nativeContentView = nativeContentView
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    nativeContentView.isVisible = false
                }
            }
        }
    }

    @Deprecated("replace with flagEnableReload")
    fun setEnableReload(isEnable: Boolean) {
        flagEnableReload = isEnable
    }

    private fun handleShowAds(adsParam: AdNativeState) {
        nativeContentView?.isGone = adsParam is AdNativeState.Cancel || !canShowAds()
        shimmerLayoutView?.isVisible = adsParam is AdNativeState.Loading
        when (adsParam) {
            is AdNativeState.Loaded -> {
                if (nativeContentView != null && shimmerLayoutView != null) {
                    AperoAd.instance?.populateNativeAdView(
                        activity,
                        adsParam.adNative,
                        nativeContentView!!,
                        shimmerLayoutView
                    )
                }
            }

            else -> Unit
        }
    }

    @Deprecated("Using cancel()")
    fun resetState() {
        logZ("resetState()")
        cancel()
    }

    fun getAdNativeState(): Flow<AdNativeState> {
        return adNativeState.asStateFlow()
    }

    private suspend fun createNativeAds(activity: Activity) {
        if (canRequestAds()) {
            AperoAd.instance?.loadNativeAdResultCallback(
                activity,
                config.idAds,
                config.layoutId,
                invokeListenerAdCallback()
            )
        }
    }


    private fun getDefaultCallback(): AperoAdCallback {
        return object : AperoAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (isActiveState()) {
                    this@NativeAdHelper.nativeAd = nativeAd
                    lifecycleOwner.lifecycleScope.launch {
                        adNativeState.emit(AdNativeState.Loaded(nativeAd))
                    }
                    logZ("onNativeAdLoaded")
                } else {
                    logInterruptExecute("onNativeAdLoaded")
                }
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                if (isActiveState()) {
                    if (nativeAd == null) {
                        lifecycleOwner.lifecycleScope.launch {
                            adNativeState.emit(AdNativeState.Fail)
                        }
                    }
                    logZ("onAdFailedToLoad")
                } else {
                    logInterruptExecute("onAdFailedToLoad")
                }
            }
        }
    }

    override fun requestAds(param: NativeAdParam) {
        lifecycleOwner.lifecycleScope.launch {
            if (canRequestAds()) {
                logZ("requestAds($param)")
                when (param) {
                    is NativeAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        if (nativeAd == null) {
                            adNativeState.emit(AdNativeState.Loading)
                        }
                        createNativeAds(activity)
                    }

                    is NativeAdParam.Ready -> {
                        flagActive.compareAndSet(false, true)
                        nativeAd = param.nativeAd
                        adNativeState.emit(AdNativeState.Loaded(param.nativeAd))
                    }
                }
            } else {
                if (!isOnline() && nativeAd == null) {
                    cancel()
                }
            }
        }
    }

    override fun cancel() {
        logZ("cancel() called")
        flagActive.compareAndSet(true, false)
        lifecycleOwner.lifecycleScope.launch {
            adNativeState.emit(AdNativeState.Cancel)
        }
    }

    fun registerAdListener(adCallback: AperoAdCallback) {
        this.listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: AperoAdCallback) {
        this.listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        this.listAdCallback.clear()
    }

    private fun invokeAdListener(action: (adCallback: AperoAdCallback) -> Unit) {
        listAdCallback.forEach(action)
    }

    private fun invokeListenerAdCallback(): AperoAdCallback {
        return object : AperoAdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                invokeAdListener { it.onNextAction() }
            }

            override fun onAdClosed() {
                super.onAdClosed()
                invokeAdListener { it.onAdClosed() }
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                invokeAdListener { it.onAdFailedToLoad(adError) }
            }

            override fun onAdFailedToShow(adError: ApAdError?) {
                super.onAdFailedToShow(adError)
                invokeAdListener { it.onAdFailedToShow(adError) }
            }

            override fun onAdLeftApplication() {
                super.onAdLeftApplication()
                invokeAdListener { it.onAdLeftApplication() }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                invokeAdListener { it.onAdLoaded() }
            }

            override fun onAdSplashReady() {
                super.onAdSplashReady()
                invokeAdListener { it.onAdSplashReady() }
            }

            override fun onInterstitialLoad(interstitialAd: ApInterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                invokeAdListener { it.onInterstitialLoad(interstitialAd) }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                super.onAdImpression()
                invokeAdListener { it.onAdImpression() }
            }

            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                invokeAdListener { it.onNativeAdLoaded(nativeAd) }
            }

            override fun onUserEarnedReward(rewardItem: ApRewardItem) {
                super.onUserEarnedReward(rewardItem)
                invokeAdListener { it.onUserEarnedReward(rewardItem) }
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                invokeAdListener { it.onInterstitialShow() }
            }
        }
    }
}
