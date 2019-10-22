package com.example.flutter_xiaomi_ssp;

import android.content.Context;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;


import com.example.flutter_xiaomi_ssp.views.FlutterBannerAdViewFactory;
import com.example.flutter_xiaomi_ssp.Consts;

import com.miui.zeus.mimo.sdk.MimoSdk;
import com.miui.zeus.mimo.sdk.api.IMimoSdkListener;
import com.miui.zeus.mimo.sdk.ad.AdWorkerFactory;
import com.miui.zeus.mimo.sdk.ad.IRewardVideoAdWorker;
import com.miui.zeus.mimo.sdk.listener.MimoRewardVideoListener;
import com.xiaomi.ad.common.pojo.AdType;

/** FlutterXiaomiSspPlugin */
public class FlutterXiaomiSspPlugin implements MethodCallHandler {
  private final Registrar mRegistrar;
  private IRewardVideoAdWorker mVideoAdWorker;
  private final MethodChannel mChannel;

  public FlutterXiaomiSspPlugin(Registrar registrar) {
    this.mRegistrar = registrar;
    mChannel = new MethodChannel(registrar.messenger(), "flutter_xiaomi_ssp");
    mChannel.setMethodCallHandler(this);
  }

  void dispose() {
    mChannel.setMethodCallHandler(null);
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    new FlutterXiaomiSspPlugin(registrar);

    registrar.platformViewRegistry().registerViewFactory("flutter_xiaomi_ssp_banner_ad_view",
        new FlutterBannerAdViewFactory(new StandardMessageCodec(), registrar.activity(), registrar.messenger()));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("initSDK")) {
      this.initSDK(call, result);
    } else if ("checkPermissions".equals(call.method)) {
      this.checkPermission(call, result);
    } else if ("loadRewardVideoAd".equals(call.method)) {
      this.loadRewardVideoAd(call, result);
    } else {
      result.notImplemented();
    }
  }

  private static final class VIDEO_RESULT_TYPE {
    static final int VIDEO_FAILED = 1;
    static final int VIDEO_ERROR = 2;
    static final int VIDEO_CLOSE = 3;
    static final int VIDEO_COMPLETE = 4;
    static final int VIDEO_REWARD_VERIFIED = 5;
  }

  private void loadRewardVideoAd(MethodCall call, final Result result) {
    try {
      String positionId = call.argument("positionId");
      if (positionId == null || positionId.isEmpty()) {
        try {
          result.success(VIDEO_RESULT_TYPE.VIDEO_FAILED);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return;
      }

      mVideoAdWorker = AdWorkerFactory.getRewardVideoAdWorker(mRegistrar.activity().getApplicationContext(), positionId,
          AdType.AD_REWARDED_VIDEO);

      mVideoAdWorker.setListener(new MimoRewardVideoListener() {
        private boolean mVideoComplete = false;

        @Override
        public void onVideoStart() {
          Log.i(Consts.TAG, "Xiaomi video ad onVideoStart");
        }

        @Override
        public void onVideoPause() {
          Log.i(Consts.TAG, "Xiaomi video ad onVideoPause");
        }

        @Override
        public void onVideoComplete() {
          Log.i(Consts.TAG, "Xiaomi video ad onVideoComplete");
          mVideoComplete = true;
        }

        @Override
        public void onAdPresent() {
          Log.i(Consts.TAG, "Xiaomi video ad onAdPresent");
        }

        @Override
        public void onAdClick() {
          Log.i(Consts.TAG, "Xiaomi video ad onAdClick");
        }

        @Override
        public void onAdDismissed() {
          Log.i(Consts.TAG, "Xiaomi video ad onAdDismissed");
          try {
            mVideoAdWorker.recycle();
            result.success(mVideoComplete ? VIDEO_RESULT_TYPE.VIDEO_COMPLETE : VIDEO_RESULT_TYPE.VIDEO_CLOSE);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onAdFailed(String message) {
          Log.e(Consts.TAG, "Xiaomi video ad onAdFailed : " + message);
          try {
            mVideoAdWorker.recycle();
            result.success(VIDEO_RESULT_TYPE.VIDEO_ERROR);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onAdLoaded(int size) {
          Log.i(Consts.TAG, "Xiaomi video ad onAdLoaded : " + size);
          try {
            mVideoAdWorker.show();
          } catch (Exception e) {
            e.printStackTrace();
            result.success(VIDEO_RESULT_TYPE.VIDEO_ERROR);
          }
        }

        @Override
        public void onStimulateSuccess() {
          Log.i(Consts.TAG, "Xiaomi video ad onStimulateSuccess");
        }
      });
      if (!mVideoAdWorker.isReady()) {
        mVideoAdWorker.load();
      }
    } catch (Exception e) {
      e.printStackTrace();
      try {
        result.success(VIDEO_RESULT_TYPE.VIDEO_FAILED);
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }
  }

  void initSDK(final MethodCall call, final Result result) {
    try {
      String appId = call.argument("appId");
      if (appId == null || appId.isEmpty()) {
        try {
          result.success(false);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return;
      }

      Boolean enableUpdate = call.argument("enableUpdate");
      if (enableUpdate != null) {
        MimoSdk.setEnableUpdate(enableUpdate);
      }

      Boolean debug = call.argument("debug");
      if (debug != null) {
        MimoSdk.setDebug(debug);
      }

      Boolean staging = call.argument("staging");
      if (staging != null) {
        MimoSdk.setStaging(staging);
      }

      MimoSdk.init(mRegistrar.activity().getApplicationContext(), appId, "fake_app_key", "fake_app_token",
          new IMimoSdkListener() {
            @Override
            public void onSdkInitSuccess() {
              try {
                result.success(true);
              } catch (Exception e) {
                e.printStackTrace();
              }

            }

            @Override
            public void onSdkInitFailed() {
              try {
                result.success(false);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
      try {
        result.success(false);
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    }

  }

  private void checkPermission(MethodCall call, Result result) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      this.checkAndRequestPermission(result);
    } else {
      try {
        result.success(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private ArrayList<String> getNeedPermissionList() {
    Activity activity = mRegistrar.activity();

    ArrayList<String> lackedPermission = new ArrayList<String>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if ((activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)) {
        lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
      }

      if ((activity
          .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
        lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      }

      if ((activity
          .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
        lackedPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
      }
    }
    return lackedPermission;
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void checkAndRequestPermission(Result result) {
    List<String> lackedPermission = getNeedPermissionList();

    // 权限都已经有了，那么直接调用SDK
    if (lackedPermission.size() == 0) {
      try {
        result.success(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      // 请求所缺少的权限，在onRequestPermissionsResult中再看是否获得权限，如果获得权限就可以调用SDK，否则不要调用SDK。
      try {
        result.success(false);
      } catch (Exception e) {
        e.printStackTrace();
      }
      String[] requestPermissions = new String[lackedPermission.size()];
      lackedPermission.toArray(requestPermissions);

      Activity activity = mRegistrar.activity();
      activity.requestPermissions(requestPermissions, 1024);
    }
  }
}
