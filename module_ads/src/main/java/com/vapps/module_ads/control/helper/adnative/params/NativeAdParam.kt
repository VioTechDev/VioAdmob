package com.vapps.module_ads.control.helper.adnative.params

import com.vapps.module_ads.control.helper.params.IAdsParam
import com.vapps.module_ads.control.model.ApNativeAd

sealed class NativeAdParam : IAdsParam {
    data class Ready(val nativeAd: ApNativeAd) : NativeAdParam()
    object Request : NativeAdParam() {
        @JvmStatic
        fun create(): Request {
            return this
        }
    }
}
