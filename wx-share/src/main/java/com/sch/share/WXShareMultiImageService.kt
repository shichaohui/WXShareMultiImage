package com.sch.share

import android.accessibilityservice.AccessibilityService
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.EditText
import android.widget.GridView
import android.widget.ListView
import com.sch.share.utils.ClipboardUtil
import java.util.*

/**
 * Created by StoneHui on 2018/10/22.
 * <p>
 * 微信多图分享服务。
 */
class WXShareMultiImageService : AccessibilityService() {

    private val snsUploadUI by lazy { "com.tencent.mm.plugin.sns.ui.SnsUploadUI" }
    private val albumPreviewUI by lazy { "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI" }

    private val doneZh by lazy { "完成" }
    private val doneEn by lazy { "Done" }

    private var prevSource: AccessibilityNodeInfo? = null
    private var prevListView: AccessibilityNodeInfo? = null

    // 当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        if (!ShareInfo.isAuto()) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (event.className.toString()) {
                    snsUploadUI -> processingSnsUploadUI(event)
                    albumPreviewUI -> selectImage(event)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                when (event.className.toString()) {
                    ListView::class.java.name -> openAlbum(event)
                }
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

        // 自动点击添加图片的 + 号按钮。
        if (ShareInfo.getWaitingImageCount() > 0) {
            val gridView = findNodeInfo(rootNodeInfo, GridView::class.java.name)
            gridView?.getChild(gridView.childCount - 1)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    // 显示待分享文字。
    private fun setTextToUI(rootNodeInfo: AccessibilityNodeInfo) {
        if (!ShareInfo.hasText() || ClipboardUtil.getPrimaryClip(this) != ShareInfo.getText()) {
            return
        }
        // 设置待分享文字。
        val editText = findNodeInfo(rootNodeInfo, EditText::class.java.name)
        // 粘贴剪切板内容
        editText?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        editText?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    // 打开相册。
    private fun openAlbum(event: AccessibilityEvent) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return
        }
        val listView = event.source
        // 过滤重复事件。
        if (listView == prevListView) {
            return
        }
        prevListView = listView

        listView.getChild(listView.childCount - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // 选择图片。
    private fun selectImage(event: AccessibilityEvent) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return
        }
        val rootNodeInfo = getRootNodeInfo() ?: return
        val gridView = findNodeInfo(rootNodeInfo, GridView::class.java.name) ?: return

        (ShareInfo.getSelectedImageCount()..ShareInfo.getWaitingImageCount())
                .map { findNodeInfo(gridView.getChild(it), View::class.java.name) }
                .forEach { it?.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

        rootNodeInfo?.findAccessibilityNodeInfosByText(doneZh)
                ?.getOrElse(0) { rootNodeInfo.findAccessibilityNodeInfosByText(doneEn).getOrNull(0) }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        ShareInfo.setImageCount(0, 0)
    }

    // 查找指定类名的节点。
    private fun findNodeInfo(rootNodeInfo: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (rootNodeInfo == null) {
            return null
        }
        val queue = LinkedList<AccessibilityNodeInfo>()
        queue.offer(rootNodeInfo)
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