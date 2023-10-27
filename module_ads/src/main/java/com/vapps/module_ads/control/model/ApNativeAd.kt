package com.vapps.module_ads.control.model

import android.view.View
import com.google.android.gms.ads.nativead.NativeAd
import com.vapps.module_ads.control.model.ApAdBase
import com.vapps.module_ads.control.utils.StatusAd

class ApNativeAd : ApAdBase {
    var layoutCustomNative = 0
    var nativeView: View? = null
    private var admobNativeAd: NativeAd? = null

    constructor(status: StatusAd?) : super(status!!)
    constructor(layoutCustomNative: Int, nativeView: View?) {
        this.layoutCustomNative = layoutCustomNative
        this.nativeView = nativeView
        status = StatusAd.AD_LOADED
    }

    constructor(layoutCustomNative: Int, admobNativeAd: NativeAd?) {
        this.layoutCustomNative = layoutCustomNative
        this.admobNativeAd = admobNativeAd
        status = StatusAd.AD_LOADED
    }

    fun getAdmobNativeAd(): NativeAd? {
        return admobNativeAd
    }

    fun setAdmobNativeAd(admobNativeAd: NativeAd?) {
        this.admobNativeAd = admobNativeAd
        if (admobNativeAd != null) status = StatusAd.AD_LOADED
    }

    constructor()

    override val isReady: Boolean
        get() = nativeView != null || admobNativeAd != null

    override fun toString(): String {
        return "Status:$status == nativeView:$nativeView == admobNativeAd:$admobNativeAd"
    }
}