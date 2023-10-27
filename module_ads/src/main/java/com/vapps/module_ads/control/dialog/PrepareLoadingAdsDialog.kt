package com.vapps.module_ads.control.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.vapps.module_ads.R

class PrepareLoadingAdsDialog(context: Context?) : Dialog(
    context!!, R.style.AppTheme
) {
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading_ads)
    }
}