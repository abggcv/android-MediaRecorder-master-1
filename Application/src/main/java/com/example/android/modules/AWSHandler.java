package com.example.android.modules;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.example.android.mediarecorder.KibaApp;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by vishal on 30/6/16.
 */
public class AWSHandler {
    private static String TAG = "AWSHANDLER";
    private static boolean mIsInit = false;

    public static final String IDENTITY_POOL_ID = "us-east-1:b8cdf50d-9098-4969-b4e3-1a433a96f1a7";
    public static final String S3_BUCKET_NAME = "taigadev";
    public static final String SQS_QUEUE_NAME = "queueTaigaProc-v2";
    public static final String S3_BUCKET_URL = "https://s3-us-west-1.amazonaws.com/taigadev";

    public static AWSHandler singleton = new AWSHandler();

    private AmazonSQS mSqsClient;
    private AmazonS3 mS3Client;
    private TransferUtility mTransferUtility;
    private CognitoCachingCredentialsProvider mCredentials;

    // Making it private, external entity can't call this
    private AWSHandler(){
        initConnection();
        return;
    }

    public static AWSHandler getInstance() {
        if(singleton == null) {
            singleton = new AWSHandler();
        }
        return singleton;
    }

    public void postSQS(String message, OnTaskCompletedInterface listener, Object appData){
        SQSSendTask task = new SQSSendTask(mSqsClient, message, listener, appData);
        task.execute();
    }

    public void uploadFile(final String key, final File file, OnTaskCompletedInterface listner, Object appdata) {
        Object appData = appdata;
        OnTaskCompletedInterface listener = listner;
        TransferObserver observer = mTransferUtility.upload(
                S3_BUCKET_NAME,
                key,
                file
        );

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "" + id + ":" + state);
                if (state == TransferState.COMPLETED) {
                    Log.d(TAG, "S3::Upload::Complete");
                    if (listener != null)
                        listener.onTaskCompleted(true, appData);
                    // file.delete();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, bytesCurrent * 100.0 / bytesTotal + "%");
                if (bytesCurrent == bytesTotal) {
                    Log.d(TAG, "S3::Upload::Total bytes:" + bytesTotal);
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "S3::ERROR::" + ex);
                if (listener != null)
                    listener.onTaskCompleted(false, appData);
            }
        });
    }


    private void initConnection() {
        try {
            if (! mIsInit) {
                mCredentials = new CognitoCachingCredentialsProvider(
                        KibaApp.get(),
                        IDENTITY_POOL_ID,
                        Regions.US_EAST_1
                );

                mS3Client = new AmazonS3Client(mCredentials);
                mTransferUtility = new TransferUtility(mS3Client, KibaApp.get());

                mSqsClient = new AmazonSQSClient(mCredentials);
                // mSqsClient.setRegion();
                mSqsClient.setEndpoint("https://sqs.us-west-1.amazonaws.com");

                mIsInit = true;
            }
        }
        catch (Exception e)
        {
            Log.d(TAG, "AWSHandler::init::" + e.toString());
            // Some exception occured. Try to reinitialize again at a later time
            mIsInit = false;
        }
    }

    public class SQSSendTask extends AsyncTask<Void, String, Boolean> {

        private AmazonSQS mClient;
        private String mMessage;
        private OnTaskCompletedInterface listener;
        private Object appData;

        SQSSendTask(AmazonSQS client, String message, OnTaskCompletedInterface listener, Object appData){
            mClient = client;
            mMessage = message;
            this.listener = listener;
            this.appData = appData;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(SQS_QUEUE_NAME);
                Map<String, String> queueAttrs = new HashMap<>();
                queueAttrs.put("VisibilityTimeout", "120");
                createQueueRequest.setAttributes(queueAttrs);
                String myQueueUrl = mClient.createQueue(createQueueRequest).getQueueUrl();
                mClient.sendMessage(new SendMessageRequest(myQueueUrl, mMessage));
                Log.d(TAG, "Posted successfully");
                return true;
            }catch (Exception e){
                Log.d(TAG, e.toString());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            listener.onTaskCompleted(success, appData);
        }
    }

    public Message ReceiveSQSMsg(String sqsQueueName){

        Message msgToProcess = null;
        try {
            if (!mIsInit)
                initConnection();

            if (mIsInit)
            {
                AmazonSQS client = mSqsClient;
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(sqsQueueName);
                Map<String, String> queueAttrs = new HashMap<>();
                queueAttrs.put("VisibilityTimeout", "120");
                createQueueRequest.setAttributes(queueAttrs);
                String myQueueUrl = client.createQueue(createQueueRequest).getQueueUrl();
                ReceiveMessageRequest receiveMsgReq = new ReceiveMessageRequest(myQueueUrl);
                receiveMsgReq.setMaxNumberOfMessages(1);
                receiveMsgReq.setVisibilityTimeout(120);
                List<Message> messages = client.receiveMessage(receiveMsgReq).getMessages();
                // Luckily aws defaults to reading one message at a time

                if (messages.size() > 0) {
                    msgToProcess = messages.get(0);
                    /*
                    Log.e(TAG,"  Message");
                    Log.e(TAG,"    MessageId:     " + msgToProcess.getMessageId());
                    Log.e(TAG,"    ReceiptHandle: " + msgToProcess.getReceiptHandle());
                    Log.e(TAG,"    MD5OfBody:     " + msgToProcess.getMD5OfBody());
                    Log.e(TAG,"    Body:          " + msgToProcess.getBody());
                    for (Map.Entry<String, String> entry : msgToProcess.getAttributes().entrySet()) {
                        Log.e(TAG,"  Attribute");
                        Log.e(TAG,"    Name:  " + entry.getKey());
                        Log.e(TAG,"    Value: " + entry.getValue());
                    }
                    */
                    Log.d(TAG, "ReceiveSQSMsg::Retrieved new message from SQS");
                }

            }
        }
        catch (AmazonServiceException ase)
        {
            Log.e(TAG, "ReceiveSQSMsg::Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            Log.e(TAG, "ReceiveSQSMsg::Error Message: " + ase.getMessage());

        } catch (AmazonClientException ace) {
            Log.e(TAG, "ReceiveSQSMsg::Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            Log.e(TAG, "ReceiveSQSMsg::Error Message: " + ace.getMessage());
        }

        return msgToProcess;
    }

    public void DeleteSQSMsg(String sqsQueueName, Message msgToDelete){

        try {
            if (!mIsInit)
                initConnection();

            if (mIsInit)
            {
                AmazonSQS client = mSqsClient;
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(sqsQueueName);
                Map<String, String> queueAttrs = new HashMap<>();
                queueAttrs.put("VisibilityTimeout", "120");
                createQueueRequest.setAttributes(queueAttrs);
                String myQueueUrl = client.createQueue(createQueueRequest).getQueueUrl();


                String messageReceiptHandle = msgToDelete.getReceiptHandle();
                mSqsClient.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
                Log.d(TAG, "DeleteSQSMsg::Successfully deleted a message.\n");
            }
        }
        catch (AmazonServiceException ase)
        {
            Log.e(TAG, "DeleteSQSMsg::Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            Log.e(TAG, "DeleteSQSMsg::Error Message: " + ase.getMessage());

        } catch (AmazonClientException ace) {
            Log.e(TAG, "Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            Log.e(TAG, "DeleteSQSMsg::Error Message: " + ace.getMessage());
        }

        return;
    }

}
