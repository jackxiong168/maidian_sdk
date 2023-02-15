/*
 * Created by dengshiwei on 2022/07/06.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.autotrack.core.autotrack;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.DsyConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.WeakSet;

import org.json.JSONObject;

import java.util.ServiceLoader;
import java.util.Set;

/**
 * Fragment 的页面浏览
 */
public class FragmentViewScreenCallbacks implements SAFragmentLifecycleCallbacks {

    private final static String TAG = "SA.FragmentViewScreenCallbacks";
    private final Set<Object> mPageFragments = new WeakSet<>();

    @Override
    public void onCreate(Object object) {

    }

    @Override
    public void onViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            //Fragment名称
            String fragmentName = object.getClass().getName();
            //如果复用的Fragment自定义了页面路径，用自定义的做缓存key，修复
            if (object instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) object;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        //复用的Fm，viewTag在trackFragmentAppViewScreen()中实时更新
                        fragmentName = trackProperties.optString(AopConstants.SCREEN_NAME);
                    } else {
                        traversViewForNoReuseFm(rootView, fragmentName);
                    }
                } else {
                    traversViewForNoReuseFm(rootView, fragmentName);
                }
            } else {
                traversViewForNoReuseFm(rootView, fragmentName);
            }
            //onViewCreated 相同的fragment只会设置最后一个实例的fragmentName，即前面实例的被覆盖掉了
            rootView.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);

            //获取所在的 Context
            Context context = rootView.getContext();
            //将 Context 转成 Activity
            Activity activity = SAViewUtils.getActivityOfView(context, rootView);
            if (activity != null) {
                Window window = activity.getWindow();
                if (window != null) {
                    window.getDecorView().getRootView().setTag(R.id.sensors_analytics_tag_view_fragment_name, "");
                }
            }
            //缓存 fragment 【实例】，这里后续只能获取最后一个实例，因为前面实例的fragmentName被覆盖了，
            // 实例不同，应用层自定义的AopConstants.SCREEN_NAME和DsyConstants.NESTED_FRAGMENT_SCREEN_NAME也就不同
            SAFragmentUtils.setFragmentToCache(fragmentName, object);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void traversViewForNoReuseFm(View rootView, String fragmentName) {
        if (rootView instanceof ViewGroup) {
            traverseView(fragmentName, (ViewGroup) rootView);
        }
    }

    @Override
    public void onStart(Object object) {

    }

    @Override
    public void onResume(Object object) {
        try {
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onPause(Object object) {
        if (object != null) {
            mPageFragments.remove(object);
        }
    }

    @Override
    public void onStop(Object object) {

    }

    @Override
    public void onHiddenChanged(Object object, boolean hidden) {
        try {
            if (object == null) {
                SALog.i(TAG, "fragment is null,return");
                return;
            }
            if (hidden) {
                mPageFragments.remove(object);
                SALog.i(TAG, "fragment hidden is true,return");
                return;
            }
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
        try {
            if (object == null) {
                SALog.i(TAG, "object is null");
                return;
            }
            if (!isVisibleToUser) {
                mPageFragments.remove(object);
                SALog.i(TAG, "fragment isVisibleToUser is false,return");
                return;
            }
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void trackFragmentAppViewScreen(Object fragment) {
        try {
            //onViewCreated中缓存的实例会被覆盖，导致点击前面实例的控件时，
            // 从缓存中获取的页面路径是最后一个实例自定义的页面路径而不是当前实例的，
            // 因此需要更新当前可见的Fm的view标签，根据view标签获取对应实例
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        String customScreenName =
                                trackProperties.optString(AopConstants.SCREEN_NAME);
                        View rootView = screenAutoTracker.rootViewOfFragment();
                        rootView.setTag(R.id.sensors_analytics_tag_view_fragment_name, customScreenName);
                        if (rootView instanceof ViewGroup) {
                            traverseView(customScreenName, (ViewGroup) rootView);
                        }
                        //缓存 fragment 【实例】
                        SAFragmentUtils.setFragmentToCache(customScreenName, fragment);
                    }
                }
            }
            //点击事件的页面路径
            JSONObject properties = SAPageTools.getFragmentPageInfo(null, fragment);
            //缓存Fragment页面路径，这里保存的路径只有可视化圈选时才用到
            AppStateTools.getInstance().setFragmentScreenName(fragment,
                    properties.optString(AopConstants.SCREEN_NAME), false);
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                //修复fragment不同场景下页面路径取值不唯一问题,不能在AppStateTools.getInstance()
                // .getFragmentPageInfo中覆盖，否则点击事件的页面路径会取到
                if (trackProperties.has(DsyConstants.NESTED_FRAGMENT_SCREEN_NAME)) {
                    String screenName =
                            trackProperties.getString(DsyConstants.NESTED_FRAGMENT_SCREEN_NAME);
                    if (!TextUtils.isEmpty(screenName)) {
                        AppStateTools.getInstance().setFragmentScreenName(fragment, screenName,
                                true);
                        //统计平台最终取的是AopConstants.SCREEN_NAME，这里仅在可视化圈选阶段
                        // 才将AopConstants.NESTED_FRAGMENT_SCREEN_NAME赋值给AopConstants.SCREEN_NAME
                        // 确保在圈选阶段同一界面中的多个Fragment页面路径统一指向最外层的组件
                        // 以此避免生产环境中切换子Fragment时重复统计该界面浏览事件的情况
                        ServiceLoader<SAModuleProtocol> serviceLoader =
                                ServiceLoader.load(SAModuleProtocol.class);
                        for (SAModuleProtocol saModuleProtocol : serviceLoader) {
                            if (ModuleConstants.ModuleName.VISUAL_NAME
                                    .equals(saModuleProtocol.getModuleName()) && saModuleProtocol.isServiceRunning()) {
                                properties.put(AopConstants.SCREEN_NAME, screenName);
                                break;
                            }
                        }
                    }
                }
                JSONUtils.mergeJSONObject(trackProperties, properties);
            }
            JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
            SensorsDataAPI.sharedInstance().trackViewScreen(SAPageTools.getScreenUrl(fragment),
                    eventProperties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private boolean isFragmentValid(Object fragment) {
        if (fragment == null) {
            SALog.i(TAG, "fragment is null,return");
            return false;
        }

        if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            SALog.i(TAG, "AutoTrackEventTypeIgnored,return");
            return false;
        }

        if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            SALog.i(TAG, "TrackFragmentAppViewScreenEnabled is false,return");
            return false;
        }

        if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
            SALog.i(TAG, "fragment is SupportRequestManagerFragment,return");
            return false;
        }

        boolean isAutoTrackFragment =
                SensorsDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
        if (!isAutoTrackFragment) {
            SALog.i(TAG, "fragment class ignored,return");
            return false;
        }
        //针对主动调用 fragment 生命周期，重复触发浏览
        if (mPageFragments.contains(fragment)) {
            SALog.i(TAG, "pageFragment contains,return");
            return false;
        }
        if (!SAFragmentUtils.isFragmentVisible(fragment)) {
            SALog.i(TAG, "fragment is not visible,return");
            return false;
        }
        return true;
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName) || root == null) {
                return;
            }
            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                String oldTag =
                        (String) child.getTag(R.id.sensors_analytics_tag_view_fragment_name);
                if (fragmentName.equals(oldTag)) {
                    //已经设置过
                    break;
                }
                child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                if (child instanceof ViewGroup && !(child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup)) {
                    traverseView(fragmentName, (ViewGroup) child);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
