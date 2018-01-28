package com.seapip.thomas.wearify;

import android.app.Application;

public class WearifyApplication extends Application {

    private static WearifyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static WearifyApplication getInstance() {
        return instance;
    }
}
