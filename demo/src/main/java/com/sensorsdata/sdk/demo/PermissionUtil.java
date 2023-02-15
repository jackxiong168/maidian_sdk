package com.sensorsdata.sdk.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import com.sensorsdata.analytics.android.sdk.SALog;

import androidx.core.app.ActivityCompat;


/**
 * Author: zqf
 * Date: 2022/10/24
 */
public class PermissionUtil {
    private static final String TAG = "SA.PermissionUtil";
    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    public static boolean perMissRequest(Activity context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //未赋予申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //不开启权限时提示用户
                SALog.i(TAG, "请开通相关权限，否则无法正常使用本应用！");
            }
            //申请权限
            ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE, AppConstant.REQUEST_CODE);
        } else {
            //权限已赋予
            SALog.i(TAG, "已授权成功！");
            return true;
        }
        return false;
    }
}
