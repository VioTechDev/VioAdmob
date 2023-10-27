package com.vapps.vioads

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vapps.module_ads.control.helper.adnative.NativeAdConfig
import com.vapps.module_ads.control.helper.adnative.NativeAdHelper
import com.vapps.module_ads.control.helper.adnative.params.NativeAdParam
import com.vapps.module_ads.control.helper.banner.BannerAdConfig
import com.vapps.module_ads.control.helper.banner.BannerAdHelper
import com.vapps.module_ads.control.helper.banner.params.BannerAdParam
import com.vapps.vioads.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val bannerAdHelper by lazy { initBannerAd() }

    private val nativeAdHelper by lazy { initNativeAd() }
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bannerAdHelper.setBannerContentView(binding.frAds)
            .apply { setTagForDebug("BANNER=>>>") }
        nativeAdHelper
            .setNativeContentView(binding.flNativeAds)
            .setShimmerLayoutView(binding.includeShimmer.shimmerContainerNative)
        nativeAdHelper.requestAds(NativeAdParam.Request)
        bannerAdHelper.requestAds(BannerAdParam.Request)
        binding.btnReload.setOnClickListener {
            bannerAdHelper.requestAds(BannerAdParam.Request)
            nativeAdHelper.requestAds(NativeAdParam.Request)
        }
        binding.btnShowDialog.setOnClickListener {
            bannerAdHelper.flagUserEnableReload = !bannerAdHelper.flagUserEnableReload
            nativeAdHelper.flagUserEnableReload = !nativeAdHelper.flagUserEnableReload
        }
    }

    private fun initBannerAd(): BannerAdHelper {
        val config = BannerAdConfig(
            idAds = "ca-app-pub-3940256099942544/6300978111",
            canShowAds = true,
            canReloadAds = true,
        )
        return BannerAdHelper(activity = this, lifecycleOwner = this, config = config)
    }

    private fun initNativeAd(): NativeAdHelper {
        val config = NativeAdConfig(
            idAds = "ca-app-pub-3940256099942544/2247696110",
            canShowAds = true,
            canReloadAds = true,
            layoutId = R.layout.custom_native_admod_medium
        )
        return NativeAdHelper(this, this, config)
    }
}