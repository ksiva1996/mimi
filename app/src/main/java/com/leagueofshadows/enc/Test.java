package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.ScrollEndCallback;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.FirebaseReceiver.NEW_MESSAGE;
import static com.leagueofshadows.enc.REST.RESTHelper.SEND_NOTIFICATION_ENDPOINT;
import static com.leagueofshadows.enc.REST.RESTHelper.SEND_STATUS_ENDPOINT;

public class Test extends AppCompatActivity implements MessagesRetrievedCallback {

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

    @Override
    protected void onCreate(Bundle savedInstanceState)   {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testfirebase(x);
                x++;
            }
        });
        findViewById(R.id.newMessage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNewMessageNotification();
            }
        });
        findViewById(R.id.status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendStatus();
            }
        });

        startService(new Intent(this,BackgroundWorker.class));
    }

    private void sendStatus() {
        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String currentUserId = sp.getString(Util.userId,null);
        HashMap<String,String> params = new HashMap<>();
        params.put("USER_ID",currentUserId);
        params.put("MESSAGE_ID","testId");
        params.put("MESSAGE_STATUS","1");
        RESTHelper restHelper = new RESTHelper(this);
        restHelper.test("sendNewMessageNotification",params,SEND_STATUS_ENDPOINT,null,null);


    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    void sendNewMessageNotification()
    {

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String currentUserId = sp.getString(Util.userId,null);


        HashMap<String,String> params = new HashMap<>();
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
                restHelper.test("token sending",params,RESTHelper.TOKEN_UPDATE_ENDPOINT,null,null);
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
    public void onUpdateMessageStatus(String messageId, String userId) {

    }
    @Override
    public void onCanceled() {

    }

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