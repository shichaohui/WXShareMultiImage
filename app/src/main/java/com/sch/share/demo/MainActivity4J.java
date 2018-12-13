package com.sch.share.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.sch.share.WXShareMultiImageHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity4J extends AppCompatActivity implements View.OnClickListener {

    // 待分享图片。
    private static List<Integer> imageList = new ArrayList<>();

    static {
        Collections.addAll(imageList, R.mipmap.img_1, R.mipmap.img_2, R.mipmap.img_3,
                R.mipmap.img_4, R.mipmap.img_5, R.mipmap.img_6);
    }

    private TextView tvShareContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGridView();
        tvShareContent = findViewById(R.id.tvShareContent);
        findViewById(R.id.btnShareToSession).setOnClickListener(this);
        findViewById(R.id.btnShareToTimeline).setOnClickListener(this);

        requestStoragePermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
    }

    @Override
    protected void onDestroy() {
        // 清理临时文件。
        WXShareMultiImageHelper.clearTmpFile(this);
        super.onDestroy();
    }

    @Override
    public void onClick(final View v) {
        loadImage(new OnLoadImageEndCallback() {
            @Override
            public void onEnd(List<Bitmap> bitmapList) {
                switch (v.getId()) {
                    case R.id.btnShareToSession:
                        shareToSession(bitmapList);
                        break;
                    case R.id.btnShareToTimeline:
                        shareToTimeline(bitmapList);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void requestStoragePermission() {
        // 申请内存权限。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !WXShareMultiImageHelper.hasStoragePermission(this)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initGridView() {
        GridView gridView = findViewById(R.id.gridView);
        gridView.setNumColumns(3);
        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return imageList.size();
            }

            @Override
            public Object getItem(int position) {
                return imageList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView iv = new ImageView(MainActivity4J.this);
                iv.setImageResource(imageList.get(position));
                iv.setAdjustViewBounds(true);
                return iv;
            }
        });
    }

    // 分享给好友。
    private void shareToSession(List<Bitmap> bitmapList) {
        WXShareMultiImageHelper.shareToSession(this, bitmapList, tvShareContent.getText().toString());
    }

    // 分享到朋友圈。
    private void shareToTimeline(List<Bitmap> bitmapList) {
        WXShareMultiImageHelper.shareToTimeline(this, bitmapList, tvShareContent.getText().toString());
    }

    private void loadImage(final OnLoadImageEndCallback callback) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在加载图片...");
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Bitmap> bitmapList = new ArrayList<>();
                for (Integer imageResId : imageList) {
                    bitmapList.add(BitmapFactory.decodeResource(getResources(), imageResId));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.cancel();
                    }
                });
                callback.onEnd(bitmapList);
            }
        }).start();
    }

    private interface OnLoadImageEndCallback {
        void onEnd(List<Bitmap> bitmapList);
    }

}
