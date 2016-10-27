package com.yeleman.snisrdcsms;

import com.orm.SugarApp;
import com.orm.SugarContext;

import android.util.Log;

public class App extends SugarApp {

    private static final String TAG = Constants.getLogTag("App");

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Init Database");
        SugarContext.init(this);
        Log.i(TAG, "DB inited");
    }

    @Override
    public void onTerminate() {
        SugarContext.terminate();
        super.onTerminate();
    }
}
