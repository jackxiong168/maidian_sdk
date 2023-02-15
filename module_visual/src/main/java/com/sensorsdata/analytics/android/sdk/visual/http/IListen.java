package com.sensorsdata.analytics.android.sdk.visual.http;

/**
 * Author: zqf
 * Date: 2022/11/04
 */
public interface IListen {
    void onFinish(String response);

    void onError(Exception e);
}
