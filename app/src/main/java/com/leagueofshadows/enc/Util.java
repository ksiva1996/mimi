package com.leagueofshadows.enc;

import android.os.Environment;


public class Util {


    static final String loginToken = "loginToken";

    static final int keySize = 128;
    static final int numberOfIterations = 10000;
    static final String factory = "PBKDF2WithHmacSHA1";
    static final String cipher = "AES/CBC/PKCS5Padding";
    static final String path = Environment.getExternalStorageDirectory().getPath()+"/Encrypto/";
    static final String sdcard = Environment.getExternalStorageDirectory().getPath();
    static final String output = Environment.getExternalStorageDirectory().getPath()+"/Encrypto/Outputs/";
    static final String temp = Environment.getExternalStorageDirectory().getPath()+"/Encrypto/temp/";
    static final String check = "check";
    public static final String preferences = "preferences";

    static final String set = "set";
    static final String saltString = "saltString";
    static final String ivString = "ivString";
    static final String encrypted = "encrypted";
    static final String open = "open";
    static final String hidden = "hidden";

    static final String PrivateKeyIV = "PrivateKeyIV";

    static final String permission = "P";
    public static String CheckMessageEncrypted = "CheckMessageEncrypted";
    static String toUserId = "toUserId";
    static String name = "name";
    static String number = "number";
    static String password = "password";
    public static final String CheckMessageIV = "CheckMessageIV";
    static String userId = "userId";
    static String PrivateKeyString = "PrivateKeyString";

    static final String PublicKeyString = "PublicKeyString";

    public static final String  CheckMessage = "CheckMessage";

}
