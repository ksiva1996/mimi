package com.leagueofshadows.enc.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Items.UserData;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_CONTENT;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_FILEPATH;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_FROM;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.ENCRYPTED_MESSAGES_RESEND;
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
import static com.leagueofshadows.enc.storage.SQLHelper.RESEND_MESSAGE_MESSAGE_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.RESEND_MESSAGE_USER_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_ENCRYPTED_MESSAGES;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_RESEND_MESSAGE;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_USERS;
import static com.leagueofshadows.enc.storage.SQLHelper.TABLE_USER_DATA;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_NAME;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_NUMBER;
import static com.leagueofshadows.enc.storage.SQLHelper.USERS_PUBLICKEY;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_MESSAGES_ID;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_NEW_MESSAGE_COUNT;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_TIME;
import static com.leagueofshadows.enc.storage.SQLHelper.USER_DATA_USERS_ID;

public class DatabaseManager2 {

    private static DatabaseManager2 instance;
    private static SQLiteOpenHelper sqLiteOpenHelper;
    private static SQLiteDatabase sqLiteDatabase;
    private static final String illegalStateException = "is not initialized. call initialize() first";
    private int counter = 0;
    private static final String TABLE ="TABLE";

    public static synchronized void initializeInstance(@NonNull SQLiteOpenHelper helper) {

        if (instance == null) {
            instance = new DatabaseManager2();
            sqLiteOpenHelper = helper;
        }
    }

    public static synchronized DatabaseManager2 getInstance() {

        if (instance == null) {
            throw new IllegalStateException(illegalStateException);
        }
        return instance;
    }

    private synchronized SQLiteDatabase openDatabase() {
        counter++;
        if (counter == 1) {
            sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
        }
        return sqLiteDatabase;
    }

    private synchronized void closeDatabase() {
        counter--;
        if (counter == 0)
            sqLiteDatabase.close();
    }


    //Messages database operation

    public void insertNewMessage(Message message, String otherUserId)
    {

        String tableName = getTableName(otherUserId);
        checkTable(tableName);

        SQLiteDatabase sqLiteDatabase = openDatabase();

        String raw = "SELECT * FROM "+tableName+" WHERE "+MESSAGES_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{message.getMessage_id()});
        if(cursor.getCount()==0) {

            ContentValues contentValues = new ContentValues();
            contentValues.put(MESSAGES_ID, message.getMessage_id());
            contentValues.put(MESSAGES_TO, message.getTo());
            contentValues.put(MESSAGES_FROM, message.getFrom());
            contentValues.put(MESSAGES_CONTENT, message.getContent());
            contentValues.put(MESSAGES_FILEPATH, message.getFilePath());
            contentValues.put(MESSAGES_TIMESTAMP, message.getTimeStamp());
            contentValues.put(MESSAGES_TYPE, message.getType());
            contentValues.put(MESSAGES_SENT, message.getSent());
            contentValues.put(MESSAGES_RECEIVED, message.getReceived());
            contentValues.put(MESSAGES_SEEN, message.getSeen());
            sqLiteDatabase.insert(tableName, null, contentValues);
            updateMessageId(otherUserId, message.getMessage_id());

            if (message.getFrom().equals(otherUserId))
                incrementNewMessageCount(otherUserId, message.getMessage_id());
            cursor.close();
        }
    }

    public void updateMessage(Message message,String otherUserId)
    {
        String tableName = getTableName(otherUserId);
        checkTable(tableName);

        SQLiteDatabase sqLiteDatabase = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_ID, message.getMessage_id());
        contentValues.put(MESSAGES_TO, message.getTo());
        contentValues.put(MESSAGES_FROM, message.getFrom());
        contentValues.put(MESSAGES_CONTENT, message.getContent());
        contentValues.put(MESSAGES_FILEPATH, message.getFilePath());
        contentValues.put(MESSAGES_TIMESTAMP, message.getTimeStamp());
        contentValues.put(MESSAGES_TYPE, message.getType());
        contentValues.put(MESSAGES_SENT, message.getSent());
        contentValues.put(MESSAGES_RECEIVED, message.getReceived());
        contentValues.put(MESSAGES_SEEN, message.getSeen());
        sqLiteDatabase.update(tableName,contentValues,MESSAGES_ID +" = ?", new String[]{message.getMessage_id()});
    }

    public boolean check(String messageId, String userId)
    {
        String tableName = getTableName(userId);
        checkTable(tableName);
        String raw = "SELECT * FROM "+tableName+" WHERE "+MESSAGES_ID+" = ?";
        Cursor cursor = openDatabase().rawQuery(raw, new String[]{messageId});
        boolean y = cursor.getCount() == 0;
        cursor.close();
        return y;

    }

    public void deleteMessage(Message message,String currentUserId)
    {
        String otherUserId = getId(message,currentUserId);
        String tableName = getTableName(otherUserId);
        checkTable(tableName);
        SQLiteDatabase sqLiteDatabase = openDatabase();
        sqLiteDatabase.delete(tableName,MESSAGES_ID+" = ?", new String[]{message.getMessage_id()});
        String raw = "SELECT * FROM "+tableName+" WHERE "+ID+" = ( SELECT MAX("+ID+") FROM "+tableName+")";
        Cursor cursor = sqLiteDatabase.rawQuery(raw,null);
        cursor.moveToFirst();
        String messageId = cursor.getString(cursor.getColumnIndex(MESSAGES_ID));
        updateMessageId(otherUserId,messageId);
        cursor.close();
    }

    public ArrayList<Message> getMessages(String otherUserId, int offset, int length)
    {
        ArrayList<Message> messages = new ArrayList<>();
        String tableName = getTableName(otherUserId);

        checkTable(tableName);

        String raw = "SELECT * FROM "+tableName+" ORDER BY "+ID+" DESC LIMIT "+length+" OFFSET "+offset;

        Cursor cursor = openDatabase().rawQuery(raw,null);
        if(cursor.moveToFirst())
        {
            do {
                Message message = new Message(cursor.getInt(0),cursor.getString(cursor.getColumnIndex(MESSAGES_ID)),cursor.getString(cursor.getColumnIndex(MESSAGES_TO))
                        ,cursor.getString(cursor.getColumnIndex(MESSAGES_FROM)),cursor.getString(cursor.getColumnIndex(MESSAGES_CONTENT)),cursor.getString(cursor.getColumnIndex(MESSAGES_FILEPATH))
                        ,cursor.getString(cursor.getColumnIndex(MESSAGES_TIMESTAMP)),cursor.getInt(cursor.getColumnIndex(MESSAGES_TYPE)),cursor.getString(cursor.getColumnIndex(MESSAGES_SENT))
                        ,cursor.getString(cursor.getColumnIndex(MESSAGES_RECEIVED)),cursor.getString(cursor.getColumnIndex(MESSAGES_SEEN)));

                messages.add(0,message);
            }while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public void updateMessageSeenStatus(String timestamp, String message_id,String otherUserId)
    {
        String tableName = getTableName(otherUserId);
        checkTable(tableName);
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_SEEN,timestamp);
        database.update(tableName,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public void updateMessageSentStatus(String timestamp,String message_id,String otherUserId)
    {
        String tableName = getTableName(otherUserId);
        checkTable(tableName);
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_SENT,timestamp);
        database.update(tableName,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public void updateMessageReceivedStatus(String timestamp, String message_id,String otherUserId)
    {
        String tableName = getTableName(otherUserId);
        checkTable(tableName);
        SQLiteDatabase database = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_RECEIVED,timestamp);
        database.update(tableName,contentValues,MESSAGES_ID+" = ?", new String[]{message_id});
    }

    public Message getMessage(String messageId,String otherUserId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String tableName = getTableName(otherUserId);
        String raw = "SELECT * FROM "+tableName+" WHERE "+MESSAGES_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{messageId});
        if (cursor.getCount()!=0) {
            cursor.moveToFirst();

            Message message = new Message(cursor.getInt(0), cursor.getString(cursor.getColumnIndex(MESSAGES_ID)), cursor.getString(cursor.getColumnIndex(MESSAGES_TO))
                    , cursor.getString(cursor.getColumnIndex(MESSAGES_FROM)), cursor.getString(cursor.getColumnIndex(MESSAGES_CONTENT)), cursor.getString(cursor.getColumnIndex(MESSAGES_FILEPATH))
                    , cursor.getString(cursor.getColumnIndex(MESSAGES_TIMESTAMP)), cursor.getInt(cursor.getColumnIndex(MESSAGES_TYPE)), cursor.getString(cursor.getColumnIndex(MESSAGES_SENT))
                    , cursor.getString(cursor.getColumnIndex(MESSAGES_RECEIVED)), cursor.getString(cursor.getColumnIndex(MESSAGES_SEEN)));
            cursor.close();
            return message;
        }
        return null;
    }

    //Encrypted Messages database operations

    public void deleteEncryptedMessage(String messageId)
    {
        SQLiteDatabase database = openDatabase();
        database.delete(TABLE_ENCRYPTED_MESSAGES,ENCRYPTED_MESSAGES_ID+" = ?", new String[]{messageId});
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
                            , cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TIMESTAMP)), cursor.getInt(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TYPE)),
                            Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(ENCRYPTED_MESSAGES_TIMESTAMP))));

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

    public void insertEncryptedMessages(ArrayList<EncryptedMessage> encryptedMessages)
    {

        SQLiteDatabase sqLiteDatabase = openDatabase();
        for (EncryptedMessage e:encryptedMessages) {
            String raw = "SELECT * FROM "+TABLE_ENCRYPTED_MESSAGES+" WHERE "+ENCRYPTED_MESSAGES_ID+" = ?";
            Cursor cursor = openDatabase().rawQuery(raw, new String[]{e.getId()});
            if(cursor.getCount()==0) {

                ContentValues contentValues = new ContentValues();
                contentValues.put(ENCRYPTED_MESSAGES_ID, e.getId());
                contentValues.put(ENCRYPTED_MESSAGES_TO, e.getTo());
                contentValues.put(ENCRYPTED_MESSAGES_FROM, e.getFrom());
                contentValues.put(ENCRYPTED_MESSAGES_CONTENT, e.getContent());
                contentValues.put(ENCRYPTED_MESSAGES_FILEPATH, e.getFilePath());
                contentValues.put(ENCRYPTED_MESSAGES_TYPE, e.getType());
                contentValues.put(ENCRYPTED_MESSAGES_TIMESTAMP, e.getTimeStamp());
                contentValues.put(ENCRYPTED_MESSAGES_RESEND,e.isResend());
                sqLiteDatabase.insert(TABLE_ENCRYPTED_MESSAGES, null, contentValues);
            }
            cursor.close();
        }
    }

    //Users database operations

    public String getPublicKey(String userId)
    {
        String Bas64EncodedPublicKey = null;
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = ?";
        Cursor cursor = openDatabase().rawQuery(raw, new String[]{userId});
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
       // Log.e("userId",userId);
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String raw = "SELECT * FROM "+TABLE_USERS+" WHERE "+USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        cursor.moveToFirst();
        User user = new User(cursor.getString(cursor.getColumnIndex(USERS_ID)),cursor.getString(cursor.getColumnIndex(USERS_NAME)),
                cursor.getString(cursor.getColumnIndex(USERS_NUMBER)),cursor.getString(cursor.getColumnIndex(USERS_PUBLICKEY)));
        cursor.close();
        return user;
    }

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

    //UserData list

    private void updateMessageId(String userId, String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();

        String raw = "SELECT * FROM "+TABLE_USER_DATA+" WHERE "+USER_DATA_USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        ContentValues contentValues = new ContentValues();
        Long time = Calendar.getInstance().getTimeInMillis();
        contentValues.put(USER_DATA_MESSAGES_ID,messageId);
        contentValues.put(USER_DATA_TIME,time);
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

    private void incrementNewMessageCount(String userId, String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();

        String raw = "SELECT * FROM "+TABLE_USER_DATA+" WHERE "+USER_DATA_USERS_ID+" = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{userId});

        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_DATA_MESSAGES_ID,messageId);

            cursor.moveToFirst();
            int count = cursor.getInt(cursor.getColumnIndex(USER_DATA_NEW_MESSAGE_COUNT));
            count++;
            contentValues.put(USER_DATA_NEW_MESSAGE_COUNT,count);
            sqLiteDatabase.update(TABLE_USER_DATA,contentValues,USER_DATA_USERS_ID+" = ?", new String[]{userId});
            cursor.close();
    }

    public void setNewMessageCounter(String userId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_DATA_NEW_MESSAGE_COUNT,0);
        sqLiteDatabase.update(TABLE_USER_DATA,contentValues,USER_DATA_USERS_ID+" = ?", new String[]{userId});
    }

    public ArrayList<UserData> getUserData()
    {
        ArrayList<UserData> userDataArrayList = new ArrayList<>();
        String raw = "SELECT * FROM "+TABLE_USER_DATA;
        SQLiteDatabase database = openDatabase();

        Cursor cursor = database.rawQuery(raw,null);
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                do {
                   // Log.e("user","data");
                    String userId = cursor.getString(cursor.getColumnIndex(USER_DATA_USERS_ID));
                    User user = getUser(userId);
                    String messageId = cursor.getString(cursor.getColumnIndex(USER_DATA_MESSAGES_ID));
                    Message message = getMessage(messageId,userId);
                    int count = cursor.getInt(cursor.getColumnIndex(USER_DATA_NEW_MESSAGE_COUNT));
                    long time = cursor.getLong(cursor.getColumnIndex(USER_DATA_TIME));
                    UserData userData = new UserData(user,message,count,time);
                    userDataArrayList.add(userData);
                }while (cursor.moveToNext());
            }
            cursor.close();
        }
        return userDataArrayList;
    }

    //meta functions

    private String getTableName(@NonNull String otherUserId) {
        return TABLE+otherUserId.substring(1);
    }

    private String getId(@NonNull Message message, String currentUserId)
    {
        String otherUserId;
        if(message.getFrom().equals(currentUserId))
            otherUserId = message.getTo();
        else
            otherUserId = message.getFrom();
        return otherUserId;
    }

    private void checkTable(String tableName) {

        String raw = "SELECT name FROM sqlite_master WHERE type = ? AND name = ?";
        SQLiteDatabase sqLiteDatabase = openDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(raw, new String[]{"table",tableName});
        if (cursor.getCount()==0)
        {

            String createTableUserId = "CREATE TABLE " + tableName + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + MESSAGES_ID + " TEXT ,"
                + MESSAGES_TO + " TEXT ,"
                +MESSAGES_FROM+"  TEXT ,"
                + MESSAGES_CONTENT + " TEXT ,"
                +MESSAGES_TYPE+" INTEGER ,"
                +MESSAGES_FILEPATH+" TEXT ,"
                +MESSAGES_TIMESTAMP+" TEXT ,"
                +MESSAGES_SENT+" TEXT ,"
                +MESSAGES_RECEIVED+" TEXT, "
                +MESSAGES_SEEN+" TEXT "
                + ")";
            sqLiteDatabase.execSQL(createTableUserId);
        }
        cursor.close();
    }

    //resend message operations

    public void insertResendMessage(String userId,String messageId)
    {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        ContentValues contentValues = new ContentValues();
        String raw = "SELECT * FROM "+TABLE_RESEND_MESSAGE +" WHERE "+RESEND_MESSAGE_MESSAGE_ID+" = ? ";
        Cursor cursor = sqLiteDatabase.rawQuery(raw,new String[]{messageId});
        if(cursor.getCount()==0) {
            contentValues.put(RESEND_MESSAGE_MESSAGE_ID,messageId);
            contentValues.put(RESEND_MESSAGE_USER_ID,userId);
            sqLiteDatabase.insert(TABLE_RESEND_MESSAGE,null,contentValues);
        }
        cursor.close();
    }

    public ArrayList<Message> getResendMessages()
    {
        ArrayList<Message> messages = new ArrayList<>();
        String raw = "SELECT * FROM "+TABLE_RESEND_MESSAGE;
        Cursor cursor =openDatabase().rawQuery(raw,null);
        if(cursor.moveToFirst())
        {
            do {
                Message message = getMessage(cursor.getString(cursor.getColumnIndex(RESEND_MESSAGE_MESSAGE_ID))
                        ,cursor.getString(cursor.getColumnIndex(RESEND_MESSAGE_USER_ID)));
                if(message!=null) {
                    messages.add(message);
                }
                else
                    deleteResendMessage(cursor.getString(cursor.getColumnIndex(RESEND_MESSAGE_MESSAGE_ID)));
            }while(cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }
    public void deleteResendMessage(String messageId) {
        openDatabase().delete(TABLE_RESEND_MESSAGE,RESEND_MESSAGE_MESSAGE_ID+" = ?", new String[]{messageId});
    }

    public void deleteConversation(String userId) {
        SQLiteDatabase sqLiteDatabase = openDatabase();
        String tableName = getTableName(userId);
        String raw = "DELETE FROM "+tableName;
        sqLiteDatabase.execSQL(raw);
        sqLiteDatabase.delete(TABLE_USER_DATA,USER_DATA_USERS_ID+" = ?", new String[]{userId});
    }
}
