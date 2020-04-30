package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.MalFormedFileException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Files;

public class Downloader extends Service {

    DatabaseManager2 databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager2.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String messageId = intent.getStringExtra(Util.id);
        final String otherUserId = intent.getStringExtra(Util.toUserId);
        final String userId = intent.getStringExtra(Util.userId);
        Message message = databaseManager.getMessage(messageId,otherUserId);

        final int notificationId = new Random().nextInt();

        String CHANNEL_ID = "file_download";
        createNotificationChannel(CHANNEL_ID);
        final NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Downloading file - "+message.getContent())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        final int progressMax = 100;

        builder.setProgress(progressMax,0,false);
        Notification notification = builder.build();
        startForeground(notificationId,notification);

        assert otherUserId != null;
        assert userId != null;

        final String finalPath;

        final String privatePath = Util.privatePath+message.getContent();

        if(message.getType()==Message.MESSAGE_TYPE_IMAGE)
            finalPath = Util.imagesPath+otherUserId+"/"+message.getContent();
        else
            finalPath = Util.documentsPath+otherUserId+"/"+message.getContent();

        File file = new File(privatePath);

        FirebaseStorage.getInstance().getReference().child(Files).child(userId).child(message.getTimeStamp()).getFile(file).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                stopForeground(true);
            }
        }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                App app = (App) getApplication();
                app.getMessagesRetrievedCallback().onUpdateMessageStatus(messageId,otherUserId);
                stopForeground(true);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(privatePath);
                            FileOutputStream fileOutputStream = new FileOutputStream(finalPath);
                            AESHelper aesHelper = new AESHelper(Downloader.this);

                            App app = (App) getApplication();
                            String publicKey = databaseManager.getPublicKey(otherUserId);

                            Log.e("userId",otherUserId);
                            aesHelper.decryptFile(fileInputStream,fileOutputStream,app.getPrivateKey(),publicKey,new File(finalPath));

                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException | MalFormedFileException
                                | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException
                                | InvalidAlgorithmParameterException | RunningOnMainThreadException e) {

                            e.printStackTrace();
                        }
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                int currentProgress = (int) ((100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount());
                builder.setProgress(progressMax,currentProgress,false);
                notificationManagerCompat.notify(notificationId,builder.build());
            }
        });

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
}
