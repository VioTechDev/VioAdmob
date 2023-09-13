package com.vapps.vioads

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.vapps.module_ads.admob.Admob
import com.vapps.module_ads.listener.AdmobAdsCallback

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("TAG", "onCreate: ")
        Admob.init(this)
        Log.e("TAG", "onCreate: load ads ")
        Admob.requestLoadInterstitialSplash(
            this@MainActivity,
            "ca-app-pub-3940256099942544/1033173712",
            5000,
            10000,
            true,
            object : AdmobAdsCallback {
                override fun onNextAction() {
                    super.onNextAction()
                    Log.e("TAG", "onNextAction: ", )
                }
            })
    }
}