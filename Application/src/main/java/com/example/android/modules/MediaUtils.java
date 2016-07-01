package com.example.android.modules;

import com.example.android.mediarecorder.FFmpegAccessor;
import com.example.android.mediarecorder.KibaApp;

import java.io.File;
import java.util.List;

/**
 * Created by vishal on 1/7/16.
 */
public class MediaUtils {
    public static void extractAudio(String videoPath, String outputAudPath, OnTaskCompletedInterface listener, Object appData){

        File fileAudPath = new File(outputAudPath);
        File dir = new File(fileAudPath.getParent());
        dir.mkdirs();

        String command = "-i " + videoPath + " -vn -acodec copy -y " + outputAudPath;

        FFmpegAccessor.sharedInstance(KibaApp.get()).execute(command, new FFmpegAccessor.ResponseHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish(boolean success, boolean cancelled) {
                if (listener != null)
                    listener.onTaskCompleted(success, appData);
                if(!success) {
                    File incomplete = new File(outputAudPath);
                    incomplete.delete();
                }
            }

            @Override
            public void onProgress(String message) {

            }
        });

    }

    public static void splitVideo(final String videoPath, final String outputDirectory, List<Integer> splitInfo, OnTaskCompletedInterface listener, Object appData){
        if(splitInfo.isEmpty()) {
            if (listener != null)
                listener.onTaskCompleted(true, appData);
            return;
        }

        File dir = new File(outputDirectory);
        dir.mkdirs();
        File video = new File(videoPath);

        Integer startSec = splitInfo.get(0);
        final String fileName =  video.getName() + "out_x"  + Integer.toString(startSec) + ".mp4";
        final String outputPath = outputDirectory+File.separator+fileName;

        String command = "-ss " + Integer.toString(startSec) + " -t 20 " + "-i " + videoPath + " -c copy -y " + outputPath;
        splitInfo.remove(0);

        final List<Integer> nextSplit = splitInfo;
        FFmpegAccessor.sharedInstance(KibaApp.get()).execute(command, new FFmpegAccessor.ResponseHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish(boolean success, boolean cancelled) {
                if(! success) {
                    File incomplete = new File(outputPath);
                    incomplete.delete();
                }
                MediaUtils.splitVideo(videoPath, outputDirectory, nextSplit, listener, appData);

            }

            @Override
            public void onProgress(String message) {

            }
        });

    }


}
