package com.sensorsdata.analytics.android.sdk.visual.view;

import android.app.ProgressDialog;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Author: zqf
 * Date: 2022/11/14
 */
public class Loading {
    private static ProgressDialog mPd;
    private static WeakReference<Context> contextRef;

    public static void show(Context context) {
        if (contextRef == null || contextRef.get() != context) {
            contextRef = new WeakReference<>(context);
        }
        mPd = new ProgressDialog(contextRef.get());
        mPd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mPd.setMessage("App连接中...");
        mPd.setCancelable(false);
        mPd.setIndeterminate(false);
        mPd.setCanceledOnTouchOutside(false);
        if (mPd.isShowing()) {
            mPd.dismiss();
        }
        mPd.show();
    }

    public static void dismiss() {
        if (mPd != null && mPd.isShowing()) {
            mPd.dismiss();
        }
    }
}
