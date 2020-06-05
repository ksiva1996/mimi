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
import com.leagueofshadows.enc.Items.FirebaseMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class FirebaseHelper {

    static final String MESSAGE_ID = "MESSAGE_ID";
    private Context context;
    private DatabaseReference databaseReference;
    private DatabaseManager databaseManager;

    public static final String Messages = "Messages";
    public static final String Users = "Users";
    public static final String Groups = "Groups";

    private static final String DeviceOfflineException = "Cannot send Message without internet connection...TODO: offline capability in the next update";

    private static final String id = "id";
    private static final String to = "to";
    private static final String from = "from";
    private static final String content = "content";
    private static final String type = "type";
    private static final String filePath = "filePath";
    private static final String timeStamp = "timeStamp";
    private static final String Base64EncodedPublicKey = "base64EncodedPublicKey";
    public static final String resend = "resend";
    public static final String Files = "Files";
    private static final String flag = "isGroupMessage";

    public FirebaseHelper(Context context)
    {
        this.context = context;
        DatabaseManager.initializeInstance(new SQLHelper(context));
        databaseManager = DatabaseManager.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    public void sendTextOnlyMessage(final Message message, final EncryptedMessage encryptedMessage, final MessageSentCallback messageSentCallback, String id) throws DeviceOfflineException {

        if(!checkConnection()) {
            throw new DeviceOfflineException(DeviceOfflineException);
        }
        DatabaseReference reference;
        if(id==null) {
            reference = databaseReference.child(Messages).push();
            final String key = reference.getKey();
            encryptedMessage.setId(key);
            message.setMessage_id(key);
        }
        final ArrayList<FirebaseMessage> firebaseMessages = encryptedMessage.getFirebaseMessageList(databaseManager);

        final int[] count = {0};
        final boolean[] allSuccess = {true};
        for (FirebaseMessage fm:firebaseMessages) {
            DatabaseReference dr = databaseReference.child(Messages).child(fm.getOtherUserId()).child(id);
            dr.setValue(fm.getEncryptedMessage()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    count[0]++;
                    if(firebaseMessages.size()== count[0]){
                        sendMessageComplete(firebaseMessages,allSuccess[0],messageSentCallback,message);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    count[0]++;
                    allSuccess[0] = false;
                    if (firebaseMessages.size()==count[0]){
                        sendMessageComplete(null,false,messageSentCallback,message);
                    }
                }
            });
        }
    }

    private void sendMessageComplete(ArrayList<FirebaseMessage> firebaseMessages, boolean success, MessageSentCallback messageSentCallback, Message message){
        if(success){
            Native restHelper = new Native(context);
            if(message.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER) {
                restHelper.sendNewMessageNotification(message.getTo(), null);
            }
            else{
                for (FirebaseMessage fm:firebaseMessages) {
                    restHelper.sendNewMessageNotification(fm.getOtherUserId(),message.getTo());
                }
            }
            message.setSent(timeStamp);
            databaseManager.insertNewMessage(message,message.getTo(),message.getFrom());
            messageSentCallback.onComplete(message,true,null);
        }
        else{
            messageSentCallback.onComplete(message,false,"something shitty happened");
        }
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

    public void getNewMessages(final String userId, final CompleteCallback completeCallback) throws DeviceOfflineException {

        if(!checkConnection()) {
            throw new DeviceOfflineException(DeviceOfflineException);
        }

        final ArrayList<EncryptedMessage> encryptedMessages = new ArrayList<>();

        databaseReference.child(Messages).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot d:dataSnapshot.getChildren())
                {
                    EncryptedMessage encryptedMessage = new EncryptedMessage();
                    encryptedMessage.setId((String) d.child(id).getValue());

                    if(encryptedMessage.getId().equals(userId))
                        return;

                    encryptedMessage.setTo((String) d.child(to).getValue());
                    encryptedMessage.setFrom((String) d.child(from).getValue());
                    encryptedMessage.setContent((String) d.child(content).getValue());
                    encryptedMessage.setFilePath((String) d.child(filePath).getValue());
                    encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
                    encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());
                    if (d.hasChild(resend))
                        encryptedMessage.setResend((boolean)d.child(resend).getValue());
                    encryptedMessage.setIsGroupMessage(Integer.parseInt(Long.toString((Long) d.child(flag).getValue())));
                    encryptedMessages.add(encryptedMessage);
                    d.getRef().removeValue();
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

    public void getUserPublic(final String userId, final PublicKeyCallback publicKeyCallback)
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

        String userId = context.getSharedPreferences(Util.preferences,Context.MODE_PRIVATE).getString(Util.userId,null);
        DatabaseManager.initializeInstance(new SQLHelper(context));
        DatabaseManager.getInstance().insertEncryptedMessages(encryptedMessages);
        Native restHelper = new Native(context);
        for (EncryptedMessage e:encryptedMessages) {
            if (e.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER)
            restHelper.sendMessageReceivedStatus(e);
            else
                restHelper.sendGroupMessageReceivedStatus(e.getId(),e.getFrom(),e.getTo(),userId,"firebase helper");
        }
    }

     boolean checkConnection() {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;
        try {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo!=null&&networkInfo.isConnected()) {
                connected = true;
            }
            return connected;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
