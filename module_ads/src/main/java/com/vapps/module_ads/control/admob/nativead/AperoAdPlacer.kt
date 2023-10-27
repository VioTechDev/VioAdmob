package com.vapps.module_ads.control.admob.nativead

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.vapps.module_ads.R
import com.vapps.module_ads.control.admob.Admob
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.module_ads.control.model.ApAdValue
import com.vapps.module_ads.control.model.ApNativeAd
import com.vapps.module_ads.control.utils.StatusAd

class AperoAdPlacer(
    private val settings: AperoAdPlacerSettings,
    private val adapterOriginal: RecyclerView.Adapter<*>,
    private val activity: Activity
) {
    var TAG = "AperoAdPlacer"
    private val listAd = HashMap<Int, ApNativeAd?>()
    private val listPositionAd: MutableList<Int> = ArrayList()
    private var countLoadAd = 0
    private var isNativeFullScreen = false

    init {
        configData()
    }

    fun setNativeFullScreen(nativeFullScreen: Boolean) {
        isNativeFullScreen = nativeFullScreen
    }

    fun configData() {
        if (settings.isRepeatingAd) {
            //calculator position add ad native to list
            var posAddAd = 0
            var countNewAdapter = adapterOriginal.itemCount
            while (posAddAd <= countNewAdapter - settings.positionFixAd) {
//                Log.i(TAG, "add native to list pos: " + posAddAd);
                posAddAd += settings.positionFixAd
                if (listAd[posAddAd] == null) {
                    listAd[posAddAd] = ApNativeAd(StatusAd.AD_INIT)
                    listPositionAd.add(posAddAd)
                }
                posAddAd++
                countNewAdapter++
            }
        } else {
            listPositionAd.add(settings.positionFixAd)
            listAd[settings.positionFixAd] = ApNativeAd(StatusAd.AD_INIT)
        }
    }

    fun renderAd(pos: Int, holder: RecyclerView.ViewHolder) {
        val adNative = listAd[pos]
        if (adNative!!.getAdmobNativeAd() == null) {
            if (listAd[pos]!!.status !== StatusAd.AD_LOADING) {
                onAdBindHolder(holder.itemView, pos)
                holder.itemView.post {
                    val nativeAd = ApNativeAd(StatusAd.AD_LOADING)
                    listAd[pos] = nativeAd
                    val adCallback: AdCallback = object : AdCallback() {
                        override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                            super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                            unifiedNativeAd.setOnPaidEventListener { adValue ->
                                onAdRevenuePaid(
                                    ApAdValue(adValue)
                                )
                            }
                            this@AperoAdPlacer.onAdLoaded(pos)
                            nativeAd.setAdmobNativeAd(unifiedNativeAd)
                            nativeAd.status = StatusAd.AD_LOADED
                            listAd[pos] = nativeAd
                            populateAdToViewHolder(holder, unifiedNativeAd, pos)
                            onAdPopulate(holder.itemView, pos)
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            val containerShimmer =
                                holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
                            containerShimmer.visibility = View.GONE
                            onAdLoadFail(holder.itemView, pos)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            this@AperoAdPlacer.onAdClicked()
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            this@AperoAdPlacer.onAdImpression()
                        }
                    }
                    if (isNativeFullScreen) {
                        Admob.instance?.loadNativeFullScreen(
                            activity, settings.adUnitId, adCallback
                        )
                    } else {
                        Admob.instance?.loadNativeAd(activity, settings.adUnitId, adCallback)
                    }
                }
            }
        } else {
            if (listAd[pos]!!.status === StatusAd.AD_LOADED) {
                populateAdToViewHolder(holder, listAd[pos]!!.getAdmobNativeAd(), pos)
            }
        }
    }

    private fun populateAdToViewHolder(
        holder: RecyclerView.ViewHolder,
        unifiedNativeAd: NativeAd?,
        pos: Int
    ) {
        val nativeAdView = LayoutInflater.from(
            activity
        )
            .inflate(settings.layoutCustomAd, null) as NativeAdView
        val adPlaceHolder = holder.itemView.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        containerShimmer.stopShimmer()
        containerShimmer.visibility = View.GONE
        adPlaceHolder.visibility = View.VISIBLE
        unifiedNativeAd?.let { Admob.instance?.populateUnifiedNativeAdView(it, nativeAdView) }
        Log.i(
            TAG,
            "native ad in recycle loaded position: " + pos + "  title: " + unifiedNativeAd!!.headline + "   count child ads:" + adPlaceHolder.childCount
        )
        adPlaceHolder.removeAllViews()
        adPlaceHolder.addView(nativeAdView)
    }

    fun loadAds() {
        countLoadAd = 0
        Admob.instance?.loadNativeAds(activity, settings.adUnitId, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val nativeAd = ApNativeAd(settings.layoutCustomAd, unifiedNativeAd)
                nativeAd.status = StatusAd.AD_LOADED
                listAd[listPositionAd[countLoadAd]] = nativeAd
                Log.i(TAG, "native ad in recycle loaded: $countLoadAd")
                countLoadAd++
            }
        }, Math.min(listAd.size, settings.positionFixAd))
    }

    fun isAdPosition(pos: Int): Boolean {
        val nativeAd = listAd[pos]
        return nativeAd != null
    }

    fun getOriginalPosition(posAdAdapter: Int): Int {
        var countAd = 0
        for (i in 0 until posAdAdapter) {
            if (listAd[i] != null) countAd++
        }
        val posOriginal = posAdAdapter - countAd
        Log.d(TAG, "getOriginalPosition: $posOriginal")
        return posOriginal
    }

    val adjustedCount: Int
        get() {
            val countMinAd: Int
            countMinAd = if (settings.isRepeatingAd) {
                adapterOriginal.itemCount / settings.positionFixAd
            } else if (adapterOriginal.itemCount >= settings.positionFixAd) {
                1
            } else {
                0
            }
            return adapterOriginal.itemCount + Math.min(countMinAd, listAd.size)
        }

    fun onAdLoaded(position: Int) {
        Log.i(TAG, "Ad native loaded in pos: $position")
        if (settings.listener != null) settings.listener?.onAdLoaded(position)
    }

    fun onAdRemoved(position: Int) {
        Log.i(TAG, "Ad native removed in pos: $position")
        if (settings.listener != null) settings.listener?.onAdRemoved(position)
    }

    fun onAdClicked() {
        Log.i(TAG, "Ad native clicked ")
        if (settings.listener != null) settings.listener?.onAdClicked()
    }

    fun onAdRevenuePaid(adValue: ApAdValue?) {
        Log.i(TAG, "Ad native revenue paid ")
        if (settings.listener != null) settings.listener?.onAdRevenuePaid(adValue)
    }

    fun onAdImpression() {
        Log.i(TAG, "Ad native impression ")
        if (settings.listener != null) settings.listener?.onAdImpression()
    }

    fun onAdLoadFail(itemView: View?, position: Int) {
        Log.i(TAG, "Ad native load fail ")
        if (settings.listener != null) settings.listener?.onAdLoadFail(itemView, position)
    }

    fun onAdPopulate(itemView: View?, position: Int) {
        Log.i(TAG, "Ad native populate ")
        if (settings.listener != null) settings.listener?.onAdPopulate(itemView, position)
    }

    fun onAdBindHolder(itemView: View?, position: Int) {
        Log.i(TAG, "Ad native bind holder ")
        if (settings.listener != null) settings.listener?.onAdBindHolder(itemView, position)
    }

    interface Listener {
        fun onAdLoaded(position: Int)
        fun onAdRemoved(position: Int)
        fun onAdClicked()
        fun onAdRevenuePaid(adValue: ApAdValue?)
        fun onAdImpression()
        fun onAdBindHolder(itemView: View?, position: Int) {}
        fun onAdLoadFail(itemView: View?, position: Int) {}
        fun onAdPopulate(itemView: View?, position: Int) {}
    }
}