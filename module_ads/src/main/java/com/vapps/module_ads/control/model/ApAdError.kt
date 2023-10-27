package com.vapps.module_ads.control.model

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

class ApAdError {
    private var loadAdError: LoadAdError? = null
    private var adError: AdError? = null
    private var message = ""

    constructor(adError: AdError?) {
        this.adError = adError
    }

    constructor(loadAdError: LoadAdError?) {
        this.loadAdError = loadAdError
    }

    constructor(message: String) {
        this.message = message
    }

    fun setMessage(message: String) {
        this.message = message
    }

    fun getMessage(): String {
        if (loadAdError != null) return loadAdError!!.message
        if (adError != null) return adError!!.message
        return if (!message.isEmpty()) message else "unknown error"
    }
}