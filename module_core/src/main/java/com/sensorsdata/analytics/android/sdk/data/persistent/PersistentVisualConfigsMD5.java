package com.sensorsdata.analytics.android.sdk.data.persistent;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

/**
 * @author xiongwenjie
 * @time 2022/11/15 11:19
 * @des 可视化圈选配置信息字符串的MD5值，当拉取可视化后台圈选信息下来后，
 * 先对圈选信息进行MD5，然后和当前保存的MD5比对，两者不一致再保存到数据库中
 * @updateAuthor $
 * @updateDate $
 * @updateDes
 */

public class PersistentVisualConfigsMD5 extends PersistentIdentity<String>{
    PersistentVisualConfigsMD5() {
        super(DbParams.PersistentName.VISUAL_CONFIGS_SIGN, new PersistentSerializer<String>() {
            @Override
            public String load(String value) {
                return value;
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String create() {
                return null;
            }
        });
    }
}
