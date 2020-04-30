package com.leagueofshadows.enc.Items;

import androidx.annotation.NonNull;

public class User {

    private String id;
    private String name;
    private String number;
    private String Base64EncodedPublicKey;

    public User(@NonNull String id, @NonNull String name, @NonNull String number, String Base64EncodedPublicKey)
    {
        this.id = id;
        this.name = name;
        this.number = number;
        this.Base64EncodedPublicKey = Base64EncodedPublicKey;
    }

    public User() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getBase64EncodedPublicKey() {
        return Base64EncodedPublicKey;
    }

    public void setBase64EncodedPublicKey(String base64EncodedPublicKey) {
        this.Base64EncodedPublicKey = base64EncodedPublicKey;
    }
}
