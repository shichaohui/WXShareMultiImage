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
    private static final String LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI";
    private static final String SHARE_IMG_UI = "com.tencent.mm.ui.tools.ShareImgUI";
    private static final String SHARE_TO_TIMELINE_UI = "com.tencent.mm.ui.tools.ShareToTimeLineUI";

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

    // 打开分享给好友界面
    private static void openShareImgUI(Context context, String text, ArrayList<Uri> uriList) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setComponent(new ComponentName(WX_PACKAGE_NAME, SHARE_IMG_UI));
        intent.setType("image/*");
        intent.putExtra("Kdescription", text);
        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, new ArrayList<String>());
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
        context.startActivity(intent);
    }

    // 打开分享到朋友圈界面
    private static  void openShareToTimeLineUI(Context context, String text, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setComponent(new ComponentName(WX_PACKAGE_NAME, SHARE_TO_TIMELINE_UI));
        intent.setType("image/*");
        intent.putExtra("Kdescription", text);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
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
                ShareInfo.getInstance().setAuto(false);
                if (checkShareEnable(activity)) {
                    if (!TextUtils.isEmpty(text)) {
                        ClipboardUtil.setPrimaryClip(activity, "", text);
                        Toast.makeText(activity, "文字已复制到剪切板", Toast.LENGTH_LONG).show();
                    }
                    clearTmpFile(activity);
                    String dir = getTmpFileDir(activity);
                    ArrayList<Uri> uriList = new ArrayList<>();
                    for (Bitmap bitmap : imageList) {
                        uriList.add(Uri.fromFile(new File(saveBitmap(dir, bitmap))));
                    }
                    openShareImgUI(activity, text, uriList);
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
                if (!isAuto) {
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

                final ProgressDialog dialog = new ProgressDialog(activity);
                dialog.setMessage("请稍候...");
                dialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        clearTmpFile(activity);

                        // 保存图片
                        String dir = getTmpFileDir(activity);
                        final String[] paths = new String[imageList.size()];
                        final String[] mimeTypes = new String[imageList.size()];
                        Collections.reverse(imageList);
                        for (int i = 0; i < imageList.size(); i++) {
                            paths[i] = saveBitmap(dir, imageList.get(i));
                            mimeTypes[i] = "image/*";
                        }

                        // 扫描图片
                        MediaScannerConnection.scanFile(
                                activity,
                                paths,
                                mimeTypes,
                                new MediaScannerConnection.OnScanCompletedListener() {

                                    List<Uri> uriList = new ArrayList<>();

                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        uriList.add(uri);
                                        if (uriList.size() < paths.length) {
                                            return;
                                        }
                                        // 扫描结束执行分享。
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!isAuto) {
                                                    shareToTimelineUIManual(activity, text);
                                                } else {
                                                    Collections.reverse(uriList);
                                                    shareToTimelineUIAuto(activity, text, uriList);
                                                }
                                                dialog.cancel();
                                            }
                                        });

                                    }
                                });
                    }
                }).start();
            }
        });
    }

    // 分享到微信朋友圈（自动模式）。
    private static void shareToTimelineUIAuto(Context context, String text, List<Uri> uriList) {

        if (!TextUtils.isEmpty(text)) {
            ClipboardUtil.setPrimaryClip(context, "", text);
        }

        ShareInfo.getInstance().setAuto(true);
        ShareInfo.getInstance().setText(text);
        ShareInfo.getInstance().setImageCount(1, uriList.size() - 1);

        openShareToTimeLineUI(context, text, uriList.get(0));
    }

    // 分享到微信朋友圈（手动模式）。
    private static void shareToTimelineUIManual(Context context, String text) {

        if (!TextUtils.isEmpty(text)) {
            ClipboardUtil.setPrimaryClip(context, "", text);
            Toast.makeText(context, "请手动选择图片，长按粘贴内容！", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "请手动选择图片！", Toast.LENGTH_LONG).show();
        }

        ShareInfo.getInstance().setAuto(false);

        // 打开微信
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(WX_PACKAGE_NAME, LAUNCHER_UI));
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
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
    private static String saveBitmap(String dir, Bitmap bitmap) {
        String path = String.format("%s%s%s.jpg", dir, File.separator, System.currentTimeMillis());
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return path;
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