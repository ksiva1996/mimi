package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.background.GroupsWorker;
import com.leagueofshadows.enc.background.ResendMessageWorker;
import com.leagueofshadows.enc.background.Worker;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.REST.Native.NEW_GROUP;
import static com.leagueofshadows.enc.REST.RESTHelper.TEMP_USER_ID;

public class FirebaseReceiver extends FirebaseMessagingService {


    public static final String RECEIVED_STATUS = "RECEIVED_STATUS";
    public static final String SEEN_STATUS = "SEEN_STATUS";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    private static final String RESEND_MESSAGE = "RESEND_MESSAGE";
    private static final String GROUP_ID = "GROUP_ID";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String,String> data = remoteMessage.getData();
        //Log.e("response",remoteMessage.getData().toString());
        if(data.containsKey(RECEIVED_STATUS)||data.containsKey(SEEN_STATUS))
        {
            String id = data.get(MESSAGE_ID);
            String userId = data.get(TEMP_USER_ID);
            String groupId = null;
            if(data.containsKey(GROUP_ID))
                groupId = data.get(GROUP_ID);

            DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
            if(data.containsKey(RECEIVED_STATUS))
                DatabaseManager2.getInstance().updateMessageReceivedStatus(data.get(RECEIVED_STATUS),id,userId,groupId);
            else
                DatabaseManager2.getInstance().updateMessageSeenStatus(data.get(SEEN_STATUS),id,userId,groupId);

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
                Intent intent = new Intent(this, ResendMessageWorker.class);
                startService(intent);
            }
        }
        else if(data.containsKey(NEW_GROUP))
        {
            String text =  data.get(NEW_GROUP);
            showNotification(text);

            Intent intent = new Intent(this, GroupsWorker.class);
            App app = (App) getApplication();
            if(app.isnull()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(intent);
                else
                    startService(intent);
            }
            else
                startService(intent);
        }
    }

    private void showNotification(String text) {
        createNotificationChannel(Util.NewMessageNotificationChannelID,Util.NewMessageNotificationChannelTitle);
        Intent notificationIntent = new Intent(getApplicationContext(), SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),1,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(getApplicationContext(),Util.NewMessageNotificationChannelID).setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).setAutoCancel(false)
                .build();
        NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis(),notification);
    }

    private void createNotificationChannel(String channelId,String channelTitle) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(channelId,channelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        Native restHelper = new Native(getApplicationContext());
        restHelper.updateToken(s);
    }
}