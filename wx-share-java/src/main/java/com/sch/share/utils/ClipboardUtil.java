package com.sch.share.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/**
 * Created by StoneHui on 2018/12/12.
 * <p>
 * 剪切板工具类。
 */
public class ClipboardUtil {

    /**
     * 复制内容到剪切板。
     */
    public static void setPrimaryClip(Context context, CharSequence label, CharSequence text) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText(label, text));
        }
    }

    /**
     * 获取剪切板的内容。
     */
    public static CharSequence getPrimaryClip(Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return "";
        }
        ClipData data = manager.getPrimaryClip();
        if (data == null || data.getItemCount() <= 0) {
            return "";
        }
        ClipData.Item item = data.getItemAt(0);
        return item != null ? item.getText() : "";
    }

}