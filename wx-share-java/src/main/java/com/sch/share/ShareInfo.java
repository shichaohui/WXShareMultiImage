package com.sch.share;

import android.text.TextUtils;

/**
 * Created by StoneHui on 2018/10/30.
 * <p>
 * 分享信息。
 */
class ShareInfo {

    private static class Singleton {
        static ShareInfo instance = new ShareInfo();
    }

    private ShareInfo() {
    }

    static ShareInfo getInstance() {
        return Singleton.instance;
    }

    private String text = "";
    private int waitingImageCount = 0;
    private int selectedImageCount = 0;
    private boolean isAuto = true;

    /**
     * 是否有待分享文本
     */
    boolean hasText() {
        return !TextUtils.isEmpty(text);
    }

    /**
     * 获取待分享文本
     */
    String getText() {
        return text;
    }

    /**
     * 设置待分享文本
     *
     * @param text 待分享文本
     */
    void setText(String text) {
        this.text = text;
    }

    /**
     * 获取待选择图片数量。
     */
    int getWaitingImageCount() {
        return waitingImageCount;
    }

    /**
     * 获取已选择图片数量。
     */
    int getSelectedImageCount() {
        return selectedImageCount;
    }

    /**
     * 设置图片数量。
     *
     * @param selectedImageCount 已选择图片数量。
     * @param waitingImageCount  待选择图片数量。
     */
    void setImageCount(int selectedImageCount, int waitingImageCount) {
        this.selectedImageCount = selectedImageCount;
        this.waitingImageCount = waitingImageCount;
    }

    /**
     * 是否自动操作。
     */
    boolean isAuto() {
        return isAuto;
    }

    /**
     * 设置是否自动操作。
     */
    void setAuto(boolean isAuto) {
        this.isAuto = isAuto;
    }

}
