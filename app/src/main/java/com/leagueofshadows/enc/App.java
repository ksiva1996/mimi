package com.leagueofshadows.enc;

import android.app.Application;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import java.security.PrivateKey;
import javax.crypto.SecretKey;

public class App extends Application {

    private PrivateKey privateKey;
    private MessagesRetrievedCallback messagesRetrievedCallback;
    private MessageSentCallback messageSentCallback;
    private CompleteCallback completeCallback;
    private ResendMessageCallback resendMessageCallback;
    private SecretKey masterKey;
    private GroupsUpdatedCallback groupsUpdatedCallback;

    public CompleteCallback getCompleteCallback() {
        return completeCallback;
    }

    public void setCompleteCallback(CompleteCallback completeCallback) { this.completeCallback = completeCallback; }

    public void setMessageSentCallback(MessageSentCallback messageSentCallback) { this.messageSentCallback = messageSentCallback; }

    public MessageSentCallback getMessageSentCallback() {
        return messageSentCallback;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }


    public MessagesRetrievedCallback getMessagesRetrievedCallback() { return messagesRetrievedCallback; }

    public void setMessagesRetrievedCallback(MessagesRetrievedCallback messagesRetrievedCallback) { this.messagesRetrievedCallback = messagesRetrievedCallback; }

    public boolean isnull() {
        return privateKey == null;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public ResendMessageCallback getResendMessageCallback() {
        return resendMessageCallback;
    }

    public void setResendMessageCallback(ResendMessageCallback resendMessageCallback) { this.resendMessageCallback = resendMessageCallback; }

    public SecretKey getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(SecretKey masterKey) {
        this.masterKey = masterKey;
    }

    public GroupsUpdatedCallback getGroupsUpdatedCallback() { return groupsUpdatedCallback; }

    public void setGroupsUpdatedCallback(GroupsUpdatedCallback groupsUpdatedCallback) { this.groupsUpdatedCallback = groupsUpdatedCallback; }
}
