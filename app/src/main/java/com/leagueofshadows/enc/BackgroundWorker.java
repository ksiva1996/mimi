package com.leagueofshadows.enc;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.FirebaseHelper.Messages;
import static com.leagueofshadows.enc.FirebaseHelper.Users;
import static com.leagueofshadows.enc.FirebaseReceiver.RECEIVED_STATUS;
import static com.leagueofshadows.enc.REST.RESTHelper.USER_ID;

public class BackgroundWorker extends Service implements com.google.firebase.database.ChildEventListener {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    String userId;
    static final String id = "id";
    static final String to = "to";
    static final String from = "from";
    static final String content = "content";
    static final String type = "type";
    static final String filePath = "filePath";
    static final String timeStamp = "timeStamp";
    DatabaseManager2 databaseManager;
    private RESTHelper restHelper;
    private AESHelper aesHelper;
    private FirebaseHelper firebaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new RESTHelper(this);
        try {
            aesHelper = new AESHelper(this);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(userId==null)
            stopSelf();

        App app = (App) getApplication();
        if(app.isnull())
            stopSelf();

        databaseReference.child(Messages).child(userId).addChildEventListener(this);

        return START_STICKY;
    }

    void decryptMessage(final EncryptedMessage e, final String Base64PublicKey)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (e.getType() == EncryptedMessage.MESSAGE_TYPE_ONLYTEXT)
                {
                    final App app = (App) getApplication();

                    try {
                        String  m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(), Base64PublicKey);
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,"not seen");
                        databaseManager.insertNewMessage(message,message.getFrom());

                        sendReceivedStatus(e);

                        if(app.getMessagesRetrievedCallback()!=null) {
                            MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                            messagesRetrievedCallback.onNewMessage(message);
                        }
                        else {
                            showNotification(message);
                            databaseManager.incrementNewMessageCount(message.getFrom(),message.getMessage_id());
                        }
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                            InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                            DataCorruptedException | RunningOnMainThreadException ex) {
                        ex.printStackTrace();
                    }
                }
                else {
                    //TODO: other types of messages

                }
            }
        });
    }

    void sendReceivedStatus(EncryptedMessage e)
    {
        Map<String,String> params = new HashMap<>();
        params.put(MESSAGE_ID,e.getId());
        String timeStamp = Calendar.getInstance().getTime().toString();
        params.put(RECEIVED_STATUS,timeStamp);
        params.put(USER_ID,e.getFrom());
        params.put("TEMP_USER_ID",e.getTo());
        restHelper.test("Message Id "+e.getId(),params, RESTHelper.SEND_STATUS_ENDPOINT,null,null);
    }

    private void showNotification(Message message) {
        //TODO : figure out notifications
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot d, @Nullable String s) {

        //Log.e("new message",d.toString());

        d.getRef().removeValue();
        final EncryptedMessage encryptedMessage = new EncryptedMessage();
        encryptedMessage.setId((String) d.child(id).getValue());
        encryptedMessage.setTo((String) d.child(to).getValue());
        encryptedMessage.setFrom((String) d.child(from).getValue());
        encryptedMessage.setContent((String) d.child(content).getValue());
        encryptedMessage.setFilePath((String) d.child(filePath).getValue());
        encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
        encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());
        String userId = encryptedMessage.getFrom();
        final String publicKey = databaseManager.getPublicKey(userId);
        if(publicKey==null) {
            firebaseHelper.getUserPublic(userId, new PublicKeyCallback() {
                @Override
                public void onSuccess(String Base64PublicKey) {
                    decryptMessage(encryptedMessage,Base64PublicKey);
                }
                @Override
                public void onCancelled(String error) {
                    Log.e("error",error);
                }
            });
        }
        else {
            decryptMessage(encryptedMessage,publicKey);
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {Log.e("removed","removed");}

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {}
}
