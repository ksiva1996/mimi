package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ScrollEndCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.RESTHelper;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.FirebaseHelper.MESSAGE_ID;
import static com.leagueofshadows.enc.FirebaseReceiver.SEEN_STATUS;
import static com.leagueofshadows.enc.REST.RESTHelper.USER_ID;

public class ChatActivity extends AppCompatActivity implements MessagesRetrievedCallback, MessageSentCallback,
        PublicKeyCallback, ScrollEndCallback
 {

    ArrayList<Message> messages;
    RecyclerView listView;
    RecyclerAdapter recyclerAdapter;
    User otherUser;
    String otherUserId;
    String currentUserId;
    DatabaseManager2 databaseManager;
    SharedPreferences sp;
    FirebaseHelper firebaseHelper;
    EditText messageField;
    ImageButton send;
    AESHelper aesHelper;
    public static  final int RECEIVE_TEXT = 0;
    public static  final int RECEIVE_IMAGE = 1;
    public static  final int RECEIVE_FILE = 2;
    public static  final int SEND_TEXT = 3;
    public static  final int SEND_IMAGE = 4;
    public static  final int SEND_FILE = 5;


     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra(Util.userId);

        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();

        otherUser = databaseManager.getUser(otherUserId);

        setTitle(otherUser.getName());
        sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        currentUserId = sp.getString(Util.userId,null);
        assert currentUserId!=null;

        messages = new ArrayList<>();
        firebaseHelper = new FirebaseHelper(this);
         try {
             aesHelper = new AESHelper(this);
         } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
             e.printStackTrace();
         }

         listView = findViewById(R.id.listView);
         LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(layoutManager);

        recyclerAdapter = new RecyclerAdapter(messages,this,currentUserId,otherUserId);
        listView.setAdapter(recyclerAdapter);

        messageField = findViewById(R.id.chat_edit_text);
        send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

    }

     private void sendMessage() {
         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 String messageString = messageField.getText().toString();
                 if(!messageString.equals(""))
                 {
                     App app = (App) getApplication();
                     try {
                         String cipherText = aesHelper.encryptMessage(messageString,otherUser.getBase64EncodedPublicKey(),app.getPrivateKey());
                         String timeStamp = Calendar.getInstance().getTime().toString();

                         Message message = new Message(0,timeStamp,otherUserId,currentUserId,messageString,null,
                                 timeStamp,Message.MESSAGE_TYPE_ONLYTEXT,timeStamp,null,null);
                         EncryptedMessage e = new EncryptedMessage(timeStamp,otherUserId,currentUserId,cipherText,null,timeStamp,EncryptedMessage.MESSAGE_TYPE_ONLYTEXT);
                         firebaseHelper.sendTextOnlyMessage(message,e,ChatActivity.this);

                     } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeySpecException | RunningOnMainThreadException | DeviceOfflineException e) {
                         e.printStackTrace();
                     }
                 }
             }
         });
     }

     void sendSeenStatus(Message e)
     {
         //TODO :
         //String currentUserId = sp.getString(Util.userId,null);
         //String accessToken = sp.getString(ACCESS_TOKEN,null);

         String timeStamp = Calendar.getInstance().getTime().toString();
         RESTHelper restHelper = new RESTHelper(this);
         HashMap<String, String> params = new HashMap<>();
         params.put(SEEN_STATUS, timeStamp);
         params.put(MESSAGE_ID, e.getMessage_id());
         params.put(USER_ID,e.getTo());
         restHelper.test("message seen status", params, RESTHelper.SEND_STATUS_ENDPOINT, null, null);
     }

     void getMessages() {

        ArrayList<Message> m = databaseManager.getMessages(otherUser.getId(),messages.size(),100);
        for (int i = m.size()-1;i>=0;i--) {
            messages.add(0,m.get(i));
        }
        listView.post(new Runnable() {
            @Override
            public void run() {
                recyclerAdapter.notifyDataSetChanged();
            }
        });
    }



     @Override
     protected void onResume() {
         super.onResume();
         App app = (App) getApplication();

         if(app.isnull())
         {
             Intent intent = new Intent(ChatActivity.this,Login.class);
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
             startActivity(intent);
             finish();
         }

         app.setMessagesRetrievedCallback(this);

         if(messages.isEmpty()) {
             getMessages();
         }
     }

     @Override
     public void onComplete(Message message, boolean success, String error) {
        if(success) {
            messages.add(message);
            recyclerAdapter.notifyDataSetChanged();
            listView.smoothScrollToPosition(messages.size()-1);
        }
        else
        {
            //TODO : see what happens
        }
     }

     @Override
     public void onNewMessage(final Message message) {

         if(message.getFrom().equals(otherUserId))
         {
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     messages.add(message);
                     recyclerAdapter.notifyDataSetChanged();
                     listView.smoothScrollToPosition(messages.size()-1);
                 }
             });

         }
         else {
             databaseManager.incrementNewMessageCount(message.getFrom(),message.getMessage_id());
         }

     }

     void updateMessage(final int position)
     {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 recyclerAdapter.notifyItemChanged(position);
             }
         });
     }

     @Override
     public void onUpdateMessageStatus(final String messageId, final String userId) {

         Log.e("user",userId);
        if (userId.equals(otherUserId))
        {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    int x=-1;
                    for(int i=messages.size()-1;i>=0;i--)
                    {
                        if(messageId.equals(messages.get(i).getMessage_id()))
                        {
                            x=i;
                            break;
                        }
                    }
                    if(x!=-1) {
                        Message message = databaseManager.getMessage(messageId,otherUserId);
                        messages.set(x,message);
                        updateMessage(x);
                    }
                }
            });
        }
     }

     @Override
     public void onCanceled() {

     }

     @Override
     public void onSuccess(String Base64PublicKey) {

     }

     @Override
     public void onCancelled(String error) {

     }

     @Override
     public void scrollEndReached() {
         getMessages();
     }

     static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<Message> messages;
        private ScrollEndCallback scrollEndCallback;
        private String otherUserId;
        private String currentUserId;

        /*void set(ArrayList<UserData> userDataArrayList) {
            this.userDataArrayList = userDataArrayList;
        }*/

        static class TextReceived extends RecyclerView.ViewHolder {

            TextView message;
            TextView time;
            SwipeRevealLayout container;

            TextReceived(View view) {
                super(view);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                container = view.findViewById(R.id.container);
            }
        }

         static class TextSent extends RecyclerView.ViewHolder {
             TextView message;
             TextView time;
             SwipeRevealLayout container;
             ImageView ticks;

             TextSent(View view) {
                 super(view);
                 message = view.findViewById(R.id.message);
                 time = view.findViewById(R.id.time);
                 container = view.findViewById(R.id.container);
                 ticks = view.findViewById(R.id.user_reply_status);
             }
         }

        RecyclerAdapter(ArrayList<Message> messages,ScrollEndCallback scrollEndCallback,String currentUserId,String otherUserId) {
            this.messages = messages;
            this.scrollEndCallback = scrollEndCallback;
            this.currentUserId = currentUserId;
            this.otherUserId = otherUserId;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            switch (viewType)
            {
                case RECEIVE_TEXT:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.receive_msg, parent, false);
                    return new TextReceived(itemView);
                }
                default:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.send_msg, parent, false);
                    return new TextSent(itemView);
                }
            }
        }

         @Override
         public int getItemViewType(int position) {
             Message message = messages.get(position);
             if(message.getFrom().equals(otherUserId))
                 return RECEIVE_TEXT;
             else
                 return SEND_TEXT;
         }

         @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {

            if(position==0&&messages.size()>100) {
                scrollEndCallback.scrollEndReached();
            }

            Message message = messages.get(position);
            if(message.getFrom().equals(otherUserId))
            {
                TextReceived h = (TextReceived) holder;
                h.message.setText(message.getContent());
                h.time.setText(formatTime(message
                .getTimeStamp()));
                h.container.close(false);

            }
            if(message.getFrom().equals(currentUserId))
            {
                TextSent h = (TextSent) holder;
                h.message.setText(message.getContent());
                h.time.setText(formatTime(message
                        .getTimeStamp()));
                if(message.getReceived()==null)
                    h.ticks.setVisibility(View.GONE);
                else
                    h.ticks.setVisibility(View.VISIBLE);
                h.container.close(false);
            }

        }

        private String formatTime(String received) {
            //TODO :
            received =received.substring(4,16);
            return received;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}
