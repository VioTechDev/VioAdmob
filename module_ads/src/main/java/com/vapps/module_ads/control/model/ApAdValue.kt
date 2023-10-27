package com.vapps.module_ads.control.model

import com.applovin.mediation.MaxAd
import com.google.android.gms.ads.AdValue

class ApAdValue {
    var admobAdValue: AdValue? = null
        private set
    var maxAdValue: MaxAd? = null
        private set

    constructor(maxAdValue: MaxAd?) {
        this.maxAdValue = maxAdValue
    }

    constructor(admobAdValue: AdValue?) {
        this.admobAdValue = admobAdValue
    }
}