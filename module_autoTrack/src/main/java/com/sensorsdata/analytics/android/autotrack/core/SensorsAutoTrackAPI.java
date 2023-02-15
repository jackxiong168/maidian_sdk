/*
 * Created by dengshiwei on 2022/07/06.
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

package com.sensorsdata.analytics.android.autotrack.core;

import android.content.ContentProviderOperation;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.AnalyticsMessages;
import com.sensorsdata.analytics.android.sdk.DsyConstants;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.autotrack.AutoTrackModuleProtocol;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentVisualConfigsMD5;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SensorsAutoTrackAPI implements AutoTrackModuleProtocol {
    private AutoTrackContextHelper mAutoTrackHelper;
    private boolean mEnable = false;

    @Override
    public void install(SAContextManager contextManager) {
        try {
            mAutoTrackHelper = new AutoTrackContextHelper(contextManager);
            setModuleState(!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK());
            //todo 拉取可视化圈选配置 拉取失败处理？
            requestVisualConfigs(contextManager);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
    }

    @Override
    public String getModuleName() {
        return ModuleConstants.ModuleName.AUTO_TRACK_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean isServiceRunning() {
        return mEnable;
    }

    @Override
    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        return mAutoTrackHelper.invokeModuleFunction(methodName, argv);
    }

    private void requestVisualConfigs(SAContextManager contextManager) {
        String projectId = SensorsDataAPI.getConfigOptions().getProjectId();
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("projectId", projectId);
        SensorsDataAPI sensorsDataAPI = contextManager.getSensorsDataAPI();
        new RequestHelper.Builder(HttpMethod.GET,
                sensorsDataAPI.getServerUrl() + DsyConstants.PATH_VISUAL_CONFIGS + projectId)
                .connectionTimeout(15 * 1000)
                .header(headerMap)
                .readTimeout(15 * 1000)
                .callback(new HttpCallback.JsonCallback() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        SALog.i(SensorsAutoTrackAPI.class.getSimpleName(),
                                "可视化圈选配置获取失败:" + errorMessage);
                    }

                    @Override
                    public void onResponse(final JSONObject response) {
                        SALog.i(SensorsAutoTrackAPI.class.getSimpleName(),
                                "可视化圈选配置获取成功:" + response.toString());
                        if (response != null) {
                            SAEventManager.getInstance().trackQueueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    storeVisualConfigs(response);
                                }
                            });
                        }
                    }
                })
                .retryCount(3)
                .execute();
    }

    /**
     *
     * @param response {
     *     "id": "1049335005882351616",
     *     "creater": null,
     *     "createTime": "2022-12-05 14:42:41",
     *     "updater": null,
     *     "updateTime": null,
     *     "deleter": null,
     *     "deleteTime": null,
     *     "delFlag": 0,
     *     "eventName": "点击_商品映射01-￥0.01_Android_1.5.2.5",
     *     "type": "click",
     *     "version": "1.5.2.5",
     *     "projectId": "1048251169345896448",
     *     "pageSign": "nullcom.onebuygz.mvp.activity.HomeActivity|com.onebuygz.mvp.fragment
     *                 .fitup.CommodityModuleFragment/android.widget.LinearLayout[0]250438579",
     *     "screenName": "com.onebuygz.mvp.activity.HomeActivity|com.onebuygz.mvp.fragment.fitup
     *                 .CommodityModuleFragment",
     *     "elementId": "250438579",
     *     "elementPath": "/android.widget.LinearLayout[0]",
     *     "elementSelector": "android.widget.LinearLayout[0]/android.widget
     *                 .FrameLayout[0]/androidx.appcompat.widget
     *                 .FitWindowsLinearLayout[0]/androidx.appcompat.widget
     *                 .ContentFrameLayout[0]/android.widget.RelativeLayout[0]/androidx
     *                 .viewpager2.widget.ViewPager2[0]/androidx.viewpager2.widget.ViewPager2
     *                 .RecyclerViewImpl[0]/android.widget.FrameLayout[0]/android.widget
     *                 .FrameLayout[0]/com.onebuygz.mvp.view.special.SpecialLayout[0]/com.scwang
     *                 .smart.refresh.layout.SmartRefreshLayout[0]/androidx.coordinatorlayout
     *                 .widget.CoordinatorLayout[0]/androidx.viewpager2.widget
     *                 .ViewPager2[0]/androidx.viewpager2.widget.ViewPager2
     *                 .RecyclerViewImpl[0]/android.widget.FrameLayout[0]/com.scwang.smart
     *                 .refresh.layout.SmartRefreshLayout[0]/androidx.recyclerview.widget
     *                 .RecyclerView[0]/android.widget.LinearLayout[2]/android.widget
     *                 .LinearLayout[0]",
     *     "pageTitle": "测试版一码贵州"
     * }
     */
    private void storeVisualConfigs(JSONObject response) {
        try {
            int code = response.getInt("code");
            if (code == 200) {
                JSONArray jsonArray = response.getJSONArray("data");
                if (jsonArray == null || jsonArray.length() <= 0) {
                    DbAdapter.getInstance().deleteAllVisualConfigs();
                } else {

                    String newSign = AnalyticsMessages.md5(jsonArray.toString());
                    PersistentVisualConfigsMD5 oldSing =
                            PersistentLoader.getInstance().getVisualConfigsMD5();
                    if (newSign.equals(oldSing.get())) {
                        return;
                    }

                    DbAdapter.getInstance().deleteAllVisualConfigs();
                    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject =
                                (JSONObject) jsonArray.get(i);

                        String event_id = jsonObject.getString("id");

                        if (!TextUtils.isEmpty(event_id)) {
                            String screenName = jsonObject.getString(
                                    "screenName");
                            String elementSelector =
                                    jsonObject.getString(
                                            "elementSelector");
                            String event_type = jsonObject.getString(
                                    "type");
                            String elementPosition = jsonObject.getString("elementPath");

                            operations.add(ContentProviderOperation
                                    .newInsert(DbParams.getInstance().getVisualConfigsUri())
                                    .withValue(DbParams.KEY_SCREEN_NAME, screenName)
                                    .withValue(DbParams.KEY_ELEMENT_SELECTOR, elementSelector)
                                    .withValue(DbParams.KEY_EVENT_ID, event_id)
                                    .withValue(DbParams.KEY_EVENT_TYPE, event_type)
                                    .withValue(DbParams.KEY_ELEMENT_POSITION, elementPosition)
                                    .build());
                        }
                    }
                    DbAdapter.getInstance().addVisualConfigsBatch(operations);

                    oldSing.commit(newSign);
                    SALog.i(SensorsAutoTrackAPI.class.getSimpleName(),
                            "可视化圈选配置缓存完成,新MD5:" + newSign);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
