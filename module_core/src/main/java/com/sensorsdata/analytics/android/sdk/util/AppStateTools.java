/*
 * Created by dengshiwei on 2022/07/05.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AppStateTools implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks {
    private static final String TAG = "SA.AppStateTools";
    // current front activity
    private WeakReference<Activity> mForeGroundActivity = new WeakReference<>(null);
    // current front fragment
    private String mCurrentFragmentName = null;
    private int mCurrentRootWindowsHashCode = -1;
    private int mActivityCount = 0;
    private final List<AppState> mAppStateList = new ArrayList<>();

    public static AppStateTools getInstance() {
        return SingleHolder.mSingleInstance;
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    private static class SingleHolder {
        private static final AppStateTools mSingleInstance = new AppStateTools();
    }

    private AppStateTools() {
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        setForegroundActivity(activity);
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivityCount++ == 0) {
            for (AppState appState : mAppStateList) {
                try {
                    appState.onForeground();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForegroundActivity(activity);
        View decorView = null;
        try {
            Window window = activity.getWindow();
            if (window != null) {
                decorView = window.getDecorView();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (!activity.isChild()) {
            if (decorView != null) {
                mCurrentRootWindowsHashCode = decorView.hashCode();
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivityCount--;
        if (mActivityCount == 0) {
            for (AppState appState : mAppStateList) {
                try {
                    appState.onBackground();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public void delayInit(Context context) {
        try {
            // delay init state count =1
            if (context instanceof Activity) {
                onActivityStarted((Activity) context);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public Activity getForegroundActivity() {
        return this.mForeGroundActivity.get();
    }

    private void setForegroundActivity(Activity activity) {
        this.mForeGroundActivity = new WeakReference(activity);
    }

    /**
     *
     * @param fragment 当前fragment
     * @param fragmentScreenName
     * @param forceSet fragment通过布局文件渲染或同一界面同时渲染多个子Fragment时设为 true
     */
    public void setFragmentScreenName(Object fragment, String fragmentScreenName,boolean forceSet) {
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            Object parentFragment = getParentFragmentMethod.invoke(fragment);
            // 如果存在 fragment 多层嵌套场景，只取父 fragment
            // todo 子fragment放入父fragment布局中使用或子fragment在自定义控件中动态加载，
            //  父fragment再在布局中使用该自定义控件时，无法获取parentFragment，
            //  因为自定义控件或布局中的fragment依附的是宿主activity，此时mCurrentFragmentName取到的是最后渲染的那个。
            //  只有通过在父fragment中使用fragmentManager加载的子fragment获取的父fragment才不为空
            //  针对这类情况，为了确保在可视化圈选阶段浏览事件页面路径的唯一性，只能由应用层开发者为这类子Fragment定义
            //  统一的页面路径:ScreenAutoTracker.getTrackProperties函数中的 AopConstants.NESTED_FRAGMENT_SCREEN_NAME
            if (parentFragment == null || forceSet) {
                mCurrentFragmentName = fragmentScreenName;
                SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is not nested fragment and set");
            } else {
                //父控件不为空，可视化圈选时取activity的
                SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is nested fragment and ignored");
            }
        } catch (Exception e) {
            //ignored
        }
    }

    public void addAppStateListener(AppState appState) {
        mAppStateList.add(appState);
    }

    public boolean isAppOnForeground() {
        return mActivityCount != 0;
    }

    public String getFragmentScreenName() {
        return mCurrentFragmentName;
    }

    public int getCurrentRootWindowsHashCode() {
        if (this.mCurrentRootWindowsHashCode == -1 && this.mForeGroundActivity != null && this.mForeGroundActivity.get() != null) {
            Activity activity = this.mForeGroundActivity.get();
            if (activity != null) {
                Window window = activity.getWindow();
                if (window != null && window.isActive()) {
                    this.mCurrentRootWindowsHashCode = window.getDecorView().hashCode();
                }
            }
        }
        return this.mCurrentRootWindowsHashCode;
    }

    public interface AppState {
        void onForeground();

        void onBackground();
    }
}
