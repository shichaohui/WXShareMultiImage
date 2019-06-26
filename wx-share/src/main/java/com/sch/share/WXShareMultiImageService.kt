package com.sch.share

import android.accessibilityservice.AccessibilityService
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ListView
import com.sch.share.utils.ClipboardUtil
import java.util.*

/**
 * Created by StoneHui on 2018/10/22.
 * <p>
 * 微信多图分享服务。
 */
class WXShareMultiImageService : AccessibilityService() {

    private val SNS_UPLOAD_UI = "com.tencent.mm.plugin.sns.ui.SnsUploadUI"
    private val ALBUM_PREVIEW_UI = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI"

    private val DONE_ZH = "完成"
    private val DONE_EN = "Done"

    private val SELECT_FROM_ALBUM_ZH = "从相册选择"
    private val SELECT_FROM_ALBUM_EN = "Select Photos or Videos from Album"

    private var prevSource: AccessibilityNodeInfo? = null
    private var prevListView: AccessibilityNodeInfo? = null

    // 当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        println(event)

        if (!ShareInfo.isAuto()) {
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
                    ListView::class.java.name -> openAlbum(event)
                }
            }
            else -> {
            }
        }

    }

    // 处理图文分享界面。
    private fun processingSnsUploadUI(event: AccessibilityEvent) {
        // 过滤重复事件。
        if (event.source == prevSource) {
            return
        }
        prevSource = event.source

        val rootNodeInfo = getRootNodeInfo() ?: return

        setTextToUI(rootNodeInfo)

        if (ShareInfo.getWaitingImageCount() <= 0) {
            return
        }
        // 自动点击添加图片的按钮。
        rootNodeInfo.getChild(GridView::class.java.name)
                ?.getChild(1)
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // 显示待分享文字。
    private fun setTextToUI(rootNodeInfo: AccessibilityNodeInfo) {
        if (!ShareInfo.hasText() || ClipboardUtil.getPrimaryClip(this) != ShareInfo.getText()) {
            return
        }
        rootNodeInfo.getChild(EditText::class.java.name)?.run {
            // 粘贴剪切板内容
            performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    // 打开相册。
    private fun openAlbum(event: AccessibilityEvent) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return
        }
        val listView = event.source
        // 过滤重复事件。
        if (listView == null || listView == prevListView) {
            return
        }
        prevListView = listView

        // 点击从相册选择。
        listView.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_ZH)
                .getOrElse(0) { listView.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_EN).getOrNull(0) }
                ?.let {
                    listView.getChild(1)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
    }

    // 选择图片。
    private fun selectImage(event: AccessibilityEvent) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return
        }
        val rootNodeInfo = getRootNodeInfo() ?: return
        val gridView = rootNodeInfo.getChild(GridView::class.java.name) ?: return

        val maxIndex = ShareInfo.getSelectedImageCount() + ShareInfo.getWaitingImageCount() - 1
        (ShareInfo.getSelectedImageCount()..maxIndex)
                .map { gridView.getChild(it).getChild(View::class.java.name) }
                .forEach { it?.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

        // 点击完成按钮。
        rootNodeInfo.findAccessibilityNodeInfosByText(DONE_ZH)
                .getOrElse(0) { rootNodeInfo.findAccessibilityNodeInfosByText(DONE_EN).getOrNull(0) }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // 自动分享结束。
        ShareInfo.setImageCount(0, 0)
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