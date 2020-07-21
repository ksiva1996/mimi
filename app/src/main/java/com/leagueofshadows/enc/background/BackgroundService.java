package com.leagueofshadows.enc.Background;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.FirebaseHelper;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.leagueofshadows.enc.FirebaseHelper.Messages;
import static com.leagueofshadows.enc.FirebaseHelper.resend;

public class BackgroundService extends Service implements ChildEventListener {

    DatabaseManager databaseManager;
    DatabaseReference databaseReference;
    String currentUserId;
    FirebaseHelper firebaseHelper;
    Native restHelper;
    AESHelper aesHelper;
    boolean notificationChannelCreated;
    ArrayList<String> ids;

    static final String id = "id";
    static final String to = "to";
    static final String from = "from";
    static final String content = "content";
    static final String type = "type";
    static final String filePath = "filePath";
    static final String timeStamp = "timeStamp";
    static final String flag = "isGroupMessage";

    @Override
    public void onCreate() {
        super.onCreate();
        notificationChannelCreated = false;
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new Native(this);
        ids = new ArrayList<>();

        try {
            aesHelper = new AESHelper(this);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        ids.add(currentUserId);
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void bindListeners() {
        for (String id:ids) {
            DatabaseReference dr = databaseReference.child(Messages).child(id);
            dr.addChildEventListener(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {

        if(currentUserId ==null)
            stopSelf();

        App app = (App) getApplication();
        if(app.isnull())
            stopSelf();

        bindListeners();

        return START_STICKY;
    }

    void decryptMessage(final EncryptedMessage e, final User otherUser)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                if (databaseManager.check(e.getId(),e.getFrom())||e.isResend()) {
                    final App app = (App) getApplication();
                    if(e.getFrom().equals(currentUserId))
                        return;
                    if (e.getType() == Message.MESSAGE_TYPE_ONLYTEXT)
                    {
                        try {

                            String m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(),otherUser, currentUserId);
                            String timeStamp = Calendar.getInstance().getTime().toString();
                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                    e.getTimeStamp(), timeStamp, null,e.getIsGroupMessage());

                            if(message.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER)
                                restHelper.sendMessageReceivedStatus(e);
                            else
                                restHelper.sendGroupMessageReceivedStatus(message.getMessage_id(),message.getFrom(),message.getTo(), currentUserId,"backgrund service");

                            if (!e.isResend()) {
                                databaseManager.insertNewMessage(message, message.getFrom(), currentUserId);
                                if (app.getMessagesRetrievedCallback() != null) {
                                    MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                    messagesRetrievedCallback.onNewMessage(message);
                                } else {
                                    showNotification(message);
                                }
                            } else {
                                databaseManager.updateMessage(message, message.getFrom());
                                if (app.getResendMessageCallback() != null) {
                                    ResendMessageCallback resendMessageCallback = app.getResendMessageCallback();
                                    resendMessageCallback.newResendMessageCallback(message);
                                }
                            }
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                                InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                                DataCorruptedException | RunningOnMainThreadException ex) {
                            restHelper.sendResendMessageNotification(e);
                            //new change
                            firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                                @Override
                                public void onSuccess(String Base64PublicKey) {
                                    databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                                }
                                @Override
                                public void onCancelled(String error) {}
                            });

                            String timeStamp = Calendar.getInstance().getTime().toString();
                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null,
                                    e.getFilePath(), e.getTimeStamp(), e.getType(), e.getTimeStamp(), timeStamp, null,e.getIsGroupMessage());

                            databaseManager.insertNewMessage(message, message.getFrom(), currentUserId);
                            if (app.getMessagesRetrievedCallback() != null) {
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                showNotification(message);
                            }
                            ex.printStackTrace();
                        }
                    }
                    else {
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        String messageString = e.getContent();
                        databaseManager.insertCipherText(messageString,e.getId());
                        try {
                            messageString = aesHelper.DecryptMessage(messageString,app.getPrivateKey(),otherUser, currentUserId);
                            Message message = new Message(0,e.getId(),e.getTo(),e.getFrom(),messageString,e.getFilePath(),e.getTimeStamp()
                                    ,e.getType(),e.getTimeStamp(),timeStamp,null,e.getIsGroupMessage());

                            databaseManager.insertNewMessage(message,message.getFrom(), currentUserId);

                            if(message.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER)
                                restHelper.sendMessageReceivedStatus(e);
                            else
                                restHelper.sendGroupMessageReceivedStatus(message.getMessage_id(),message.getFrom(),message.getTo(), currentUserId,"background service");

                            if(app.getMessagesRetrievedCallback()!=null) {
                                app.getMessagesRetrievedCallback().onNewMessage(message);
                            }
                            else {
                                showNotification(message);
                            }

                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException |
                                InvalidKeySpecException | InvalidAlgorithmParameterException | DataCorruptedException |
                                RunningOnMainThreadException | IllegalBlockSizeException ex) {
                            firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                                @Override
                                public void onSuccess(String Base64PublicKey) {
                                    databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                                }
                                @Override
                                public void onCancelled(String error) {}
                            });

                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                    e.getTimeStamp(), timeStamp, null,e.getIsGroupMessage());

                            databaseManager.insertNewMessage(message, message.getFrom(), currentUserId);
                            if (app.getMessagesRetrievedCallback() != null) {
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                showNotification(message);
                            }
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void showNotification(Message message) {
        if(!notificationChannelCreated) {
            Util.createMessageNotificationChannel(this);
            notificationChannelCreated = true;
        }
        Util.sendNewMessageNotification(message,databaseManager,this,Util.getNotificationIntent(message,this));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot d, @Nullable String s) {
        try {

            d.getRef().removeValue();

            final EncryptedMessage encryptedMessage = new EncryptedMessage();
            encryptedMessage.setId((String) d.child(id).getValue());
            encryptedMessage.setTo((String) d.child(to).getValue());
            encryptedMessage.setFrom((String) d.child(from).getValue());
            encryptedMessage.setContent((String) d.child(content).getValue());
            encryptedMessage.setFilePath((String) d.child(filePath).getValue());
            encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
            encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());
            encryptedMessage.setIsGroupMessage(Integer.parseInt(Long.toString((Long) d.child(flag).getValue())));

            if (d.hasChild(resend))
                encryptedMessage.setResend((boolean) d.child(resend).getValue());


            final String userId = encryptedMessage.getFrom();
            if (databaseManager.getPublicKey(userId) == null)
            {
                firebaseHelper.getUserPublic(userId, new PublicKeyCallback() {
                    @Override
                    public void onSuccess(String Base64PublicKey) {
                        databaseManager.insertPublicKey(Base64PublicKey, encryptedMessage.getFrom());
                        final User otherUser = databaseManager.getUser(userId);
                        decryptMessage(encryptedMessage, otherUser);
                    }
                    @Override
                    public void onCancelled(String error) {
                        Log.e("error", error);
                    }
                });
            }
            else {
                final User otherUser = databaseManager.getUser(userId);
                decryptMessage(encryptedMessage,otherUser);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) { Log.e(id+" - database error",databaseError.toString()); }
}
