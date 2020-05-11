import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class QrReaderPlugin {
  static const MethodChannel _channel = const MethodChannel('qr_reader_plugin');

  /// 识别YUV信号的图片中的QR Code
  /// 一般用于Android摄像头图像信号
  static Future<String> scanYUVImage(
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

  /// 识别图像文件中的QR Code
  static Future<String> scanByImageFile(@required String filePath) async {
    if (filePath == null || filePath.isEmpty) {
      return null;
    }
    return await _channel
        .invokeMethod('scanByImageFile', {"filePath": filePath});
  }
}
