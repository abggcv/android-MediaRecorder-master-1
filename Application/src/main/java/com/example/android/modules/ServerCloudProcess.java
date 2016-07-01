package com.example.android.modules;
import android.util.Log;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import com.example.android.common.media.KibaFileManager;
// import android.gms.ads.identifier.AdvertisingIdClient;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONStringer;

/**
 * Created by vishal on 30/6/16.
 */


// Note this is a singleton class
public class ServerCloudProcess implements OnTaskCompletedInterface{

    // private static final String SQS_COMMAND_NAME = "DUMMY_FEAT_CALL";   // PROCESS_FEATURES
    private static final String SQS_COMMAND_NAME = "PROCESS_FEATURES";   //
    private static final String TAG = "ServerCloudProcess";
    private static ServerCloudProcess singleton = new ServerCloudProcess();
    private LinkedHashMap<String, PostProcEntry> postProcEntries = new LinkedHashMap<>();

    /* A private Constructor prevents any other
    * class from instantiating.
    */
    private ServerCloudProcess()
    {
        Thread t = new Thread(new MainProcessingLoop(this));
        t.start();
    }

    public void onTaskCompleted(boolean success, Object appData)
    {
        String entryID = (String)appData;
        PostProcEntry entry = postProcEntries.get(entryID);
        entry.dirtyBit = true;
        Log.e(TAG, "onTaskCompleted::entryID= "+ entryID + ". Stage= "+ entry.stageProcessing+". status=" + success);
        if (success)
            entry.stageProcessing++;
        postProcEntries.put(entryID, entry);

        return;
    }

    public static ServerCloudProcess getInstance() {
        if(singleton == null) {
            singleton = new ServerCloudProcess();
        }
        return singleton;
    }

    private class MainProcessingLoop implements Runnable {
        ServerCloudProcess context;
        public MainProcessingLoop(ServerCloudProcess context){
            this.context = context;
        }

        public void run() {
            try {
                while (true) {
                    Log.d(TAG, "MainProcessingLoop++");
                    AWSHandler.getInstance().ReceiveSQSMsg(KibaFileManager.deviceSQSQueueName());
                    for (Map.Entry<String, PostProcEntry> entry : postProcEntries.entrySet()) {
                        String key = entry.getKey();
                        PostProcEntry postProcEntry = entry.getValue();

                        //
                        if (postProcEntry.dirtyBit)
                        {
                            Log.d(TAG, "MainProcessingLoop:: To Process Entry : " + postProcEntry);
                            if (postProcEntry.stageProcessing == 0)
                            {
                                // Create audio clip
                                Log.d(TAG, "Next step: extract audio file");
                                MediaUtils.extractAudio(postProcEntry.vidFileName,
                                                        postProcEntry.audFileName,
                                                        this.context,
                                                        postProcEntry.id);
                                postProcEntry.dirtyBit = false;

                            }
                            else if (postProcEntry.stageProcessing == 1)
                            {
                                // Upload audio file
                                Log.d(TAG, "Next step: upload audio file");
                                AWSHandler.getInstance().uploadFile(postProcEntry.awsKeyAudFile,
                                                                    new File(postProcEntry.audFileName),
                                                                    this.context,
                                                                    postProcEntry.id);
                                postProcEntry.dirtyBit = false;
                            }
                            else if (postProcEntry.stageProcessing == 2)
                            {
                                // Upload audio file
                                Log.d(TAG, "Next step: upload yaml file");
                                AWSHandler.getInstance().uploadFile(postProcEntry.awsKeyYamlFile,
                                                                    new File(postProcEntry.yamlFileName),
                                                                    this.context,
                                                                    postProcEntry.id);
                                postProcEntry.dirtyBit = false;
                            }
                            else if (postProcEntry.stageProcessing == 3)
                            {
                                // Send sqs message
                                Log.d(TAG, "Next step: post SQS message.");
                                AWSHandler.getInstance().postSQS(postProcEntry.sqsMessage,
                                                                 this.context,
                                                                 postProcEntry.id);
                                postProcEntry.dirtyBit = false;
                            }
                            if (postProcEntry.stageProcessing == 4)
                            {
                                Log.d(TAG, "PROCESSING COMPLETED. Removing entry.");
                                // Remove this entry from the list
                                postProcEntries.remove(key);
                            }
                            else {
                                postProcEntries.put(key, postProcEntry);
                            }
                            break;
                        }
                    }
                    Log.d(TAG, "");
                    Log.d(TAG, "");

                    // Pause for 5 seconds
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "MainProcessingLoop:: Post-proc thread interuppted. Exiting.");
            }
        }
    }


    private String createSqsMessageForEntry(PostProcEntry postProcEntry) {
        String sqsMsgStr = "DEFAULT";
        try{

            JSONObject awsAppData = new JSONObject();
            awsAppData.put("folder", (new File(postProcEntry.vidFileName)).getParent());
            awsAppData.put("vidFile", postProcEntry.vidFileName);
            awsAppData.put("fps", 20.0);

            /*
            JSONObject emotConfParams = new JSONObject();
            emotConfParams.put("gridSize", "8x8");
            emotConfParams.put("faceBoxSize", "80x80");
            emotConfParams.put("s3RelLocFaceCollageCSVDescFile", <loc>);
            */

            JSONObject awsMsgParams = new JSONObject();
            awsMsgParams.put("s3BucketUrl", AWSHandler.S3_BUCKET_URL);
            awsMsgParams.put("s3RelLocFeatFile", postProcEntry.awsKeyYamlFile);
            awsMsgParams.put("s3RelLocAudFile", postProcEntry.awsKeyAudFile);
            awsMsgParams.put("userEmail", "default");
            awsMsgParams.put("sqsResponseQueue", KibaFileManager.deviceSQSQueueName());
            awsMsgParams.put("appData", awsAppData);
            awsMsgParams.put("userIdentity", "default");
            awsMsgParams.put("boolUseFolder", true);
            awsMsgParams.put("boolUpdateDb", false);
            awsMsgParams.put("processingType", (new JSONArray()).put("heuristic"));
            // awsMsgParams.put("sectionEmotionAnalysis", new JSONObject());


            JSONObject sqsMsg = new JSONObject();
            sqsMsg.put("id", KibaFileManager.genRandomUUID());
            sqsMsg.put("cmd", SQS_COMMAND_NAME);
            sqsMsg.put("params", awsMsgParams);
            sqsMsg.put("ver", "v2.0");

            sqsMsgStr = sqsMsg.toString();
        } catch (Exception e) {
            Log.e(TAG, "MainProcessingLoop:: Post-proc thread interuppted. Exiting.");
        }

        return sqsMsgStr;
    }

    public void pushToServerVidWithAdditionalFiles(String vidFilePath, HashMap<String, String> additionalFiles)
    {
        String yamlFilePath = additionalFiles.get("yamlFilePath");
        Log.e(TAG, "pushToServerVidWithAdditionalFiles::vidFile: " + vidFilePath);
        Log.e(TAG, "pushToServerVidWithAdditionalFiles::yamlFile: " + yamlFilePath);

        PostProcEntry postProcEntry = new PostProcEntry();
        postProcEntry.vidFileName = vidFilePath;
        postProcEntry.yamlFileName = yamlFilePath;

        File pathVidFile = new File(postProcEntry.vidFileName);
        postProcEntry.audFileName = pathVidFile.getParent() + File.separator + "raw_audio.m4a";

        postProcEntry.awsKeyAudFile = KibaFileManager.getAWSKeyForFile(new File(postProcEntry.audFileName));
        postProcEntry.awsKeyYamlFile = KibaFileManager.getAWSKeyForFile(new File(postProcEntry.yamlFileName));

        postProcEntry.sqsMessage = createSqsMessageForEntry(postProcEntry);

        File temp = new File(vidFilePath);
        String path = temp.getParent();
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        postProcEntry.id = idStr;
        postProcEntries.put(postProcEntry.id, postProcEntry);

        return;
    }


    class PostProcEntry {
        boolean dirtyBit;
        String  id;
        String  vidFileName;
        String  audFileName;
        String  yamlFileName;
        String  awsKeyAudFile;
        String  awsKeyYamlFile;
        String  sqsMessage;
        int     stageProcessing;

        public PostProcEntry()
        {
            dirtyBit = true;
            id = "-1";
            vidFileName = null;
            audFileName = null;
            yamlFileName = null;
            awsKeyAudFile = null;
            awsKeyYamlFile = null;
            sqsMessage = "DEFAULT";
            stageProcessing = 0;
        }

        public String toString() {
            return "id: "+ id +". vidFile: " + vidFileName+". dirtyBit: " + dirtyBit + ". stageProcessing: " + stageProcessing;
        }
    }
}

