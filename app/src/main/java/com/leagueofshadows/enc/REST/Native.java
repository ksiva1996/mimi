package com.leagueofshadows.enc.REST;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Util;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.REST.RESTHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.FirebaseHelper.Users;
import static com.leagueofshadows.enc.FirebaseReceiver.NEW_MESSAGE;
import static com.leagueofshadows.enc.FirebaseReceiver.RECEIVED_STATUS;
import static com.leagueofshadows.enc.FirebaseReceiver.SEEN_STATUS;
import static com.leagueofshadows.enc.REST.RESTHelper.RESEND_MESSAGE;
import static com.leagueofshadows.enc.REST.RESTHelper.TEMP_USER_ID;
import static com.leagueofshadows.enc.REST.RESTHelper.USER_ID;

public class Native {

    private Context context;
    private DatabaseReference databaseReference;
    private final String TOKEN = "TOKEN";

    public Native(Context context) {
        this.context = context;
        databaseReference = FirebaseDatabase.getInstance().getReference().child(Users);
    }

    public void sendNewMessageNotification(final String userId)
    {
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(NEW_MESSAGE, NEW_MESSAGE);
                    jsonObject.put(USER_ID,userId);
                    update(jsonObject,token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void sendResendMessageNotification(final EncryptedMessage message)
    {
        final String userId = message.getFrom();
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(RESEND_MESSAGE, message.getId());
                    jsonObject.put(USER_ID, userId);
                    jsonObject.put(MESSAGE_ID,message.getId());
                    jsonObject.put(TEMP_USER_ID,message.getTo());
                    update(jsonObject, token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void sendMessageReceivedStatus(final EncryptedMessage encryptedMessage)
    {
        String userId = encryptedMessage.getFrom();
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String timeStamp = Calendar.getInstance().getTime().toString();
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(RECEIVED_STATUS,timeStamp);
                    jsonObject.put(USER_ID,encryptedMessage.getFrom());
                    jsonObject.put(MESSAGE_ID,encryptedMessage.getId());
                    jsonObject.put(TEMP_USER_ID,encryptedMessage.getTo());
                    update(jsonObject,token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void sendMessageSeenStatus(final Message message)
    {
        String userId = message.getFrom();
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String timeStamp = Calendar.getInstance().getTime().toString();
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(SEEN_STATUS,timeStamp);
                    jsonObject.put(USER_ID,message.getFrom());
                    jsonObject.put(MESSAGE_ID,message.getMessage_id());
                    jsonObject.put(TEMP_USER_ID,message.getTo());
                    update(jsonObject,token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void update(final JSONObject j, final String token) {

        String url = "https://fcm.googleapis.com/fcm/send";
        try {
            JSONObject f = new JSONObject();
            f.put("data", j);
            f.put("to", token);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,url, f, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    //Log.e("response",response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("error",error.toString());
                }
            }){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=" + getKey());
                    //headers.put("Content-Type","application/json");
                    return headers;
                }
            };
            VolleyHelper.getInstance(context).addToRequestQueue(jsonObjectRequest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getKey() {
        //TODO : more secure management of api key
        return Util.key;
    }

    public void updateToken(String s) {
        String userId = context.getSharedPreferences(Util.preferences,Context.MODE_PRIVATE).getString(Util.userId,null);
        if(userId!=null) {
            databaseReference.child(userId).child(TOKEN).setValue(s);
        }
    }
}
