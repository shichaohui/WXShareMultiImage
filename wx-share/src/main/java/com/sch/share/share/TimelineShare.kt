package com.sch.share.share

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import com.sch.share.Options
import com.sch.share.R
import com.sch.share.ShareInfo
import com.sch.share.WXShareMultiImageHelper
import com.sch.share.constant.WX_LAUNCHER_UI
import com.sch.share.constant.WX_PACKAGE_NAME
import com.sch.share.constant.WX_SHARE_TO_TIMELINE_UI
import com.sch.share.manager.ImageManager
import com.sch.share.service.ServiceManager
import com.sch.share.utils.ClipboardUtil
import java.io.File
import kotlin.concurrent.thread

/**
 * Created by StoneHui on 2019-11-28.
 * <p>
 * 分享到朋友圈
 */
object TimelineShare : BaseShare() {

    /**
     * 分享图片 [images] 到朋友圈。
     * 使用 [options] 设置是否自动填充文案、是否自动发布、回调函数等配置。
     */
    fun share(activity: Activity, images: Array<Bitmap>, options: Options) {
        if (!checkShareEnable(activity)) {
            return
        }
        WXShareMultiImageHelper.clearTmpFile(activity)
        startShare(activity, images.asList(), options)
    }

    /**
     * 分享图片 [images] 到朋友圈。
     * 使用 [options] 设置是否自动填充文案、是否自动发布、回调函数等配置。
     */
    fun share(activity: Activity, images: Array<File>, options: Options) {
        share(activity, images.map { BitmapFactory.decodeFile(it.path) }.toTypedArray(), options)
    }

    /**
     * 分享到微信朋友圈。
     *
     * @param activity [Context]
     * @param images 图片列表。
     * @param options [Options] 可选项。
     */
    private fun startShare(activity: Activity, images: List<Bitmap>, options: Options) {
        if (!options.isAutoFill || WXShareMultiImageHelper.isServiceEnabled(activity)) {
            internalShare(activity, images, options)
            return
        }
        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(activity.getString(R.string.wx_share_dialog_title))
                    .setMessage(activity.getString(R.string.wx_share_dialog_message))
                    .setPositiveButton(activity.getString(R.string.wx_share_dialog_positive_button_text)) { dialog, _ ->
                        dialog.cancel()
                        ServiceManager.openService(activity) {
                            options.isAutoFill = it
                            internalShare(activity, images, options)
                        }
                    }
                    .setNegativeButton(activity.getString(R.string.wx_share_dialog_negative_button_text)) { dialog, _ ->
                        dialog.cancel()
                        options.isAutoFill = false
                        internalShare(activity, images, options)
                    }
                    .show()
        }
    }

    private fun internalShare(activity: Activity, images: List<Bitmap>, options: Options) {
        var dialog: ProgressDialog? = null
        if (options.needShowLoading) {
            activity.runOnUiThread {
                dialog = ProgressDialog(activity).apply {
                    setCancelable(false)
                    setMessage("请稍候...")
                    show()
                }
            }
        }
        thread(true) {
            // 扫描图片
            val uriList = ImageManager.insertImage(activity, images)
            // 扫描结束执行分享
            activity.runOnUiThread {
                dialog?.cancel()
                options.onPrepareOpenWXListener?.invoke()
                if (options.isAutoFill) {
                    shareToTimelineUIAuto(activity, options, uriList.reversed())
                } else {
                    shareToTimelineUIManual(activity, options)
                }
            }
        }
    }

    // 分享到微信朋友圈（自动模式）。
    private fun shareToTimelineUIAuto(context: Context, options: Options, uriList: List<Uri>) {
        if (!TextUtils.isEmpty(options.text)) {
            ClipboardUtil.setPrimaryClip(context, "", options.text)
        }
        ShareInfo.options = options
        ShareInfo.setImageCount(0, uriList.size)
        openWeiXin(context)
    }

    // 分享到微信朋友圈（手动模式）。
    private fun shareToTimelineUIManual(context: Context, options: Options) {
        if (!TextUtils.isEmpty(options.text)) {
            ClipboardUtil.setPrimaryClip(context, "", options.text)
            Toast.makeText(context, "请手动选择图片，长按粘贴内容！", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "请手动选择图片！", Toast.LENGTH_LONG).show()
        }
        openWeiXin(context)
    }

    // 打开微信
    private fun openWeiXin(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.action = Intent.ACTION_MAIN
        intent.component = ComponentName(WX_PACKAGE_NAME, WX_LAUNCHER_UI)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

}