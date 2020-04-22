package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Worker extends Service implements CompleteCallback{

    public static final String CHANNEL_ID = "service_retrieve";
    public static final int id = 1547;
    private DatabaseManager2 databaseManager;
    FirebaseHelper firebaseHelper;
    ArrayList<EncryptedMessage> encryptedMessages;

    @Override
    public void onCreate() {
        super.onCreate();
        encryptedMessages = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();
        Intent notificationIntent = new Intent(this,SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,1,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle(getString(R.string.app_name))
                .setContentText("getting new messages...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).build();

        startForeground(id,notification);


        DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager2.getInstance();

        firebaseHelper = new FirebaseHelper(getApplicationContext());
        final String userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        try
        {
            firebaseHelper.getNewMessages(userId,this);
        }
        catch (DeviceOfflineException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private void work(final int no)
    {
        ArrayList<EncryptedMessage> encryptedMessages = databaseManager.getEncryptedMessages();

        this.encryptedMessages.addAll(encryptedMessages);
        final App app = (App) getApplication();

        if(encryptedMessages.isEmpty())
        {
            if(app.isnull())
            stopSelf();
            else
                stopForeground(true);
            return;
        }

        if (app.isnull())
        {
            for(final EncryptedMessage e:encryptedMessages)
            {
                if(databaseManager.getPublicKey(e.getFrom())==null)
                {
                    firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey) {
                            databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                            update(e);
                        }
                        @Override
                        public void onCancelled(String error) {
                            Log.e("Worker",error);
                            update(e);
                        }
                    });
                }
            }

            Intent notificationIntent = new Intent(getApplicationContext(),SplashActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),1,notificationIntent,0);
            Notification notification = new NotificationCompat.Builder(getApplicationContext(),CHANNEL_ID).setContentTitle(getString(R.string.app_name))
                    .setContentText("open app to view new messages - "+no+" new messages")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();
            NotificationManagerCompat.from(getApplicationContext()).notify(id,notification);

        }
        else {
            try {
                final AESHelper aesHelper = new AESHelper(getApplicationContext());

                for (final EncryptedMessage e : encryptedMessages)
                {
                    final String Base64PulicKey = databaseManager.getPublicKey(e.getFrom());
                    if (Base64PulicKey != null) {
                        decryptMessage(e,aesHelper,Base64PulicKey);
                    }
                    else {

                        firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                            @Override
                            public void onSuccess(String Base64PublicKey) {
                                databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                                decryptMessage(e,aesHelper,Base64PublicKey);
                            }

                            @Override
                            public void onCancelled(String error) {
                                Log.e("Worker",error);
                                update(e);
                            }
                        });
                    }
                }
            }
            catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                ex.printStackTrace();

            }
        }
    }

    private synchronized void update(EncryptedMessage e) {

        encryptedMessages.remove(e);

        if(encryptedMessages.isEmpty()) {
            stopForeground(true);
            App app = (App) getApplication();
            if(app.isnull())
                stopSelf();
        }
    }

    void decryptMessage(final EncryptedMessage e, final AESHelper aesHelper, final String Base64PulicKey)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (e.getType() == EncryptedMessage.MESSAGE_TYPE_ONLYTEXT)
                {
                    final App app = (App) getApplication();

                    try {
                        String  m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(), Base64PulicKey);
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,"not seen");
                        databaseManager.insertNewMessage(message,message.getFrom());
                        databaseManager.deleteEncryptedMessage(e.getId());

                        if(app.getMessagesRetrievedCallback()!=null) {

                            MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                            messagesRetrievedCallback.onNewMessage(message);
                        }
                        else {
                            showNotification(message);
                            databaseManager.incrementNewMessageCount(message.getFrom(),message.getMessage_id());
                        }
                        update(e);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                            InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                            DataCorruptedException | RunningOnMainThreadException ex) {
                        update(e);
                        ex.printStackTrace();
                    }
                }
                else {
                    //TODO: other types of messages
                }
            }
        });
    }

    private void showNotification(Message message) {
        //TODO : figure out notifications
    }

    private void createNotificationChannel() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID,getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
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
    public void onComplete(int numberOfMessages) {
        work(numberOfMessages);
    }

    @Override
    public void onCanceled() {
        Log.e("something","wrong in getting new messages");
    }
}
