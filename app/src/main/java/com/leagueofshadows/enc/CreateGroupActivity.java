package com.leagueofshadows.enc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Interfaces.CheckUser;
import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.GroupFirebase;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CreateGroupActivity extends AppCompatActivity implements Select, CheckUser {

    ArrayList<User> groupParticipants = new ArrayList<>();
    User currentUser;
    ArrayList<User> users;

    RecyclerView contacts;
    RecyclerView participantsListView;
    ContactListAdapter contactListAdapter;
    ParticipantsListViewAdapter participantsListViewAdapter;

    DatabaseManager2 databaseManager;

    static String[] colors = new String[]{"#0f4c75","#FFF44336","#FFFFC107","#FF009688","#FF9C27B0","#FFE91E63"};

    ProgressDialog progressDialog;
    boolean allSuccess = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        setTitle("Add Participants");

        users = new ArrayList<>();

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String id = sp.getString(Util.userId,null);
        String name = sp.getString(Util.name,null);
        String number = sp.getString(Util.number,null);

        contacts = findViewById(R.id.contacts);
        participantsListView = findViewById(R.id.icons);

        contactListAdapter = new ContactListAdapter(users,this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        contacts.setLayoutManager(linearLayoutManager);
        contacts.setAdapter(contactListAdapter);

        participantsListViewAdapter = new ParticipantsListViewAdapter(groupParticipants,this);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        linearLayoutManager1.setOrientation(RecyclerView.HORIZONTAL);
        participantsListView.setLayoutManager(linearLayoutManager1);
        participantsListView.setAdapter(participantsListViewAdapter);

        assert id != null;
        assert name != null;
        assert number != null;

        currentUser = new User(id,name,number,null);
        addParticipant(currentUser);

        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupActivity.this,R.style.AlertDialog);
                View view1 = LayoutInflater.from(CreateGroupActivity.this).inflate(R.layout.create_group_name,null);
                builder.setView(view1);
                builder.setCancelable(true);

                final EditText name = view1.findViewById(R.id.name);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String n = name.getText().toString();
                        if(!n.equals("")) {
                            askConfirmation(n);
                        }
                        else {
                            Toast.makeText(CreateGroupActivity.this,"Name cannot be empty",Toast.LENGTH_SHORT).show();
                        }

                    }
                }).create().show();
            }
        });

        progressDialog = new ProgressDialog(this);
        loadContacts();

    }

    private void askConfirmation(final String name) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        builder.setMessage("Create Group with name - \""+name+"\" and with "+groupParticipants.size()+" participants");
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createGroup(name);
            }
        }).create().show();
    }

    private void loadContacts() {
        users.clear();
        users.addAll(databaseManager.getUsers());
        users.remove(currentUser);
        contactListAdapter.notifyDataSetChanged();
    }

    void addParticipant(User user)
    {
        if (groupParticipants.size()<6) {
            groupParticipants.add(user);
            participantsListViewAdapter.notifyDataSetChanged();
            int position = users.indexOf(user);
            contactListAdapter.notifyItemChanged(position);
        }
        else {
            Toast.makeText(this,"Currently we only support maximum 6 participants ",Toast.LENGTH_SHORT).show();
        }
    }

    void createGroup(String name)
    {
        progressDialog.setMessage("Creating group...");
        progressDialog.show();

        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String groupId = databaseReference.child(FirebaseHelper.Groups).push().getKey();

        final ArrayList<String> userIds = new ArrayList<>();
        for (User u:users) {
            userIds.add(u.getId());
        }
        final GroupFirebase group = new GroupFirebase(groupId,name,userIds);

        databaseReference.child(FirebaseHelper.Groups).child(groupId).setValue(group).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                for (final String id:userIds ){
                    databaseReference.child(FirebaseHelper.Users).child(id).child(FirebaseHelper.Groups).setValue(group).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            users.remove(userIds.indexOf(id));
                            if(users.size()==0)
                                finalMethod(groupId);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            users.remove(userIds.indexOf(id));
                            allSuccess = false;
                            if (users.size()==0)
                                finalMethod(groupId);
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CreateGroupActivity.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
            }
        });
    }

    void finalMethod(String groupId)
    {

    }

    @Override
    public void onClick(User user) {

        if(user.equals(currentUser))
            return;

        if(groupParticipants.contains(user)){
            groupParticipants.remove(user);
            contactListAdapter.notifyItemChanged(users.indexOf(user));
            participantsListViewAdapter.notifyDataSetChanged();
        }
        else
            addParticipant(user);
    }

    @Override
    public boolean check(User user) {
        return groupParticipants.contains(user);
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Select select;
        private CheckUser checkUser;
        private  Context context;

        ContactListAdapter(ArrayList<User> msgList,Context context) {
            this.users = msgList;
            this.select = (Select) context;
            this.context = context;
            this.checkUser = (CheckUser) context;
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

            if(checkUser.check(user)) {

                mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((context.getColor(R.color.colorPrimary))));
                mainListItem.alphabet.setText("");
            }
            else {
                mainListItem.alphabet.setText(user.getName().substring(0,1));
                mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));
            }

            mainListItem.username.setText(user.getName());
            mainListItem.number.setText(user.getNumber());
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

    static class ParticipantsListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        ArrayList<User> participants;
        Context context;

        ParticipantsListViewAdapter(ArrayList<User> participants, Context context){
            this.participants = participants;
            this.context = context;
        }

        static class Item extends RecyclerView.ViewHolder{
            TextView textView;
            Item(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.icon);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
           View view = LayoutInflater.from(context).inflate(R.layout.create_group_icon,parent,false);
           return new Item(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            Item h = (Item) holder;

            h.textView.setBackgroundTintList(ColorStateList.valueOf((Color.parseColor(colors[position]))));

            final User user = participants.get(position);

            h.textView.setText(user.getName().substring(0,1));
            h.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.AlertDialog);

                    View view1 = LayoutInflater.from(context).inflate(R.layout.create_group_icon_click_dialog,null);
                    builder.setView(view1);
                    builder.setCancelable(true);

                    TextView name = view1.findViewById(R.id.name);
                    TextView number = view1.findViewById(R.id.number);
                    TextView thumbnail = view1.findViewById(R.id.thumbnail);
                    ImageButton delete = view1.findViewById(R.id.delete);

                    thumbnail.setText(user.getName().substring(0,1));
                    thumbnail.setBackgroundTintList(ColorStateList.valueOf((Color.parseColor(colors[position]))));
                    name.setText(user.getName());
                    number.setText(user.getNumber());

                    final AlertDialog alertDialog = builder.create();
                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Select select = (Select) context;
                            select.onClick(user);
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();
                }
            });
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }
    }
}
