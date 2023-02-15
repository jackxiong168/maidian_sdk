package com.sensorsdata.analytics.android.sdk.visual.http;

import android.app.Activity;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.ToastUtil;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;
import com.sensorsdata.analytics.android.sdk.visual.view.Loading;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Author: zqf
 * Date: 2022/11/07
 */
public class NetWrapper {
    private static final String TAG = "SA.NetReq";
    private volatile static NetWrapper instance = null;

    private NetWrapper() {
    }

    public static NetWrapper getInstance() {
        if (instance == null) {
            synchronized (NetWrapper.class) {
                if (instance == null) {
                    instance = new NetWrapper();
                }
            }
        }
        return instance;
    }

    /**
     * 连接状态确认
     * /connect/{sign} sdk 二维码连接
     *
     * @param context     上下文
     * @param featureCode featureCode
     * @param postUrl     postUrl
     */
    public void get(final Activity context, final String featureCode, final String postUrl) {
        //disconnect();
        String url = SensorsDataAPI.sharedInstance().getServerUrl() + "/connect/" + featureCode;
        SALog.i(TAG, "connect web visual url address: >> " + url);
        Loading.show(context);
        Net.get(url, new IListen() {
            @Override
            public void onFinish(String response) {
                Loading.dismiss();
                SALog.i(TAG, "response: >> " + response);
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optInt("code") == 200) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                VisualizedAutoTrackService.getInstance().start(context, featureCode, postUrl);
                                SensorsDataDialogUtils.startLaunchActivity(context);
                                //SAStoreManager.getInstance().setString("sign", featureCode);
                            }
                        });
                    } else {
                        ToastUtil.showShort(context, json.optString("msg"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    ToastUtil.showLong(context, "连接异常，请扫码重试！");
                }
            }

            @Override
            public void onError(Exception e) {
                Loading.dismiss();
                SALog.i(TAG, "Exception: >> " + e);
                ToastUtil.showLong(context, "连接异常，请扫码重试！");
            }
        });
    }

    /**
     * 可视化断开连接
     * /connect/disconnect/{sign} sdk
     */
    public void disconnect() {
        try {
            String sign = SAStoreManager.getInstance().getString("sign", "");
            if (!TextUtils.isEmpty(sign)) {
                String url = SensorsDataAPI.sharedInstance().getServerUrl() + "/connect/disconnect/" + sign;
                new RequestHelper.Builder(HttpMethod.GET, url).callback(new HttpCallback.StringCallback() {
                    @Override
                    public void onFailure(int code, String msg) {
                        SALog.i(TAG, "reqDisconnect error Msg: >> " + msg);
                    }

                    @Override
                    public void onResponse(String response) {
                        SALog.i(TAG, "reqDisconnect success: >> " + response);
                    }

                    @Override
                    public void onAfter() {

                    }
                }).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
