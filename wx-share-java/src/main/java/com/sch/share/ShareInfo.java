package com.sch.share;

import android.text.TextUtils;

/**
 * Created by StoneHui on 2018/10/30.
 * <p>
 * 分享信息。
 */
public class ShareInfo {

    private static String text = "";
    private static int waitingImageCount = 0;
    private static int selectedImageCount = 0;
    private static boolean isAuto = true;

    protected static boolean hasText() {
        return !TextUtils.isEmpty(text);
    }

    protected static String getText() {
        return text;
    }

    protected static void setText(String text) {
        ShareInfo.text = text;
    }

    /**
     * 获取待选择图片数量。
     */
    protected static int getWaitingImageCount() {
        return waitingImageCount;
    }

    /**
     * 获取已选择图片数量。
     */
    protected static int getSelectedImageCount() {
        return selectedImageCount;
    }

    /**
     * 设置图片数量。
     *
     * @param selectedImageCount 已选择图片数量。
     * @param waitingImageCount  待选择图片数量。
     */
    protected static void setImageCount(int selectedImageCount, int waitingImageCount) {
        ShareInfo.selectedImageCount = selectedImageCount;
        ShareInfo.waitingImageCount = waitingImageCount;
    }

    /**
     * 是否自动操作。
     */
    protected static boolean isAuto() {
        return isAuto;
    }

    /**
     * 设置是否自动操作。
     */
    public static void setAuto(boolean isAuto) {
        ShareInfo.isAuto = isAuto;
    }

}
