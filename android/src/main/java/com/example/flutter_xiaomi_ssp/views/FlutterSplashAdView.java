package com.example.flutter_xiaomi_ssp.views;

import android.util.Log;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.os.Build;

import com.example.flutter_xiaomi_ssp.Consts;

import java.lang.reflect.Field;
import java.util.HashMap;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.BinaryMessenger;

import com.xiaomi.ad.common.pojo.AdType;
import com.miui.zeus.mimo.sdk.ad.FloatAd;
import com.miui.zeus.mimo.sdk.ad.AdWorkerFactory;
import com.miui.zeus.mimo.sdk.ad.IAdWorker;
import com.miui.zeus.mimo.sdk.listener.MimoAdListener;

public class FlutterSplashAdView implements PlatformView, MethodChannel.MethodCallHandler {
    private LinearLayout mLinearLayout;
    private Activity mActivity;
    private IAdWorker mAdWorker;
    private MethodChannel methodChannel;

    FlutterSplashAdView(Activity activity, BinaryMessenger messenger, int id) {
        methodChannel = new MethodChannel(messenger, "flutter_xiaomi_ssp_splash_ad_view_" + id);
        methodChannel.setMethodCallHandler(this);
        this.mActivity = activity;
        if (mLinearLayout == null) {
            mLinearLayout = new LinearLayout(activity);
        }
    }

    @Override
    public View getView() {
        if (mActivity != null && mLinearLayout == null) {
            mLinearLayout = new LinearLayout(mActivity);
        }

        // 为了让platformView的背景透明
        if (mLinearLayout != null) {
            mLinearLayout.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ViewParent parent = mLinearLayout.getParent();
                        if (parent == null) {
                            return;
                        }
                        while (parent.getParent() != null) {
                            parent = parent.getParent();
                        }
                        Object decorView = parent.getClass().getDeclaredMethod("getView").invoke(parent);
                        final Field windowField = decorView.getClass().getDeclaredField("mWindow");
                        windowField.setAccessible(true);
                        final Window window = (Window) windowField.get(decorView);
                        windowField.setAccessible(false);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE);
                            window.setLocalFocus(true, true);
                        }
                    } catch (Exception e) {
                        // log the exception
                    }
                }
            });
        }
        return mLinearLayout;
    }

    @Override
    public void dispose() {
        try {
            methodChannel.setMethodCallHandler(null);

            if (mAdWorker != null) {
                mAdWorker.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        if (Consts.FunctionName.RENDER_SPLASH_AD.equals(methodCall.method)) {
            renderSplashAd(methodCall, result);
        }
    }

    private void renderSplashAd(MethodCall call, final MethodChannel.Result result) {
        try {
            String positionId = (String) call.argument(Consts.ParamKey.POSITION_ID);

            if (TextUtils.isEmpty(positionId)) {
                Log.i(Consts.TAG, "Xiaomi splash empty positionId");
                return;
            }

            if (mAdWorker != null) {
                try {
                    mAdWorker.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mAdWorker = AdWorkerFactory.getAdWorker(mActivity, mLinearLayout, new MimoAdListener() {
                @Override
                public void onAdPresent() {
                    Log.i(Consts.TAG, "Xiaomi splash onAdPresent");

                    try {
                        result.success(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAdClick() {
                    Log.i(Consts.TAG, "Xiaomi splash onAdClick");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adClicked", null);
                        }
                    });
                }

                @Override
                public void onAdDismissed() {
                    Log.i(Consts.TAG, "Xiaomi splash onAdDismissed");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adDismissed", null);
                        }
                    });
                }

                @Override
                public void onAdFailed(String message) {
                    Log.i(Consts.TAG, String.format("Xiaomi splash onAdFailed, %s", message));

                    try {
                        result.success(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAdLoaded(int size) {
                    Log.i(Consts.TAG, "Xiaomi splash onAdLoaded");
                }

                @Override
                public void onStimulateSuccess() {
                    Log.i(Consts.TAG, "Xiaomi splash onStimulateSuccess");
                }
            }, AdType.AD_SPLASH);

            mAdWorker.loadAndShow(positionId);
        } catch (Exception e) {
            try {
                result.success(false);
            } catch (Exception e1) {
                e.printStackTrace();
            }
        }
    }
}
