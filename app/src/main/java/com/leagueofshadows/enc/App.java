package com.leagueofshadows.enc;

import android.app.Application;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import java.security.PrivateKey;

public class App extends Application {

    private PrivateKey privateKey;
    private String otherUser;
    private MessagesRetrievedCallback messagesRetrievedCallback;
    private String currentUserId;
    private MessageSentCallback messageSentCallback;
    private CompleteCallback completeCallback;

    public CompleteCallback getCompleteCallback() {
        return completeCallback;
    }

    public void setCompleteCallback(CompleteCallback completeCallback) {
        this.completeCallback = completeCallback;
    }

    public void setMessageSentCallback(MessageSentCallback messageSentCallback) {
        this.messageSentCallback = messageSentCallback;
    }

    public MessageSentCallback getMessageSentCallback() {
        return messageSentCallback;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public MessagesRetrievedCallback getMessagesRetrievedCallback() {
        return messagesRetrievedCallback;
    }

    public void setMessagesRetrievedCallback(MessagesRetrievedCallback messagesRetrievedCallback) {
        this.messagesRetrievedCallback = messagesRetrievedCallback;
    }

    boolean isnull() {
        return privateKey == null;
    }

    public String getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
