package com.vapps.module_ads.purchase

import androidx.annotation.IntDef

class PurchaseConfig {
    companion object {
        const val PRODUCT_ID_TEST = "android.test.purchased"
    }
}

annotation class TYPE_IAP {
    companion object {
        var PURCHASE = 1
        var SUBSCRIPTION = 2
    }
}