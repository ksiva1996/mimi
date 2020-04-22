package com.leagueofshadows.enc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.UserCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.FirebaseReceiver.RECEIVED_STATUS;
import static com.leagueofshadows.enc.REST.RESTHelper.SEND_NOTIFICATION_ENDPOINT;
import static com.leagueofshadows.enc.REST.RESTHelper.USER_ID;

class FirebaseHelper {

    static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String NEW_MESSAGE = "NEW_MESSAGE";
    private Context context;
    private DatabaseReference databaseReference;
    private DatabaseManager2 databaseManager;

    public static final String Messages = "Messages";
    static final String Users = "Users";

    private static final String DeviceOfflineException = "Cannot send Message without internet connection...TODO: offline capability in the next update";

    static final String id = "id";
    static final String to = "to";
    static final String from = "from";
    static final String content = "content";
    static final String type = "type";
    static final String filePath = "filePath";
    static final String timeStamp = "timeStamp";
    static final String Base64EncodedPublicKey = "base64EncodedPublicKey";


    FirebaseHelper(Context context)
    {
        this.context = context;
        DatabaseManager2.initializeInstance(new SQLHelper(context));
        databaseManager = DatabaseManager2.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }



    void sendTextOnlyMessage(final Message message, final EncryptedMessage encryptedMessage, final MessageSentCallback messageSentCallback) throws DeviceOfflineException {

        if(!checkConnection()) {
            throw new DeviceOfflineException(DeviceOfflineException);
        }

        DatabaseReference reference = databaseReference.child(Messages).child(message.getTo()).push();
        final String key = reference.getKey();

        encryptedMessage.setId(key);
        message.setMessage_id(key);

        reference.setValue(encryptedMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                message.setSent(timeStamp);
                databaseManager.insertNewMessage(message,message.getTo());
                sendNewMessageNotification(encryptedMessage.getTo());
                messageSentCallback.onComplete(message,true,null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                messageSentCallback.onComplete(message,false,e.getLocalizedMessage());
            }
        });
    }

    void sendUserData(final User user, final UserCallback callback)
    {
        databaseReference.child(Users).child(user.getId()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                callback.result(true,user.getId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.result(false,e.toString());
            }
        });
    }

    private void sendNewMessageNotification(String toUserId) {

        //SharedPreferences sp = context.getSharedPreferences(Util.preferences,MODE_PRIVATE);
        //TODO : security
        //String currentUserId = sp.getString(Util.userId,null);
        //String accessToken = sp.getString(ACCESS_TOKEN,null);

        HashMap<String,String> params = new HashMap<>();

        params.put(USER_ID,toUserId);
        params.put(NEW_MESSAGE,NEW_MESSAGE);
        RESTHelper restHelper = new RESTHelper(context);
        restHelper.test("sendNewMessageNotification",params,SEND_NOTIFICATION_ENDPOINT,null,null);

    }

    void getNewMessages(String userId, final CompleteCallback completeCallback) throws DeviceOfflineException {

        if(!checkConnection()) {
            throw new DeviceOfflineException(DeviceOfflineException);
        }

        final ArrayList<EncryptedMessage> encryptedMessages = new ArrayList<>();

        databaseReference.child(Messages).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot d:dataSnapshot.getChildren())
                {
                    d.getRef().removeValue();
                    EncryptedMessage encryptedMessage = new EncryptedMessage();
                    encryptedMessage.setId((String) d.child(id).getValue());
                    encryptedMessage.setTo((String) d.child(to).getValue());
                    encryptedMessage.setFrom((String) d.child(from).getValue());
                    encryptedMessage.setContent((String) d.child(content).getValue());
                    encryptedMessage.setFilePath((String) d.child(filePath).getValue());
                    encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
                    encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());
                    encryptedMessages.add(encryptedMessage);
                }
                syncLocalDatabase(encryptedMessages);
                completeCallback.onComplete(encryptedMessages.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("getNewMessages",databaseError.toString());
                completeCallback.onCanceled();
            }
        });
    }

    void getUserPublic(String userId, final PublicKeyCallback publicKeyCallback)
    {
        databaseReference.child(Users).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                publicKeyCallback.onSuccess((String) dataSnapshot.child(Base64EncodedPublicKey).getValue());
                else
                    publicKeyCallback.onCancelled("no user");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                publicKeyCallback.onCancelled(databaseError.toString());
            }
        });
    }

    private void syncLocalDatabase(ArrayList<EncryptedMessage> encryptedMessages) {

        DatabaseManager2.initializeInstance(new SQLHelper(context));
        DatabaseManager2.getInstance().insertEncryptedMessages(encryptedMessages);

        RESTHelper restHelper = new RESTHelper(context);

        for (EncryptedMessage e:encryptedMessages) {
            Map<String,String> params = new HashMap<>();
            params.put(MESSAGE_ID,e.getId());
            String timeStamp = Calendar.getInstance().getTime().toString();
            params.put(RECEIVED_STATUS,timeStamp);
            params.put(USER_ID,e.getFrom());
            params.put("TEMP_USER_ID",e.getTo());
            restHelper.test("Message Id "+e.getId(),params,RESTHelper.SEND_STATUS_ENDPOINT,null,null);
        }
    }

     boolean checkConnection()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;
        try
        {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo!=null&&networkInfo.isConnected())
            {
                connected = true;
            }
            return connected;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //TODO: delete this

    /*public void sendMessage(Message message) {
        DatabaseReference ref = databaseReference.child(Messages).child("+919440186376").push();
        String key = ref.getKey();
        ref.child(key).setValue(message);
    }*/
}
