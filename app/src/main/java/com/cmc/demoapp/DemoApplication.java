package com.cmc.demoapp;


import android.app.Application;
import android.content.Context;

import timber.log.Timber;

public class DemoApplication extends Application {
    private static DemoApplication instance;

    public DemoApplication() {
        super();
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
//        if (BuildConfig.DEBUG) {
//            Timber.plant(new Timber.DebugTree());
//        }

        Timber.plant(new Timber.DebugTree());

    }

    public static Context getKoiAppContext() {
        return instance.getApplicationContext();
    }
}