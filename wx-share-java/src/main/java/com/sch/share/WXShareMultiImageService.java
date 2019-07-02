package com.sch.share;

import android.accessibilityservice.AccessibilityService;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.sch.share.utils.ClipboardUtil;

import java.util.*;

/**
 * Created by StoneHui on 2018/12/12.
 * <p>
 * 微信多图分享服务。
 */
public class WXShareMultiImageService extends AccessibilityService {

    private static final String SNS_UPLOAD_UI = "com.tencent.mm.plugin.sns.ui.SnsUploadUI";
    private static final String ALBUM_PREVIEW_UI = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI";

    private static final String DONE_ZH = "完成";
    private static final String DONE_EN = "Done";

    private static final String SELECT_FROM_ALBUM_ZH = "从相册选择";
    private static final String SELECT_FROM_ALBUM_EN = "Select Photos or Videos from Album";

    private AccessibilityNodeInfo prevSource = null;
    private AccessibilityNodeInfo prevListView = null;

    // 当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (!ShareInfo.isAuto()) {
            return;
        }

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                switch (event.getClassName().toString()) {
                    case SNS_UPLOAD_UI:
                        processingSnsUploadUI(event);
                        break;
                    case ALBUM_PREVIEW_UI:
                        selectImage(event);
                        break;
                    default:
                        break;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (event.getClassName().toString().equals(ListView.class.getName())) {
                    openAlbum(event);
                }
                break;
            default:
                break;
        }

    }

    // 处理图文分享界面。
    private void processingSnsUploadUI(AccessibilityEvent event) {
        // 过滤重复事件。
        if (event.getSource().equals(prevSource)) {
            return;
        }
        prevSource = event.getSource();

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();
        if (rootNodeInfo == null) {
            return;
        }

        setTextToUI(rootNodeInfo);

        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }

        // 自动点击添加图片的按钮。
        AccessibilityNodeInfo gridView = findNodeInfo(rootNodeInfo, GridView.class.getName());
        if (gridView != null && gridView.getChildCount() > 2) {
            gridView.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

    }

    // 显示待分享文字。
    private void setTextToUI(AccessibilityNodeInfo rootNodeInfo) {
        if (!ShareInfo.hasText() || !ClipboardUtil.getPrimaryClip(this).equals(ShareInfo.getText())) {
            return;
        }
        // 设置待分享文字。
        AccessibilityNodeInfo editText = findNodeInfo(rootNodeInfo, EditText.class.getName());
        // 粘贴剪切板内容
        if (editText != null) {
            editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            editText.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
        ShareInfo.setText("");
    }

    // 打开相册。
    private void openAlbum(AccessibilityEvent event) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }
        AccessibilityNodeInfo listView = event.getSource();
        // 过滤重复事件。
        if (listView == null || listView == prevListView) {
            return;
        }
        prevListView = listView;

        // 点击从相册选择。
        List<AccessibilityNodeInfo> albumNodeInfoList = listView.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_ZH);
        if (albumNodeInfoList.isEmpty()) {
            albumNodeInfoList = listView.findAccessibilityNodeInfosByText(SELECT_FROM_ALBUM_EN);
        }
        if (!albumNodeInfoList.isEmpty()) {
            listView.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    // 选择图片。
    private void selectImage(AccessibilityEvent event) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }
        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();
        if (rootNodeInfo == null) {
            return;
        }
        AccessibilityNodeInfo gridView = findNodeInfo(rootNodeInfo, GridView.class.getName());
        if (gridView == null) {
            return;
        }
        int index = ShareInfo.getSelectedImageCount();
        int maxIndex = ShareInfo.getSelectedImageCount() + ShareInfo.getWaitingImageCount();
        for (; index < maxIndex; index++) {
            AccessibilityNodeInfo viewNode = findNodeInfo(gridView.getChild(index), View.class.getName());
            if (viewNode != null) {
                viewNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        // 点击完成按钮。
        List<AccessibilityNodeInfo> doneNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DONE_ZH);
        if (doneNodeList.isEmpty()) {
            doneNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DONE_EN);
        }
        if (!doneNodeList.isEmpty()) {
            doneNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        // 自动分享结束。
        ShareInfo.setImageCount(0, 0);
    }

    // 查找指定类名的父节点。
    private AccessibilityNodeInfo findParent(AccessibilityNodeInfo childNodeInfo, String parentClassName) {
        AccessibilityNodeInfo parentNodeInfo = childNodeInfo.getParent();
        if (parentNodeInfo == null) {
            return null;
        }
        if (parentNodeInfo.getClassName().equals(parentClassName)) {
            return parentNodeInfo;
        }
        return findParent(parentNodeInfo, parentClassName);
    }

    // 查找指定类名的节点。
    @Nullable
    private AccessibilityNodeInfo findNodeInfo(AccessibilityNodeInfo rootNodeInfo, String className) {
        if (rootNodeInfo == null) {
            return null;
        }
        LinkedList<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.offer(rootNodeInfo);
        AccessibilityNodeInfo info;
        while (!queue.isEmpty()) {
            info = queue.poll();
            if (info == null) {
                continue;
            }
            if (info.getClassName().toString().contains(className)) {
                return info;
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                queue.offer(info.getChild(i));
            }
        }
        return null;
    }

    // 获取 APPLICATION 的 Window 根节点。
    private AccessibilityNodeInfo getRootNodeInfo() {
        AccessibilityNodeInfo rootNodeInfo = null;
        for (AccessibilityWindowInfo windowInfo : getWindows()) {
            if (windowInfo.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                rootNodeInfo = windowInfo.getRoot();
            }
        }
        return rootNodeInfo != null ? rootNodeInfo : getRootInActiveWindow();
    }

    @Override
    public void onInterrupt() {
        //当服务要被中断时调用.会被调用多次
    }

}