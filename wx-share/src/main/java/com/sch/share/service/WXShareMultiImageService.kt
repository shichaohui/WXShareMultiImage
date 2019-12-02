package com.sch.share.service

import android.accessibilityservice.AccessibilityService
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.EditText
import android.widget.GridView
import com.sch.share.ShareInfo
import com.sch.share.utils.ClipboardUtil
import java.util.*

private const val SNS_UPLOAD_UI = "com.tencent.mm.plugin.sns.ui.SnsUploadUI"
private const val ALBUM_PREVIEW_UI = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI"

private const val SELECT_FROM_ALBUM_ZH = "从相册选择"
private const val SELECT_FROM_ALBUM_EN = "Select Photos or Videos from Album"
private const val SELECT_FROM_ALBUM_EN_2 = "Choose from Album"

private const val DONE_ZH = "完成"
private const val DONE_EN = "Done"

private const val POST_ZH = "发表"
private const val POST_EN = "Post"

/**
 * Created by StoneHui on 2018/10/22.
 * <p>
 * 微信多图分享服务。
 */
class WXShareMultiImageService : AccessibilityService() {

    private var textFlag = 0
    private var prepareOpenAlbumFlag = 0
    private var openAlbumFlag = 0
    private var postFlag = 0

    // 当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        if (!ShareInfo.options.isAutoFill) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (event.className.toString()) {
                    SNS_UPLOAD_UI -> {
                        processingSnsUploadUI(event)
                    }
                    ALBUM_PREVIEW_UI -> {
                        selectImage(event)
                    }
                    else -> {
                    }
                }
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                when (event.className.toString()) {
                    "android.widget.ListView",
                    "android.support.v7.widget.RecyclerView",
                    "androidx.recyclerview.widget.RecyclerView" ->
                        openAlbum(event)
                }
            }
            else -> {
            }
        }

    }

    // 处理图文分享界面。
    private fun processingSnsUploadUI(event: AccessibilityEvent) {

        val rootNodeInfo = getRootNodeInfo() ?: return

        setTextToUI(rootNodeInfo)

        if (ShareInfo.waitingImageCount > 0) {
            prepareOpenAlbum(rootNodeInfo)
        } else if (ShareInfo.options.isAutoPost) {
            post(rootNodeInfo)
        }
    }

    // 显示待分享文字。
    private fun setTextToUI(rootNodeInfo: AccessibilityNodeInfo) {
        if (textFlag == rootNodeInfo.hashCode()) {
            return
        } else {
            textFlag = rootNodeInfo.hashCode()
        }
        if (!ShareInfo.hasText()) {
            return
        }
        if (ClipboardUtil.getPrimaryClip(this) != ShareInfo.options.text) {
            return
        } else {
            ShareInfo.options.text = ""
        }
        rootNodeInfo.getChild(EditText::class.java.name)?.run {
            // 粘贴剪切板内容
            performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    // 准备打开相册
    private fun prepareOpenAlbum(rootNodeInfo: AccessibilityNodeInfo) {
        if (ShareInfo.waitingImageCount <= 0) {
            return
        }
        if (prepareOpenAlbumFlag == rootNodeInfo.hashCode()) {
            return
        } else {
            prepareOpenAlbumFlag = rootNodeInfo.hashCode()
        }
        // 自动点击添加图片的按钮。
        rootNodeInfo.getChild(GridView::class.java.name)
                ?.getChild(1)
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // 打开相册。
    private fun openAlbum(event: AccessibilityEvent) {
        if (ShareInfo.waitingImageCount <= 0) {
            return
        }
        val listView = event.source
        if (listView == null || openAlbumFlag == listView.hashCode()) {
            return
        } else {
            openAlbumFlag = listView.hashCode()
        }
        // 查找从相册选择选项并点击。
        for (i in (0 until listView.childCount)) {
            val child = listView.getChild(i) ?: continue
            if (child.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_ZH).isNotEmpty() ||
                    child.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_EN_2).isNotEmpty() ||
                    child.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_EN).isNotEmpty()
            ) {
                child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
        }
    }

    // 选择图片。
    private fun selectImage(event: AccessibilityEvent) {
        if (ShareInfo.waitingImageCount <= 0) {
            return
        }
        val rootNodeInfo = getRootNodeInfo() ?: return
        val gridView = rootNodeInfo.getChild(GridView::class.java.name) ?: return

        val maxIndex = ShareInfo.selectedImageCount + ShareInfo.waitingImageCount - 1
        (ShareInfo.selectedImageCount..maxIndex)
                .map { gridView.getChild(it).getChild(View::class.java.name) }
                .forEach { it?.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

        // 选图结束。
        ShareInfo.setImageCount(0, 0)

        // 点击完成按钮。
        rootNodeInfo.findAccessibilityNodeInfosByText(DONE_ZH)
                .getOrElse(0) { rootNodeInfo.findAccessibilityNodeInfosByText(DONE_EN).getOrNull(0) }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // 发布
    private fun post(rootNodeInfo: AccessibilityNodeInfo) {
        if (postFlag == rootNodeInfo.hashCode()) {
            return
        } else {
            postFlag = rootNodeInfo.hashCode()
        }
        // 准备发布
        ShareInfo.options.isAutoPost = false
        // 点击发表按钮。
        rootNodeInfo.findAccessibilityNodeInfosByText(POST_ZH)
                .getOrElse(0) { rootNodeInfo.findAccessibilityNodeInfosByText(POST_EN).getOrNull(0) }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // 查找指定类名的父节点。
    private fun AccessibilityNodeInfo.getParent(parentClassName: String): AccessibilityNodeInfo? {
        return when {
            parent == null -> null
            parent.className == parentClassName -> parent
            else -> parent.getParent(parentClassName)
        }
    }

    // 查找指定类名的节点。
    private fun AccessibilityNodeInfo.getChild(className: String): AccessibilityNodeInfo? {
        val queue = LinkedList<AccessibilityNodeInfo>()
        queue.offer(this)
        var info: AccessibilityNodeInfo?
        while (!queue.isEmpty()) {
            info = queue.poll()
            if (info == null) {
                continue
            }
            if (info.className.toString().contains(className)) {
                return info
            }
            for (i in 0 until info.childCount) {
                queue.offer(info.getChild(i))
            }
        }
        return null
    }

    // 获取最后一个子节点。
    private fun AccessibilityNodeInfo.getLastChild(): AccessibilityNodeInfo? {
        return if (childCount <= 0) null else getChild(childCount - 1)

    }

    // 获取 APPLICATION 的 Window 根节点。
    private fun getRootNodeInfo(): AccessibilityNodeInfo? {
        var rootNodeInfo: AccessibilityNodeInfo? = null
        for (window in windows) {
            if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                rootNodeInfo = window.root
            }
        }
        return rootNodeInfo ?: rootInActiveWindow
    }

    override fun onInterrupt() {
        //当服务要被中断时调用.会被调用多次
    }

}