package com.leagueofshadows.enc.Items;

public class UserData {

    private User user;
    private Message latestMessage;
    private int count;

    public UserData(User user, Message latestMessage, int count)
    {
        this.user = user;
        this.latestMessage = latestMessage;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Message latestMessage) {
        this.latestMessage = latestMessage;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}

