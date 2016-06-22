package com.example.android.mediarecorder;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by mbp on 5/9/16.
 */
public class FFmpegAccessor {

    public interface ResponseHandler {
        public void onStart();
        public void onFinish(boolean success, boolean cancelled);
        public void onProgress(String message);
    }

    private class ExecuteTask extends AsyncTask<Void, String, Integer>{
        private String mCommand;
        private Process mProcess;
        private ResponseHandler mHandler;

        ExecuteTask(String command, ResponseHandler handler){
            mCommand = command;
            mHandler = handler;
        }

        @Override
        protected void onPreExecute() {
            if (mHandler != null) {
                mHandler.onStart();
            }
        }
        @Override
        protected void onProgressUpdate(String... values) {
            if (values != null && values[0] != null && mHandler != null) {
                mHandler.onProgress(values[0]);
            }
        }
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                mProcess = Runtime.getRuntime().exec(mCommand);

                do{
                    String line;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mProcess.getErrorStream()));
                    while((line = reader.readLine()) != null){
                        Log.d("t", line);
                        publishProgress(line);
                    }

                }while(!isProcessCompleted());

            } catch (IOException e) {
                return -1;
            }
            return mProcess.exitValue();
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mHandler != null) {
                if(result == 0){
                    mHandler.onFinish(true, false);
                }
                else {
                    mHandler.onFinish(false, false);
                }
            }
        }

        private boolean isProcessCompleted(){
            try {
                if(mProcess == null) return true;
                mProcess.exitValue();
                return true;
            }
            catch(IllegalThreadStateException e){
            }
            return false;
        }
    }

    private ExecuteTask mTask;
    private Context context;
    private static FFmpegAccessor _instance = null;
    private String mArchName;
    private String mPath;

    private FFmpegAccessor(Context context){
        this.context = context.getApplicationContext();
    }
    public static FFmpegAccessor sharedInstance(Context context){
        if(_instance == null){
            _instance = new FFmpegAccessor(context);
        }
        return _instance;
    }

    public int load(){
        if(Build.CPU_ABI.equals("x86")){
            mArchName = "x86";
        }
        else if(Build.CPU_ABI.equals("armeabi-v7a")){
            mArchName = "arm";
        }
        else if(Build.CPU_ABI.equals("arm64-v8a")){
            mArchName = "arm64";
        }
        else {
            Log.e("cp ffmpeg", "does not support CPU Type:" + Build.CPU_ABI);
            return -1;
        }

        mPath = context.getFilesDir().getAbsolutePath()+ File.separator+"ffmpeg";
        File ffmpegFile = new File(mPath);

        long fs = ffmpegFile.length();
        if(ffmpegFile.exists()) return 0;

        if(!ffmpegFile.exists()){
            try {
                FileOutputStream os = new FileOutputStream(mPath);
                InputStream is = context.getAssets().open("ffmpeg_bin" + File.separator + mArchName + File.separator + "ffmpeg");
                byte[] buf = new byte[1024*1024];
                int read = 0;
                while(-1 != (read = is.read(buf))){
                    os.write(buf, 0, read);
                }
                os.close();
                is.close();
            }
            catch(IOException e) {
                Log.e("cp ffmpeg", "failed to copy ffmpeg", e);
            }
        }
        if(ffmpegFile.exists()) {
            if(!ffmpegFile.canExecute()) {
                ffmpegFile.setExecutable(true);
            }
        }


        if(ffmpegFile.exists() && ffmpegFile.canExecute()) return 0;
        return -1;
    }

    public int execute(String cmd, ResponseHandler handler){
        if(mTask != null && !mTask.isProcessCompleted()){
            return -1;
        }
        String command = mPath + " " + cmd;
        mTask = new ExecuteTask(command, handler);
        mTask.execute();

        return 0;
    }

    public int execute(String cmd){
        if(mTask != null && !mTask.isProcessCompleted()){
            return -1;
        }
        String command = mPath + " " + cmd;
        mTask = new ExecuteTask(command, null);
        mTask.execute();

        return 0;
    }
    public void waitCurrentTask() throws InterruptedException {
        mTask.wait();
    }


}
