package com.sch.share.manager

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import com.sch.share.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
/**
 * Created by StoneHui on 2019-11-28.
 * <p>
 * 文件管理
 */
object FileManager {

    /**
     * 文件临时保存目录。
     */
    fun getTmpFileDir(context: Context): String {
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
     * 清理临时文件。
     */
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

    /**
     * 保存图片并返回对应的 [File] 对象。
      */
    fun saveBitmap(context: Context, bitmap: Bitmap): File {
        val path = "${getTmpFileDir(context)}${File.separator}${System.currentTimeMillis()}.jpg"
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(path))
        return File(path)
    }

}

