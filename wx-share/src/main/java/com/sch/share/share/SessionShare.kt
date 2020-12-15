package com.sch.share.share

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.widget.Toast
import com.sch.share.constant.WX_PACKAGE_NAME
import com.sch.share.constant.WX_SHARE_IMG_UI
import com.sch.share.manager.ImageManager
import com.sch.share.utils.ClipboardUtil
import java.io.File

/**
 * Created by StoneHui on 2019-11-28.
 * <p>
 * 分享给好友
 */
object SessionShare : BaseShare() {

    /**
     * 分享图片（[images]） 和文字（[text]）给好友
     */
    fun share(activity: Activity, images: Array<Bitmap>, text: String) {
        activity.runOnUiThread {
            if (!checkShareEnable(activity)) return@runOnUiThread
            internalShareToSession(activity, images.asList(), text)
        }
    }

    /**
     * 分享图片（[images]） 和文字（[text]）给好友
     */
    fun share(activity: Activity, images: Array<File>, text: String) {
        share(activity, images.map { BitmapFactory.decodeFile(it.path) }.toTypedArray(), text)
    }

    private fun internalShareToSession(activity: Activity, images: List<Bitmap>, text: String = "") {
        if (!TextUtils.isEmpty(text)) {
            ClipboardUtil.setPrimaryClip(activity, "", text)
            Toast.makeText(activity, "请长按粘贴内容", Toast.LENGTH_LONG).show()
        }
        val uriList = ImageManager.insertImage(activity,images)
        // 打开分享给好友界面
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.component = ComponentName(WX_PACKAGE_NAME, WX_SHARE_IMG_UI)
        intent.type = "image/*"
        intent.putExtra("Kdescription", text)
        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, arrayListOf())
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uriList))
        activity.startActivity(intent)
    }

}