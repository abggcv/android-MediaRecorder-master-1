package com.example.android.modules;

import android.util.Log;

import com.example.android.common.media.KibaFileManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.sqs.model.Message;

/**
 * Created by vishal on 1/7/16.
 */
public class ServerCloudListener implements OnTaskCompletedInterface {

    private static final String TAG = "ServerCloudListener";
    private static ServerCloudListener singleton = new ServerCloudListener();

    private ServerCloudListener()
    {
        Thread t = new Thread(new CloudProcessingLoop(this));
        t.start();
    }

    public void onTaskCompleted(boolean success, Object appData) {
        Log.e(TAG, "onTaskCompleted::status= " + success + ". vidFile= " + (String)appData);
    }

    public static ServerCloudListener getInstance() {
        if(singleton == null) {
            singleton = new ServerCloudListener();
        }
        return singleton;
    }

    private class CloudProcessingLoop implements Runnable {
        ServerCloudListener context;

        public CloudProcessingLoop(ServerCloudListener context){
            this.context = context;
        }

        public void run() {
            try {
                while (true) {
                    try {
                        Log.d(TAG, "CloudProcessingLoop++");
                        Message msgToProcess = AWSHandler.getInstance().ReceiveSQSMsg(KibaFileManager.deviceSQSQueueName());

                        if (msgToProcess != null) {
                            /*
                            {"cmd": "CREATECLIPS_FROM_POINTS",
                          "params": {"clipPoints": [2, 2, 2, 2],
                                        "appData": {"folder": "/storage/emulated/0/Documents/Kiba/20160701_082007",
                                                   "vidFile": "/storage/emulated/0/Documents/Kiba/20160701_082007/VID_20160701_082007.mp4",
                                                       "fps": 20}},
                              "id": "d71c95bb-9137-4d7c-b284-aaf7b0043ba5"}
                            */

                            String jsonString = msgToProcess.getBody();


                            JSONObject msg = new JSONObject(jsonString);

                            String cmd = (String)msg.get("cmd");
                            // String id = (String)msg.get("id");

                            if (cmd.equals("CREATECLIPS_FROM_POINTS")) {
                                JSONObject params = (JSONObject)msg.get("params");
                                JSONObject appData = (JSONObject)params.get("appData");
                                String folder = (String)appData.get("folder");
                                String vidFile = (String) appData.get("vidFile");
                                JSONArray clipPoints = (JSONArray) params.get("clipPoints");
                                Double   fps = appData.getDouble("fps");

                                List<Double> listClipPointsInSec = new ArrayList<>();
                                for (int i=0; i<clipPoints.length(); i++) {
                                    listClipPointsInSec.add(clipPoints.getDouble(i)/1000.0f);
                                }

                                Log.e(TAG, "Planning to process: " + vidFile + ". fps= " + fps + ". folder= " + folder);
                                String strSplitPoints = "";
                                for (int i=0; i<listClipPointsInSec.size(); i++)
                                {
                                    strSplitPoints = strSplitPoints + listClipPointsInSec.get(i).doubleValue() + ", ";
                                }
                                Log.e(TAG, "Attempting to split: " + vidFile + " at " + strSplitPoints);
                                MediaUtils.splitVideo(vidFile, folder, listClipPointsInSec, this.context, vidFile, 1);
                                Log.e(TAG, "splitVideo call returned");
                            }

                            // Delete a message
                            //System.out.println("Deleting a message.\n");
                            //String messageReceiptHandle = messages.get(0).getReceiptHandle();
                            //mSqsClient.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));


                        /*
                        System.out.println("Deleting a message.\n");
                        String messageReceiptHandle = msgToProcess.getReceiptHandle();
                        mSqsClient.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
                        */
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "ERROR: " + e.toString());
                    }
                    // Pause for 5 seconds
                    Thread.sleep(5000);
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "CloudProcessingLoop:: Post-proc thread interuppted. Exiting.");
            }
        }
    }

}
