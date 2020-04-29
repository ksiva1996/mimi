package com.leagueofshadows.enc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Files;

public class FileUploadService extends Service implements MessageSentCallback {

    Message message;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String otherUserId = intent.getStringExtra(Util.toUserId);
        final String currentUserId = intent.getStringExtra(Util.userId);
        final String timeStamp = intent.getStringExtra(Util.timeStamp);
        final Uri uri = Uri.parse(intent.getStringExtra(Util.uri));
        String userName = intent.getStringExtra(Util.name);
        final String fileName = intent.getStringExtra(Util.fileName);
        final String id = intent.getStringExtra(Util.id);
        final int type = intent.getIntExtra(Util.type,Message.MESSAGE_TYPE_FILE);

        final int notificationId =1457;

        String CHANNEL_ID = "file_upload";
        createNotificationChannel(CHANNEL_ID);
        final NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("sending file to "+userName)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        final int progressMax = 100;

        builder.setProgress(progressMax,0,false);
        notificationManagerCompat.notify(notificationId,builder.build());

        assert otherUserId != null;
        assert timeStamp != null;
        assert currentUserId != null;
        assert id != null;

        FirebaseStorage.getInstance().getReference().child(Files).child(otherUserId).child(timeStamp).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.e("success","success");
                builder.setProgress(0,0,false);
                notificationManagerCompat.cancelAll();

                message = new Message(0,id,otherUserId,currentUserId,fileName,uri.toString(),timeStamp,
                        type,null,null,null);

                EncryptedMessage encryptedMessage = new EncryptedMessage(id,message.getTo(),message.getFrom(),fileName,timeStamp,timeStamp,EncryptedMessage.MESSAGE_TYPE_FILE);
                FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                try {
                    firebaseHelper.sendTextOnlyMessage(message,encryptedMessage,FileUploadService.this,id);
                } catch (DeviceOfflineException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("failure","failure");
                App app = (App) getApplication();
                app.getMessageSentCallback().onComplete(message,false,e.toString());

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                int currentProgress = (int) ((100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount());
                builder.setProgress(progressMax,currentProgress,false);
                notificationManagerCompat.notify(notificationId,builder.build());
            }
        });
        return START_NOT_STICKY;
    }

    private void createNotificationChannel(String channelId) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(channelId,getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onComplete(Message message, boolean success, String error) {

        App app = (App) getApplication();
        if(success) {
            String timeStamp = Calendar.getInstance().getTime().toString();
            message.setSent(timeStamp);
            DatabaseManager2.initializeInstance(new SQLHelper(this));
            DatabaseManager2.getInstance().insertNewMessage(message, message.getTo());
            Native n = new Native(this);
            n.sendNewMessageNotification(message.getTo());
            app.getMessageSentCallback().onComplete(message, true, null);
        }
        else {
            app.getMessageSentCallback().onComplete(message,false,error);
        }
    }
}
