package com.vapps.module_ads.utils

import android.app.Activity
import android.content.Context

object AdmobHelper {
    private const val FILE_SETTING = "setting_ads.pref"
    private const val FILE_SETTING_ADMOD = "setting_admob.pref"
    private const val IS_PURCHASE = "IS_PURCHASE"
    private const val IS_FIRST_OPEN = "IS_FIRST_OPEN"
    private const val KEY_FIRST_TIME = "KEY_FIRST_TIME"
    fun setPurchased(activity: Activity, isPurchased: Boolean) {
        val pref = activity.getSharedPreferences(FILE_SETTING, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putBoolean(IS_PURCHASE, isPurchased)
        editor.apply()
    }

    fun isPurchased(activity: Activity): Boolean {
        return activity.getSharedPreferences(FILE_SETTING, Context.MODE_PRIVATE).getBoolean(
            IS_PURCHASE, false
        )
    }

    /**
     * Trả về số click của 1 ads nào đó
     *
     * @param context
     * @param idAds
     * @return
     */
    fun getNumClickAdsPerDay(context: Context, idAds: String?): Int {
        return context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE)
            .getInt(idAds, 0)
    }

    /**
     * Tăng số click trên 1 ads
     *
     * @param context
     * @param idAds
     */
    fun increaseNumClickAdsPerDay(context: Context, idAds: String?) {
        val pre = context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE)
        val count = pre.getInt(idAds, 0)
        pre.edit().putInt(idAds, count + 1).apply()
    }

    /**
     * nếu lần đầu mở app lưu thời gian đầu tiên vào SharedPreferences
     * nếu thời gian hiện tại so với thời gian đầu được 1 ngày thì reset lại data của admod.
     *
     * @param context
     */
    fun setupAdmobData(context: Context) {
        if (isFirstOpenApp(context)) {
            context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE).edit().putLong(
                KEY_FIRST_TIME, System.currentTimeMillis()
            ).apply()
            context.getSharedPreferences(FILE_SETTING, Context.MODE_PRIVATE).edit().putBoolean(
                IS_FIRST_OPEN, true
            ).apply()
            return
        }
        val firstTime =
            context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE).getLong(
                KEY_FIRST_TIME, System.currentTimeMillis()
            )
        val rs = System.currentTimeMillis() - firstTime
        if (rs >= 24 * 60 * 60 * 1000) {
            resetAdmobData(context)
        }
    }

    private fun resetAdmobData(context: Context) {
        context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE).edit().clear()
            .apply()
        context.getSharedPreferences(FILE_SETTING_ADMOD, Context.MODE_PRIVATE).edit().putLong(
            KEY_FIRST_TIME, System.currentTimeMillis()
        ).apply()
    }

    private fun isFirstOpenApp(context: Context): Boolean {
        return context.getSharedPreferences(FILE_SETTING, Context.MODE_PRIVATE).getBoolean(
            IS_FIRST_OPEN, false
        )
    }
}