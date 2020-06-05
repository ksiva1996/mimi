package com.leagueofshadows.enc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.leagueofshadows.enc.Items.MessageInfo;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

public class GroupMessageInfo extends AppCompatActivity {

    ArrayList<MessageInfo> messageInfos;
    String messageId;
    String groupId;
    DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_info);

        messageInfos = new ArrayList<>();
        Intent intent = getIntent();

        messageId = intent.getStringExtra(Util.messageId);
        groupId = intent.getStringExtra(Util.id);
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();
        load();
    }

    private void load() {
        messageInfos.clear();
        messageInfos.addAll(databaseManager.getMessageInfo(messageId,groupId));
    }
}
