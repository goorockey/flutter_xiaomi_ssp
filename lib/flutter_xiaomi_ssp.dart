import 'dart:async';

import 'package:flutter/services.dart';

class FlutterXiaomiSsp {
  static const MethodChannel _channel =
      const MethodChannel('flutter_xiaomi_ssp');

  static Future<bool> initSDK(final String appId, {
    final bool enableUpdate,
    final bool debug,
    final bool staging,
  }) async {
    return await _channel.invokeMethod('initSDK', {
      "appId": appId,
      "enableUpdate": enableUpdate ?? false,
      "debug": debug ?? false,
      "staging": staging ?? false,
    });
  }

  static Future<int> loadRewardVideoAd(final String positionId) async {
    return await _channel.invokeMethod('loadRewardVideoAd', {
      "positionId": positionId,
    });
  }
}
