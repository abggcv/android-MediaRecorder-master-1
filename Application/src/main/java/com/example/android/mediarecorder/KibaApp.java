package com.example.android.mediarecorder;

import android.app.Application;

import com.example.android.modules.ServerPostProc;

/**
 * Created by vishal on 30/6/16.
 */
public class KibaApp extends Application {
    private static KibaApp instance;
    public static KibaApp get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize the PostProc engine
        ServerPostProc.getInstance();
    }

}
