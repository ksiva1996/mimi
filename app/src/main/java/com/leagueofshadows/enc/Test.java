package com.leagueofshadows.enc;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.leagueofshadows.enc.Interfaces.ScrollEndCallback;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Test extends AppCompatActivity implements ScrollEndCallback {

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
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_list);

        final RecyclerView recyclerView = findViewById(R.id.recycler_view);
        chatList = (ArrayList<String>) getChatList();
        final MainListAdapter mainListAdapter = new MainListAdapter(chatList,this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(mainListAdapter);
        recyclerView.smoothScrollToPosition(14);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatList.add("item new");
                            mainListAdapter.notifyDataSetChanged();
                            recyclerView.smoothScrollToPosition(15);
                            chatList.set(10,"new item 10");
                            mainListAdapter.notifyItemChanged(10);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

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

    @Override
    public void scrollEndReached() {
        Log.e("scroll","end");
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
                    firebaseHelper.sendMessage(message);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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