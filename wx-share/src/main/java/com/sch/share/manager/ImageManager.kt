package com.sch.share.manager

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

/**
 * Created by StoneHui on 2020/12/15.
 * <p>
 * 图片管理
 */
object ImageManager {

    private const val spFileName = "ShareWxUri"
    private const val spUriListFieldName = "list"

    private fun getSp(context: Context): SharedPreferences {
        return context.getSharedPreferences(spFileName, Context.MODE_PRIVATE)
    }

    // 保存 uri 列表
    private fun saveUriList(context: Context, list: List<String>) {
        getSp(context).edit().putStringSet(spUriListFieldName, list.toMutableSet()).apply()
    }

    // 获取 uri 列表
    private fun getUriList(context: Context): List<String> {
        return getSp(context).getStringSet(spUriListFieldName, mutableSetOf())!!.toList()
    }


    /**
     * 插入图片 [image] 到相册，并返回对应的 [Uri]
     */
    private fun insertImage(context: Context, image: Bitmap): String {
        return MediaStore.Images.Media.insertImage(
                context.contentResolver,
                image,
                System.currentTimeMillis().toString(),
                ""
        )
    }

    /**
     * 插入图片 [images] 到相册，并返回对应的 [Uri] 列表
     */
    fun insertImage(context: Context, images: List<Bitmap>): List<Uri> {
        val urlList = mutableListOf<String>()
        val uriList = images.map {
            val url = insertImage(context, it)
            urlList.add(url)
            Uri.parse(url)
        }
        saveUriList(context, urlList)
        return uriList
    }

    /**
     * 清理插入相册的图片
     */
    fun clear(context: Context) {
        getUriList(context).forEach {
            context.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "=?",
                    arrayOf(getRealFilePath(context, Uri.parse(it)))
            )
        }
    }

    // 获取 Uri 的真实路径
    private fun getRealFilePath(context: Context, uri: Uri?): String {
        if (uri == null) {
            return ""
        }
        val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.DATA),
                null,
                null,
                null
        )
        if (cursor == null || !cursor.moveToFirst()) {
            return ""
        }
        val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        if (index < 0) {
            cursor.close()
            return ""
        }
        val path = cursor.getString(index)
        cursor.close()
        return path
    }
}