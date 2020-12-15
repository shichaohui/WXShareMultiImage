package com.sch.share.demo

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.sch.share.Options
import com.sch.share.WXShareMultiImageHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // 待分享图片。
    val imageList = listOf(R.mipmap.img_1, R.mipmap.img_2, R.mipmap.img_3,
            R.mipmap.img_4, R.mipmap.img_5, R.mipmap.img_6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGridView()
        btnShareToSession.setOnClickListener { shareToSession() }
        btnShareToTimeline.setOnClickListener { shareToTimeline() }

        requestStoragePermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission()
        }
    }

    override fun onDestroy() {
        // 清理临时文件。
        WXShareMultiImageHelper.clearTmpFile(this)
        super.onDestroy()
    }

    // 是否有存储权限。
    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        // 申请存储权限。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasStoragePermission()) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private fun initGridView() {
        gridView.numColumns = 3
        gridView.adapter = object : BaseAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val iv = ImageView(this@MainActivity)
                iv.setImageResource(imageList[position])
                iv.adjustViewBounds = true
                return iv
            }

            override fun getItem(position: Int): Any = imageList[position]

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getCount(): Int = imageList.size

        }
    }

    // 分享给好友。
    private fun shareToSession() {
        share {
            WXShareMultiImageHelper.shareToSession(this, it, tvShareContent.text.toString())
        }
    }

    // 分享到朋友圈。
    private fun shareToTimeline() {
        share {
            val options = Options().apply {
                text = tvShareContent.text.toString()
            }
            WXShareMultiImageHelper.shareToTimeline(this, it, options)
        }
    }

    private fun share(realShare: (Array<Bitmap>) -> Unit) {
        val dialog = ProgressDialog(this)
        dialog.setMessage("正在加载图片...")
        dialog.show()
        thread(true) {
            val bitmapList = imageList.map { BitmapFactory.decodeResource(resources, it) }
            runOnUiThread { dialog.cancel() }
            realShare(bitmapList.toTypedArray())
        }
    }

}
