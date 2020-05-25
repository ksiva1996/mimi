package com.leagueofshadows.enc.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.R;
import com.leagueofshadows.enc.SplashActivity;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Groups;
import static com.leagueofshadows.enc.FirebaseHelper.Users;

public class GroupsWorker extends Service {

    DatabaseManager2 databaseManager;
    DatabaseReference databaseReference;
    int id = 1478;

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final App app = (App) getApplication();
        if(app.isnull())
        {
            createNotificationChannel(Util.ServiceNotificationChannelID,Util.ServiceNotificationChannelTitle);
            Intent notificationIntent = new Intent(this, SplashActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,1,notificationIntent,0);
            Notification notification = new NotificationCompat.Builder(this,Util.ServiceNotificationChannelID).setContentTitle(getString(R.string.app_name))
                    .setContentText("updating groups...")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(pendingIntent).build();

            startForeground(id,notification);
        }

        String id =getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        assert id != null;

        databaseReference.child(Users).child(id).child(Groups).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot d:dataSnapshot.getChildren()) {
                    try {
                        Group group = (Group) d.getValue();
                        ArrayList<User> users = group.getUsers();
                        for (User u:users) {
                            if(databaseManager.getUser(u.getId())==null)
                                databaseManager.insertUser(u);
                        }
                        databaseManager.addNewGroup(group);
                        if (!app.isnull()) {
                            app.getGroupsUpdatedCallback().onComplete();
                        }
                    }catch (Exception e) { e.printStackTrace(); }
                }
                stopSelf();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel(String channelId,String channelTitle) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(channelId,channelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
