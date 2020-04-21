package com.leagueofshadows.enc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import static com.leagueofshadows.enc.ContactsWorker.FLAG;

public class ContactsActivity extends AppCompatActivity implements CompleteCallback {


    ArrayList<User> users;
    ArrayList<String> names;
    ArrayAdapter<String> arrayAdapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        users = new ArrayList<>();
        names = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);
        listView = findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                   String id = users.get(i).getId();
                    Intent intent = new Intent(ContactsActivity.this,ChatActivity.class);
                    intent.putExtra(Util.userId,id);
                    startActivity(intent);
                    finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        App app = (App) getApplication();
        app.setCompleteCallback(this);
        load();

    }

    private void load() {

        DatabaseManager.initializeInstance(new SQLHelper(this));
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        users.clear();
        names.clear();
        users = databaseManager.getUsers();
        for(User u:users) {
            names.add(u.getName());
        }
        arrayAdapter.notifyDataSetChanged();

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
    public void onCanceled() {

    }
}
