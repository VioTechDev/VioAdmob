package com.vapps.module_ads.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import com.vapps.module_ads.R
import com.vapps.module_ads.databinding.DialogLoadingAdsBinding

class LoadingDialog(
    private var mContext: Context
) : Dialog(
    mContext, R.style.DialogTheme
) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogLoadingAdsBinding.inflate(LayoutInflater.from(mContext))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window!!.setGravity(Gravity.CENTER);
        setContentView(binding.root)
        setCancelable(false)
    }
}
