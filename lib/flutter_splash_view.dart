import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterMimoSplashView extends StatefulWidget {
  final String positionId;
  final int timeout;
  final Function onLoaded;
  final Function onError;
  final Function onClick;
  // final Function onTick;
  final Function onFinish;

  FlutterMimoSplashView(
    this.positionId, {
    this.timeout = 3000,
    this.onLoaded,
    this.onError,
    this.onClick,
    // this.onTick,
    this.onFinish,
  });

  @override
  _FlutterMimoSplashViewState createState() => _FlutterMimoSplashViewState();
}

class _FlutterMimoSplashViewState extends State<FlutterMimoSplashView> {
  MethodChannel _channel;
  int _channelId;
  bool loaded = false;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return _androidView();
    }

    if (defaultTargetPlatform == TargetPlatform.iOS) {
      return _iosView();
    }

    print('Xiaomi Splash 不支持的平台');
    return Container(width: 0, height: 0);
  }

  Future<dynamic> _onMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'adClicked':
        {
          widget.onClick?.call(() {
            _loadView();
          });
          break;
        }
      // case 'adTick':
      //   {
      //     int timeLeft = call.arguments;
      //     widget.onTick?.call(timeLeft);
      //     break;
      //   }
      case 'adDismissed':
        {
          widget.onFinish?.call();
          break;
        }
      case 'adError':
        {
          widget.onError?.call();
          break;
        }
      default:
        break;
    }
  }

  _loadView() async {
    if (_channel == null) {
      _channel =
          MethodChannel("flutter_xiaomi_ssp_splash_ad_view_" + _channelId.toString());
      _channel.setMethodCallHandler(_onMethodCall);
    }

    final result = await _channel.invokeMethod("renderSplashAd", {
      "positionId": widget.positionId,
      "timeout": widget.timeout,
    });

    if (mounted && loaded != result) {
      setState(() {
        loaded = result;
      });
    }

    if (result == true) {
      widget.onLoaded?.call(() {
        _loadView();
      });
    } else {
      widget.onError?.call(() {
        _loadView();
      });
    }
  }

  Widget _androidView() {
    return AndroidView(
      viewType: "flutter_xiaomi_ssp_splash_ad_view",
      onPlatformViewCreated: (int id) async {
        _channelId = id;
        _loadView();
      },
    );
  }

  Widget _iosView() {
    return UiKitView(
      viewType: "flutter_xiaomi_ssp_splash_ad_view",
      creationParamsCodec: new StandardMessageCodec(),
      onPlatformViewCreated: (int id) async {
        _channelId = id;
        _loadView();
      },
    );
  }
}
