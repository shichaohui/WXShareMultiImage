package com.sch.share.share

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.sch.share.utils.WXDetectUtil

/**
 * Created by StoneHui on 2019-11-28.
 * <p>
 * 分享接口
 */
open class BaseShare {

    // 是否有存储权限。
    private fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

    /**
     * 检查是否可以分享。
     */
    protected fun checkShareEnable(context: Context): Boolean {
        if (!hasStoragePermission(context)) {
            Toast.makeText(context, "没有存储权限，无法分享", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!WXDetectUtil.isWXInstalled(context)) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}