/*
 * Created by dengshiwei on 2022/06/28.
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

package com.sensorsdata.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private static final String TAG = "SA.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLambdaButton();
        initButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void test(View view) {
        TestUtil testUtil = new TestUtil();
        testUtil.testMethod();
        System.out.println("打的什么鬼");
    }

    private void initLambdaButton() {
        Button button = (Button) findViewById(R.id.lambdaButton);
        button.setOnClickListener(v -> {
            //                        startActivity(new Intent(MainActivity.this, BActivity.class));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String screenName = "com.onebuygz.mvp.activity.HomeActivity|com.onebuygz.mvp" +
                            ".fragment.mine.MineFragment";
                    String selector = "android.widget.LinearLayout[0]/android.widget" +
                            ".FrameLayout[0]/androidx.appcompat.widget" +
                            ".FitWindowsLinearLayout[0]/androidx.appcompat.widget" +
                            ".ContentFrameLayout[0]/android.widget" +
                            ".RelativeLayout[0]/androidx.viewpager2.widget" +
                            ".ViewPager2[0]/androidx.viewpager2.widget.ViewPager2" +
                            ".RecyclerViewImpl[0]/android.widget.FrameLayout[4]/android" +
                            ".widget.FrameLayout[0]/com.onebuygz.mvp.view.special" +
                            ".SpecialLayout[0]/com.scwang.smart.refresh.layout" +
                            ".SmartRefreshLayout[0]/androidx.coordinatorlayout.widget" +
                            ".CoordinatorLayout[0]/com.google.android.material.appbar" +
                            ".AppBarLayout[0]/android.widget.FrameLayout[0]/android" +
                            ".widget.LinearLayout[0]/androidx.constraintlayout.widget" +
                            ".ConstraintLayout[0]/androidx.constraintlayout.widget" +
                            ".ConstraintLayout[0]/android.widget.LinearLayout[0]";
                    /*JSONArray jsonArray = new JSONArray();
                    try {
                        for (int i = 0; i < 5; i++) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("screenName", screenName);
                            jsonObject.put("elementSelector", selector);
                            jsonObject.put("id", "20191825698421");
                            if (i == 0) {
                                jsonObject.put("type", DbParams.VISUAL_CONFIG_TYPE_BROWSE);
                            } else {
                                jsonObject.put("type", DbParams.VISUAL_CONFIG_TYPE_CLICK);
                            }
                            jsonArray.put(jsonObject);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    DbAdapter.getInstance().addVisualConfigs(jsonArray);*/
                    //批量插入
                    /*ArrayList<ContentProviderOperation> operations=new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        operations.add(ContentProviderOperation.newInsert(DbParams.getInstance().getVisualConfigsUri())
                                .withValue(DbParams.KEY_SCREEN_NAME, screenName)
                                .withValue(DbParams.KEY_ELEMENT_SELECTOR, selector)
                                .withValue(DbParams.KEY_EVENT_ID, "20191825698421")
                                .withValue(DbParams.KEY_EVENT_TYPE, i==0 ? DbParams.VISUAL_CONFIG_TYPE_BROWSE : DbParams.VISUAL_CONFIG_TYPE_CLICK)
                                .build());
                    }
                    DbAdapter.getInstance().addVisualConfigsBatch(operations);*/
                    //查询
//                    DbAdapter.getInstance().deleteAllVisualConfigs();
//                    String clickEventId = DbAdapter.getInstance()
//                            .queryVisualConfigEventId(screenName, selector,
//                                    DbParams.VISUAL_CONFIG_TYPE_CLICK);
//                    SALog.i(DbAdapter.class.getSimpleName(), "查询出来点击事件的eventId:" + clickEventId);
//                    String browseEventId = DbAdapter.getInstance()
//                            .queryVisualConfigEventId(screenName, null,
//                                    DbParams.VISUAL_CONFIG_TYPE_BROWSE);
//                    SALog.i(DbAdapter.class.getSimpleName(), "查询出来浏览事件的eventId:" + browseEventId);
                }
            }).start();
        });
    }

    private void initButton() {
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                Uri uri = Uri.parse(AppConstant.localTestScanConnectUrl);
//                intent.setData(uri);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
            }
        });
    }
}
