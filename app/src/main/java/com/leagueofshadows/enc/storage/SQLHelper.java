package com.leagueofshadows.enc.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "SAL-1";

    static final String ID = "ID";
    static final String TABLE_MESSAGES = "Messages";
    static final String MESSAGES_ID = "Messages_id";
    static final String MESSAGES_TO = "Messages_to";
    static final String MESSAGES_FROM = "Messages_from";
    static final String MESSAGES_CONTENT = "Messages_content";
    static final String MESSAGES_TYPE = "Messages_type";
    static final String MESSAGES_FILEPATH = "Messages_filepath";
    static final String MESSAGES_TIMESTAMP = "Messages_timestamp";
    static final String MESSAGES_SENT = "Messages_sent";
    static final String MESSAGES_RECEIVED = "Messages_received";
    static final String MESSAGES_SEEN = "Messages_seen";

    static final String TABLE_ENCRYPTED_MESSAGES = "Encrypted_Messages";
    static final String ENCRYPTED_MESSAGES_ID = "Encrypted_Messages_id";
    static final String ENCRYPTED_MESSAGES_TO = "Encrypted_Messages_to";
    static final String ENCRYPTED_MESSAGES_FROM = "Encrypted_Messages_from";
    static final String ENCRYPTED_MESSAGES_CONTENT = "Encrypted_Messages_content";
    static final String ENCRYPTED_MESSAGES_TYPE = "Encrypted_Messages_type";
    static final String ENCRYPTED_MESSAGES_FILEPATH = "Encrypted_Messages_filepath";
    static final String ENCRYPTED_MESSAGES_TIMESTAMP = "Encrypted_Messages_timestamp";
    static final String ENCRYPTED_MESSAGES_RESEND = "Encrypted_Messages_resend";

    static final String TABLE_USERS = "Users";
    static final String USERS_ID = "Users_id";
    static final String USERS_NUMBER = "Users_number";
    static final String USERS_NAME = "Users_name";
    static final String USERS_PUBLICKEY = "Users_publickey";

    static final String TABLE_USER_DATA = "Users_data";
    static final String USER_DATA_USERS_ID = "Users_id";
    static final String USER_DATA_MESSAGES_ID = "Messages_Id";
    static final String USER_DATA_NEW_MESSAGE_COUNT = "Message_count";
    static final String USER_DATA_TIME = "USER_DATA_TIME";

    static final String TABLE_RESEND_MESSAGE = "Resend_messages";
    static final String RESEND_MESSAGE_USER_ID = "User_id";
    static final String RESEND_MESSAGE_MESSAGE_ID = "Messages_Id";

    public SQLHelper(Context context) {
        super(context,DATABASE_NAME,null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
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

        String CREATE_ENCRYPTED_MESSAGES_TABLE = "CREATE TABLE " + TABLE_ENCRYPTED_MESSAGES + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + ENCRYPTED_MESSAGES_ID + " TEXT ,"
                + ENCRYPTED_MESSAGES_TO + " TEXT ,"
                +ENCRYPTED_MESSAGES_FROM+"  TEXT ,"
                + ENCRYPTED_MESSAGES_CONTENT + " TEXT ,"
                +ENCRYPTED_MESSAGES_TYPE+" INTEGER ,"
                +ENCRYPTED_MESSAGES_FILEPATH+" TEXT ,"
                +ENCRYPTED_MESSAGES_TIMESTAMP+" TEXT ,"
                +ENCRYPTED_MESSAGES_RESEND+" BOOLEAN "
                + ")";

        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + USERS_ID + " TEXT PRIMARY KEY  ,"
                + USERS_NAME + " TEXT ,"
                + USERS_NUMBER+"  TEXT ,"
                + USERS_PUBLICKEY + " TEXT "
                + ")";

        String CREATE_USER_DATA_TABLE = "CREATE TABLE " + TABLE_USER_DATA + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + USER_DATA_USERS_ID + " TEXT ,"
                + USER_DATA_MESSAGES_ID+"  TEXT ,"
                + USER_DATA_NEW_MESSAGE_COUNT+"  INTEGER ,"
                + USER_DATA_TIME+"  INTEGER "
                + ")";

        String CREATE_RESEND_MESSAGE_TABLE = "CREATE TABLE " + TABLE_RESEND_MESSAGE + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + RESEND_MESSAGE_MESSAGE_ID + " TEXT ,"
                + RESEND_MESSAGE_USER_ID+"  TEXT "
                + ")";

        sqLiteDatabase.execSQL(CREATE_MESSAGES_TABLE);
        sqLiteDatabase.execSQL(CREATE_ENCRYPTED_MESSAGES_TABLE);
        sqLiteDatabase.execSQL(CREATE_USERS_TABLE);
        sqLiteDatabase.execSQL(CREATE_USER_DATA_TABLE);
        sqLiteDatabase.execSQL(CREATE_RESEND_MESSAGE_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_MESSAGES);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_USERS);
        onCreate(sqLiteDatabase);
    }
}
