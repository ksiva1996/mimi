package com.leagueofshadows.enc;

import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.Map;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.REST.RESTHelper.TEMP_USER_ID;

public class FirebaseReceiver extends FirebaseMessagingService {


    public static final String RECEIVED_STATUS = "RECEIVED_STATUS";
    public static final String SEEN_STATUS = "SEEN_STATUS";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    private static final String RESEND_MESSAGE = "RESEND_MESSAGE";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String,String> data = remoteMessage.getData();
        //Log.e("response",remoteMessage.getData().toString());
        if(data.containsKey(RECEIVED_STATUS)||data.containsKey(SEEN_STATUS))
        {
            String id = data.get(MESSAGE_ID);
            String userId = data.get(TEMP_USER_ID);
            DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
            if(data.containsKey(RECEIVED_STATUS))
                DatabaseManager2.getInstance().updateMessageReceivedStatus(data.get(RECEIVED_STATUS),id,userId);
            else
                DatabaseManager2.getInstance().updateMessageSeenStatus(data.get(SEEN_STATUS),id,userId);

            App app = (App) getApplication();
            MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
            if(messagesRetrievedCallback!=null) {
                messagesRetrievedCallback.onUpdateMessageStatus(id,userId);
            }
        }
        else if(data.containsKey(NEW_MESSAGE))
        {
            App app = (App) getApplication();
            if(app.isnull()) {
                Intent intent = new Intent(getApplicationContext(), Worker.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }
        else if(data.containsKey(RESEND_MESSAGE))
        {
            App app = (App) getApplication();
            String messageId = data.get(RESEND_MESSAGE);
            String userId = data.get(TEMP_USER_ID);
            DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
            DatabaseManager2.getInstance().insertResendMessage(userId,messageId);
            if(!app.isnull()) {


                Intent intent = new Intent(this,ResendMessageWorker.class);
                startService(intent);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {

        Native restHelper = new Native(getApplicationContext());
        restHelper.updateToken(s);
    }
}
