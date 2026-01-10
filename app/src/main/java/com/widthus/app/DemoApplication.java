package com.widthus.app;


import android.app.Application;
import android.content.Context;

import com.kakao.sdk.common.KakaoSdk;
import com.withus.app.BuildConfig;

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

        KakaoSdk.init(this, "94d0bc6cedd010ec852d5c8be7c9113a");

//        if (BuildConfig.DEBUG) {
//            Timber.plant(new Timber.DebugTree());
//        }

        Timber.plant(new Timber.DebugTree());

    }

    public static Context getKoiAppContext() {
        return instance.getApplicationContext();
    }
}