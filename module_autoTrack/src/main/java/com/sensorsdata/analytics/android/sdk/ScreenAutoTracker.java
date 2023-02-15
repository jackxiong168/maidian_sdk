/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk;

import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

public interface ScreenAutoTracker {
    /**
     * 返回当前页面的Url
     * 用作下个页面的referrer
     *
     * @return String
     */
    String getScreenUrl();

    /**
     * 返回自定义属性集合
     * 我们内置了两个属性:
     * $screen_name,代表当前页面名称, 默认情况下,该属性会采集当前Activity的CanonicalName,即:
     * activity.getClass().getCanonicalName()
     *
     * $nested_fragment_screen_name:即DsyConstants.NESTED_FRAGMENT_SCREEN_NAME
     * 场景一（横向）：通过viewpage等组件预加载多个Fragment时，如果Fragment相同，那么这些Fragment需要自定义
     * 不同的 DsyConstants.NESTED_FRAGMENT_SCREEN_NAME确保浏览事件页面路径不同；
     * 不同的 AopConstants.SCREEN_NAME确保点击事件页面路径不同；
     * 场景二（纵向）：同一界面内有多个Fragment，那么这些Fragment需要自定义
     * 相同的 DsyConstants.NESTED_FRAGMENT_SCREEN_NAME确保浏览事件页面路径唯一；
     * 不同的 AopConstants.SCREEN_NAME确保点击事件页面路径不同；
     *
     * 如果想自定义页面名称, 可以在Map里put该key进行覆盖。
     *注意:两个key的前面必须要要加"$"符号
     *
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getTrackProperties() throws JSONException;

    View rootViewOfFragment();
}
