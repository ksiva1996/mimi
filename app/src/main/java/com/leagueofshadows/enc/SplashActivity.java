package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String uid = FirebaseAuth.getInstance().getUid();
        String userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        try {
            String x = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
            Log.e("tag",uid+"   "+x);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        Intent intent;

        if(uid==null) {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
            sp.edit().clear().apply();
            intent = new Intent(this,Register.class);
        }
        else {
            if(userId == null) {
                FirebaseAuth.getInstance().signOut();
                SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
                sp.edit().clear().apply();
                intent = new Intent(this,Register.class);
            }
            else {
                intent = new Intent(this, Login.class);
            }
        }
        startActivity(intent);
        finish();
    }
}
