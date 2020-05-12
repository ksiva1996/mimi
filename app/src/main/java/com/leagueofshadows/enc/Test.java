package com.leagueofshadows.enc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static com.leagueofshadows.enc.ChatActivity.FILE_ATTACHMENT_REQUEST;
import static com.leagueofshadows.enc.ChatActivity.IMAGE_ATTACHMENT_REQUEST;

public class Test extends AppCompatActivity implements ScaleGestureDetector.OnScaleGestureListener {

    ArrayList<String> chatList;
    ArrayList<Message> messages;
    ArrayAdapter<String> arrayAdapter;
    DatabaseManager databaseManager;
    SQLHelper sqlHelper;
    long[] startTime = new long[100];
    long[] endtime = new long[100];
    int x = 0;
    int y = 0;
    long totaltime = 0;
    ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        imageView = findViewById(R.id.image);
        scaleGestureDetector = new ScaleGestureDetector(this,this);
        final EditText bytes = findViewById(R.id.bytes);
        findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            int x = getCompressionFactor(Integer.parseInt(bytes.getText().toString()));
            Log.e("factor", String.valueOf(x));

            }
        });

    }


    private int getCompressionFactor(int byteCount) {

        final int byteUpperLimit = 3145728;
        final int byteLowerLimit = 307200;
        final int upperCompression = 10;
        final int lowerCompression = 80;
        if(byteCount>byteUpperLimit)
            return upperCompression;
        else if(byteCount<byteLowerLimit)
            return lowerCompression;
        else
        {
            int factor = (byteUpperLimit - byteLowerLimit)/(lowerCompression-upperCompression);
            return 80 - (byteCount-byteLowerLimit)/factor;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1)
        {
            if(resultCode==RESULT_OK)
            {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(this.getFilesDir()+"/test.jpeg");
                    try {
                        ExifInterface exifInterface = new ExifInterface(getApplicationContext().getFilesDir()+"/test.jpeg");
                        Bitmap correctBitmap;

                        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
                        switch (orientation)
                        {
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                correctBitmap = rotateBitmap(bitmap,90);
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                correctBitmap = rotateBitmap(bitmap,180);
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                correctBitmap = rotateBitmap(bitmap,270);
                                break;
                            case ExifInterface.ORIENTATION_NORMAL:
                            default: correctBitmap = bitmap;
                        }
                        imageView.setImageBitmap(correctBitmap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                    File file = new File(this.getFilesDir()+"/test.jpeg");
                    Log.e("file size", String.valueOf(file.length()));

                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(this,"camera closed",Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == 2)
        {
            if(resultCode == RESULT_OK)
            {
                Log.e("uug","bo");
                try {
                    assert data != null;
                    Uri uri = data.getData();

                    Intent intent = new Intent(this,FileUploadService.class);
                    intent.putExtra(Util.uri,uri.toString());
                    intent.putExtra(Util.userId,"siva");
                    String timeStamp = Calendar.getInstance().getTime().toString();
                    intent.putExtra(Util.timeStamp,timeStamp);
                    startService(intent);

                    /*File file = new File(Environment.getExternalStorageDirectory()+"/Enc");
                    if(!file.exists())
                        Log.e("file", String.valueOf(file.mkdir()));

                    assert uri != null;
                    InputStream inputStream = getContentResolver().openInputStream(uri);

                    Log.e("bytes", String.valueOf(inputStream.available()));

                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(Environment.getExternalStorageDirectory()+"/Enc/test.jpeg"));


                    byte[] buffer = new byte[4096];
                    int count;
                    while((count = bufferedInputStream.read(buffer))>0)
                    {
                        Log.e("iub","ugiu");
                        bufferedOutputStream.write(buffer,0,count);
                    }
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    bufferedInputStream.close();*/

                }catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        if(requestCode==3)
        {
            if(resultCode==RESULT_OK)
            {
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMDD-HHmmss");
                    String fileName = "IMG-"+simpleDateFormat.format(new Date())+".jpg";
                    String path = Util.imagesPath;
                    checkPath(path,IMAGE_ATTACHMENT_REQUEST);


                    FileOutputStream fileOutputStream = new FileOutputStream(Util.imagesPath+"80"+fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,80,fileOutputStream);

                    fileOutputStream = new FileOutputStream(Util.imagesPath+"50"+fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,50,fileOutputStream);

                    fileOutputStream = new FileOutputStream(Util.imagesPath+"30"+fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,30,fileOutputStream);

                    fileOutputStream = new FileOutputStream(Util.imagesPath+"10"+fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,10,fileOutputStream);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkPath(String path, int x) {
        File file = new File(Util.originalPath);
        if(!file.exists())
            file.mkdir();
        if(x == IMAGE_ATTACHMENT_REQUEST) {
            file = new File(Util.imagesPath);
            if(!file.exists())
                file.mkdir();
        }
        if(x==FILE_ATTACHMENT_REQUEST) {
            file = new File(Util.documentsPath);
            if(!file.exists())
                file.mkdir();
        }

        file = new File(path);
        if(!file.exists())
            file.mkdir();

        file = new File(path+"/sent");
        if(!file.exists())
            file.mkdir();
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float i) {
        Matrix matrix = new Matrix();
        matrix.setRotate(i);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

        scaleFactor = scaleFactor*scaleGestureDetector.getScaleFactor();
        imageView.setScaleX(scaleFactor);
        imageView.setScaleY(scaleFactor);
        //Log.e("kugu","lugugu");
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }
}
   /* private void sendStatus() {
        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String currentUserId = sp.getString(Util.userId,null);
        HashMap<String,String> params = new HashMap<>();
        params.put("USER_ID",currentUserId);
        params.put("MESSAGE_ID","testId");
        params.put("MESSAGE_STATUS","1");
        RESTHelper restHelper = new RESTHelper(this);
        //restHelper.test("sendNewMessageNotification",params,SEND_STATUS_ENDPOINT,null,null);


    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    void sendNewMessageNotification()
    {

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String currentUserId = sp.getString(Util.userId,null);


       /* HashMap<String,String> params = new HashMap<>();
        params.put("USER_ID",currentUserId);
        params.put(NEW_MESSAGE,NEW_MESSAGE);
        RESTHelper restHelper = new RESTHelper(this);
        restHelper.test("sendNewMessageNotification",params,SEND_NOTIFICATION_ENDPOINT,null,null);

    }

    void sendToken()
    {
        final String userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                Log.e("token",token);
                HashMap<String,String> params = new HashMap<>();
                params.put("TOKEN",token);
                params.put("USER_ID",userId);

                RESTHelper restHelper = new RESTHelper(Test.this);
                //restHelper.test("token sending",params,RESTHelper.TOKEN_UPDATE_ENDPOINT,null,null);
            }
        });
    }

    public List<String> getChatList() {
        List<String> chatList = new ArrayList<>();
        chatList.add("User 1");
        chatList.add("User 2");
        chatList.add("User 3");
        chatList.add("User 4");
        chatList.add("User 5");
        chatList.add("User 6");
        chatList.add("User 7");
        chatList.add("User 8");
        chatList.add("User 9");
        chatList.add("User 10");
        chatList.add("User 11");
        chatList.add("User 12");
        chatList.add("User 13");
        chatList.add("User 14");
        chatList.add("User 15");
        return chatList;
    }

    void testfirebase(int x)
    {
        startTime[x] = Calendar.getInstance().getTimeInMillis();
        final Message message = new Message(x,"test Message Id"+x,"to"+x,"from"+x
                ,"message content"+x,"filepath"+x,"timetamp"+x
                ,Message.MESSAGE_TYPE_ONLYTEXT+x,"sent"+x,"received"+x,"seen"+x);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FirebaseHelper firebaseHelper = new FirebaseHelper(Test.this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onNewMessage(Message message) {

    }

    @Override
    public void onUpdateMessageStatus(String messageId, String userId) {}

    static class MainListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<String> msgList;
        private ScrollEndCallback scrollEndCallback;

        MainListAdapter(List<String> msgList,ScrollEndCallback scrollEndCallback) {
            this.msgList = msgList;
            this.scrollEndCallback = scrollEndCallback;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType==1) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.receive_msg, parent, false);
                return new receiveItem(view);
            }
            else
            {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.send_msg, parent, false);
                return new sendItem(view);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position%2==0)
                return 1;
            else
                return 0;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (position==0) {
                scrollEndCallback.scrollEndReached();
            }

            if(position%2==0)
            {
                receiveItem receiveItem = (MainListAdapter.receiveItem) holder;
                receiveItem.msg.setText(msgList.get(position));
                receiveItem.swipeRevealLayout.close(false);
            }
            else
            {
                sendItem sendItem = (MainListAdapter.sendItem) holder;
                sendItem.msg.setText(msgList.get(position));
                sendItem.swipeRevealLayout.close(false);
            }
        }

        @Override
        public int getItemCount() {
            return msgList.size();
        }


         static class receiveItem extends RecyclerView.ViewHolder{
             TextView msg;
             SwipeRevealLayout swipeRevealLayout;

             receiveItem(View itemView) {
                 super(itemView);
                 msg = itemView.findViewById(R.id.textview_message);
                 swipeRevealLayout = itemView.findViewById(R.id.container);
             }
         }

         static class sendItem extends RecyclerView.ViewHolder {

            TextView msg;
            SwipeRevealLayout swipeRevealLayout;

             sendItem(View itemView) {
                super(itemView);
                msg = itemView.findViewById(R.id.textview_message);
                swipeRevealLayout = itemView.findViewById(R.id.container);
            }
        }
    }

}
        /*RESTHelper restHelper = new RESTHelper(this);
        restHelper.test(new HashMap<String, String>(),RESTHelper.ACCESS_TOKEN,this,this);

        /*final Message message = new Message(x,"test Message Id"+x,"to"+x,"from"+x
                ,"message content"+x,"filepath"+x,"timetamp"+x
                ,Message.MESSAGE_TYPE_ONLYTEXT+x,"sent"+x,"received"+x,"seen"+x);

        //final FirebaseHelper  firebaseHelper = new FirebaseHelper(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FirebaseHelper.messageBroadcast);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String id  = intent.getStringExtra("id");
                Log.e("fired","fired + "+id);
            }
        };
        registerReceiver(broadcastReceiver,intentFilter);
        findViewById(R.id.ADD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message1 = null;
                try {
                    message1 = firebaseHelper.sendMessage(message);
                } catch (DeviceOfflineException e) {
                    e.printStackTrace();
                }

            }
        });*/



    /*IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FirebaseHelper.broadcast);
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int pos = intent.getIntExtra("id",x);
            Log.e("fired","fired");
            endtime[pos] = Calendar.getInstance().getTimeInMillis();
            y++;
            if(y==100)
                log();
        }
    };
    registerReceiver(broadcastReceiver,intentFilter);
    findViewById(R.id.ADD).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for(int i=0;i<100;i++)
            {
                x++;
                testfirebase(i);
            }
        }
    });

    private void log() {
        Log.e("tests - "+x,"results - "+y);

        for(int i=0;i<x;i++)
        {
            long y  = endtime[i]-startTime[i];
            Log.e(i+" - message time - ", String.valueOf((float)y/1000));
            totaltime = totaltime +y;
        }

        Log.e("total time - ", String.valueOf((float)totaltime/1000));
        Log.e("average time - ",String.valueOf((float)totaltime/(1000*x)));
    }

    private void load() {
        messages.clear();
        names.clear();
        messages = databaseManager.getMessages("from"+x,0,10);
        for (Message message:messages) {
            //Log.e("bk","i");
            names.add(message.getContent());
        }
        arrayAdapter.notifyDataSetChanged();
    }
}

/*names = new ArrayList<>();
        messages = new ArrayList<>();
        sqlHelper = new SQLHelper(this);
        DatabaseManager.initializeInstance(sqlHelper);
        databaseManager = DatabaseManager.getInstance();



        ListView listView = findViewById(R.id.list);
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                boolean x = databaseManager.deleteMessage(messages.get(i).getMessage_id());
                Log.e("matter", String.valueOf(x));
                load();
                arrayAdapter.notifyDataSetChanged();
            }
        });
        load();

        findViewById(R.id.reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load();
            }
        });
        findViewById(R.id.ADD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                x++;
                Message message = new Message(0,"test Message Id"+x,"to"+x,"from"+x
                        ,"message content"+x,"filepath"+x,"timetamp"+x
                        ,Message.MESSAGE_TYPE_ONLYTEXT+x,"sent"+x,"received"+x,"seen"+x);
                boolean x = databaseManager.insertNewMessage(message);
                Log.e("matter", String.valueOf(x));
                load();
            }
        });

 /*firebaseAuth = FirebaseAuth.getInstance();
        EditText p = findViewById(R.id.phone);
        final EditText o = findViewById(R.id.OTP);

        SQLHelper sqlHelper = new SQLHelper(this);

        DatabaseManager.initializeInstance(sqlHelper);
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {

            String number = "+";

            @Override
            public void onClick(View view) {
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        number,
                        60,
                        TimeUnit.SECONDS,
                        Test.this,
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                Log.e("code","successful");
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Log.e("code","");
                                e.printStackTrace();

                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                Log.e("code","sent");
                                verificationId = s;
                                resendToken = forceResendingToken;
                            }


                        });
            }
        });
        findViewById(R.id...verify).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,o.getText().toString());
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener(Test.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Test.this, "Verification Success", Toast.LENGTH_SHORT).show();
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(Test.this, "Verification Failed, Invalid credentials", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });*/