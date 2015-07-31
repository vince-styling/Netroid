package com.vincestyling.netroid.sample;

import android.app.Activity;
import android.os.Bundle;
import com.vincestyling.netroid.sample.netroid.Netroid;

public abstract class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initNetroid();
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    protected abstract void initNetroid();

    @Override
    public void finish() {
        Netroid.destroy();
        super.finish();
    }
}
