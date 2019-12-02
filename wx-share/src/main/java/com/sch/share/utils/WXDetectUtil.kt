package com.sch.share.utils

import android.content.Context
import com.sch.share.constant.WX_PACKAGE_NAME

/**
 * Created by StoneHui on 2019-11-28.
 * <p>
 * 微信检测工具
 */

object WXDetectUtil {

    fun isWXInstalled(context: Context): Boolean {
        context.packageManager.getInstalledPackages(0)
                ?.filter { it.packageName.equals(WX_PACKAGE_NAME, true) }
                ?.let { return it.isNotEmpty() }
        return false
    }

    fun getWXVersionCode(context: Context): Int {
        return context.packageManager.getInstalledPackages(0)
                ?.filter { it.packageName.equals(WX_PACKAGE_NAME, true) }
                ?.let { if (it.isEmpty()) 0 else it[0].versionCode }
                ?: 0
    }
}