package com.leagueofshadows.enc.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Items.ChatData;

import java.util.ArrayList;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_CONTENT;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_FILEPATH;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_FROM;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_TIMESTAMP;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_TO;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_TYPE;

import static com.leagueofshadows.enc.storage.SQLHelper.ID;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_CONTENT;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_FILEPATH;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_FROM;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_RECEIVED;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_SEEN;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_SENT;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_TIMESTAMP;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_TO;
import static com.leagueofshadows.enc.storage.SQLHelper.MESSAGES_TYPE;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_ENCRYPTED_MESSAGES;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_MESSAGES;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_USERS;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_USER_DATA;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_NAME;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_NUMBER;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_PUBLICKEY;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_MESSAGES_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_NEW_MESSAGE_COUNT;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_USERS_ID;
@Deprecated
public class DatabaseManager  {

    private static DatabaseManager instance;
    private static SQLiteOpenHelper sqLiteOpenHelper;
    private static SQLiteDatabase sqLiteDatabase;
    private static final String illegalStateException = "is not initialized. call initialize() first";
    private int counter = 0;

    public static synchronized void initializeInstance(@NonNull SQLiteOpenHelper helper) {

        if(instance == null) {
            instance = new DatabaseManager();
            sqLiteOpenHelper = helper;
        }
    }

    public static synchronized DatabaseManager getInstance() {

        if(instance == null) {
            throw new IllegalStateException(illegalStateException);
        }
        return instance;
    }

    private synchronized SQLiteDatabase openDatabase() {
        counter++;
        if(counter==1) {
            sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
        }
        return sqLiteDatabase;
    }

    private synchronized void closeDatabase() {
        counter--;
        if(counter==0)
            sqLiteDatabase.close();
    }

    public boolean insertNewMessage(Message message)
    {
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_ID,message.getMessage_id());
        contentValues.put(MESSAGES_TO,message.getTo());
        contentValues.put(MESSAGES_FROM,message.getFrom());
        contentValues.put(MESSAGES_CONTENT,message.getContent());
        contentValues.put(MESSAGES_FILEPATH,message.getFilePath());
        contentValues.put(MESSAGES_TIMESTAMP,message.getTimeStamp());
        contentValues.put(MESSAGES_TYPE,message.getType());
        contentValues.put(MESSAGES_SENT,message.getSent());
        contentValues.put(MESSAGES_RECEIVED,message.getReceived());
        contentValues.put(MESSAGES_SEEN,message.getSeen());
        long x = database.insert(TABLE_MESSAGES,null,contentValues);
        return x!=-1;
    }

    public void updateMessageSeenStatus(String timestamp, String message_id)
    {
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_SEEN,timestamp);
        database.update(TABLE_MESSAGES,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public void updateMessageSentStatus(String timestamp,String message_id)
    {
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_SENT,timestamp);
        database.update(TABLE_MESSAGES,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public void updateMessageReceivedStatus(String timestamp, String message_id)
    {
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_RECEIVED,timestamp);
        database.update(TABLE_MESSAGES,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public void deleteEncryptedMessage(String messageId)
    {
        SQLiteDatabase database = openDatabase();
        database.delete(TABLE_ENCRYPTED_MESSAGES,ENCRYPTED_MESSAGES_ID+" = ?", new String[]{messageId});
    }

    public boolean deleteMessage(String messageId)
    {
        SQLiteDatabase database = openDatabase();
        int x = database.delete(TABLE_MESSAGES,MESSAGES_ID+" = ?", new String[]{messageId});
        return x!=0;
    }

    public ArrayList<EncryptedMessage> getEncryptedMessages()
    {
        ArrayList<EncryptedMessage> encryptedMessages = new ArrayList<>();
        String rawQuery = "SELECT * FROM "+TABLE_ENCRYPTED_MESSAGES;
        SQLiteDatabase sqLiteDatabase = openDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(rawQuery,null);
        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {

                    EncryptedMessage encryptedMessage = new EncryptedMessage(cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_ID)), cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TO))
                            , cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_FROM)), cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_CONTENT)), cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_FILEPATH))
                            , cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TIMESTAMP)), cursor.getInt(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TYPE)));

                    encryptedMessages.add(encryptedMessage);

                } while (cursor.moveToNext());
            }
            try {
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return encryptedMessages;
    }


    @Deprecated
    public  ArrayList<Message> getMessages(@NonNull String otherUser, int offset, int length)
    {
        ArrayList<Message> messages = new ArrayList<>();
        if(instance == null) {
            throw new IllegalStateException(illegalStateException);
        }

        SQLiteDatabase database = openDatabase();
        String rawQuery = "SELECT * FROM "+TABLE_MESSAGES+" WHERE "+MESSAGES_FROM
                +" = ? "+" OR "+MESSAGES_TO+" = ? "+" ORDER BY "
                +ID+" DESC";

        String raw ="SELECT * FROM ("+rawQuery+")"+" LIMIT "+length +" OFFSET "+ offset;

        Cursor cursor = database.rawQuery(raw,new String[]{otherUser,otherUser});
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    //Log.e("eeee","eee");
                    Message message = new Message(cursor.getInt(0),cursor.getString(cursor.getColumnIndex(MESSAGES_ID)),cursor.getString(cursor.getColumnIndex(MESSAGES_TO))
                    ,cursor.getString(cursor.getColumnIndex(MESSAGES_FROM)),cursor.getString(cursor.getColumnIndex(MESSAGES_CONTENT)),cursor.getString(cursor.getColumnIndex(MESSAGES_FILEPATH))
                    ,cursor.getString(cursor.getColumnIndex(MESSAGES_TIMESTAMP)),cursor.getInt(cursor.getColumnIndex(MESSAGES_TYPE)),cursor.getString(cursor.getColumnIndex(MESSAGES_SENT))
                            ,cursor.getString(cursor.getColumnIndex(MESSAGES_RECEIVED)),cursor.getString(cursor.getColumnIndex(MESSAGES_SEEN)));

                    messages.add(0,message);
                }while (cursor.moveToNext());
            }
            try {
                cursor.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return messages;
    }


    public void insertEncryptedMessages(ArrayList<EncryptedMessage> encryptedMessages)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        for (EncryptedMessage e:encryptedMessages) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ENCRYPTED_MESSAGES_ID,e.getId());
            contentValues.put(ENCRYPTED_MESSAGES_TO,e.getTo());
            contentValues.put(ENCRYPTED_MESSAGES_FROM,e.getFrom());
            contentValues.put(ENCRYPTED_MESSAGES_CONTENT,e.getContent());
            contentValues.put(ENCRYPTED_MESSAGES_FILEPATH,e.getFilePath());
            contentValues.put(ENCRYPTED_MESSAGES_TYPE,e.getType());
            contentValues.put(ENCRYPTED_MESSAGES_TIMESTAMP,e.getTimeStamp());
            sqLiteDatabase.insert(TABLE_ENCRYPTED_MESSAGES,null,contentValues);
        }
    }

    public String getPublicKey(String userId)
    {
        String Bas64EncodedPublicKey = null;
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = "+userId;
        Cursor cursor = openDatabase().rawQuery(raw,null);
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                Bas64EncodedPublicKey = cursor.getString(cursor.getColumnIndex(USERS_PUBLICKEY));
            }
            cursor.close();
        }
        return Bas64EncodedPublicKey;
    }

    public User getUser(String userId){
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        cursor.moveToFirst();
        User user = new User(cursor.getString(cursor.getColumnIndex(USERS_ID)),cursor.getString(cursor.getColumnIndex(USERS_NAME)),
                cursor.getString(cursor.getColumnIndex(USERS_NUMBER)),cursor.getString(cursor.getColumnIndex(USERS_PUBLICKEY)));
        cursor.close();
        return user;
    }

   /* public boolean updateUserPublicKey(String userId,String Bas64EncodedPublicKey)
    {
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(USERS_PUBLICKEY,Bas64EncodedPublicKey);
        int x = database.update(TABLE_USERS,contentValues,USERS_ID+" = ?", new String[]{userId});
        return x!=0;
    }*/

    public void insertUser(User user)
    {
        SQLiteDatabase database = openDatabase();
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = ?";
        Cursor cursor = database.rawQuery(raw, new String[]{user.getId()});
        if(cursor.getCount()==0)
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_ID,user.getId());
            contentValues.put(USERS_NAME,user.getName());
            contentValues.put(USERS_NUMBER,user.getNumber());
            contentValues.put(USERS_PUBLICKEY,user.getBase64EncodedPublicKey());

            database.insert(TABLE_USERS,null,contentValues);
            cursor.close();
        }
        else
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_NAME,user.getName());
            contentValues.put(USERS_NUMBER,user.getNumber());
            contentValues.put(USERS_PUBLICKEY,user.getBase64EncodedPublicKey());
            database.update(TABLE_USERS,contentValues,USERS_ID+" = ?", new String[]{user.getId()});
            cursor.close();
        }

    }

    public boolean deleteUser(String userId)
    {
        SQLiteDatabase database = openDatabase();
        int x =database.delete(TABLE_USERS,USERS_ID+" = ?", new String[]{userId});
        return x!=0;
    }

    public ArrayList<User> getUsers()
    {
        ArrayList<User> users = new ArrayList<>();

        String raw = "SELECT * FROM "+TABLE_USERS;
        SQLiteDatabase database = openDatabase();
        Cursor cursor = database.rawQuery(raw,null);
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    User user = new User(cursor.getString(cursor.getColumnIndex(USERS_ID)),cursor.getString(cursor.getColumnIndex(USERS_NAME)),
                            cursor.getString(cursor.getColumnIndex(USERS_NUMBER)),cursor.getString(cursor.getColumnIndex(USERS_PUBLICKEY)));
                    users.add(user);
                }while (cursor.moveToNext());
            }
            try {
                cursor.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return users;
    }

    public void updateMessageId(String userId,String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();

        String raw = "SELECT * FROM "+TABLE_USER_DATA+" WHERE "+USER_DATA_USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_DATA_MESSAGES_ID,messageId);
        if(cursor.getCount()==0)
        {

            contentValues.put(USER_DATA_USERS_ID,userId);
            contentValues.put(USER_DATA_NEW_MESSAGE_COUNT,0);

            sqLiteDatabase.insert(TABLE_USER_DATA,null,contentValues);
        }
        else {
            sqLiteDatabase.update(TABLE_USER_DATA,contentValues,USER_DATA_USERS_ID+" = ?", new String[]{userId});
        }
        cursor.close();
    }

    public void incrementNewMessageCount(String userId,String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();

        String raw = "SELECT * FROM "+TABLE_USER_DATA+" WHERE "+USER_DATA_USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_DATA_MESSAGES_ID,messageId);
        int count=1;
        if(cursor.getCount()==0)
        {
            contentValues.put(USER_DATA_USERS_ID,userId);
            contentValues.put(USER_DATA_NEW_MESSAGE_COUNT,count);
            sqLiteDatabase.insert(TABLE_USER_DATA,null,contentValues);
        }
        else
        {
            count = cursor.getInt(cursor.getColumnIndex(USER_DATA_NEW_MESSAGE_COUNT));
            count++;
            contentValues.put(USER_DATA_NEW_MESSAGE_COUNT,count);
            sqLiteDatabase.update(TABLE_USER_DATA,contentValues,USER_DATA_USERS_ID+" = ?", new String[]{userId});
        }
        cursor.close();
    }

    private Message getMessage(String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String raw = "SELECT * FROM "+TABLE_MESSAGES+" WHERE "+MESSAGES_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{messageId});
        cursor.moveToFirst();

        Message message = new Message(cursor.getInt(0),cursor.getString(cursor.getColumnIndex(MESSAGES_ID)),cursor.getString(cursor.getColumnIndex(MESSAGES_TO))
                ,cursor.getString(cursor.getColumnIndex(MESSAGES_FROM)),cursor.getString(cursor.getColumnIndex(MESSAGES_CONTENT)),cursor.getString(cursor.getColumnIndex(MESSAGES_FILEPATH))
                ,cursor.getString(cursor.getColumnIndex(MESSAGES_TIMESTAMP)),cursor.getInt(cursor.getColumnIndex(MESSAGES_TYPE)),cursor.getString(cursor.getColumnIndex(MESSAGES_SENT))
                ,cursor.getString(cursor.getColumnIndex(MESSAGES_RECEIVED)),cursor.getString(cursor.getColumnIndex(MESSAGES_SEEN)));
        cursor.close();
        return message;

    }

    public ArrayList<ChatData> getUserData()
    {
        ArrayList<ChatData> chatDataArrayList = new ArrayList<>();
        String raw = "SELECT * FROM "+TABLE_USER_DATA;
        SQLiteDatabase database = openDatabase();

        Cursor cursor = database.rawQuery(raw,null);
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    String userId = cursor.getString(cursor.getColumnIndex(USER_DATA_USERS_ID));
                    User user = getUser(userId);
                    String messageId = cursor.getString(cursor.getColumnIndex(USER_DATA_MESSAGES_ID));
                    Message message = getMessage(messageId);
                    int count = cursor.getInt(cursor.getColumnIndex(USER_DATA_NEW_MESSAGE_COUNT));

                    ChatData chatData = new ChatData(user,message,count,0);
                    chatDataArrayList.add(chatData);
                }while (cursor.moveToNext());
            }
            cursor.close();
        }
        return chatDataArrayList;
    }

    public void insertPublicKey(String Base64PublicKey,String userId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});
        if(cursor.getCount()==0) {
            insertUser(new User(userId,userId,userId,Base64PublicKey));
        }
        else
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_PUBLICKEY,Base64PublicKey);
            sqLiteDatabase.update(TABLE_USERS,contentValues,USERS_ID+" = ?", new String[]{userId});
        }
        cursor.close();
    }

}
