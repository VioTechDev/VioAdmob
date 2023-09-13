package com.vapps.module_ads.model

class PurchaseItem {
    var itemId: String
    var trialId: String? = null
    var type: Int

    constructor(itemId: String, type: Int) {
        this.itemId = itemId
        this.type = type
    }

    constructor(itemId: String, trialId: String?, type: Int) {
        this.itemId = itemId
        this.trialId = trialId
        this.type = type
    }
}