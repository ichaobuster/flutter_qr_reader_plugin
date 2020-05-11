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

## Example说明

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
