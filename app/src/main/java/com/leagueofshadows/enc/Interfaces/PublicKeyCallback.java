package com.leagueofshadows.enc.Interfaces;

public interface PublicKeyCallback {
    void onSuccess(String Base64PublicKey);
    void onCancelled(String error);
}
