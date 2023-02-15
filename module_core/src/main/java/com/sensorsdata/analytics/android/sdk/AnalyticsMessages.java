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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.exceptions.ResponseErrorException;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;


/**
 * Manage communication of events with the internal database and the SensorsData servers.
 * This class straddles the thread boundary between user threads and
 * a logical SensorsData thread.
 */
public class AnalyticsMessages {
    private static final String TAG = "SA.AnalyticsMessages";
    private static final int FLUSH_QUEUE = 3;
    private static final int DELETE_ALL = 4;
    private static final int FLUSH_SCHEDULE = 5;
    private static final Map<Context, AnalyticsMessages> S_INSTANCES = new HashMap<>();
    private final Worker mWorker;
    private final Context mContext;
    private final DbAdapter mDbAdapter;
    private final SensorsDataAPI mSensorsDataAPI;
    private final InternalConfigOptions mInternalConfigs;

    /**
     * 不要直接调用，通过 getInstance 方法获取实例
     */
    private AnalyticsMessages(final Context context, SensorsDataAPI sensorsDataAPI, InternalConfigOptions internalConfigOptions) {
        mContext = context;
        mDbAdapter = DbAdapter.getInstance();
        mWorker = new Worker();
        mSensorsDataAPI = sensorsDataAPI;
        mInternalConfigs = internalConfigOptions;
    }

    /**
     * 获取 AnalyticsMessages 对象
     *
     * @param messageContext Context
     */
    public static AnalyticsMessages getInstance(final Context messageContext, final SensorsDataAPI sensorsDataAPI, InternalConfigOptions internalConfigOptions) {
        synchronized (S_INSTANCES) {
            final Context appContext = messageContext.getApplicationContext();
            final AnalyticsMessages ret;
            if (!S_INSTANCES.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext, sensorsDataAPI, internalConfigOptions);
                S_INSTANCES.put(appContext, ret);
            } else {
                ret = S_INSTANCES.get(appContext);
            }
            return ret;
        }
    }

    public static AnalyticsMessages getInstance(Context context) {
        return S_INSTANCES.get(context);
    }

    private static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public void flushEventMessage(boolean isSendImmediately) {
        try {
            synchronized (mDbAdapter) {
                final Message m = Message.obtain();
                m.what = FLUSH_QUEUE;
                if (isSendImmediately) {
                    mWorker.runMessage(m);
                } else {
                    mWorker.runMessageOnce(m, mSensorsDataAPI.getFlushInterval());
                }
            }
        } catch (Exception e) {
            SALog.i(TAG, "enqueueEventMessage error:" + e);
        }
    }

    public void flush() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_QUEUE;

            mWorker.runMessage(m);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void flushScheduled() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_SCHEDULE;

            mWorker.runMessageOnce(m, mSensorsDataAPI.getFlushInterval());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void deleteAll() {
        try {
            final Message m = Message.obtain();
            m.what = DELETE_ALL;

            mWorker.runMessage(m);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void sendData() {
        try {
            if (!mSensorsDataAPI.isNetworkRequestEnable()) {
                SALog.i(TAG, "NetworkRequest is disabled");
                return;
            }

            if (TextUtils.isEmpty(mSensorsDataAPI.getServerUrl())) {
                SALog.i(TAG, "Server url is null or empty.");
                return;
            }

            //无网络
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return;
            }

            //不符合同步数据的网络策略
            String networkType = NetworkUtils.networkType(mContext);
            if (!NetworkUtils.isShouldFlush(networkType, mSensorsDataAPI.getFlushNetworkPolicy())) {
                SALog.i(TAG, String.format("Invalid NetworkType = %s", networkType));
                return;
            }

            // 如果开启多进程上报
            if (mInternalConfigs.saConfigOptions.isMultiProcessFlush()) {
                // 已经有进程在上报
                if (DbAdapter.getInstance().isSubProcessFlushing()) {
                    return;
                }
                DbAdapter.getInstance().commitSubProcessFlushState(true);
            } else if (!mInternalConfigs.isMainProcess) {//不是主进程
                return;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return;
        }
        int count = 100;
        while (count > 0) {
            boolean deleteEvents = true;
            String[] eventsData;
            synchronized (mDbAdapter) {
                if (mSensorsDataAPI.isDebugMode()) {
                    /* debug 模式下服务器只允许接收 1 条数据 */
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 1);
                } else {
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 50);
                }
            }

            if (eventsData == null) {
                DbAdapter.getInstance().commitSubProcessFlushState(false);
                return;
            }

            final String lastId = eventsData[0];
            final String rawMessage = eventsData[1];
            final String gzip = eventsData[2];
            String errorMessage = null;

            try {
                String data = rawMessage;
                //
                if (DbParams.GZIP_DATA_EVENT.equals(gzip)) {
                    data = encodeData(rawMessage);
                }

                if (!TextUtils.isEmpty(data)) {
                    sendHttpRequest(mSensorsDataAPI.getServerUrl(), data, gzip, rawMessage, false);
                }
            } catch (ConnectErrorException e) {
                deleteEvents = false;
                errorMessage = "Connection error: " + e.getMessage();
            } catch (InvalidDataException e) {
                errorMessage = "Invalid data: " + e.getMessage();
            } catch (ResponseErrorException e) {
                deleteEvents = isDeleteEventsByCode(e.getHttpCode());
                errorMessage = "ResponseErrorException: " + e.getMessage();
            } catch (Exception e) {
                deleteEvents = false;
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                boolean isDebugMode = mSensorsDataAPI.isDebugMode();
                if (!TextUtils.isEmpty(errorMessage)) {
                    if (isDebugMode || SALog.isLogEnabled()) {
                        SALog.i(TAG, errorMessage);
                        if (isDebugMode && mInternalConfigs.isShowDebugView) {
                            SensorsDataDialogUtils.showHttpErrorDialog(AppStateTools.getInstance().getForegroundActivity(), errorMessage);
                        }
                    }
                }
                if (deleteEvents || isDebugMode) {
                    count = mDbAdapter.cleanupEvents(lastId);
                    SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "Events flushed. [left = %d]", count));
                } else {
                    count = 0;
                }

            }
        }
        if (mInternalConfigs.saConfigOptions.isMultiProcessFlush()) {
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }
    }

    private void sendHttpRequest(String path, String data, String gzip, String rawMessage, boolean isRedirects) throws ConnectErrorException, ResponseErrorException {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            final URL url = new URL(path+ DsyConstants.PATH_EVENT_DATA_REPORT);
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                SALog.i(TAG, String.format("can not connect %s, it shouldn't happen", url));
                return;
            }
            /*SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
            if (configOptions != null && configOptions.mSSLSocketFactory != null
                    && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(configOptions.mSSLSocketFactory);

            }
            connection.setInstanceFollowRedirects(false);
            if (mSensorsDataAPI.getDebugMode() == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                connection.addRequestProperty("Dry-Run", "true");
            }

            String cookie = mSensorsDataAPI.getCookie(false);
            if (!TextUtils.isEmpty(cookie)) {
                connection.setRequestProperty("Cookie", cookie);
            }

            Uri.Builder builder = new Uri.Builder();
            //先校验crc
            if (!TextUtils.isEmpty(data)) {
                builder.appendQueryParameter("crc", String.valueOf(data.hashCode()));
            }

            builder.appendQueryParameter("gzip", gzip);
            builder.appendQueryParameter("data_list", data);

            String query = builder.build().getEncodedQuery();
            if (TextUtils.isEmpty(query)) {
                return;
            }*/

            connection.addRequestProperty("Content-Type","application/json");
            String projectId = SensorsDataAPI.getConfigOptions().getProjectId();
            connection.addRequestProperty("projectId",projectId);
            String md5 = md5(data);
            if (TextUtils.isEmpty(md5)) {
                return;
            }
            JSONObject jsonObject=new JSONObject();
            try {
                jsonObject.put("dataAbstract",md5);
                jsonObject.put("data",data);
//                jsonObject.put("gzip", gzip);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            String query = jsonObject.toString();
            connection.setFixedLengthStreamingMode(query.getBytes(CHARSET_UTF8).length);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            //设置连接超时时间
            connection.setConnectTimeout(30 * 1000);
            //设置读取超时时间
            connection.setReadTimeout(30 * 1000);
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);
            bout.write(query.getBytes(CHARSET_UTF8));
            bout.flush();

            int responseCode = connection.getResponseCode();
            SALog.i(TAG, "responseCode: " + responseCode);
            if (!isRedirects && NetworkUtils.needRedirects(responseCode)) {
                String location = NetworkUtils.getLocation(connection, path);
                if (!TextUtils.isEmpty(location)) {
                    closeStream(bout, out, null, connection);
                    sendHttpRequest(location, data, gzip, rawMessage, true);
                    return;
                }
            }
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            in.close();
            in = null;

            String response = new String(responseBody, CHARSET_UTF8);
            if (SALog.isLogEnabled()) {
                String jsonMessage = JSONUtils.formatJson(rawMessage);
                // 状态码 200 - 300 间都认为正确
                if (responseCode >= HttpURLConnection.HTTP_OK &&
                        responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    SALog.i(TAG, "valid message: \n" + jsonMessage);
                } else {
                    SALog.i(TAG, "invalid message: \n" + jsonMessage);
                    SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "ret_code: %d", responseCode));
                    SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "ret_content: %s", response));
                }
            }
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误
                throw new ResponseErrorException(String.format("flush failure with response '%s', the response code is '%d'",
                        response, responseCode), responseCode);
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 在服务器正常返回状态码的情况下，目前只有 (>= 500 && < 600) || 404 || 403 才不删数据
     *
     * @param httpCode 状态码
     * @return true: 删除数据，false: 不删数据
     */
    private boolean isDeleteEventsByCode(int httpCode) {
        boolean shouldDelete = httpCode != HttpURLConnection.HTTP_NOT_FOUND &&
                httpCode != HttpURLConnection.HTTP_FORBIDDEN &&
                (httpCode < HttpURLConnection.HTTP_INTERNAL_ERROR || httpCode >= 600);
        return shouldDelete;
    }

    private void closeStream(BufferedOutputStream bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                SALog.i(TAG, e.getMessage());
            }
        }
    }

    private String encodeData(final String rawMessage) throws InvalidDataException {
        GZIPOutputStream gos = null;
        try {
            byte[] bytes = rawMessage.getBytes(CHARSET_UTF8);
            ByteArrayOutputStream os = new ByteArrayOutputStream(bytes.length);
            gos = new GZIPOutputStream(os);
            gos.write(bytes);
            gos.close();
            byte[] compressed = os.toByteArray();
            os.close();
            return new String(Base64Coder.encode(compressed));
        } catch (IOException exception) {
            // 格式错误，直接将数据删除
            throw new InvalidDataException(exception);
        } finally {
            if (gos != null) {
                try {
                    gos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    // Worker will manage the (at most single) IO thread associated with
    // this AnalyticsMessages instance.
    // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
    private class Worker {

        private final Object mHandlerLock = new Object();
        private final Handler mHandler;

        Worker() {
            final HandlerThread thread =
                    new HandlerThread("com.sensorsdata.analytics.android.sdk.AnalyticsMessages.Worker",
                            Thread.MIN_PRIORITY);
            thread.start();
            mHandler = new AnalyticsMessageHandler(thread.getLooper());
        }

        void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    SALog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    SALog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
                        mHandler.sendMessageDelayed(msg, delay);
                    }
                }
            }
        }

        private class AnalyticsMessageHandler extends Handler {

            AnalyticsMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (msg.what == FLUSH_QUEUE) {
                        sendData();
                    } else if (msg.what == DELETE_ALL) {
                        try {
                            mDbAdapter.deleteAllEvents();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    } else if (msg.what == FLUSH_SCHEDULE) {
                        flushScheduled();
                        sendData();
                    } else {
                        SALog.i(TAG, "Unexpected message received by SensorsData worker: " + msg);
                    }
                } catch (final RuntimeException e) {
                    SALog.i(TAG, "Worker threw an unhandled exception", e);
                }
            }
        }
    }
}