package com.vapps.module_ads.control.admob.nativead

import androidx.recyclerview.widget.RecyclerView
import com.applovin.mediation.nativeAds.adPlacer.MaxRecyclerAdapter

class AperoAdAdapter {
    private var admobRecyclerAdapter: AdmobRecyclerAdapter? = null
    private var maxRecyclerAdapter: MaxRecyclerAdapter? = null

    constructor(admobRecyclerAdapter: AdmobRecyclerAdapter?) {
        this.admobRecyclerAdapter = admobRecyclerAdapter
    }

    constructor(maxRecyclerAdapter: MaxRecyclerAdapter?) {
        this.maxRecyclerAdapter = maxRecyclerAdapter
    }

    val adapter: RecyclerView.Adapter<*>?
        get() = if (admobRecyclerAdapter != null) admobRecyclerAdapter else maxRecyclerAdapter

    fun notifyItemRemoved(pos: Int) {
        if (maxRecyclerAdapter != null) {
            maxRecyclerAdapter!!.notifyItemRemoved(pos)
        }
    }

    fun getOriginalPosition(pos: Int): Int {
        if (maxRecyclerAdapter != null) {
            return maxRecyclerAdapter!!.getOriginalPosition(pos)
        }
        return if (admobRecyclerAdapter != null) {
            admobRecyclerAdapter!!.getOriginalPosition(pos)
        } else 0
    }

    fun loadAds() {
        if (maxRecyclerAdapter != null) maxRecyclerAdapter!!.loadAds()
    }

    fun destroy() {
        if (maxRecyclerAdapter != null) maxRecyclerAdapter!!.destroy()
    }

    fun setCanRecyclable(canRecyclable: Boolean) {
        if (admobRecyclerAdapter != null) {
            admobRecyclerAdapter!!.setCanRecyclable(canRecyclable)
        }
    }

    fun setNativeFullScreen(nativeFullScreen: Boolean) {
        if (admobRecyclerAdapter != null) {
            admobRecyclerAdapter!!.setNativeFullScreen(nativeFullScreen)
        }
    }
}