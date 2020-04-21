package com.leagueofshadows.enc.Items;

import androidx.annotation.NonNull;

public class Message {

    private String message_id;
    private int id;
    private String to;
    private String from;
    private String content;
    private String filePath;
    private String timeStamp;
    private int type;
    private String sent;
    private String received;
    private String seen;

    public static final int MESSAGE_TYPE_ONLYTEXT = 1;
    public static final int MESSAGE_TYPE_IMAGE = 2;
    public static final int MESSAGE_TYPE_FILE = 3;
    public static final String MESSAGE_SENDING_FAILED = "MESSAGE_SENDING_FAILED";
    public static final String MESSAGE_SENDING_SUCCESSFULL = "MESSAGE_SENDING_SUCCESSFULL";

    public Message() { }

    public Message(int id, @NonNull String message_id, @NonNull String to, @NonNull String from, @NonNull String content,
                   String filePath, @NonNull String timeStamp, int type, String sent, String received, String seen)
    {
        this.message_id = message_id;
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.filePath = filePath;
        this.timeStamp = timeStamp;
        this.type = type;
        this.sent = sent;
        this.received = received;
        this.seen = seen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getReceived() {
        return received;
    }

    public void setReceived(String received) {
        this.received = received;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }
}
