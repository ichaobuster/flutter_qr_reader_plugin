#import "QrReaderPlugin.h"
#if __has_include(<qr_reader_plugin/qr_reader_plugin-Swift.h>)
#import <qr_reader_plugin/qr_reader_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "qr_reader_plugin-Swift.h"
#endif

@implementation QrReaderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQrReaderPlugin registerWithRegistrar:registrar];
}
@end
