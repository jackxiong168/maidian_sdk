package com.sensorsdata.sdk.demo;

/**
 * Author: zqf
 * Date: 2022/10/24
 * debug 常量类
 */
public class AppConstant {

    //测试的服务地址 https://buryingpoint.gcongo.com.cn/
    public static String serverUrl = "https://buryingpoint.gcongo.com.cn";
    //本地测试扫码连接地址
    public static final String localTestFeatureCode = "ae541474-1cac-49ae-8b22-977ac4930002";
    private static final String localTestScanScheme = "testsenexample://visualized?feature_code=" + localTestFeatureCode + "&url=";
    public static String localTestScanConnectUrl = localTestScanScheme + serverUrl + "/app/upload/";
    //request permission code
    public static final int REQUEST_CODE = 101;

}
