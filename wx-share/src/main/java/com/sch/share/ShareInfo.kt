package com.sch.share

import android.text.TextUtils

/**
 * Created by StoneHui on 2018/10/30.
 *
 *
 * 分享信息。
 */
internal object ShareInfo {

    /**
     * 待分享文本
     */
    var text = ""

    /**
     * 是否有待分享文本
     */
    fun hasText(): Boolean {
        return !TextUtils.isEmpty(text)
    }

    /**
     * 待选择图片数量。
     */
    var waitingImageCount = 0
        private set

    /**
     * 已选择图片数量。
     */
    var selectedImageCount = 0
        private set

    /**
     * 设置图片数量。
     *
     * @param selectedImageCount 已选择图片数量。
     * @param waitingImageCount  待选择图片数量。
     */
    fun setImageCount(selectedImageCount: Int, waitingImageCount: Int) {
        ShareInfo.selectedImageCount = selectedImageCount
        ShareInfo.waitingImageCount = waitingImageCount
    }

    /**
     * 是否自动操作。
     */
    var isAuto = true

}