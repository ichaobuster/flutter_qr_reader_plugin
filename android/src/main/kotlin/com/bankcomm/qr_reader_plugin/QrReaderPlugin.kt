package com.bankcomm.qr_reader_plugin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
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
        var cameraManager: CameraManager? = null
        var cameraId: String? = null

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
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val codeReader = getQRCodeReaderInstance()
                    var resultText: String? = null
                    try {
                        val decodeResult = codeReader.decode(binaryBitmap)
                        resultText = decodeResult.text
                    } catch (e: ReaderException) {
                        // do nothing
                    } finally {
                        // 通过以下代码让result.success在主线程运行，否则会产生RuntimeException
                        Handler(Looper.getMainLooper()).post {
                            result.success(resultText)
                        }
                        Thread.currentThread().interrupt()
                    }
                }).start()
            }
            "scanByImageFile" -> {
                val filePath = call.argument<String>("filePath")
                if (TextUtils.isEmpty(filePath)) {
                    result.error("NOT_NULL", "Parameter filePath should not be null.", null)
                    return
                }
                BufferedInputStream(FileInputStream(filePath)).use {
                    val sizedOptions = BitmapFactory.Options()
                    sizedOptions.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(it, null, sizedOptions)
                    if (sizedOptions.outWidth == null || sizedOptions.outHeight == null || sizedOptions.outWidth == 0 || sizedOptions.outHeight == 0) {
                        result.success(null)
                        return
                    }
                    BufferedInputStream(FileInputStream(filePath)).use {
                        val options = BitmapFactory.Options()
                        val maxLength = Math.max(sizedOptions.outWidth, sizedOptions.outHeight)
                        if (maxLength > 1000) {
                            options.inSampleSize = maxLength / 1000
                        }
                        val srcBitmap: Bitmap? = BitmapFactory.decodeStream(it, null, options)
                        if (srcBitmap == null) {
                            result.success(null)
                            return
                        }
                        var resultString: String? = null

                        val pixels = IntArray(srcBitmap.width * srcBitmap.height)
                        srcBitmap.getPixels(pixels, 0, srcBitmap.width, 0, 0, srcBitmap.width, srcBitmap.height)
                        val source = RGBLuminanceSource(srcBitmap.width, srcBitmap.height, pixels)
                        val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                        val codeReader = getQRCodeReaderInstance()
                        try {
                            val decodeResult = codeReader.decode(binaryBitmap)
                            resultString = decodeResult.text
                        } catch (e: Exception) {
                            // do nothing
                        } finally {
                            result.success(resultString)
                        }
                    }
                }
            }
            "toggleFlashlight" -> {
                try {
                    val turnOn = call.argument<Boolean>("turnOn")
                    if (turnOn == null) {
                        result.error("NOT_NULL", "Parameter turnOn should not be null.", null)
                        return
                    }
                    val flashCamManager = getCameraManagerInstance()
                    val flashCamId: String? = getCameraId()
                    if (flashCamManager == null || flashCamId == null) {
                        result.success(false)
                        return
                    }
                    flashCamManager.setTorchMode(flashCamId, turnOn)
                    result.success(true)
                } catch (e: CameraAccessException) {
                    Log.e("FLASHLIGHT", e.message, e)
                    result.success(false)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun getQRCodeReaderInstance(): QRCodeMultiReader {
        if (reader == null) {
            reader = QRCodeMultiReader()
        }
        return reader!!
    }

    private fun getCameraManagerInstance(): CameraManager? {
        if (cameraManager == null) {
            if (applicationContext != null) {
                cameraManager = applicationContext!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            }
        }
        return cameraManager
    }

    private fun getCameraId(): String? {
        if (cameraId != null) {
            return cameraId
        }
        val cameraManager = getCameraManagerInstance()
        if (cameraManager == null) {
            return null
        }
        if (cameraManager.getCameraIdList().size == 0) {
            return null
        }
        cameraId = cameraManager.getCameraIdList()[0]
        return cameraId
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
