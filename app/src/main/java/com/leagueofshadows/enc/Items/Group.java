package com.leagueofshadows.enc.Items;

import java.util.ArrayList;

public class Group  {
    private String id;
    private String name;
    private ArrayList<User> users;


    public Group(String id, String name, ArrayList<User> users)
    {
        this.id = id;
        this.name = name;
        this.users = users;
    }
    public Group(){}

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

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
    public int getSize() {
        return users.size();
    }
}
