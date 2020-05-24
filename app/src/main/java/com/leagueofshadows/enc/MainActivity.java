package com.leagueofshadows.enc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.OptionsCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Items.ChatData;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.Util.getMessageContent;

public class MainActivity extends AppCompatActivity implements MessagesRetrievedCallback, ResendMessageCallback, MessageSentCallback, OptionsCallback {

    ArrayList<ChatData> chatDataArrayList;
    RecyclerAdapter recyclerAdapter;
    DatabaseManager2 databaseManager;
    static String userId;

    final static int DELETE_CONSERVATION = 1;

    FloatingActionButton fab;
    FloatingActionButton newGroup;
    FloatingActionButton newUser;
    Boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        assert userId!=null;
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();

        RecyclerView recyclerView = findViewById(R.id.listView);
        chatDataArrayList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerAdapter = new RecyclerAdapter(chatDataArrayList,this,this);
        recyclerView.setAdapter(recyclerAdapter);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { setFloatingActionLayout(); }
        });

        newGroup = findViewById(R.id.newGroup);
        newUser = findViewById(R.id.newUser);

        newGroup.setVisibility(View.GONE);
        newUser.setVisibility(View.GONE);

        newGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,CreateGroupActivity.class));
            }
        });

        newUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ContactsActivity.class));
            }
        });

        startService(new Intent(this,BackgroundService.class));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancelAll();
    }

    private void setFloatingActionLayout() {
        if(flag)
        {
            fab.setImageResource(R.drawable.add);
            newUser.animate().translationY(0);
            newGroup.animate().translationY(0);
            newUser.setVisibility(View.GONE);
            newGroup.setVisibility(View.GONE);
        }
        else
        {
            fab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            newUser.setVisibility(View.VISIBLE);
            newGroup.setVisibility(View.VISIBLE);
            newUser.animate().translationY(getResources().getDimension(R.dimen.st_130));
            newGroup.animate().translationY(getResources().getDimension(R.dimen.st_65));
        }
        flag = !flag;
    }


    @Override
    protected void onResume() {
        super.onResume();
        App app = (App) getApplication();

        if(app.isnull())
        {
            Intent intent = new Intent(MainActivity.this,Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        app.setMessagesRetrievedCallback(this);
        app.setResendMessageCallback(this);
        app.setMessageSentCallback(this);
        loadUserData();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this,BackgroundService.class));
        super.onDestroy();
    }

    void loadUserData()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatDataArrayList.clear();
                chatDataArrayList.addAll( databaseManager.getUserData());
                sort(chatDataArrayList);
                //Log.e("size", String.valueOf(userDataArrayList.size()));
                recyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void sort(ArrayList<ChatData> chatDataArrayList) {

        Collections.sort(chatDataArrayList, new Comparator<ChatData>() {
            @Override
            public int compare(ChatData u1, ChatData u2) {
                return (int)(u2.getTime()-u1.getTime());
            }
        });
    }

    @Override
    public void onNewMessage(Message message) {
        loadUserData();
    }

    @Override
    public void onUpdateMessageStatus(String messageId, String userId) {}

    @Override
    public void newResendMessageCallback(Message message) {
        loadUserData();
    }

    @Override
    public void onComplete(Message message, boolean success, String error) {
    }

    @Override
    public void onOptionsSelected(int option, final int position) {
        if(option==DELETE_CONSERVATION)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
            final User user = chatDataArrayList.get(position).getUser();
            builder.setMessage("Delete conversation with "+user.getName()+" ?");
            builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String userId = user.getId();
                    databaseManager.deleteConversation(userId);
                    loadUserData();
                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        }
    }

    static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

        private ArrayList<ChatData> chatDataArrayList;
        Context context;
        private OptionsCallback optionsCallback;

       /* void set(ArrayList<UserData> userDataArrayList) {
            this.userDataArrayList = userDataArrayList;
        }*/

        static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView thumbNail;
            TextView name;
            TextView message;
            TextView time;
            TextView count;
            SwipeRevealLayout swipe;
            RelativeLayout container;
            ImageButton deleteButton;

            MyViewHolder(View view) {
                super(view);
                thumbNail = view.findViewById(R.id.thumbnail);
                name = view.findViewById(R.id.name);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                count = view.findViewById(R.id.count);
                container = view.findViewById(R.id.container);
                swipe = view.findViewById(R.id.swipe);
                deleteButton = view.findViewById(R.id.delete_button);
            }
        }

        RecyclerAdapter(ArrayList<ChatData> chatDataArrayList, Context context, OptionsCallback optionsCallback) {
            this.context = context;
            this.chatDataArrayList = chatDataArrayList;
            this.optionsCallback = optionsCallback;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_main, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
            final ChatData chatData = chatDataArrayList.get(position);
            holder.name.setText(chatData.getUser().getName());

            if(chatData.getLatestMessage()!=null) {
                if (chatData.getLatestMessage().getFrom().equals(userId)) {
                    holder.message.setText("you: " + getMessageContent(chatData.getLatestMessage().getContent()));
                } else {
                    holder.message.setText(chatData.getUser().getName() + ": " + getMessageContent(chatData.getLatestMessage().getContent()));
                }
            }
            else {
                holder.message.setText("Send you first message");
            }


            holder.thumbNail.setText(chatData.getUser().getName().substring(0,1));
            //TODO: time
            holder.time.setText(formatTime(chatData.getLatestMessage().getTimeStamp()));
            int count = chatData.getCount();
            if(count!=0) {
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setText(String.valueOf(chatData.getCount()));
                holder.time.setTextColor(context.getResources().getColor(R.color.msg_received_time,null));
            }
            else
            {
                holder.count.setText("");
                holder.count.setVisibility(View.INVISIBLE);
                holder.time.setTextColor(context.getResources().getColor(R.color.main_screen_normal,null));
            }
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {
                        Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
                        intent.putExtra(Util.userId, chatData.getUser().getId());
                        context.startActivity(intent);
                    }
                    else
                    {
                        Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
                        intent.putExtra(Util.userId, chatData.getGroup().getId());
                        context.startActivity(intent);
                    }
                }
            });
            holder.swipe.close(false);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.swipe.isOpen()) {
                        optionsCallback.onOptionsSelected(DELETE_CONSERVATION,position);
                    }
                }
            });
        }

        private String formatTime(String received) {
            received =received.substring(4,16);
            return received;
        }

        @Override
        public int getItemCount() {
            return chatDataArrayList.size();
        }
    }
}