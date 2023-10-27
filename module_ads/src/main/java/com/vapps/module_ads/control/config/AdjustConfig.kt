package com.vapps.module_ads.control.config

/**
 * Created by lamlt on 05/12/2022.
 */
class AdjustConfig(adjustToken: String) {
    /**
     * adjustToken enable adjust and setup adjust token
     */
    var adjustToken = ""

    /**
     * eventNamePurchase push event to adjust when user purchased
     */
    var eventNamePurchase = ""

    /**
     * eventNamePurchase push event to adjust when ad impression
     */
    var eventAdImpression = ""

    init {
        this.adjustToken = adjustToken
    }
}