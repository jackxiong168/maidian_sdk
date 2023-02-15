package com.sensorsdata.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author xiongwenjie
 * @time 2022/10/21 18:14
 * @des
 * @updateAuthor $
 * @updateDate $
 * @updateDes
 */

public class BActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_b);
        findViewById(R.id.button_b).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
