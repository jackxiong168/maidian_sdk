package com.sensorsdata.analytics.android.sdk.visual.http;

import android.accounts.NetworkErrorException;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: zqf
 * Date: 2022/11/04
 */
public class Net {

    static ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * GET方法 返回数据会解析成字符串String
     * 开启一个线程来执行网络请求耗时操作任务
     *
     * @param urlString 请求的url
     * @param listen    回调监听
     */
    public static void get(final String urlString, final IListen listen) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection urlConnection = null;
                try {
                    url = new URL(urlString);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setRequestProperty("projectId", SensorsDataAPI.getConfigOptions().getProjectId());
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(8000);
                    if (urlConnection.getResponseCode() == 200) {
                        InputStream is = urlConnection.getInputStream();
                        BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder buffer = new StringBuilder();
                        String line = "";
                        while ((line = bf.readLine()) != null) {
                            buffer.append(line);
                        }
                        bf.close();
                        is.close();
                        listen.onFinish(buffer.toString());
                    } else {
                        listen.onError(new NetworkErrorException("response err code:" + urlConnection.getResponseCode()));
                    }
                } catch (MalformedURLException e) {
                    if (listen != null) {
                        listen.onError(e);
                    }
                } catch (IOException e) {
                    if (listen != null) {
                        listen.onError(e);
                    }
                } finally {
                    if (urlConnection != null) {
                        // 断开连接，释放资源
                        urlConnection.disconnect();
                    }
                }
            }
        });
    }
}
