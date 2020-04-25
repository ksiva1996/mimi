package com.leagueofshadows.enc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.ContactsWorker.FLAG;

public class ContactsActivity extends AppCompatActivity implements CompleteCallback, Select {


    ArrayList<User> users;
    RecyclerView listView;
    ContactListAdapter contactListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        users = new ArrayList<>();
        listView = findViewById(R.id.recycler_view);
        contactListAdapter = new ContactListAdapter(users,this,this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(linearLayoutManager);
        listView.setAdapter(contactListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        App app = (App) getApplication();
        app.setCompleteCallback(this);
        load();

    }

    private void load() {
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        DatabaseManager2 databaseManager = DatabaseManager2.getInstance();
        users.clear();
        users.addAll(databaseManager.getUsers());
        contactListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.refresh)
        {
            Intent intent1 = new Intent(ContactsActivity.this,ContactsWorker.class);
            intent1.putExtra(FLAG,0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent1);
            }
            else {
                startService(intent1);
            }
        }
        return true;
    }

    @Override
    public void onComplete(int x) {
        load();
    }

    @Override
    public void onCanceled() {}

    @Override
    public void onClick() {
        finish();
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Context context;
        private Select select;

        ContactListAdapter(ArrayList<User> msgList, Context context,Select select) {
            this.users = msgList;
            this.context = context;
            this.select = select;
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

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);

            int randomColor = Color.rgb(r, g, b);

            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);
            mainListItem.username.setText(user.getName());
            mainListItem.number.setText(user.getNumber());
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context,ChatActivity.class);
                    intent.putExtra(Util.userId,user.getId());
                    context.startActivity(intent);
                    select.onClick();
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
