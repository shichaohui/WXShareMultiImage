package com.sch.share.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Created by StoneHui on 2018/10/30.
 * <p>
 * 剪切板工具类。
 */
object ClipboardUtil {

    /**
     * 复制内容到剪切板。
     */
    fun setPrimaryClip(context: Context, label: CharSequence, text: CharSequence) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip = ClipData.newPlainText(label, text)
    }

    /**
     * 获取剪切板的内容。
     */
    fun getPrimaryClip(context: Context): CharSequence {
        return (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip?.getItemAt(0)?.text ?: ""
    }

}