package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author xiongwenjie
 * @time 2022/11/14 14:00
 * @des 可视化后台圈选的配置表操作类
 * @updateAuthor $
 * @updateDate $
 * @updateDes
 */

class VisualConfigsDataOperation extends DataOperation {
    private Context mContext;

    VisualConfigsDataOperation(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            contentResolver.insert(uri, contentValues);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        return new String[0];
    }

    /**
     * 批量插入数据提高性能
     *
     * @param operations
     * @return
     */
    int insertInBatch(ArrayList<ContentProviderOperation> operations) {
        try {
            String packageName;
            try {
                packageName = mContext.getApplicationContext().getPackageName();
            } catch (UnsupportedOperationException e) {
                packageName = "com.sensorsdata.analytics.android.sdk.test";
            }
            ContentProviderResult[] results = contentResolver
                    .applyBatch(packageName + ".SensorsDataContentProvider", operations);
            return results.length;
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 可视化后台无差别圈选：可能会导致圈选的控件的路径比APP点击产生路径更具体，只能对element_selector模糊查询
     * 例如：一个groupView中包含一个ImageView和一个TextView，可视化后台可能圈选到的是ImageView或TextView，
     * 但APP实现的点击事件是groupView的
     *
     * @param screen_name
     * @param element_selector
     * @param type
     * @param element_position 列表item的集合位置,修复item复用时路径重复问题,一般是item点击事件才采集
     * @return
     */
    String queryEventId(String screen_name, String element_selector, String type,String element_position) {
        String event_id = "";
        Cursor cursor = null;
        try {
            String[] projection = {DbParams.KEY_EVENT_ID};
            String selection =
                    DbParams.KEY_SCREEN_NAME + " = ? and " + DbParams.KEY_EVENT_TYPE + " = ?";
            String[] selectionArgs = new String[]{screen_name, type};
            if (!TextUtils.isEmpty(element_position)){
                selection += " and " + DbParams.KEY_ELEMENT_POSITION + " = ?";
                selectionArgs=new String[]{screen_name, type,element_position};
            }
            if (!TextUtils.isEmpty(element_selector)) {
                selection += " and " + DbParams.KEY_ELEMENT_SELECTOR + " like '%" + element_selector + "%'";
            }
            cursor = contentResolver.query(DbParams.getInstance().getVisualConfigsUri(),
                    projection, selection, selectionArgs, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    event_id =
                            cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_EVENT_ID));
                    if (!TextUtils.isEmpty(event_id)) {
                        break;
                    }
                }
            }

        } catch (SQLiteException e) {
            SALog.i(TAG, "可视化圈选配置表查询失败:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return event_id;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }
}
