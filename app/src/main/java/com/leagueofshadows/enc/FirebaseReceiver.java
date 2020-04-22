package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.REST.RESTHelper.USER_ID;

public class FirebaseReceiver extends FirebaseMessagingService {


    public static final String RECEIVED_STATUS = "RECEIVED_STATUS";
    public static final String SEEN_STATUS = "SEEN_STATUS";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        //TODO:
        //Log.e("on receive","on receive");
        Map<String,String> data = remoteMessage.getData();
        if(data.containsKey(RECEIVED_STATUS)||data.containsKey(SEEN_STATUS))
        {
            //Log.e("new status","new status");
            String id = data.get(MESSAGE_ID);
            String userId = data.get(USER_ID);
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
           // Log.e("new message","new message");

            App app = (App) getApplication();
            if(app.isnull()) {
               // Log.e("calling","calling");
                Intent intent = new Intent(getApplicationContext(), Worker.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }
    }

    private void showNotification(String id) {
        //TODO: show notification


    }

    @Override
    public void onNewToken(@NonNull String s) {

        //TODO: implement background service

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);

        String userId = sp.getString(USER_ID,null);
        String accessToken = sp.getString(RESTHelper.ACCESS_TOKEN,null);

        if(userId!=null && accessToken!=null)
        {
            RESTHelper restHelper = new RESTHelper(getApplicationContext());
            Map<String,String> params = new HashMap<>();

            params.put(RESTHelper.ACCESS_TOKEN,accessToken);
            params.put(USER_ID,userId);
            params.put(RESTHelper.FIREBASE_TOKEN,s);
            restHelper.test("Firebase token refresh",params,RESTHelper.FIREBASE_TOKEN,null,null);
        }
    }
}
