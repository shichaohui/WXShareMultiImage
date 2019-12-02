package com.sch.share

/**
 * Created by StoneHui on 2019-11-28.
 * <p>
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