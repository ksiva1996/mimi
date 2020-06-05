package com.leagueofshadows.enc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.CreateGroupActivity.userColors;
import static com.leagueofshadows.enc.FirebaseHelper.Groups;

public class GroupInfo extends AppCompatActivity {

    Group group;
    String groupId;
    DatabaseManager databaseManager;
    RecyclerView listView;
    TextView name;
    ImageButton deleteButton;
    String currentUserId;
    User currentUser;
    String admins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        name = findViewById(R.id.name);
        deleteButton = findViewById(R.id.delete);
        setTitle("Group info");
        Intent intent = getIntent();
        groupId = intent.getStringExtra(Util.id);
        listView = findViewById(R.id.participants);
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();

        currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        group = databaseManager.getGroup(groupId);
        admins = group.getAdmins();
        currentUser = new User(currentUserId,"you",currentUserId,null);
        if (!group.getUsers().contains(currentUser))
            group.getUsers().add(currentUser);
        else {
            for (User u:group.getUsers()) {
                if (u.getId().equals(currentUserId)) {
                    u.setName("you");
                    break;
                }
            }
        }

        name.setText(group.getName());
        if(admins.contains(currentUserId)){
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteGroupDialog();
                }
            });
        }
        else{
            deleteButton.setVisibility(View.GONE);
        }
        ContactListAdapter contactListAdapter = new ContactListAdapter(group.getUsers(),this,currentUserId,admins);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setAdapter(contactListAdapter);
        listView.setLayoutManager(linearLayoutManager);
    }

    private void showDeleteGroupDialog() {
        if(admins.contains(currentUserId)){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this group?");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteGroup();
            }
        }).create().show();
        }
    }

    private void deleteGroup() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId);
        databaseReference.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendGroupDeleteNotification();
                databaseManager.markGroupAsDeleted(groupId);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendGroupDeleteNotification() {
        Native restHelper = new Native(this);
        String name = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.name,currentUserId);
        String text = name+" has deleted group - \""+group.getName()+"\"";
        restHelper.sendGroupDeleteNotification(groupId,group.getUsers(),text,currentUserId);
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Context context;
        String currentUserId;
        String admins;

        ContactListAdapter(ArrayList<User> users, Context context,String currentUserId,String admins) {
            this.users = users;
            this.context = context;
            this.currentUserId = currentUserId;
            this.admins = admins;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_contact, parent, false);
            return new MainListItem(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            
            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);

            String name = user.getName();
            if(admins.contains(user.getId())){
                name = name+" - \"Group admin\"";
            }

            mainListItem.username.setText(name);
            mainListItem.number.setText(user.getNumber());

            mainListItem.alphabet.setText(user.getName().substring(0,1));
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(userColors[position])));
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!user.getId().equals(currentUserId))
                    openPrivateChat(user.getId(),user.getName());
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
        @SuppressLint("InflateParams")
        void openPrivateChat(final String userId, String name){

            AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.AlertDialog);
            View view = LayoutInflater.from(context).inflate(R.layout.open_private_chat_dialog,null);

            TextView n = view.findViewById(R.id.name);
            n.setText("Send message to - "+name);
            n.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
                    intent.putExtra(Util.userId,userId);
                    context.startActivity(intent);
                }
            });
            builder.setView(view);
            builder.create().show();
        }
    }
}
