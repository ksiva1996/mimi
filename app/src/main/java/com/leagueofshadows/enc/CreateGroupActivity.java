package com.leagueofshadows.enc;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.User;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class CreateGroupActivity extends AppCompatActivity implements Select {

    ArrayList<User> groupParticipants = new ArrayList<>();
    User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        setTitle("Add Participants");

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String id = sp.getString(Util.userId,null);
        String name = sp.getString(Util.name,null);
        String number = sp.getString(Util.number,null);

        assert id != null;
        assert name != null;
        assert number != null;

        currentUser = new User(id,name,number,null);
        addParticipant(currentUser);
    }

    void addParticipant(User user)
    {
        if (groupParticipants.size()<6) {
            groupParticipants.add(user);
            //TODO:
        }
        else {
            Toast.makeText(this,"Currently we only support maximum 6 participants ",Toast.LENGTH_SHORT).show();
        }

    }

    void removeParticipant(User user)
    {
        groupParticipants.remove(user);
        //TODO:
    }

    void createGroup()
    {

    }

    @Override
    public void onClick(User user) {
        addParticipant(user);
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Select select;

        ContactListAdapter(ArrayList<User> msgList,Select select) {
            this.users = msgList;
            this.select = select;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_contact, parent, false);
            return new ContactListAdapter.MainListItem(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);

            int randomColor = Color.rgb(r, g, b);

            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);
            mainListItem.username.setText(user.getName());
            mainListItem.number.setText(user.getNumber());
            mainListItem.alphabet.setText(user.getName().substring(0,1));
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    select.onClick(user);
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class MainListItem extends RecyclerView.ViewHolder {

            TextView username;
            TextView alphabet;
            TextView number;
            RelativeLayout relativeLayout;

            MainListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                alphabet = itemView.findViewById(R.id.thumbnail);
                number = itemView.findViewById(R.id.number);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
    }
}
