package com.leagueofshadows.enc;

import android.os.Environment;


public class Util {


    public static final String TOKEN_SENT = "TOKEN_SENT";

    static final String originalPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/";
    static final String imagesPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Images/";
    static final String documentsPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Documents/";
    static final String privatePath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/private/";

    public static final String preferences = "preferences";
    public static String type = "type";
    public static String path = "path";
    static String camera = "camera";
    static String currentFocus = "currentFocus";


    static String CheckMessageEncrypted = "CheckMessageEncrypted";
    public static String accessToken = "accessToken";

    //TODO:
    public static String key = "AAAA9mn4XrI:APA91bHmWqrRds17hK0IZCD-5MkA4kpLWV27WhRPhW2NTX0bHzj_onjpQNwzKmKhouKKy-gjI7p4XhtvdnOVvusRlN2x0Ee4DRy1grMHrpx8YVjV5_66cxr47iRwtO5M09ZUj4OYsney";
    static String uri = "uri";
    public static String timeStamp = "timeStamp";
    static String fileName = "fileName";
    public static String id = "id";
    static String toUserId = "toUserId";
    static String name = "name";
    static String number = "number";
    static String password = "password";
    public static final String CheckMessageIV = "CheckMessageIV";
    public static String userId = "userId";

    static final String PublicKeyString = "PublicKeyString";

    static final String  CheckMessage = "CheckMessage";

}
