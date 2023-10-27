package com.vapps.module_ads.control.model

import com.vapps.module_ads.control.utils.StatusAd

/**
 * Created by lamlt on 12/08/2022.
 */
abstract class ApAdBase {
    @JvmField
    var status = StatusAd.AD_INIT

    constructor(status: StatusAd) {
        this.status = status
    }

    constructor()

    abstract val isReady: Boolean
    val isNotReady: Boolean
        get() = !isReady
    val isLoading: Boolean
        get() = status === StatusAd.AD_LOADING
    val isLoadFail: Boolean
        get() = status === StatusAd.AD_LOAD_FAIL
}