package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.background.GroupsWorker;
import com.leagueofshadows.enc.background.ResendMessageWorker;
import com.leagueofshadows.enc.background.Worker;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Groups;
import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.FirebaseHelper.Users;
import static com.leagueofshadows.enc.REST.Native.GROUP_DELETE;
import static com.leagueofshadows.enc.REST.Native.GROUP_UPDATE;
import static com.leagueofshadows.enc.REST.Native.NEW_GROUP;
import static com.leagueofshadows.enc.REST.Native.NOTIFICATION_TEXT;
import static com.leagueofshadows.enc.REST.Native.TEMP_USER_ID;
import static com.leagueofshadows.enc.REST.Native.USER_ID;

public class FirebaseReceiver extends FirebaseMessagingService {


    public static final String RECEIVED_STATUS = "RECEIVED_STATUS";
    public static final String SEEN_STATUS = "SEEN_STATUS";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    private static final String RESEND_MESSAGE = "RESEND_MESSAGE";
    private static final String GROUP_ID = "GROUP_ID";
    private String currentUserId;
    private DatabaseManager databaseManager;
    private App app;
    private FirebaseHelper firebaseHelper;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String,String> data = remoteMessage.getData();
        currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();
        firebaseHelper = new FirebaseHelper(this);

        if(data.containsKey(RECEIVED_STATUS)||data.containsKey(SEEN_STATUS))
        {
            String id = data.get(MESSAGE_ID);
            String userId = data.get(TEMP_USER_ID);
            String groupId = null;
            if(data.containsKey(GROUP_ID))
                groupId = data.get(GROUP_ID);

            if(data.containsKey(RECEIVED_STATUS))
                databaseManager.updateMessageReceivedStatus(data.get(RECEIVED_STATUS),id,userId,groupId);
            else
                databaseManager.updateMessageSeenStatus(data.get(SEEN_STATUS),id,userId,groupId,currentUserId);

            app = (App) getApplication();
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
                if(data.containsKey(USER_ID))
                    intent.putExtra(Util.userId,data.get(USER_ID));

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

            databaseManager.insertResendMessage(userId,messageId);
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
        else if(data.containsKey(GROUP_DELETE)){
            String groupId = data.get(GROUP_DELETE);
            String notificationText = data.get(NOTIFICATION_TEXT);
            try{
                databaseManager.markGroupAsDeleted(groupId);
                showNotification(notificationText);
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(Users).child(currentUserId).child(groupId);
                removeNode(databaseReference);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if(data.containsKey(GROUP_UPDATE)){
            String groupId = data.get(GROUP_UPDATE);
            try{
                updateGroup(groupId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void updateGroup(String groupId) {
        FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot d) {
                String groupName = (String) d.child(Util.name).getValue();
                String groupId = (String) d.child(Util.id).getValue();
                String admins = (String) d.child(Util.admins).getValue();
                int groupActive = Integer.parseInt(Long.toString((Long) d.child(Util.groupActive).getValue()));

                ArrayList<User> users = new ArrayList<>();
                for(DataSnapshot d1:d.child("users").getChildren()){
                    String name = (String) d1.child(Util.name).getValue();
                    String id = (String) d1.child(Util.id).getValue();
                    String number = (String) d1.child(Util.number).getValue();
                    String Base64PublicKey = (String) d1.child(Util.base64EncodedPublicKey).getValue();
                    if (!id.equals(currentUserId))
                        users.add(new User(id,name,number,Base64PublicKey));
                }
                for (final User u:users) {
                    if(databaseManager.getUser(u.getId())==null)
                        databaseManager.insertUser(u);

                    firebaseHelper.getUserPublic(u.getId(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey) {
                            databaseManager.insertPublicKey(Base64PublicKey,u.getId());
                        }
                        @Override
                        public void onCancelled(String error) { }
                    });
                }
                Group group = new Group(groupId,groupName,users,admins,groupActive);
                databaseManager.updateGroupUsers(group);
                if (!app.isnull()) {
                    ArrayList<GroupsUpdatedCallback> groupsUpdatedCallbacks = app.getGroupsUpdatedCallback();
                    for (GroupsUpdatedCallback g:groupsUpdatedCallbacks) { g.onComplete(); }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void removeNode(DatabaseReference databaseReference) {
        databaseReference.removeValue();
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