import 'dart:async';
import 'dart:developer';
import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:qr_reader_plugin/qr_reader_plugin.dart';

Future<void> main() async {
  // Ensure that plugin services are initialized so that `availableCameras()`
  // can be called before `runApp()`
  WidgetsFlutterBinding.ensureInitialized();

  runApp(MyApp());
}

// A screen that allows users to take a picture using a given camera.
class MyApp extends StatefulWidget {
  @override
  MyAppState createState() => MyAppState();
}

class MyAppState extends State<MyApp> {
  var _controllerInited = false;
  CameraController _controller;
  Future<void> _initializeControllerFuture;
  var _processingStream = false;
  var _processingImagePickup = false;
  var _result = 'Unknown';

  Future<void> _initCameraController() async {
    // Obtain a list of the available cameras on the device.
    final cameras = await availableCameras();
    if (cameras.length == 0) {
      return;
    }
    _controller = CameraController(
      cameras.first,
      ResolutionPreset.high,
      enableAudio: false,
    );
    _initializeControllerFuture = _controller.initialize();
    setState(() {});
    _initializeControllerFuture.then((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _controllerInited = true;
      });
      _controller.startImageStream(_onLatestImageAvailable);
    });
  }

  _onLatestImageAvailable(CameraImage availableImage) async {
    if (_processingStream || _processingImagePickup) {
      return;
    }
    _processingStream = true;
    try {
      final result = await QrReaderPlugin.scanYUVImage(
        bytesList: availableImage.planes.map((plane) {
          return plane.bytes;
        }).toList(),
        width: availableImage.width,
        height: availableImage.height,
      );
      if (result != null && result.isNotEmpty) {
        setState(() {
          _result = result;
        });
      }
    } catch (e) {
      print('Error invokeMethod "scanYUVImage"');
    }
    _processingStream = false;
  }

  _onPickupImageToScan() async {
    if (_controller == null || !_controllerInited) {
      return;
    }
    _controller.stopImageStream();
    setState(() {
      _processingStream = true;
      _processingImagePickup = true;
    });
    try {
      final imageFile =
          await ImagePicker.pickImage(source: ImageSource.gallery);
      if (imageFile != null) {
        final result = await QrReaderPlugin.scanByImageFile(imageFile.path);
        if (result != null && result.isNotEmpty) {
          setState(() {
            _result = result;
          });
        }
      }
    } catch (e) {
      print('Pickup Image Error');
    }finally{
      setState(() {
        _processingImagePickup = false;
        _processingStream = false;
      });
      _controller.startImageStream(_onLatestImageAvailable);
    }
  }

  @override
  void initState() {
    super.initState();
    _initCameraController();
  }

  @override
  void dispose() {
    // Dispose of the controller when the widget is disposed.
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text(_result)),
        body: FutureBuilder<void>(
          future: _initializeControllerFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.done) {
              if (_processingImagePickup){
                return Stack(
                  children: <Widget>[
                    CameraPreview(_controller),
                    Center(child: CircularProgressIndicator()),
                  ],
                );
              }else{
                return CameraPreview(_controller);
              }
            } else {
              // Otherwise, display a loading indicator.
              return Center(child: CircularProgressIndicator());
            }
          },
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.image),
          onPressed: () => _onPickupImageToScan(),
        ),
      ),
    );
  }
}
