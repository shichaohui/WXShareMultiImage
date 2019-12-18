# WXShareMultiImage

基于无障碍服务实现微信多图分享。

[ ![Download](https://api.bintray.com/packages/shichaohui/maven/wx-share/images/download.svg) ](https://bintray.com/shichaohui/maven/wx-share/_latestVersion)

## 功能

* 分享多图+文字给好友。
* 分享多图+文字到朋友圈。
* 可自定义引导用户打开无障碍服务的弹窗。
* 朋友圈自动选图完成后可自动发布。

## Gradle 依赖

```groovy
implementation 'com.sch.share:wx-share:1.1.2'
```

## 配置

在 strings.xml 中自定义无障碍服务标签。
```xml
<string name="wx_share_multi_image_service_label">ShareDemo【多图分享】</string>
```

## 权限

由于 SDK 涉及文件操作，请添加相关权限。
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

## API

[查看所有 API](./wx-share/src/main/java/com/sch/share/WXShareMultiImageHelper.kt)

## 使用方法

### 分享给好友

```kotlin
// 仅分享图片
WXShareMultiImageHelper.shareToSession(activity, bitmapList)

// 分享图片和文字
WXShareMultiImageHelper.shareToSession(activity, bitmapList, text)
```

### 分享到朋友圈

```kotlin
// 仅分享图片
WXShareMultiImageHelper.shareToTimeline(activity, bitmapList)

// 分享图片和文字，并设置本次分享是否自动发布
val options = WXShareMultiImageHelper.Options().apply {
    text = "待分享文案"
    isAutoFill = true
    isAutoPost = false
    needShowLoading = true
    onPrepareOpenWXListener = {
        // do something
    }
}
WXShareMultiImageHelper.shareToTimeline(activity, bitmapList, options)
```
`WXShareMultiImageHelper.Options()` 是分享可选配置。

* `text`：待分享文案，默认空字符串 "" 。
* `isAutoFill`：是否自动填充文案和图片，默认 true 。
  * **true**：自动填充文案和图片。尝试使用无障碍服务，若无障碍服务未打开，会弹窗引导用户打开服务。
  * **false**：手动填充文案和图片。
* `isAutoPost`：填充文案和图片后是否自动发布，默认 false。该属性仅在 `isAutoFill` 为 true 时有效。
  * **true**：自动发布。
  * **false**：手动发布。
* `needShowLoading` ：是否显示默认的加载进度对话框，默认 true 。
* `onPrepareOpenWXListener`：本次分享即将打开微信时的回调。可以在此处关闭自定义的加载进度对话框。

### 清理临时文件

分享时会产生临时文件，每次分享前都会自动清理临时文件夹，也可以自行调用 API 清理。

```kotlin
WXShareMultiImageHelper.clearTmpFile(activity)
```

### 判断无障碍服务是否可用

```kotlin
if(WXShareMultiImageHelper.isServiceEnabled(activity)) {
    // do something.
} else {
    // do something.
}
```

### 打开无障碍服务

```kotlin
// Kotlin
WXShareMultiImageHelper.openService(activity) {
    // 结果回调，it: Boolean 表示是否打开了无障碍服务。
    isServiceEnabled = it
}
```

```java
// Java
WXShareMultiImageHelper.openService(activity, new ServiceManager.OnOpenServiceListener() {
    @Override
    public void onResult(boolean isOpen) {
        // do something.
    }
});
```

### 自定义引导弹窗

```kotlin
if (WXShareMultiImageHelper.isServiceEnabled(context)) {
    // 服务可用，直接分享
    WXShareMultiImageHelper.shareToTimeline(context, bitmapList)
    return
}
// 服务不可用，先弹窗引导用户打开服务，再根据结果分享
AlertDialog.Builder(context)
    .setCancelable(false)
    .setTitle("开启多图分享")
    .setMessage("到[设置->辅助功能->无障碍]开启多图分享至朋友圈功能。")
    .setPositiveButton("开启") { dialog, _ ->
        dialog.cancel()
        // 跳转到服务开关页面
        WXShareMultiImageHelper.openService(context) {
            // 服务开关页面关闭，执行分享
            val options = WXShareMultiImageHelper.Options()
            options.isAutoFill = it
            WXShareMultiImageHelper.shareToTimeline(context, bitmapList, options)
        }
    }
    .setNegativeButton("取消") { dialog, _ ->
        dialog.cancel()
        // 用户取消操作，执行分享
        val options = WXShareMultiImageHelper.Options()
        options.isAutoFill = false
        WXShareMultiImageHelper.shareToTimeline(context, bitmapList, options)
    }
    .show()
```

# License

```
 Copyright 2018 StoneHui
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and limitations under the License.
```
