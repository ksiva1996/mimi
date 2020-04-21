package com.leagueofshadows.enc.Items;

public class EncryptedMessage {

    private String id;
    private String to;
    private String from;
    private String content;
    private String timeStamp;
    private String filePath;
    private int type;

    public static final int MESSAGE_TYPE_ONLYTEXT = 1;
    public static final int MESSAGE_TYPE_IMAGE = 2;
    public static final int MESSAGE_TYPE_FILE = 3;

    public EncryptedMessage(String id, String to, String from, String content, String filePath, String timeStamp, int type)
    {
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.timeStamp = timeStamp;
        this.type = type;
        this.filePath = filePath;
    }

    public EncryptedMessage(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
