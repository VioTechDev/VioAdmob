package com.vapps.module_ads.control.admob.nativead

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AdmobRecyclerAdapter(
    settings: AperoAdPlacerSettings,
    private val adapterOriginal: RecyclerView.Adapter<*>?,
    activity: Activity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var TYPE_AD_VIEW = 0
    private val TYPE_CONTENT_VIEW = 1
    private val settings: AperoAdPlacerSettings
    private val activity: Activity
    private val adPlacer: AperoAdPlacer?
    private val adapterDataObserver: AdapterDataObserver = AdapterDataObserver()
    private var canRecyclable = false
    private var isNativeFullScreen = false

    init {
        registerAdapterDataObserver(adapterDataObserver)
        this.activity = activity
        this.settings = settings
        adPlacer = AperoAdPlacer(settings, adapterOriginal!!, activity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD_VIEW) {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(settings.layoutAdPlaceHolder, parent, false)
            AperoViewHolder(view)
        } else {
            adapterOriginal!!.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (adPlacer!!.isAdPosition(position)) {
            adPlacer.onAdBindHolder(holder.itemView, position)
            holder.setIsRecyclable(canRecyclable)
            adPlacer.renderAd(position, holder)
        } else {
            adapterOriginal?.onBindViewHolder(holder as Nothing, adPlacer.getOriginalPosition(position))
        }
    }

    fun loadAds() {
        adPlacer!!.loadAds()
    }

    override fun getItemViewType(position: Int): Int {
        return if (adPlacer!!.isAdPosition(position)) {
            TYPE_AD_VIEW
        } else {
            TYPE_CONTENT_VIEW
        }
    }

    override fun getItemCount(): Int {
        return adPlacer!!.adjustedCount
    }

    fun getOriginalPosition(pos: Int): Int {
        return adPlacer?.getOriginalPosition(pos) ?: 0
    }

    private inner class AperoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun destroy() {
        adapterOriginal?.unregisterAdapterDataObserver(adapterDataObserver)
    }

    private inner class AdapterDataObserver :
        RecyclerView.AdapterDataObserver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged() {
            adPlacer!!.configData()
            Log.d("AdapterDataObserver", "onChanged: ")
        }

        override fun onItemRangeChanged(var1: Int, var2: Int) {
            Log.d("AdapterDataObserver", "onItemRangeChanged: ")
        }

        override fun onItemRangeInserted(var1: Int, var2: Int) {
            Log.d("AdapterDataObserver", "onItemRangeInserted: ")
        }

        override fun onItemRangeRemoved(var1: Int, var2: Int) {
            Log.d("AdapterDataObserver", "onItemRangeRemoved: ")
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onItemRangeMoved(var1: Int, var2: Int, var3: Int) {
            Log.d("AdapterDataObserver", "onItemRangeMoved: ")
            notifyDataSetChanged()
        }
    }

    fun setCanRecyclable(canRecyclable: Boolean) {
        this.canRecyclable = canRecyclable
    }

    fun setNativeFullScreen(nativeFullScreen: Boolean) {
        isNativeFullScreen = nativeFullScreen
        adPlacer?.setNativeFullScreen(isNativeFullScreen)
    }
}