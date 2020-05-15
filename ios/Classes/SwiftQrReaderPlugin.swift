import Flutter
import UIKit

public class SwiftQrReaderPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "qr_reader_plugin", binaryMessenger: registrar.messenger())
    let instance = SwiftQrReaderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }
  
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "scanRGBAImage":
      let queue = DispatchQueue(label: "flutter_qr_reader.scanRGBAImage")
      queue.async {
        if let args = call.arguments as? Dictionary<String, Any>,
          let uint8listData = args["uint8list"] as? FlutterStandardTypedData,
          let width = args["width"] as? Int,
          let height = args["height"] as? Int{
          let uint8list = [UInt8](uint8listData.data)
          let ciImage = CIImage(bitmapData: Data(uint8list), bytesPerRow: width * 4, size: CGSize(width: width, height: height), format: CIFormat.RGBA8, colorSpace: CGColorSpaceCreateDeviceRGB())
          result(self.detectQRCode(ciImage: ciImage))
        } else {
          result(FlutterError.init(code: "bad arguments", message: nil, details: nil))
        }
      }
    case "scanByImageFile":
      if let args = call.arguments as? Dictionary<String, Any>,
        let filePath = args["filePath"] as? String{
        let ciImage = CIImage(contentsOf: URL(fileURLWithPath: filePath))
        result(self.detectQRCode(ciImage: ciImage))
      } else {
        result(FlutterError.init(code: "bad arguments", message: nil, details: nil))
      }
    default:
      result(FlutterMethodNotImplemented)
    }
  }
  
  private var ciDetector:CIDetector? = nil
  
  private func getDetector() -> CIDetector {
    if (ciDetector == nil) {
      let context = CIContext(options: nil)
      ciDetector = CIDetector(ofType: CIDetectorTypeQRCode, context: context, options: [CIDetectorAccuracy:CIDetectorAccuracyHigh])
    }
    return ciDetector!
  }
  
  private func detectQRCode(ciImage:CIImage?) -> String?{
    if (ciImage == nil) {
      return nil
    }
    let features = self.getDetector().features(in: ciImage!)
    if (features.count > 0) {
      let feature = features[0] as! CIQRCodeFeature
      return feature.messageString
    }
    return nil
  }
  
}
