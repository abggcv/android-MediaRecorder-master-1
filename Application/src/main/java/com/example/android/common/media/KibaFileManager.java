package com.example.android.common.media;

/**
 * Created by vishal on 29/6/16.
 */

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class KibaFileManager {
    private static final String TAG = "KibaFileManager";
    private static final File rootDirKibaData = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), "Kiba");
    private static String deviceUniqueID = null;

    public static File[] getNewRecordingMediaFileNames(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return  null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaStorageDir = new File(rootDirKibaData, timeStamp);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name

        File vidFile = new File(mediaStorageDir.getPath() + File.separator +
                "VID_"+ timeStamp + ".mp4");
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "vidFeatures.yaml");

        Log.e(TAG, "KibaFileManager::VidFileName= " + vidFile.getAbsolutePath());

        File[] out = {vidFile, mediaFile};

        return out;
    }

    public static String getAWSKeyForFile(File objFilePath)
    {
        String folderPath = objFilePath.getParent();
        String folderName = folderPath.substring(folderPath.lastIndexOf('/') + 1);
        String fileName = objFilePath.getName();
        String awsKey = "userdata/default/device/" + getUniqueDeviceID()
                + File.separator + folderName + File.separator + fileName;
        return awsKey;
    }

    public static String genRandomUUID(){
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    public static String deviceSQSQueueName()
    {
        String sqsQueueName = "queueRespANDR-" + KibaFileManager.getUniqueDeviceID();
        return sqsQueueName;
    }

    public static String getUniqueDeviceID() {

        if (deviceUniqueID != null)
        {
            return new String(deviceUniqueID);
        }
        else
        {
            String uniqueIden = "";
            File uniqueIDFile = new File(rootDirKibaData, ".id");
            if (uniqueIDFile.exists())
            {
                try {
                    // Read uniqueIDFromFile;
                    FileInputStream fIn = new FileInputStream(uniqueIDFile);
                    BufferedReader myReader = new BufferedReader(new InputStreamReader(
                            fIn));
                    String aDataRow = "";
                    while ((aDataRow = myReader.readLine()) != null) {
                        uniqueIden += aDataRow;
                    }
                    myReader.close();
                }
                catch (Exception e){

                }

            }
            if (uniqueIden.equals(""))
            {
                // Generate a new uniqueIden
                uniqueIden = genRandomUUID();
                try {
                    FileOutputStream out = new FileOutputStream(uniqueIDFile);
                    out.write(uniqueIden.getBytes());
                    out.close();
                }
                catch (Exception e) {

                }
            }
            deviceUniqueID = uniqueIden;

            Log.e(TAG, "deviceUniqueID: " + uniqueIden);
            return new String(deviceUniqueID);
        }
    }
}
