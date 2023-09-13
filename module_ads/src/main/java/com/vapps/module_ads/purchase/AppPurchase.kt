package com.vapps.module_ads.purchase

import android.app.Application
import android.os.Handler
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.vapps.module_ads.config.VioAdsConfig
import com.vapps.module_ads.event.VioLogEventManager
import com.vapps.module_ads.listener.BillingListener
import com.vapps.module_ads.listener.PurchaseListener
import com.vapps.module_ads.model.PurchaseItem
import com.vapps.module_ads.model.PurchaseResult

object AppPurchase {
    private val TAG = AppPurchase::class.simpleName
    private var purchaseItems: MutableList<PurchaseItem> = mutableListOf()
    private lateinit var billingClient: BillingClient
    private var listSubscriptionId: MutableList<QueryProductDetailsParams.Product> = mutableListOf()
    private var listINAPId: MutableList<QueryProductDetailsParams.Product> = mutableListOf()

    private val skuDetailsINAPMap: Map<String, ProductDetails> = mutableMapOf()
    private val skuDetailsSubsMap: Map<String, ProductDetails> = HashMap()
    private val idPurchaseCurrent by lazy { "" }
    private val typeIap by lazy { TYPE_IAP.PURCHASE }
    private val purchaseListener: PurchaseListener? = null
    private var isAvailable = false
    private var isInitBillingFinish = false
    private var skuListINAPFromStore: List<ProductDetails> = arrayListOf()
    private var skuListSubsFromStore: List<ProductDetails> = arrayListOf()
    private var isListGot = false
    private var verifyFinish = false
    private var isPurchase = false
    private val ownerIdInApps: MutableList<String> = mutableListOf()
    private var isVerifyINAP = false
    private var isVerifySUBS = false
    private var billingListener: BillingListener? = null
    private val handlerTimeout: Handler? = null
    private val rdTimeout: Runnable? = null

    private val ownerIdSubs: MutableList<PurchaseResult> = mutableListOf()

    private val ownerIdInapps: List<String> = mutableListOf()
    private const val isConsumePurchase = false


    private var purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, list ->
            Log.e(TAG, "onPurchasesUpdated code: " + billingResult.responseCode)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                for (purchase in list) {
                    val sku: List<String> = purchase.skus
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                purchaseListener?.onUserCancelBilling()
                Log.d(TAG, "onPurchasesUpdated:USER_CANCELED ")
            } else {
                Log.d(TAG, "onPurchasesUpdated:... ")
            }
        }

    private var purchaseClientStateListener: BillingClientStateListener =
        object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                isAvailable = false
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished:  " + billingResult.responseCode)
                if (!isInitBillingFinish) {
                    verifyPurchased(true)
                }
                isInitBillingFinish = true
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isAvailable = true
                    // check product detail INAP
                    if (listINAPId.size > 0) {
                        val paramsINAP = QueryProductDetailsParams.newBuilder()
                            .setProductList(listINAPId)
                            .build()
                        billingClient.queryProductDetailsAsync(
                            paramsINAP
                        ) { _, productDetailsList ->
                            Log.d(
                                TAG,
                                "onSkuINAPDetailsResponse: " + productDetailsList.size
                            )
                            skuListINAPFromStore = productDetailsList
                            isListGot = true
                            addSkuINAPToMap(productDetailsList)
                        }
                    }
                    // check product detail SUBS
                    if (listSubscriptionId.size > 0) {
                        val paramsSUBS = QueryProductDetailsParams.newBuilder()
                            .setProductList(listSubscriptionId)
                            .build()
                        for (item in listSubscriptionId) {
                            Log.d(TAG, "onBillingSetupFinished: " + item.zza())
                        }
                        billingClient.queryProductDetailsAsync(
                            paramsSUBS
                        ) { _, productDetailsList ->
                            Log.d(
                                TAG,
                                "onSkuSubsDetailsResponse: " + productDetailsList.size
                            )
                            skuListSubsFromStore = productDetailsList
                            isListGot = true
                            addSkuSubsToMap(productDetailsList)
                        }
                    } else {
                        Log.d(TAG, "onBillingSetupFinished: listSubscriptionId empty")
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
                    || billingResult.responseCode == BillingClient.BillingResponseCode.ERROR
                ) {
                    Log.e(TAG, "onBillingSetupFinished:ERROR ")
                }
            }
        }


    fun initBilling(application: Application?, purchaseItemList: MutableList<PurchaseItem>) {
        if (VioAdsConfig.VARIANT_DEV) {
            // auto add purchase test when dev
            purchaseItemList.add(
                PurchaseItem(
                    PurchaseConfig.PRODUCT_ID_TEST,
                    "",
                    TYPE_IAP.PURCHASE
                )
            )
        }
        this.purchaseItems = purchaseItemList
        syncPurchaseItemsToListProduct(this.purchaseItems)
        billingClient = BillingClient.newBuilder(application!!)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(purchaseClientStateListener)
    }

    fun isPurchased(): Boolean {
        return isPurchase
    }


    /**
     * Listener init billing app
     * When init available auto call onInitBillingFinish with resultCode = 0
     *
     * @param billingListener
     */
    fun setBillingListener(billingListener: BillingListener) {
        this.billingListener = billingListener
        if (isAvailable) {
            billingListener.onInitBillingFinished(0)
            isInitBillingFinish = true
        }
    }

    private fun syncPurchaseItemsToListProduct(purchaseItems: List<PurchaseItem>) {
        val listInAppProduct = ArrayList<QueryProductDetailsParams.Product>()
        val listSubsProduct = ArrayList<QueryProductDetailsParams.Product>()
        for (item in purchaseItems) {
            var product: QueryProductDetailsParams.Product
            if (item.type == TYPE_IAP.PURCHASE) {
                product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(item.itemId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
                listInAppProduct.add(product)
            } else {
                product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(item.itemId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                listSubsProduct.add(product)
            }
        }
        this.listINAPId = listInAppProduct
        Log.d(TAG, "syncPurchaseItemsToListProduct: listINAPId " + this.listINAPId.size)
        this.listSubscriptionId = listSubsProduct
        Log.d(
            TAG,
            "syncPurchaseItemsToListProduct: listSubscriptionId " + this.listSubscriptionId.size
        )
    }

    fun verifyPurchased(isCallback: Boolean) {
        Log.d(TAG, "isPurchased : " + listSubscriptionId.size)
        verifyFinish = false
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult: BillingResult, list: List<Purchase> ->
            Log.d(
                TAG,
                "verifyPurchased INAPP  code:" + billingResult.responseCode + " ===   size:" + list.size
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    for (id in listINAPId) {
                        if (purchase.products.contains(id.zza())) {
                            Log.d(TAG, "verifyPurchased INAPP: true")
                            ownerIdInApps.add(id.zza())
                            isPurchase = true
                        }
                    }
                }
                isVerifyINAP = true
                if (isVerifySUBS) {
                    if (billingListener != null && isCallback) {
                        billingListener!!.onInitBillingFinished(billingResult.responseCode)
                        if (handlerTimeout != null && rdTimeout != null) {
                            handlerTimeout.removeCallbacks(rdTimeout)
                        }
                    }
                    verifyFinish = true
                }
            } else {
                isVerifyINAP = true
                if (isVerifySUBS) {
                    // chưa mua subs và IAP
                    billingListener?.onInitBillingFinished(billingResult.responseCode)
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout)
                    }
                    verifyFinish = true
                }
            }
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult: BillingResult, list: List<Purchase> ->
            Log.d(
                TAG,
                "verifyPurchased SUBS  code:" + billingResult.responseCode + " ===   size:" + list.size
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    for (id in listSubscriptionId) {
                        if (purchase.products.contains(id.zza())) {
                            val purchaseResult = purchase.orderId?.let {
                                PurchaseResult(
                                    it,
                                    purchase.packageName,
                                    purchase.products,
                                    purchase.purchaseTime,
                                    purchase.purchaseState,
                                    purchase.purchaseToken,
                                    purchase.quantity,
                                    purchase.isAutoRenewing,
                                    purchase.isAcknowledged
                                )
                            }
                            if (purchaseResult != null) {
                                addOrUpdateOwnerIdSub(purchaseResult, id.zza())
                            }
                            Log.d(TAG, "verifyPurchased SUBS: true")
                            isPurchase = true
                        }
                    }
                }
                isVerifySUBS = true
                if (isVerifyINAP) {
                    if (billingListener != null && isCallback) {
                        billingListener?.onInitBillingFinished(billingResult.responseCode)
                        if (handlerTimeout != null && rdTimeout != null) {
                            handlerTimeout.removeCallbacks(rdTimeout)
                        }
                    }
                    verifyFinish = true
                }
            } else {
                isVerifySUBS = true
                if (isVerifyINAP) {
                    // chưa mua subs và IAP
                    if (billingListener != null && isCallback) {
                        billingListener?.onInitBillingFinished(billingResult.responseCode)
                        if (handlerTimeout != null && rdTimeout != null) {
                            handlerTimeout.removeCallbacks(rdTimeout)
                        }
                        verifyFinish = true
                    }
                }
            }
        }
    }

    private fun addOrUpdateOwnerIdSub(purchaseResult: PurchaseResult, id: String) {
        var isExistId = false
        for (p in ownerIdSubs) {
            if (p.productId.contains(id)) {
                isExistId = true
                ownerIdSubs.remove(p)
                ownerIdSubs.add(purchaseResult)
                break
            }
        }
        if (!isExistId) {
            ownerIdSubs.add(purchaseResult)
        }
    }


    private fun handlePurchase(purchase: Purchase) {
        //tracking adjust
        val price: Double = getPriceWithoutCurrency(idPurchaseCurrent, typeIap)
        val currency: String = getCurrency(idPurchaseCurrent, typeIap)
        VioLogEventManager.onTrackRevenuePurchase(
            price.toFloat(),
            currency,
            idPurchaseCurrent,
            typeIap
        )
        if (purchaseListener != null) {
            isPurchase = true
            purchaseListener.onProductPurchased(purchase.orderId, purchase.originalJson)
        }
        if (isConsumePurchase) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val listener =
                ConsumeResponseListener { billingResult, _ ->
                    Log.d(
                        TAG,
                        "onConsumeResponse: " + billingResult.debugMessage
                    )
                }
            billingClient.consumeAsync(consumeParams, listener)
        } else {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                if (!purchase.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        acknowledgePurchaseParams
                    ) { billingResult ->
                        Log.d(
                            TAG,
                            "onAcknowledgePurchaseResponse: " + billingResult.debugMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Get Price Amount Micros subs or IAP
     * Get final price with id
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    private fun getPriceWithoutCurrency(productId: String?, typeIAP: Int): Double {
        val skuDetails: ProductDetails =
            (if (typeIAP == TYPE_IAP.PURCHASE)
                skuDetailsINAPMap[productId]
            else skuDetailsSubsMap[productId])
                ?: return 0.0
        return if (typeIAP == TYPE_IAP.PURCHASE) skuDetails.oneTimePurchaseOfferDetails!!.priceAmountMicros
            .toDouble() else {
            val subsDetail = skuDetails.subscriptionOfferDetails
            val pricingPhaseList = subsDetail!![subsDetail.size - 1].pricingPhases.pricingPhaseList
            pricingPhaseList[pricingPhaseList.size - 1].priceAmountMicros.toDouble()
        }
    }

    /**
     * Get Currency subs or IAP by country
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    private fun getCurrency(productId: String?, typeIAP: Int): String {
        val skuDetails =
            (if (typeIAP == TYPE_IAP.PURCHASE) skuDetailsINAPMap[productId] else skuDetailsSubsMap[productId])
                ?: return ""
        return if (typeIAP == TYPE_IAP.PURCHASE) skuDetails.oneTimePurchaseOfferDetails!!.priceCurrencyCode else {
            val subsDetail = skuDetails.subscriptionOfferDetails
            val pricingPhaseList = subsDetail!![subsDetail.size - 1].pricingPhases.pricingPhaseList
            pricingPhaseList[pricingPhaseList.size - 1].priceCurrencyCode
        }
    }

    private fun addSkuINAPToMap(skuList: List<ProductDetails>) {
        for (skuDetails in skuList) {
            skuDetailsINAPMap.plus(Pair(skuDetails.productId, skuDetails))
        }
    }

    private fun addSkuSubsToMap(skuList: List<ProductDetails>) {
        for (skuDetails in skuList) {
            skuDetailsSubsMap.plus(Pair(skuDetails.productId, skuDetails))
        }
    }
}