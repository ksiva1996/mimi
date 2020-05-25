package com.leagueofshadows.enc;

import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import static com.leagueofshadows.enc.ChatActivity.MESSAGE_CONTENT;


public class Util {


    public static final String TOKEN_SENT = "TOKEN_SENT";

    static final String originalPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/";
    public static final String imagesPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Images/";
    static final String sentImagesPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Images/sent/";
    public static final String documentsPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Documents/";
    static final String sentDocumentsPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Documents/sent/";
    public static final String privatePath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/private/";

    public static final String preferences = "preferences";
    public static String type = "type";
    public static String path = "path";
    public static String messageId = "messageId";
    public static String content = "content";
    public static String cipherText = "cipherText";
    static String camera = "camera";

    public static String ServiceNotificationChannelID = "ServiceNotificationChannelID";
    public static String NewMessageNotificationChannelID = "NewMessageNotificationChannelID";
    public static String ServiceNotificationChannelTitle = "Service notifications channel";
    public static String NewMessageNotificationChannelTitle = "New Message notifications";


    static String CheckMessageEncrypted = "CheckMessageEncrypted";
    public static String accessToken = "accessToken";

    //TODO:
    public static String key = "AAAA9mn4XrI:APA91bHmWqrRds17hK0IZCD-5MkA4kpLWV27WhRPhW2NTX0bHzj_onjpQNwzKmKhouKKy-gjI7p4XhtvdnOVvusRlN2x0Ee4DRy1grMHrpx8YVjV5_66cxr47iRwtO5M09ZUj4OYsney";
    public static String uri = "uri";
    public static String timeStamp = "timeStamp";
    public static String fileName = "fileName";
    public static String id = "id";
    public static String toUserId = "toUserId";
    public static String name = "name";
    static String number = "number";
    static String password = "password";
    public static final String CheckMessageIV = "CheckMessageIV";
    public static String userId = "userId";

    static final String PublicKeyString = "PublicKeyString";

    static final String  CheckMessage = "CheckMessage";

    static String getMessageContent(String messageContent)
    {
        try {
            JSONObject jsonObject = new JSONObject(messageContent);
            return jsonObject.getString(MESSAGE_CONTENT);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

}
