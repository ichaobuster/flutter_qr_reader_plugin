[toc]

**!!!Documents in Chinese only!!!**

# qr_reader_plugin

A new Flutter plugin for reading QR Code.

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our 
[online documentation](https://flutter.dev/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.

## 功能

### 扫描二维码

可以配合`camera`插件使用。

调用**scanByCameraStream**可以识别`camera`插件的ImageStream信号，其中Android为YUV信号，iOS为RGBA信号。

如果扫描成功，返回String类型的扫描结果；没有扫描到时，返回null。

为了防止因为OOM导致crash，扫描间隔为100ms。

### 识别相册中的二维码

可以配合`image_picker`插件使用。

调用**scanByImageFile**可以扫描照片中的二维码，输入参数`filePath`为照片文件路径，可以通过`image_picker`插件获取。

## example说明

plugin本身不需要额外申请权限，但是例子代码中使用camera需要配合添加配置：

### Android

Andorid使用照相机`camera`插件，需要修改`AndroidManifest.xml`配置文件（位置**android/app/src/main/AndroidManifest.xml**），追加以下配置：

```xml
<uses-permission android:name="android.permission.CAMERA"/>
```

### iOS

iOS使用照相机`camera`和照片选取`image_picker`插件，需要修改`Info.plist`配置文件（位置**ios/Runner/Info.plist**），追加以下配置：

```plist
	<key>NSCameraUsageDescription</key>
    <string>扫描二维码</string>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>识别相册中的二维码</string>
```
