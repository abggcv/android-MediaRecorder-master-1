package com.example.android.mediarecorder;

import android.app.Application;

import com.example.android.modules.ServerCloudListener;
import com.example.android.modules.ServerCloudProcess;

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
        ServerCloudProcess.getInstance();
        ServerCloudListener.getInstance();
    }

}
