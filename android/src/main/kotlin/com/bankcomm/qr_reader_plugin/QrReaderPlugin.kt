package com.bankcomm.qr_reader_plugin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.annotation.NonNull
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.BufferedInputStream
import java.io.FileInputStream


/** QrReaderPlugin */
public class QrReaderPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext;
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "qr_reader_plugin")
        channel.setMethodCallHandler(this);
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        var applicationContext: Context? = null
        var reader: QRCodeMultiReader? = null

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "qr_reader_plugin")
            channel.setMethodCallHandler(QrReaderPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "scanYUVImage" -> {
                val bytesList = call.argument<ArrayList<ByteArray>>("bytesList")
                val width = call.argument<Int>("width")
                val height = call.argument<Int>("height")
                if (bytesList == null || width == null || height == null) {
                    result.error("NOT_NULL", "All input parameters should not be null.", null)
                    return
                }
                Thread(Runnable {
                    // 目前只取灰度信息识别
                    val source = PlanarYUVLuminanceSource(bytesList[0], width, height, 0, 0, width, height, false)
                    val scanResult = scanQRCode(source, width * height)
                    // 通过以下代码让result.success在主线程运行，否则会产生RuntimeException
                    Handler(Looper.getMainLooper()).post {
                        result.success(scanResult)
                    }
                }).start()
            }
            "scanByImageFile" -> {
                val filePath = call.argument<String>("filePath")
                if (TextUtils.isEmpty(filePath)) {
                    result.error("NOT_NULL", "Parameter filePath should not be null.", null)
                    return
                }
                Thread(Runnable {
                    BufferedInputStream(FileInputStream(filePath)).use {
                        val srcBitmap: Bitmap? = BitmapFactory.decodeStream(it)
                        if (srcBitmap == null) {
                            Handler(Looper.getMainLooper()).post {
                                result.success(null)
                            }
                        }else{
                            val pixels = IntArray(srcBitmap.width * srcBitmap.height)
                            srcBitmap.getPixels(pixels, 0, srcBitmap.width, 0, 0, srcBitmap.width, srcBitmap.height)
                            val source = RGBLuminanceSource(srcBitmap.width, srcBitmap.height, pixels)
                            val scanResult = scanQRCode(source, pixels.size)
                            Handler(Looper.getMainLooper()).post {
                                result.success(scanResult)
                            }
                        }
                    }
                }).start()
            }
            else -> result.notImplemented()
        }
    }

    private fun scanQRCode(source: LuminanceSource, size: Int): String? {
        var result: String? = null
        if (size > 1280 * 720) {
            // 优先使用scanQRCodeByGlobalHistogramBinarizer
            result = scanQRCodeByGlobalHistogramBinarizer(source)
            if (result != null) {
                return result
            }
            result = scanQRCodeByHybridBinarizer(source)
        } else {
            result = scanQRCodeByHybridBinarizer(source)
            if (result != null) {
                return result
            }
            result = scanQRCodeByGlobalHistogramBinarizer(source)
        }
        return result
    }

    private fun scanQRCodeByHybridBinarizer(source: LuminanceSource): String? {
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val codeReader = getQRCodeReaderInstance()
        var resultString: String? = null
        try {
            val decodeResult = codeReader.decode(binaryBitmap)
            resultString = decodeResult.text
        } catch (e: Exception) {
            // do nothing
        }
        return resultString
    }

    private fun scanQRCodeByGlobalHistogramBinarizer(source: LuminanceSource): String? {
        val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
        val codeReader = getQRCodeReaderInstance()
        var resultString: String? = null
        try {
            val decodeResult = codeReader.decode(binaryBitmap)
            resultString = decodeResult.text
        } catch (e: Exception) {
            // do nothing
        }
        return resultString
    }

    private fun getQRCodeReaderInstance(): QRCodeMultiReader {
        if (reader == null) {
            reader = QRCodeMultiReader()
        }
        return reader!!
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
