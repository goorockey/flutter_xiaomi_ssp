package com.example.flutter_xiaomi_ssp.views;

import android.util.Log;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.Gravity;
import android.widget.LinearLayout;

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

public class FlutterBannerAdView implements PlatformView, MethodChannel.MethodCallHandler {
    private LinearLayout mLinearLayout;
    private Activity mActivity;
    private MethodChannel methodChannel;
    private IAdWorker mTemplateAdWorker;

    FlutterBannerAdView(Activity activity, BinaryMessenger messenger, int id) {
        methodChannel = new MethodChannel(messenger, "flutter_xiaomi_ssp_banner_ad_view_" + id);
        methodChannel.setMethodCallHandler(this);
        this.mActivity = activity;
        if (mLinearLayout == null) {
            mLinearLayout = new LinearLayout(activity);
        }
    }

    @Override
    public View getView() {
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

            if (mTemplateAdWorker != null) {
                mTemplateAdWorker.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        if (Consts.FunctionName.RENDER_BANNER_AD.equals(methodCall.method)) {
            renderBannerAd(methodCall, result);
        }
    }

    private void renderBannerAd(MethodCall call, final MethodChannel.Result result) {
        try {
            String positionId = (String) call.argument(Consts.ParamKey.POSITION_ID);

            if (TextUtils.isEmpty(positionId)) {
                Log.i(Consts.TAG, "Xiaomi banner empty positionId");
                return;
            }

            if (mTemplateAdWorker != null) {
                try {
                    mTemplateAdWorker.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mTemplateAdWorker = AdWorkerFactory.getAdWorker(mActivity, mLinearLayout, new MimoAdListener() {
                @Override
                public void onAdPresent() {
                    Log.i(Consts.TAG, "Xiaomi banner onAdPresent");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adLoaded", null);
                        }
                    });
                }

                @Override
                public void onAdClick() {
                    Log.i(Consts.TAG, "Xiaomi banner onAdClick");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adClicked", null);
                        }
                    });
                }

                @Override
                public void onAdDismissed() {
                    Log.i(Consts.TAG, "Xiaomi banner onAdDismissed");
                }

                @Override
                public void onAdFailed(String message) {
                    Log.i(Consts.TAG, "Xiaomi banner onAdFailed");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adError", null);
                        }
                    });
                }

                @Override
                public void onAdLoaded(int size) {
                    Log.i(Consts.TAG, "Xiaomi banner onAdLoaded");
                }

                @Override
                public void onStimulateSuccess() {
                    Log.i(Consts.TAG, "Xiaomi banner onStimulateSuccess");
                }
            }, AdType.AD_BANNER);

            mTemplateAdWorker.loadAndShow(positionId);

            try {
                result.success(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();

            try {
                result.success(false);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
