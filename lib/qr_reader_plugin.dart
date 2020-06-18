import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class QrReaderPlugin {
  static const MethodChannel _channel = const MethodChannel('qr_reader_plugin');

  static int lastScanned = 0;

  /// 识别camera的ImageStream信号
  /// Android为YUV信号，iOS为RGBA信号
  static Future<String> scanByCameraStream(
      {@required List<Uint8List> bytesList,
        @required int width,
        @required int height}) async {
    // 至少间隔100ms进行扫描，以免发生crash
    if (DateTime.now().millisecondsSinceEpoch - lastScanned < 100) {
      return null;
    }
    String result;
    if (Platform.isAndroid){
      result = await _scanYUVImage(bytesList: bytesList, width: width, height: height);
    }else if (Platform.isIOS){
      // iOS上仅单数组通道
      result = await _scanRGBAImage(uint8list: bytesList[0], width: width, height: height);
    }
    lastScanned = DateTime.now().millisecondsSinceEpoch;
    return result;
  }

  /// 识别YUV信号的图片中的QR Code
  /// 一般用于Android摄像头图像信号
  static Future<String> _scanYUVImage(
      {@required List<Uint8List> bytesList,
      @required int width,
      @required int height}) async {
    if (bytesList == null ||
        bytesList.length == 0 ||
        width == null ||
        width == 0 ||
        height == null ||
        height == 0) {
      return null;
    }
    return await _channel.invokeMethod('scanYUVImage',
        {"bytesList": bytesList, "width": width, "height": height});
  }

  /// 识别RGBA信号的图片中的QR Code
  /// 一般用于iOS摄像头图像信号
  static Future<String> _scanRGBAImage(
      {@required Uint8List uint8list,
        @required int width,
        @required int height}) async {
    if (uint8list == null ||
        uint8list.length == 0 ||
        width == null ||
        width == 0 ||
        height == null ||
        height == 0) {
      return null;
    }
    return await _channel.invokeMethod('scanRGBAImage',
        {"uint8list": uint8list, "width": width, "height": height});
  }

  /// 识别图像文件中的QR Code
  static Future<String> scanByImageFile(@required String filePath) async {
    if (filePath == null || filePath.isEmpty) {
      return null;
    }
    return await _channel
        .invokeMethod('scanByImageFile', {"filePath": filePath});
  }
}
