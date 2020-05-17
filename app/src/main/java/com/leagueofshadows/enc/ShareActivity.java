package com.leagueofshadows.enc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.ChatData;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ShareActivity extends AppCompatActivity implements Select {

    ArrayList<ChatData> chatDataArrayList;
    Intent receivedIntent;
    private ChatListAdapter chatListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        setTitle("Share to");

        receivedIntent = getIntent();

        chatDataArrayList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatDataArrayList,this,this,receivedIntent);
        RecyclerView listView = findViewById(R.id.recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(linearLayoutManager);
        listView.setAdapter(chatListAdapter);
        load();
    }

    void load()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DatabaseManager2.initializeInstance(new SQLHelper(ShareActivity.this));
                DatabaseManager2 databaseManager = new DatabaseManager2();
                chatDataArrayList.clear();
                chatDataArrayList.addAll(databaseManager.getUserData());
                sort(chatDataArrayList);
                chatListAdapter.notifyDataSetChanged();
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
    public void onClick(User user) {
        finish();
    }

    static class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ChatData> chatDataArrayList;
        private Context context;
        private Select select;
        private Intent receivedIntent;

        ChatListAdapter(ArrayList<ChatData> chatDataArrayList, Context context,Select select,Intent receivedIntent) {
            this.chatDataArrayList = chatDataArrayList;
            this.context = context;
            this.select = select;
            this.receivedIntent = receivedIntent;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_contact, parent, false);
            return new ChatListAdapter.MainListItem(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);

            int randomColor = Color.rgb(r, g, b);

            ContactsActivity.ContactListAdapter.MainListItem mainListItem = (ContactsActivity.ContactListAdapter.MainListItem) holder;

            final ChatData chatData = chatDataArrayList.get(position);
            final String name;
            String number;

            if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {
                name = chatData.getUser().getName();
                number = chatData.getUser().getNumber();
            }
            else {
                name = chatData.getGroup().getName();
                number = "Share to group...";
            }
            mainListItem.username.setText(name);
            mainListItem.number.setText(number);
            mainListItem.alphabet.setText(name.substring(0,1));
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));

            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (receivedIntent.getAction()!=null)
                    {
                        if(receivedIntent.getAction().equals(Intent.ACTION_SEND))
                        {
                            Intent intent;
                            if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {
                                intent = new Intent(context, ChatActivity.class);
                                intent.putExtra(Util.userId, chatData.getUser().getId());
                            }
                            else {
                                intent = new Intent(context, ChatActivity.class);
                                intent.putExtra(Util.userId, chatData.getGroup().getId());
                            }

                            intent.setAction(Intent.ACTION_SEND);
                            intent.setType(receivedIntent.getType());
                            intent.putExtra(Intent.EXTRA_TEXT,receivedIntent.getStringExtra(Intent.EXTRA_TEXT));
                            intent.putExtra(Intent.EXTRA_SUBJECT,receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));
                            intent.putExtra(Intent.EXTRA_STREAM,receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                            context.startActivity(intent);
                            select.onClick(null);
                        }
                    }
                    else {
                        Intent intent;
                        if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {
                            intent = new Intent(context, ChatActivity.class);
                            intent.putExtra(Util.userId, chatData.getUser().getId());
                        }
                        else {
                            intent = new Intent(context, ChatActivity.class);
                            intent.putExtra(Util.userId, chatData.getGroup().getId());
                        }
                        context.startActivity(intent);
                        select.onClick(null);
                    }
                }
            });
        }

        @Override
        public int getItemCount() { return chatDataArrayList.size(); }

        static class MainListItem extends RecyclerView.ViewHolder {

            TextView username;
            TextView alphabet;
            TextView number;
            RelativeLayout relativeLayout;

            MainListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                alphabet = itemView.findViewById(R.id.thumbnail);
                number = itemView.findViewById(R.id.number);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
    }

}
