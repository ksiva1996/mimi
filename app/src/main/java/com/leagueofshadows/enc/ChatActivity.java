package com.leagueofshadows.enc;

import android.content.DialogInterface;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.MessageOptionsCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Interfaces.ScrollEndCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class ChatActivity extends AppCompatActivity implements MessagesRetrievedCallback, MessageSentCallback,
        ScrollEndCallback, PublicKeyCallback, ResendMessageCallback, MessageOptionsCallback
 {

    ArrayList<Message> messages;
    ArrayList<String> messageIds;
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
    Native restHelper;
    public static  final int RECEIVE_TEXT = 0;
    public static  final int RECEIVE_IMAGE = 1;
    public static  final int RECEIVE_FILE = 2;
    public static  final int SEND_TEXT = 3;
    public static  final int SEND_IMAGE = 4;
    public static  final int SEND_FILE = 5;
    public static final int RECEIVE_ERROR = 6;

    public static final int MESSAGE_INFO = 1;
    public static final int MESSAGE_DELETE = 2;
    public static final int MESSAGE_COPY = 3;
    public static final int MESSAGE_REPLY = 4;

     RecyclerView.SmoothScroller smoothScroller;
     private LinearLayoutManager layoutManager;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra(Util.userId);

        getSupportActionBar().setDisplayShowHomeEnabled(true);

        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        databaseManager.setNewMessageCounter(otherUserId);

        otherUser = databaseManager.getUser(otherUserId);

        setTitle(otherUser.getName());
        sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        currentUserId = sp.getString(Util.userId,null);
        assert currentUserId!=null;

        messages = new ArrayList<>();
        messageIds = new ArrayList<>();
        firebaseHelper = new FirebaseHelper(this);
         try {
             aesHelper = new AESHelper(this);
         } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
             e.printStackTrace();
         }

         restHelper = new Native(this);
         listView = findViewById(R.id.listView);
         layoutManager = new LinearLayoutManager(this);
         layoutManager.setStackFromEnd(true);
         listView.setLayoutManager(layoutManager);

         recyclerAdapter = new RecyclerAdapter(messages,this,currentUserId,otherUserId,this);
         listView.setAdapter(recyclerAdapter);
         smoothScroller = new LinearSmoothScroller(this){
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_END;
            }
        };

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
                 messageString = messageString.trim().replaceAll("\n","");
                 if(!messageString.equals(""))
                 {
                     App app = (App) getApplication();
                     try {
                         String cipherText = aesHelper.encryptMessage(messageString,otherUser.getBase64EncodedPublicKey(),app.getPrivateKey());
                         String timeStamp = Calendar.getInstance().getTime().toString();

                         Message message = new Message(0,timeStamp,otherUserId,currentUserId,messageString,null,
                                 timeStamp,Message.MESSAGE_TYPE_ONLYTEXT,null,null,null);
                         EncryptedMessage e = new EncryptedMessage(timeStamp,otherUserId,currentUserId,cipherText,null,timeStamp,EncryptedMessage.MESSAGE_TYPE_ONLYTEXT);
                         firebaseHelper.sendTextOnlyMessage(message,e,ChatActivity.this);

                     } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeySpecException | RunningOnMainThreadException | DeviceOfflineException e) {
                         e.printStackTrace();
                     }
                 }
             }
         });
     }

     void getMessages() {

        ArrayList<Message> m = databaseManager.getMessages(otherUser.getId(),messages.size(),100);
        for (int i = m.size()-1;i>=0;i--) {
            Message message = m.get(i);
            if(message.getSeen()==null && message.getFrom().equals(otherUserId))
            {
                String timeStamp = Calendar.getInstance().getTime().toString();
                message.setSeen(timeStamp);
                databaseManager.updateMessageSeenStatus(timeStamp,message.getMessage_id(),otherUserId);
                if(message.getContent()!=null)
                restHelper.sendMessageSeenStatus(message);
            }
            messages.add(0,message);
            messageIds.add(message.getMessage_id());
        }
        listView.post(new Runnable() {
            @Override
            public void run() {
                recyclerAdapter.notifyDataSetChanged();
                int x = messages.size()-1;
                if(x>=0) {
                    smoothScroller.setTargetPosition(messages.size() - 1);
                    layoutManager.startSmoothScroll(smoothScroller);
                }
            }
        });
    }

    //options for messages

    void deleteMessage(final Message message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String m;
        if(message.getFrom().equals(currentUserId))
            m = "Delete message ?";
        else
            m = "Delete message from "+otherUser.getName();
        builder.setMessage(m);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseManager.deleteMessage(message,currentUserId);
                messages.remove(message);
                messageIds.remove(message.getMessage_id());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    void messageInfo(final Message message)
    {

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
         app.setResendMessageCallback(this);
         firebaseHelper.getUserPublic(otherUserId,this);
         if(messages.isEmpty()) {
             getMessages();
         }
     }

     @Override
     protected void onPause() {
         super.onPause();
         App app = (App) getApplication();
         app.setMessagesRetrievedCallback(null);
         app.setResendMessageCallback(null);
     }

     void updateRecyclerAdapter(final int position)
     {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 recyclerAdapter.notifyItemChanged(position);
             }
         });
     }

     //MessageSentCallback override methods

     @Override
     public void onComplete(final Message message, boolean success, String error) {
        if(success) {

            final String messageId = message.getMessage_id();
            if(messageIds.contains(messageId))
            {
                int position = messageIds.indexOf(messageId);
                messages.set(position,message);
                updateRecyclerAdapter(position);
            }
        }
        else {
            Log.e("error",error);
        }
     }

     @Override
     public void onKey(final Message message) {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 messageField.setText("");
                 messages.add(message);
                 messageIds.add(message.getMessage_id());
                 recyclerAdapter.notifyDataSetChanged();
                 smoothScroller.setTargetPosition(messages.size()-1);
                 layoutManager.startSmoothScroll(smoothScroller);
             }
         });
     }

     //MessageRetrievedCallback override methods

     @Override
     public void onNewMessage(final Message message) {

         if(message.getFrom().equals(otherUserId))
         {
             databaseManager.setNewMessageCounter(otherUserId);
             String timeStamp = Calendar.getInstance().getTime().toString();
             message.setSeen(timeStamp);
             restHelper.sendMessageSeenStatus(message);
             databaseManager.updateMessageSeenStatus(timeStamp,message.getMessage_id(),otherUserId);
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     if(!messageIds.contains(message.getMessage_id())) {
                         messages.add(message);
                         messageIds.add(message.getMessage_id());
                         recyclerAdapter.notifyDataSetChanged();
                         smoothScroller.setTargetPosition(messages.size() - 1);
                         layoutManager.startSmoothScroll(smoothScroller);
                     }
                 }
             });
         }
             //TODO : show notifications
     }


     @Override
     public void onUpdateMessageStatus(final String messageId, final String userId) {

        if (userId.equals(otherUserId))
        {
            if(messageIds.contains(messageId))
            {
                int position = messageIds.indexOf(messageId);
                Message message = databaseManager.getMessage(messageId,userId);
                messages.set(position,message);
                updateRecyclerAdapter(position);
            }
        }
     }

     //ScrollEndCallback override methods

     @Override
     public void scrollEndReached() { getMessages(); }


     //PublicKeyCallback override methods

     @Override
     public void onSuccess(String Base64PublicKey) {
         databaseManager.insertPublicKey(Base64PublicKey,otherUserId);
         otherUser = databaseManager.getUser(otherUserId);
     }

     @Override
     public void onCancelled(String error) {}

     //MessageResendCallback override methods

     @Override
     public void newResendMessageCallback(Message message) {
         if(messageIds.contains(message.getMessage_id()))
         {
             int position = messageIds.indexOf(message.getMessage_id());
             messages.set(position,message);
             updateRecyclerAdapter(position);
         }
     }

     //Message options callback

     @Override
     public void onOptionsSelected(int option, int position) {

     }

     static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<Message> messages;
        private ScrollEndCallback scrollEndCallback;
        private String otherUserId;
        private String currentUserId;
        private MessageOptionsCallback messageOptionsCallback;

        /*void set(ArrayList<UserData> userDataArrayList) {
            this.userDataArrayList = userDataArrayList;
        }*/

        static class TextReceivedError extends RecyclerView.ViewHolder{
            TextView time;
            ImageView corner;
            TextReceivedError(View view)
            {
                super(view);
                time = view.findViewById(R.id.time);
                corner = view.findViewById(R.id.corner);
            }
        }

        static class TextReceived extends RecyclerView.ViewHolder {

            TextView message;
            TextView time;
            SwipeRevealLayout container;
            ImageView corner;
            ImageButton copyButton;
            ImageButton replyButton;
            ImageButton deleteButton;
            ImageButton infoButton;

            TextReceived(View view) {
                super(view);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                container = view.findViewById(R.id.container);
                corner = view.findViewById(R.id.triangle);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                infoButton = view.findViewById(R.id.info_button);
            }
        }

         static class TextSent extends RecyclerView.ViewHolder {
             TextView message;
             TextView time;
             SwipeRevealLayout container;
             ImageView corner;
             ProgressBar pg;
             ImageView sent;
             ImageView received;
             ImageView seen;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             ImageButton infoButton;

             TextSent(View view) {
                 super(view);
                 message = view.findViewById(R.id.message);
                 time = view.findViewById(R.id.time);
                 container = view.findViewById(R.id.container);
                 sent = view.findViewById(R.id.sent);
                 received = view.findViewById(R.id.received);
                 seen = view.findViewById(R.id.seen);
                 corner = view.findViewById(R.id.triangle);
                 pg = view.findViewById(R.id.waiting);
                 copyButton = view.findViewById(R.id.copy_button);
                 replyButton = view.findViewById(R.id.reply_button);
                 deleteButton = view.findViewById(R.id.delete_button);
                 infoButton = view.findViewById(R.id.info_button);
             }
         }

        RecyclerAdapter(ArrayList<Message> messages,ScrollEndCallback scrollEndCallback,
                        String currentUserId,String otherUserId,MessageOptionsCallback messageOptionsCallback) {

            this.messages = messages;
            this.scrollEndCallback = scrollEndCallback;
            this.currentUserId = currentUserId;
            this.otherUserId = otherUserId;
            this.messageOptionsCallback = messageOptionsCallback;
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
                case RECEIVE_ERROR:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_msg_error, parent, false);
                    return new TextReceivedError(itemView);
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
             if(message.getType()==Message.MESSAGE_TYPE_ONLYTEXT) {
                 if (message.getFrom().equals(otherUserId)) {
                     if (message.getContent()==null)
                         return RECEIVE_ERROR;
                     else
                         return RECEIVE_TEXT;
                 } else
                     return SEND_TEXT;
             }
             else if(message.getType()==Message.MESSAGE_TYPE_IMAGE) {
                 if(message.getFrom().equals(otherUserId))
                     return RECEIVE_IMAGE;
                 else
                     return SEND_IMAGE;
             }
             else if(message.getType()==Message.MESSAGE_TYPE_FILE)
             {
                 if(message.getFrom().equals(otherUserId))
                     return RECEIVE_FILE;
                 else
                     return SEND_FILE;
             }
             else {
                 return -1;
             }
         }

         @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {

            if(position==0&&messages.size()>100) {
                scrollEndCallback.scrollEndReached();
            }

            Message message = messages.get(position);
            if(message.getType()==Message.MESSAGE_TYPE_ONLYTEXT)
             {
                 boolean flag = false;
                 if (position != 0) {
                     Message prev = messages.get(position - 1);
                     flag = check(message, prev);
                 }
                 if (message.getFrom().equals(otherUserId)) {
                     if(message.getContent()==null)
                     {
                         TextReceivedError h = (TextReceivedError) holder;
                         if(flag)
                             h.corner.setVisibility(View.INVISIBLE);
                         else
                             h.corner.setVisibility(View.INVISIBLE);
                         h.time.setText(formatTime(message.getTimeStamp()));
                     }
                     else
                     {
                         TextReceived h = (TextReceived) holder;
                         h.message.setText(message.getContent());
                         h.time.setText(formatTime(message
                                 .getTimeStamp()));
                         h.container.close(false);
                         if (flag)
                             h.corner.setVisibility(View.INVISIBLE);
                         else
                             h.corner.setVisibility(View.VISIBLE);
                         h.infoButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {

                             }
                         });
                         h.deleteButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {

                             }
                         });
                         h.replyButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {

                             }
                         });
                         h.copyButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {

                             }
                         });
                     }

                 }
                 if (message.getFrom().equals(currentUserId)) {
                     TextSent h = (TextSent) holder;
                     h.message.setText(message.getContent());
                     h.time.setText(formatTime(message
                             .getTimeStamp()));
                     if(message.getSeen()!=null)
                     {
                         h.received.setVisibility(View.GONE);
                         h.sent.setVisibility(View.GONE);
                         h.pg.setVisibility(View.GONE);
                         h.seen.setVisibility(View.VISIBLE);
                     }
                     else if(message.getReceived()!=null)
                     {
                         h.seen.setVisibility(View.GONE);
                         h.sent.setVisibility(View.GONE);
                         h.pg.setVisibility(View.GONE);
                         h.received.setVisibility(View.VISIBLE);
                     }
                     else if(message.getSent()!=null)
                     {
                         h.seen.setVisibility(View.GONE);
                         h.received.setVisibility(View.GONE);
                         h.pg.setVisibility(View.GONE);
                         h.sent.setVisibility(View.VISIBLE);
                     }
                     else
                     {
                         h.seen.setVisibility(View.GONE);
                         h.received.setVisibility(View.GONE);
                         h.sent.setVisibility(View.GONE);
                         h.pg.setVisibility(View.VISIBLE);
                     }

                     h.container.close(false);

                     if (flag)
                         h.corner.setVisibility(View.INVISIBLE);
                     else
                         h.corner.setVisibility(View.VISIBLE);
                 }
             }
            else
            {
                //TODO:
            }
        }

         private boolean check(@NonNull Message message, @NonNull Message prev) {
             return message.getFrom().equals(prev.getFrom());
         }

         private String formatTime(String received) {
            received =received.substring(4,16);
            return received;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}
