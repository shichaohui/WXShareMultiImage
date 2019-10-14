package com.sch.share

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.sch.share.utils.ClipboardUtil
import com.sch.share.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * Created by StoneHui on 2018/10/25.
 * <p>
 * 微信多图分享辅助类。
 */
object WXShareMultiImageHelper {

    private const val WX_PACKAGE_NAME = "com.tencent.mm"
    private const val LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI"
    private const val SHARE_IMG_UI = "com.tencent.mm.ui.tools.ShareImgUI"
    private const val SHARE_TO_TIMELINE_UI = "com.tencent.mm.ui.tools.ShareToTimeLineUI"

    /**
     * 是否安装了微信。
     */
    @JvmStatic
    fun isWXInstalled(context: Context): Boolean {
        context.packageManager.getInstalledPackages(0)
                ?.filter { it.packageName.equals(WX_PACKAGE_NAME, true) }
                ?.let { return it.isNotEmpty() }
        return false
    }

    /**
     * 获取微信的版本号。
     */
    @JvmStatic
    fun getWXVersionCode(context: Context): Int {
        return context.packageManager.getInstalledPackages(0)
                ?.filter { it.packageName.equals(WX_PACKAGE_NAME, true) }
                ?.let { if (it.isEmpty()) 0 else it[0].versionCode }
                ?: 0
    }

    /**
     * 是否有存储权限。
     */
    @JvmStatic
    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // 检查是否可以分享。
    private fun checkShareEnable(context: Context): Boolean {
        if (!hasStoragePermission(context)) {
            Toast.makeText(context, "没有存储权限，无法分享", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isWXInstalled(context)) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 打开分享给好友界面
    private fun openShareImgUI(context: Context, text: String, uriList: List<Uri>) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.component = ComponentName(WX_PACKAGE_NAME, SHARE_IMG_UI)
        intent.type = "image/*"
        intent.putExtra("Kdescription", text)
        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, arrayListOf())
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uriList))
        (context as Activity).startActivity(intent)
    }

    // 打开分享到朋友圈界面
    private fun openShareToTimeLineUI(context: Context, text: String, uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.component = ComponentName(WX_PACKAGE_NAME, SHARE_TO_TIMELINE_UI)
        intent.type = "image/*"
        intent.putExtra("Kdescription", text)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        (context as Activity).startActivity(intent)
    }

    /**
     * 分享到好友会话。
     *
     * @param activity [Context]
     * @param imageList 图片列表。
     * @param text 分享文本。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToSession(activity: Activity, imageList: List<Bitmap>, text: String = "") {
        activity.runOnUiThread {
            if (checkShareEnable(activity)) {
                val dir = getTmpFileDir(activity)
                internalShareToSession(activity, imageList.map { File(saveBitmap(dir, it)) }, text)
            }
        }
    }

    /**
     * 分享到好友会话。
     *
     * @param activity [Context]
     * @param fileList 图片列表。
     * @param text 分享文本。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToSessionByFile(activity: Activity, fileList: List<File>, text: String = "") {
        activity.runOnUiThread {
            if (checkShareEnable(activity)) {
                internalShareToSession(activity, fileList, text)
            }
        }
    }

    private fun internalShareToSession(activity: Activity, fileList: List<File>, text: String = "") {
        if (!TextUtils.isEmpty(text)) {
            ClipboardUtil.setPrimaryClip(activity, "", text)
            Toast.makeText(activity, "请长按粘贴内容", Toast.LENGTH_LONG).show()
        }
        openShareImgUI(activity, text, fileList.map { Uri.fromFile(it) })
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity [Context]
     * @param imageList 图片列表。
     * @param options [Options] 可选项。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToTimeline(activity: Activity, imageList: List<Bitmap>, options: Options = Options()) {
        activity.runOnUiThread {
            if (checkShareEnable(activity)) {
                clearTmpFile(activity)
                val dir = getTmpFileDir(activity)
                val fileList = imageList.map { File(saveBitmap(dir, it)) }
                internalShareToTimeline(activity, fileList, options)
            }
        }
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity [Context]
     * @param fileList 图片列表。
     * @param options [Options] 可选项。
     */
    @JvmStatic
    @JvmOverloads
    fun shareToTimelineByFile(activity: Activity, fileList: List<File>, options: Options = Options()) {
        activity.runOnUiThread {
            if (checkShareEnable(activity)) {
                clearTmpFile(activity)
                val dir = getTmpFileDir(activity)
                val copyFileList = fileList.map { it.copyTo(File(dir, it.name), true) }
                internalShareToTimeline(activity, copyFileList, options)
            }
        }
    }

    // 显示打开服务的对话框。
    private fun showOpenServiceDialog(activity: Activity, openListener: () -> Unit, cancelListener: () -> Unit) {
        AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(activity.getString(R.string.wx_share_dialog_title))
                .setMessage(activity.getString(R.string.wx_share_dialog_message))
                .setPositiveButton(activity.getString(R.string.wx_share_dialog_positive_button_text)) { dialog, _ ->
                    dialog.cancel()
                    openListener()
                }
                .setNegativeButton(activity.getString(R.string.wx_share_dialog_negative_button_text)) { dialog, _ ->
                    dialog.cancel()
                    cancelListener()
                }
                .show()
    }

    /**
     * 分享到微信朋友圈。
     *
     * @param activity [Context]
     * @param fileList 图片列表。
     * @param options [Options] 可选项。
     */
    private fun internalShareToTimeline(activity: Activity, fileList: List<File>, options: Options) {
        fun share() {
            var dialog: ProgressDialog? = null
            if (options.needShowLoading) {
                dialog = ProgressDialog(activity).apply {
                    setCancelable(false)
                    setMessage("请稍候...")
                    show()
                }
            }

            thread(true) {
                // 获取图片路径
                val paths = fileList.reversed().map { it.absolutePath }.toTypedArray()
                val mimeTypes = Array(paths.size) { "image/*" }
                // 扫描图片
                val uriList = mutableListOf<Uri>()
                MediaScannerConnection.scanFile(activity, paths, mimeTypes) { _, uri ->
                    uriList.add(uri)
                    if (uriList.size >= paths.size) {
                        // 扫描结束执行分享。
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
            }
        }
        if (!options.isAutoFill) {
            share()
        } else if (isServiceEnabled(activity)) {
            share()
        } else {
            showOpenServiceDialog(
                    activity,
                    {
                        openService(activity) {
                            options.apply { isAutoFill = it }
                            share()
                        }
                    },
                    {
                        options.apply { isAutoFill = false }
                        share()
                    }
            )
        }
    }

    // 分享到微信朋友圈（自动模式）。
    private fun shareToTimelineUIAuto(context: Context, options: Options, uriList: List<Uri>) {

        if (!TextUtils.isEmpty(options.text)) {
            ClipboardUtil.setPrimaryClip(context, "", options.text)
        }

        ShareInfo.options = options
        ShareInfo.setImageCount(1, uriList.size - 1)

        openShareToTimeLineUI(context, options.text, uriList[0])
    }

    // 分享到微信朋友圈（手动模式）。
    private fun shareToTimelineUIManual(context: Context, options: Options) {

        if (!TextUtils.isEmpty(options.text)) {
            ClipboardUtil.setPrimaryClip(context, "", options.text)
            Toast.makeText(context, "请手动选择图片，长按粘贴内容！", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "请手动选择图片！", Toast.LENGTH_LONG).show()
        }

        // 打开微信
        val intent = Intent(Intent.ACTION_MAIN)
        intent.action = Intent.ACTION_MAIN
        intent.component = ComponentName(WX_PACKAGE_NAME, LAUNCHER_UI)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 文件临时保存目录。
    private fun getTmpFileDir(context: Context): String {
        val parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val child = "${context.packageName}${File.separator}shareTmp"
        return File(parent, child)
                .run {
                    if (!exists()) {
                        mkdirs()
                    }
                    absolutePath
                }
    }

    /**
     * 清理临时文件。可在分享完成后调用该函数。
     */
    @JvmStatic
    fun clearTmpFile(context: Context) {

        val fileDir = File(getTmpFileDir(context))

        // 通知相册删除图片。
        fileDir.listFiles().forEach {
            context.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "=?",
                    arrayOf(it.absolutePath))
        }

        // 删除图片文件。
        FileUtil.clearDir(fileDir)
    }

    // 保存图片并返回 Uri 。
    private fun saveBitmap(dir: String, bitmap: Bitmap): String {
        val path = "$dir${File.separator}${System.currentTimeMillis()}.jpg"
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(path))
        return path
    }

    /**
     * 检查服务是否开启。
     */
    @JvmStatic
    fun isServiceEnabled(context: Context): Boolean {
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .filter { it.id == "${context.packageName}/${WXShareMultiImageService::class.java.name}" }
                .let { return it.isNotEmpty() }
    }

    /**
     * 打开服务。
     *
     * @param listener 打开服务结果监听。
     */
    @JvmStatic
    fun openService(context: Context, listener: (Boolean) -> Unit) {
        if (isServiceEnabled(context)) {
            listener(true)
            return
        }
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityResumed(activity: Activity?) {
                if (context::class.java == activity!!::class.java) {
                    (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(this)
                    listener(isServiceEnabled(context))
                }
            }
        })
        //打开系统无障碍设置界面
        val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(accessibleIntent)
    }

    /**
     * 打开服务。
     *
     * @param listener 打开服务结果监听。
     */
    @JvmStatic
    fun openService(context: Context, listener: OnOpenServiceListener) {
        openService(context) { listener.onResult(it) }
    }

    /**
     * 打开服务的回调。
     */
    interface OnOpenServiceListener {
        /**
         * 打开服务的回调。
         *
         * @param isOpening 服务是否开启。
         */
        fun onResult(isOpening: Boolean)
    }

    /**
     * 分享可选择项
     */
    class Options {
        /**
         * 待分享文本
         */
        var text = ""
        /**
         * 是否自动填充文本和图片
         */
        var isAutoFill = true
        /**
         * 是否自动发布
         */
        var isAutoPost = false
        /**
         * 是否显示进度对话框
         */
        var needShowLoading = true

        /**
         * 设置准备打开微信时的回调
         */
        var onPrepareOpenWXListener: (() -> Unit)? = null

        /**
         * 设置准备打开微信时的回调
         */
        fun setOnPrepareOpenWXListener(_onPrepareOpenWXListener: OnPrepareOpenWXListener) {
            this.onPrepareOpenWXListener = { _onPrepareOpenWXListener.onPrepareOpenWX() }
        }

        /**
         * 准备打开微信时的回调
         */
        interface OnPrepareOpenWXListener {
            fun onPrepareOpenWX()
        }

    }

}