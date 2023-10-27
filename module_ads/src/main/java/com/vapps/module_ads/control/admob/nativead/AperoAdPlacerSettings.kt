package com.vapps.module_ads.control.admob.nativead

class AperoAdPlacerSettings {
    var adUnitId: String? = ""
    var positionFixAd = -1
        private set
    var isRepeatingAd = false
        private set
    var layoutCustomAd = -1
    @JvmField
    var layoutAdPlaceHolder = -1
    var listener: AperoAdPlacer.Listener? = null

    constructor(adUnitId: String, layoutCustomAd: Int, layoutPlaceHolderAd: Int) {
        this.adUnitId = adUnitId
        this.layoutCustomAd = layoutCustomAd
        layoutAdPlaceHolder = layoutPlaceHolderAd
    }

    constructor(layoutCustomAd: Int, layoutPlaceHolderAd: Int) {
        this.layoutCustomAd = layoutCustomAd
        layoutAdPlaceHolder = layoutPlaceHolderAd
    }

    fun setFixedPosition(positionAd: Int) {
        positionFixAd = positionAd
        isRepeatingAd = false
    }

    fun setRepeatingInterval(positionAd: Int) {
        positionFixAd = positionAd - 1
        isRepeatingAd = true
    }
}