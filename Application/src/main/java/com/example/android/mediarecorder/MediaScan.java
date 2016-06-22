package com.example.android.mediarecorder;

/**
 * Created by ABGG on 05/11/2015.
 */

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;


public class MediaScan implements MediaScannerConnection.MediaScannerConnectionClient {

    private MediaScannerConnection mMs;
    private File mFile;

    public MediaScan(Context context, File f){

        mFile = f;
        mMs = new MediaScannerConnection(context, this);
        mMs.connect();
    }

    @Override
    public void onMediaScannerConnected(){
        mMs.scanFile(mFile.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri){
        mMs.disconnect();
    }

}
