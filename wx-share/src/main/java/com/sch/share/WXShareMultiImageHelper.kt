package com.sch.share

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.sch.share.manager.FileManager
import com.sch.share.service.ServiceManager
import com.sch.share.share.SessionShare
import com.sch.share.share.TimelineShare
import java.io.File

/**
 * Created by StoneHui on 2018/10/25.
 * <p>
 * 微信多图分享辅助类。
 */
object WXShareMultiImageHelper {

    /**
     * 分享到好友会话。
     *
     * @param activity [Context]
     * @param images 图片列表。
     * @param text 分享文本。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToSession(activity: Activity, images: Array<Bitmap>, text: String = "") {
        SessionShare.share(activity, images, text)
    }

    /**
     * 分享到好友会话。
     *
     * @param activity [Context]
     * @param images 图片列表。
     * @param text 分享文本。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToSession(activity: Activity, images: Array<File>, text: String = "") {
        SessionShare.share(activity, images, text)
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity [Context]
     * @param images 图片列表。
     * @param options [Options] 可选项。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToTimeline(activity: Activity, images: Array<Bitmap>, options: Options = Options()) {
        TimelineShare.share(activity, images, options)
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity [Context]
     * @param images 图片列表。
     * @param options [Options] 可选项。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToTimeline(activity: Activity, images: Array<File>, options: Options = Options()) {
        TimelineShare.share(activity, images, options)
    }

    /**
     * 清理临时文件。可在分享完成后调用该函数。
     */
    @JvmStatic
    fun clearTmpFile(context: Context) {
        FileManager.clearTmpFile(context)
    }

    /**
     * 检查服务是否开启。
     */
    @JvmStatic
    fun isServiceEnabled(context: Context): Boolean {
        return ServiceManager.isServiceEnabled(context)
    }

    /**
     * 打开服务。
     *
     * @param listener 打开服务结果监听。
     */
    @JvmStatic
    fun openService(context: Context, listener: (Boolean) -> Unit) {
        ServiceManager.openService(context, listener)
    }

    /**
     * 打开服务。
     *
     * @param listener 打开服务结果监听。
     */
    @JvmStatic
    fun openService(context: Context, listener: ServiceManager.OnOpenServiceListener) {
        ServiceManager.openService(context, listener)
    }

}