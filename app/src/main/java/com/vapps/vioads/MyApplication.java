package com.vapps.vioads;

import com.vapps.module_ads.control.admob.Admob;
import com.vapps.module_ads.control.admob.AdsMultiDexApplication;
import com.vapps.module_ads.control.admob.AperoAd;
import com.vapps.module_ads.control.billing.AppPurchase;
import com.vapps.module_ads.control.config.AdjustConfig;
import com.vapps.module_ads.control.config.AperoAdConfig;
import com.vapps.module_ads.control.model.PurchaseItem;

import java.util.ArrayList;
import java.util.List;


public class MyApplication extends AdsMultiDexApplication {

    private final String APPSFLYER_TOKEN = "2PUNpdyDTkedZTgeKkWCyB";
    private final String ADJUST_TOKEN = "cc4jvudppczk";
    private final String EVENT_PURCHASE_ADJUST = "gzel1k";
    private final String EVENT_AD_IMPRESSION_ADJUST = "gzel1k";
    private final String TAG = "MainApplication";

    private static MyApplication context;

    public static MyApplication getApplication() {
        return context;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        initBilling();
        initAds();
    }

    private void initAds() {
        String environment = AperoAdConfig.ENVIRONMENT_DEVELOP;
        aperoAdConfig = new AperoAdConfig(this, AperoAdConfig.PROVIDER_ADMOB, environment);

        // Optional: setup Adjust event
        AdjustConfig adjustConfig = new AdjustConfig(ADJUST_TOKEN);
        adjustConfig.setEventAdImpression(EVENT_AD_IMPRESSION_ADJUST);
        adjustConfig.setEventNamePurchase(EVENT_PURCHASE_ADJUST);
        aperoAdConfig.setAdjustConfig(adjustConfig);

        // Optional: setup Appsflyer event
//        AppsflyerConfig appsflyerConfig = new AppsflyerConfig(true,APPSFLYER_TOKEN);
//        aperoAdConfig.setAppsflyerConfig(appsflyerConfig);

        // Optional: enable ads resume
        aperoAdConfig.setIdAdResume("dksjad");
        aperoAdConfig.setNumberOfTimesReloadAds(3);

        // Optional: setup list device test - recommended to use
        listTestDevice.add("EC25F576DA9B6CE74778B268CB87E431");
        aperoAdConfig.setListDeviceTest((ArrayList<String>) listTestDevice);
        aperoAdConfig.setIntervalInterstitialAd(0);

        AperoAd.Companion.getInstance().init(this, aperoAdConfig, false);

        // Auto disable ad resume after user click ads and back to app
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        // If true -> onNextAction() is called right after Ad Interstitial showed
    }

    private void initBilling() {
        List<PurchaseItem> listPurchaseItem = new ArrayList<>();
        listPurchaseItem.add(new PurchaseItem("MainActivity.PRODUCT_ID", AppPurchase.TYPE_IAP.PURCHASE));
        AppPurchase.getInstance().initBilling(this, listPurchaseItem);
    }
}
