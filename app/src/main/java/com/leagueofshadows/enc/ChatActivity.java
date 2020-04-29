package com.leagueofshadows.enc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.GONE;
import static com.leagueofshadows.enc.FirebaseHelper.Messages;

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
    DatabaseReference databaseReference;

    EditText messageField;
    LinearLayout chatReplyLayout;
    TextView replyName;
    TextView replyMessageText;
    ImageButton closeReplyLayout;

    ImageButton send;
    AESHelper aesHelper;
    Native restHelper;

    private Message replyMessage;

    ImageButton addFile;
    ImageButton addImage;
    ImageButton openCamera;
    ImageButton attachment;
    boolean isAttachmentLayoutOpen = false;

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

    public static final int FILE_ATTACHMENT_REQUEST = 1;
    public static final int IMAGE_ATTACHMENT_REQUEST = 2;
    public static final int OPEN_CAMERA_REQUEST = 3;
    public static final int IMAGE_SELECTED = 4;

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

        databaseReference = FirebaseDatabase.getInstance().getReference().child(Messages).child(otherUserId);

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

         addFile = findViewById(R.id.file);
         addImage = findViewById(R.id.image);
         openCamera = findViewById(R.id.camera);

         addFile.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("*/*");
                 startActivityForResult(intent,FILE_ATTACHMENT_REQUEST);
             }
         });
         addImage.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("image/*");
                 startActivityForResult(Intent.createChooser(intent,"Select picture"),IMAGE_ATTACHMENT_REQUEST);
             }
         });
         openCamera.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                 File file = new File(getApplicationContext().getFilesDir(),"current.jpg");
                 Uri uri = FileProvider.getUriForFile(ChatActivity.this,"com.leagueofshadows.enc.fileProvider",file);
                 intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
                 intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                 startActivityForResult(Intent.createChooser(intent,"take picture using"),OPEN_CAMERA_REQUEST);
             }
         });

         attachment = findViewById(R.id.attachment);
         attachment.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 setAttachmentLayout();
             }
         });

         recyclerAdapter = new RecyclerAdapter(messages,this,currentUserId,otherUserId,this);
         listView.setAdapter(recyclerAdapter);
         smoothScroller = new LinearSmoothScroller(this){
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_END;
            }
        };

        messageField = findViewById(R.id.chat_edit_text);

        chatReplyLayout = findViewById(R.id.chat_reply);
        chatReplyLayout.setVisibility(GONE);

        replyName = findViewById(R.id.reply_name);
        replyMessageText = findViewById(R.id.reply_message);
        closeReplyLayout = findViewById(R.id.close_reply);

        closeReplyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatReplyLayout.setVisibility(GONE);
                replyMessage = null;
            }
        });

        send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

     private void setAttachmentLayout() {
         if(isAttachmentLayoutOpen)
         {
             isAttachmentLayoutOpen = false;
             attachment.setImageResource(R.drawable.add);
             addFile.animate().translationY(getResources().getDimension(R.dimen.st_55));
             addFile.setVisibility(View.VISIBLE);
             addImage.animate().translationY(getResources().getDimension(R.dimen.st_100));
             addImage.setVisibility(View.VISIBLE);
             openCamera.animate().translationY(getResources().getDimension(R.dimen.st_145));
             openCamera.setVisibility(View.VISIBLE);
         }
         else
         {
             isAttachmentLayoutOpen = true;
             attachment.setImageResource(R.drawable.baseline_attachment_white_24);
             addFile.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             addImage.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             openCamera.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             addFile.setVisibility(View.GONE);
             addImage.setVisibility(View.GONE);
             openCamera.setVisibility(View.GONE);
         }
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

                         String id = databaseReference.push().getKey();

                         assert id != null;

                         Message message = new Message(0,id,otherUserId,currentUserId,messageString,null,
                                 timeStamp,Message.MESSAGE_TYPE_ONLYTEXT,null,null,null);

                         EncryptedMessage e = new EncryptedMessage(id,otherUserId,currentUserId,cipherText,null,timeStamp,EncryptedMessage.MESSAGE_TYPE_ONLYTEXT);
                         firebaseHelper.sendTextOnlyMessage(message,e,ChatActivity.this,id);
                         updateNewMessage(message);

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

     @Override
     protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

         String wrongString = "Something went wrong please try again";

         super.onActivityResult(requestCode, resultCode, data);
         if (resultCode==RESULT_OK)
         {
             if (requestCode==FILE_ATTACHMENT_REQUEST) {
                 try {
                     assert data != null;
                     Uri uri = data.getData();
                     sendFile(uri);
                 }catch (Exception e) {
                     e.printStackTrace();
                     Toast.makeText(this,wrongString,Toast.LENGTH_SHORT).show();
                 }
             }
             if (requestCode==IMAGE_ATTACHMENT_REQUEST) {
                 try{
                     assert data != null;
                     Uri uri = data.getData();
                     sendImage(uri);
                 }catch (Exception e) {
                     e.printStackTrace();
                 }
             }
             if (requestCode==OPEN_CAMERA_REQUEST) {
                 Intent intent = new Intent(this,ImagePreview.class);
                 startActivityForResult(intent,IMAGE_SELECTED);
             }
             if (requestCode==IMAGE_SELECTED) {
                 Uri uri = Uri.fromFile(new File(getApplicationContext().getFilesDir(),"current.jpg"));
                 sendImage(uri);
             }
         }
         else { Toast.makeText(this,"Canceled",Toast.LENGTH_SHORT).show(); }
     }

     private void sendImage(final Uri uri)
     {
         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 Message message = null;
                 try {
                     Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                     int compressionFactor = getCompressionFactor(bitmap.getByteCount());
                     String path = Util.imagesPath+otherUser.getName();
                     checkPath(path,IMAGE_ATTACHMENT_REQUEST);
                     SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMDD-HHmmss");
                     String fileName = "IMG-"+simpleDateFormat.format(new Date())+".jpg";
                     String timeStamp = Calendar.getInstance().getTime().toString();

                     path = path+"/sent/"+fileName;
                     FileOutputStream fileOutputStream = new FileOutputStream(path);
                     bitmap.compress(Bitmap.CompressFormat.JPEG,compressionFactor,fileOutputStream);

                     String id = databaseReference.push().getKey();

                     assert id != null;
                     message = new Message(0,id,otherUserId,currentUserId,fileName,path,timeStamp,Message.MESSAGE_TYPE_IMAGE,
                             null,null,null);

                     updateNewMessage(message);
                     AESHelper aesHelper = new AESHelper(ChatActivity.this);
                     FileInputStream fileInputStream = new FileInputStream(path);
                     fileOutputStream = new FileOutputStream(Util.privatePath+fileName);
                     App app = (App) getApplication();
                     aesHelper.encryptFile(fileInputStream,fileOutputStream,app.getPrivateKey(),otherUser.getBase64EncodedPublicKey());
                     Intent intent = new Intent(ChatActivity.this,FileUploadService.class);
                     intent.putExtra(Util.toUserId,otherUserId);
                     intent.putExtra(Util.userId,currentUserId);
                     intent.putExtra(Util.fileName,fileName);
                     intent.putExtra(Util.timeStamp,timeStamp);
                     intent.putExtra(Util.name,otherUser.getName());
                     intent.putExtra(Util.uri,uri.toString());
                     intent.putExtra(Util.id,id);
                     intent.putExtra(Util.type, Message.MESSAGE_TYPE_IMAGE);
                     startService(intent);

                 } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                         BadPaddingException | RunningOnMainThreadException | InvalidKeyException |
                         InvalidAlgorithmParameterException | IOException | IllegalBlockSizeException e) {
                     if(message!=null) {
                         int position = messageIds.indexOf(message.getMessage_id());
                         messages.remove(message);
                         messageIds.remove(message.getMessage_id());
                         updateMessageRemoved(position);
                     }
                     e.printStackTrace();
                 }
             }
         });
     }

     private int getCompressionFactor(int byteCount) {

         final int byteUpperLimit = 3145728;
         final int byteLowerLimit = 307200;
         final int upperCompression = 10;
         final int lowerCompression = 80;
         if(byteCount>byteUpperLimit)
             return upperCompression;
         else if(byteCount<byteLowerLimit)
             return lowerCompression;
         else
         {
             int factor = (byteUpperLimit - byteLowerLimit)/(lowerCompression-upperCompression);
             return (byteCount-byteLowerLimit)/factor + 10;
         }
     }

     private void sendFile(final Uri uri) throws FileNotFoundException {

         final FileInputStream fileInputStream = (FileInputStream) getContentResolver().openInputStream(uri);
         final String fileName = getFileName(uri);
         String path = Util.documentsPath+otherUser.getName();
         checkPath(path,FILE_ATTACHMENT_REQUEST);
         path = path+"/sent/"+fileName;
         final FileOutputStream fileOutputStream  = new FileOutputStream(path);

         final String timeStamp = Calendar.getInstance().getTime().toString();

         final String id = databaseReference.push().getKey();

         assert id != null;
         final Message message = new Message(0,id,otherUserId,currentUserId,fileName,uri.toString(),timeStamp,
                 Message.MESSAGE_TYPE_FILE,null,null,null);
         updateNewMessage(message);

         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 try {
                     AESHelper aesHelper = new AESHelper(ChatActivity.this);
                     assert fileInputStream != null;
                     App app = (App) getApplication();
                     aesHelper.encryptFile(fileInputStream,fileOutputStream,app.getPrivateKey(),otherUser.getBase64EncodedPublicKey());
                     Intent intent = new Intent(ChatActivity.this,FileUploadService.class);
                     intent.putExtra(Util.toUserId,otherUserId);
                     intent.putExtra(Util.userId,currentUserId);
                     intent.putExtra(Util.fileName,fileName);
                     intent.putExtra(Util.timeStamp,timeStamp);
                     intent.putExtra(Util.name,otherUser.getName());
                     intent.putExtra(Util.uri,uri.toString());
                     intent.putExtra(Util.id,id);
                     startService(intent);

                 } catch (NoSuchAlgorithmException | NoSuchPaddingException | RunningOnMainThreadException |
                         IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException |
                         InvalidKeyException | IOException | InvalidAlgorithmParameterException e) {
                            int position = messageIds.indexOf(message.getMessage_id());
                            messages.remove(message);
                            messageIds.remove(message.getMessage_id());
                            updateMessageRemoved(position);
                     e.printStackTrace();
                 }
             }
         });

     }

     @SuppressWarnings("ResultOfMethodCallIgnored")
     private void checkPath(String path, int x) {
         File file = new File(Util.originalPath);
         if(!file.exists())
             file.mkdir();

         file = new File(Util.privatePath);
         if(!file.exists())
             file.mkdir();

         if(x == IMAGE_ATTACHMENT_REQUEST) {
             file = new File(Util.imagesPath);
             if(!file.exists())
                 file.mkdir();
         }
         if(x==FILE_ATTACHMENT_REQUEST) {
             file = new File(Util.documentsPath);
             if(!file.exists())
                 file.mkdir();
         }

         file = new File(path);
         if(!file.exists())
             file.mkdir();

         file = new File(path+"/sent");
         if(!file.exists())
             file.mkdir();
     }

     private String getFileName(Uri uri) {
         Cursor cursor = getContentResolver().query(uri,null,null,null,null);
         int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
         cursor.moveToFirst();
         String name = cursor.getString(index);
         cursor.close();
         return name;
     }


     //options for messages

    void deleteMessage(final Message message, final int position)
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
                recyclerAdapter.notifyItemRangeRemoved(position,1);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.activity_info,null);

        TextView deliveredTime = view.findViewById(R.id.deliver);
        TextView readTime = view.findViewById(R.id.read);

        if(message.getReceived()!=null)
            deliveredTime.setText(message.getReceived());
        if(message.getSeen()!=null)
            readTime.setText(message.getSeen());

        builder.setView(view);
        builder.create().show();
    }

    void messageCopy(Message message)
    {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("message text",message.getContent());
            assert clipboardManager != null;
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(this,"text copied to clipboard",Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void replyToMessage(Message message)
    {
        chatReplyLayout.setVisibility(View.VISIBLE);
        replyMessageText.setText(message.getContent());
        String name;
        if(message.getFrom().equals(currentUserId))
            name = "you";
        else
            name = otherUser.getName();
        replyName.setText(name);
        replyMessage = message;
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

     void updateMessageRemoved(final int position)
     {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 recyclerAdapter.notifyItemRangeRemoved(position,1);
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
            if(messageIds.contains(message.getMessage_id()))
            {
                int position = messageIds.indexOf(message.getMessage_id());
                messages.remove(position);
                messageIds.remove(message.getMessage_id());
                updateMessageRemoved(position);
            }
        }
     }

     public void updateNewMessage(final Message message) {
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
         Message message = messages.get(position);
         switch (option)
         {
             case MESSAGE_COPY: {
                 messageCopy(message);
                 return;
             }
             case MESSAGE_DELETE:{
                 deleteMessage(message,position);
                 return;
             }
             case MESSAGE_INFO:{
                 messageInfo(message);
                 return;
             }
             case MESSAGE_REPLY:{
                 replyToMessage(message);
             }
         }

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
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

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
                         final TextReceived h = (TextReceived) holder;
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
                                 if(h.container.isOpen()) {
                                     messageOptionsCallback.onOptionsSelected(MESSAGE_INFO, position);
                                     h.container.close(true);
                                 }
                             }
                         });
                         h.deleteButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {
                                 if(h.container.isOpen()) {
                                     messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                     h.container.close(true);
                                 }
                             }
                         });
                         h.replyButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {
                                 if(h.container.isOpen()) {
                                     messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                     h.container.close(true);
                                 }
                             }
                         });
                         h.copyButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {
                                 if(h.container.isOpen()) {
                                     messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                     h.container.close(true);
                                 }
                             }
                         });
                     }
                 }
                 if (message.getFrom().equals(currentUserId)) {

                     final TextSent h = (TextSent) holder;
                     h.message.setText(message.getContent());
                     h.time.setText(formatTime(message
                             .getTimeStamp()));
                     if(message.getSeen()!=null)
                     {
                         h.received.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.seen.setVisibility(View.VISIBLE);
                     }
                     else if(message.getReceived()!=null)
                     {
                         h.seen.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.received.setVisibility(View.VISIBLE);
                     }
                     else if(message.getSent()!=null)
                     {
                         h.seen.setVisibility(GONE);
                         h.received.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.sent.setVisibility(View.VISIBLE);
                     }
                     else
                     {
                         h.seen.setVisibility(GONE);
                         h.received.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(View.VISIBLE);
                     }
                     h.container.close(false);

                     if (flag)
                         h.corner.setVisibility(View.INVISIBLE);
                     else
                         h.corner.setVisibility(View.VISIBLE);
                     h.infoButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_INFO, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.deleteButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.replyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.copyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                 }
             }
            else
            {
                //TODO: other views
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
