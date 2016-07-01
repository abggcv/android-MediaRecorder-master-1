package com.example.android.mediarecorder;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class CaptureListActivity extends ListActivity {
    public static final String IDENTITY_POOL_ID = "us-east-1:b8cdf50d-9098-4969-b4e3-1a433a96f1a7";
    public static final String BUCKET_NAME = "taigadev";
    public static final String QUEUE_NAME = "android-trial";

    private List<String> mPaths;
    FFmpegAccessor mFFmpeg;

    private AmazonSQS mSqsClient;
    private AmazonS3 mS3Client;
    private TransferUtility mTransferUtility;
    private CognitoCachingCredentialsProvider mCredentials;

    public class SQSTask extends AsyncTask<Void, String, Integer>{

        private AmazonSQS mClient;
        private String mMessage;

        SQSTask(AmazonSQS client, String message){
            mClient = client;
            mMessage = message;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                Log.d("SQS", "Point 1");
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(QUEUE_NAME);
                Log.d("SQS", "Point 2");
                if (mClient == null)
                {
                    Log.d("SQS", "Point 2.5 (NULL)");
                }
                String myQueueUrl = mClient.createQueue(createQueueRequest).getQueueUrl();
                Log.d("SQS", "Point 3");
                mClient.sendMessage(new SendMessageRequest(myQueueUrl, mMessage));
                Log.d("SQS", "Posted successfully");
                return 0;
            }catch (Exception e){
                Log.d("SQS", e.toString());
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_list);

        mCredentials = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                IDENTITY_POOL_ID, // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        mS3Client = new AmazonS3Client(mCredentials);
        if (mS3Client == null)
        {
            Log.e("SQS", "Empty mS3Client");
        }
        else
        {
            Log.d("SQS", "Non Empty mS3Client");
        }

        mTransferUtility = new TransferUtility(mS3Client, getApplicationContext());

        if (mTransferUtility == null)
        {
            Log.e("SQS", "Empty mTransferUtility");

        }
        else
        {
            Log.d("SQS", "Non Empty mTransferUtility");
        }

        File localFile1 = new File("/storage/emulated/0/Documents/Kiba/20160630_102713/VID_20160630_102713.mp4");
        this.uploadFile(localFile1.getName(), localFile1);

        File localFile2 = new File("/storage/emulated/0/Documents/Kiba/20160630_102733/VID_20160630_102733.mp4");
        this.uploadFile(localFile2.getName(), localFile2);

        mSqsClient = new AmazonSQSClient(mCredentials);
        // mSqsClient.setRegion();
        mSqsClient.setEndpoint("https://sqs.us-west-1.amazonaws.com");

        if (mSqsClient == null)
        {
            Log.e("SQS", "Empty mSqsClient");
        }
        else
        {
            Log.d("SQS", "Non Empty mSqsClient");
        }

        postSQS("Test Msg");


        mFFmpeg = FFmpegAccessor.sharedInstance(getApplicationContext());

        mPaths = new ArrayList<String>();
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraSample");

        mPaths.clear();
        for(File file : mediaStorageDir.listFiles()){
            mPaths.add(file.getAbsolutePath());
        }

        this.extractAudio(localFile1.getAbsolutePath(), localFile1.getParent());

        Log.e("SQS", "Audio extraction done");

        ListViewAdapter adapter = new ListViewAdapter(this, R.layout.list_items, mPaths);
        setListAdapter(adapter);
        this.registerForContextMenu(this.getListView());

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String path = mPaths.get(position);
        super.onListItemClick(l, v, position, id);
//        this.onCreateContextMenu(null, v, null);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
//        getMenuInflater().inflate(R.menu.main_context, menu);
        menu.add(0, 0, 0, "Audio Extract");
        menu.add(0, 1, 0, "Video Trim");
        menu.add(0, 2, 0, "Upload to S3");
        menu.add(0, 3, 0, "Post SQS");

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        String path = mPaths.get(info.position);

        if(item.getItemId() == 0){
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "CameraSample");
            final String outputPath = mediaStorageDir + File.separator + "AudioOut";
            this.extractAudio(path, outputPath);
        }
        else if(item.getItemId() == 1){

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "CameraSample");
            final String outputPath = mediaStorageDir + File.separator + "SplitOut";

            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(2);
            list.add(4);
            list.add(6);
            this.splitVideo(path, outputPath, list);

        }
        else if(item.getItemId() == 2){
            File f = new File(path);
            this.uploadFile(f.getName(), f);
        }
        else if(item.getItemId() == 3){
            postSQS(path);
        }

        return super.onContextItemSelected(item);

    }


    private void extractAudio(String videoPath, String outputDirectory){
        File dir = new File(outputDirectory);
        dir.mkdirs();
        File video = new File(videoPath);


//        final String fileName = DateFormat.format("ssmmkkddMMyyyy", Calendar.getInstance()) + ".m4a";
        final String fileName = video.getName() + "_audio" + ".m4a";
        final String outputPath = outputDirectory+File.separator+fileName;
        String command = "-i " + videoPath + " -vn -acodec copy -y " + outputPath;

        final CaptureListActivity me = this;
        mFFmpeg.execute(command, new FFmpegAccessor.ResponseHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish(boolean success, boolean cancelled) {
                if(success) {
                    me.uploadFile(fileName, new File(outputPath));
                }
                else {
                    File incomplete = new File(outputPath);
                    incomplete.delete();
                }
            }

            @Override
            public void onProgress(String message) {

            }
        });

    }

    private void splitVideo(final String videoPath, final String outputDirectory, List<Integer> splitInfo){
        if(splitInfo.isEmpty()) return;

        File dir = new File(outputDirectory);
        dir.mkdirs();
        File video = new File(videoPath);

        Integer startSec = splitInfo.get(0);
        final String fileName =  video.getName() + "_split_"  + Integer.toString(startSec) + ".mp4";
        final String outputPath = outputDirectory+File.separator+fileName;

        String command = "-ss " + Integer.toString(startSec) + " -t 20 " + "-i " + videoPath + " -c copy -y " + outputPath;
        splitInfo.remove(0);

        final List<Integer> nextSplit = splitInfo;
        final CaptureListActivity me = this;
        mFFmpeg.execute(command, new FFmpegAccessor.ResponseHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish(boolean success, boolean cancelled) {
                if(success) {
                    me.uploadFile(fileName, new File(outputPath), !nextSplit.isEmpty());
                    me.splitVideo(videoPath, outputDirectory, nextSplit);
                }
                else {
                    File incomplete = new File(outputPath);
                    incomplete.delete();
                }
            }

            @Override
            public void onProgress(String message) {

            }
        });

    }

    public void postSQS(String message){
        SQSTask task = new SQSTask(mSqsClient, message);
        task.execute();
    }

    private void uploadFile(final String key, final File file) {this.uploadFile(key, file, false);}
    private void uploadFile(final String key, final File file, final boolean sendSQSAtTheEnd) {
        TransferObserver observer = mTransferUtility.upload(
                BUCKET_NAME,
                key,
                file
        );

        final CaptureListActivity me = this;
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d("S3", "" + id + ":" + state);
                if (state == TransferState.COMPLETED) {
                    if(sendSQSAtTheEnd) {
                        me.postSQS("Complete!");
                    }
                    Log.d("S3", "Complete");
                    // file.delete();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("S3", bytesCurrent * 100.0 / bytesTotal + "%");
                if (bytesCurrent == bytesTotal) {
                    Log.d("S3", "Total bytes:" + bytesTotal);

                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("S3", "ERROR: " + ex);
            }
        });
    }


    class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

    public class ListViewAdapter extends ArrayAdapter<String> {
        private LayoutInflater inflater;
        private int itemLayout;
        private String data;

        public ListViewAdapter(Context context, int itemLayout, List<String> list) {
            super(context, 0, list);
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.itemLayout = itemLayout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(itemLayout, parent, false);
                holder = new ViewHolder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            data = getItem(position);
            File f = new File(data);
            holder.textView.setText(f.getName());
            return convertView;
        }
    }

}
