package com.sch.share;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.sch.share.utils.ClipboardUtil;
import com.sch.share.utils.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by StoneHui on 2018/12/12.
 * <p>
 * 微信多图分享辅助类。
 */
public class WXShareMultiImageHelper {

    private static final String WX_PACKAGE_NAME = "com.tencent.mm";
    private static final String SHARE_IMG_UI = "com.tencent.mm.ui.tools.ShareImgUI";
    private static final String SHARE_TO_TIMELINE_UI = "com.tencent.mm.ui.tools.ShareToTimeLineUI";

    // 微信 v6.7.3 版本的 versionCode 。
    private static final int WX_V673 = 1360;

    /**
     * 是否安装了微信。
     */
    public static boolean isWXInstalled(Context context) {
        List<PackageInfo> packageInfoList = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : packageInfoList) {
            if (info.packageName.equalsIgnoreCase(WX_PACKAGE_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取微信的版本号。
     */

    public static int getWXVersionCode(Context context) {
        List<PackageInfo> packageInfoList = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : packageInfoList) {
            if (info.packageName.equalsIgnoreCase(WX_PACKAGE_NAME)) {
                return info.versionCode;
            }
        }
        return 0;
    }

    /**
     * 是否有存储权限。
     */
    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // 检查是否可以分享。
    private static boolean checkShareEnable(Context context) {
        if (!hasStoragePermission(context)) {
            Toast.makeText(context, "没有存储权限，无法分享", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isWXInstalled(context)) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // 打开分享界面
    private static void openShareUI(Context context, String text, List<Uri> uriList, String ui) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setComponent(new ComponentName("com.tencent.mm", ui));
        intent.setType("image/*");
        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, new ArrayList<String>());
        intent.putExtra("Kdescription", text);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uriList));
        context.startActivity(intent);
    }

    /**
     * 分享到好友会话。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     */
    public static void shareToSession(Activity activity, List<Bitmap> imageList) {
        shareToSession(activity, imageList, "");
    }

    /**
     * 分享到好友会话。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     * @param text      分享文本。
     */
    public static void shareToSession(final Activity activity, final List<Bitmap> imageList, final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (checkShareEnable(activity)) {
                    if (!TextUtils.isEmpty(text)) {
                        ClipboardUtil.setPrimaryClip(activity, "", text);
                        Toast.makeText(activity, "文字已复制到剪切板", Toast.LENGTH_LONG).show();
                    }
                    clearTmpFile(activity);
                    String dir = getTmpFileDir(activity);
                    List<Uri> uriList = new ArrayList<>();
                    for (Bitmap bitmap : imageList) {
                        uriList.add(saveBitmap(dir, bitmap));
                    }
                    openShareUI(activity, text, uriList, SHARE_IMG_UI);
                }
            }
        });
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     */
    public static void shareToTimeline(Activity activity, List<Bitmap> imageList) {
        shareToTimeline(activity, imageList, "", true);
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     * @param text      分享文本。
     */
    public static void shareToTimeline(Activity activity, List<Bitmap> imageList, String text) {
        shareToTimeline(activity, imageList, text, true);
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     * @param isAuto    是否由 SDK 自动粘贴文字、选择选图。
     */
    public static void shareToTimeline(Activity activity, List<Bitmap> imageList, boolean isAuto) {
        shareToTimeline(activity, imageList, "", isAuto);
    }

    /**
     * 分享到朋友圈。
     *
     * @param activity  [Context]
     * @param imageList 图片列表。
     * @param text      分享文本。
     * @param isAuto    是否由 SDK 自动粘贴文字、选择选图。
     */
    public static void shareToTimeline(final Activity activity, final List<Bitmap> imageList, final String text, final boolean isAuto) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isAuto || getWXVersionCode(activity) < WX_V673) {
                    internalShareToTimeline(activity, text, imageList, false);
                } else if (WXShareMultiImageHelper.isServiceEnabled(activity)) {
                    internalShareToTimeline(activity, text, imageList, true);
                } else {
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle(activity.getString(R.string.wx_share_dialog_title))
                            .setMessage(activity.getString(R.string.wx_share_dialog_message))
                            .setPositiveButton(activity.getString(R.string.wx_share_dialog_positive_button_text), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    WXShareMultiImageHelper.openService(activity, new OnOpenServiceListener() {
                                        @Override
                                        public void onResult(Boolean isOpening) {
                                            internalShareToTimeline(activity, text, imageList, isOpening);
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.wx_share_dialog_negative_button_text), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    internalShareToTimeline(activity, text, imageList, false);
                                }
                            })
                            .show();
                }
            }
        });
    }

    /**
     * 分享到微信朋友圈。
     *
     * @param activity  [Context]
     * @param text      分享文本。
     * @param imageList 图片列表。
     * @param isAuto    false 表示由用户手动粘贴文字、选择选图，不会执行无障碍操作；
     *                  true 表示使用无障碍操作，若用户未打开无障碍服务，将和 false 等同。
     */
    private static void internalShareToTimeline(final Activity activity, final String text, final List<Bitmap> imageList, final Boolean isAuto) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!checkShareEnable(activity)) {
                    return;
                }

                ShareInfo.setAuto(isAuto);
                if (!TextUtils.isEmpty(text)) {
                    ClipboardUtil.setPrimaryClip(activity, "", text);
                }

                final ProgressDialog dialog = new ProgressDialog(activity);
                dialog.setMessage("请稍候...");
                dialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isAuto) {
                            // 因为第一张由微信 API 带进去，所以不需要自动选择。
                            ShareInfo.setImageCount(1, imageList.size() - 1);

                            ShareInfo.setText(text);
                        }

                        clearTmpFile(activity);

                        String dir = getTmpFileDir(activity);
                        final List<Uri> uriList = new ArrayList<>();
                        MediaScannerConnection.OnScanCompletedListener scanCompletedListener = new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                            }
                        };
                        Collections.reverse(imageList);
                        for (Bitmap bitmap : imageList) {
                            // 保存图片
                            Uri uri = saveBitmap(dir, bitmap);
                            uriList.add(uri);
                            // 扫描图片
                            MediaScannerConnection.scanFile(activity, new String[]{uri.getPath()}, new String[]{"image/*"}, scanCompletedListener);
                        }

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 打开分享
                                Collections.reverse(uriList);
                                if (getWXVersionCode(activity) < WX_V673) {
                                    openShareUI(activity, text, uriList, SHARE_TO_TIMELINE_UI);
                                } else {
                                    openShareUI(activity, text, uriList.subList(0, 1), SHARE_TO_TIMELINE_UI);
                                }
                                if (!isAuto) {
                                    // 文字提示。
                                    StringBuilder stringBuilder = new StringBuilder();
                                    if (!TextUtils.isEmpty(text)) {
                                        stringBuilder.append("长按粘贴文字");
                                    }
                                    if (imageList.size() > 0 && getWXVersionCode(activity) >= WX_V673) {
                                        if (stringBuilder.length() > 0) {
                                            stringBuilder.append(",");
                                        }
                                        stringBuilder.append("点击加号添加剩余图片");
                                    }
                                    if (stringBuilder.length() > 0) {
                                        Toast.makeText(activity, stringBuilder.toString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                                dialog.cancel();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // 文件临时保存目录。
    private static String getTmpFileDir(Context context) {
        File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String child = String.format("%s%sshareTmp", context.getPackageName(), File.separator);
        File file = new File(parent, child);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 清理临时文件。可在分享完成后调用该函数。
     */
    public static void clearTmpFile(Context context) {

        File fileDir = new File(getTmpFileDir(context));

        // 通知相册删除图片。
        for (File file : fileDir.listFiles()) {
            context.getContentResolver().delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "=?",
                    new String[]{file.getAbsolutePath()});
        }

        // 删除图片文件。
        FileUtil.clearDir(fileDir);
    }

    // 保存图片并返回 Uri 。
    private static Uri saveBitmap(String dir, Bitmap bitmap) {
        String target = String.format("%s%s%s.jpg", dir, File.separator, System.currentTimeMillis());
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(target));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(new File(target));
    }

    /**
     * 检查服务是否开启。
     */
    public static boolean isServiceEnabled(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) {
            return false;
        }
        List<AccessibilityServiceInfo> infoList = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : infoList) {
            if (info.getId().equals(String.format("%s/%s", context.getPackageName(), WXShareMultiImageService.class.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开服务。
     *
     * @param listener 打开服务结果监听。
     */
    public static void openService(final Context context, final OnOpenServiceListener listener) {
        if (WXShareMultiImageHelper.isServiceEnabled(context)) {
            listener.onResult(true);
            return;
        }
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacksAdapter() {
                    @Override
                    public void onActivityResumed(Activity activity) {
                        if (context.getClass().equals(activity.getClass())) {
                            ((Application) context.getApplicationContext()).unregisterActivityLifecycleCallbacks(this);
                            listener.onResult(WXShareMultiImageHelper.isServiceEnabled(context));
                        }
                    }
                });
        //打开系统无障碍设置界面
        context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    /**
     * 打开服务的回调。
     */
    interface OnOpenServiceListener {
        /**
         * 打开服务的回调。
         *
         * @param isOpening 服务是否开启。
         */
        void onResult(Boolean isOpening);
    }

}