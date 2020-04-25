package com.leagueofshadows.enc;

import android.app.Application;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;

import java.security.PrivateKey;

public class App extends Application {

    private PrivateKey privateKey;
    private MessagesRetrievedCallback messagesRetrievedCallback;
    private MessageSentCallback messageSentCallback;
    private CompleteCallback completeCallback;
    private ResendMessageCallback resendMessageCallback;

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


    public MessagesRetrievedCallback getMessagesRetrievedCallback() {
        return messagesRetrievedCallback;
    }

    public void setMessagesRetrievedCallback(MessagesRetrievedCallback messagesRetrievedCallback) {
        this.messagesRetrievedCallback = messagesRetrievedCallback;
    }

    boolean isnull() {
        return privateKey == null;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public ResendMessageCallback getResendMessageCallback() {
        return resendMessageCallback;
    }

    public void setResendMessageCallback(ResendMessageCallback resendMessageCallback) {
        this.resendMessageCallback = resendMessageCallback;
    }
}
