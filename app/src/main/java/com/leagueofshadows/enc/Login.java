package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Crypt.RSAHelper;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.DestroyFailedException;

import androidx.appcompat.app.AppCompatActivity;
import static com.leagueofshadows.enc.ContactsWorker.FLAG;
import static com.leagueofshadows.enc.ContactsWorker.UPDATE_EXISTING;

public class Login extends AppCompatActivity {

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editText = findViewById(R.id.password);
        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String p = editText.getText().toString();
                if(!p.equals(""))
                {
                    checkPassword(p);
                }
                else
                {
                    editText.setError("password Empty");
                }
            }
        });
    }

    private void checkPassword(final String p) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
                    AESHelper aesHelper = new AESHelper(Login.this);

                    String checkMessage = sp.getString(Util.CheckMessage,null);
                    String encryptedCheckMessage = sp.getString(Util.CheckMessageEncrypted,null);

                    assert checkMessage != null;
                    String x = aesHelper.encryptCheckMessage(checkMessage,p);
                    if(x.equals(encryptedCheckMessage)) {
                        show();
                        setUp(p);

                        Intent intent1 = new Intent(Login.this,ContactsWorker.class);
                        intent1.putExtra(FLAG,UPDATE_EXISTING);
                        startService(intent1);

                        Intent intent = new Intent(Login.this,MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        setError();
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                        InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException |
                        RunningOnMainThreadException e) {

                    e.printStackTrace();
                }
            }
        });
    }

    private void setUp(String p) {
        RSAHelper rsaHelper = new RSAHelper(this);
        try {
            PrivateKey privateKey = rsaHelper.getPrivateKey(p);
            App app = (App) getApplication();
            app.setPrivateKey(privateKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException | DestroyFailedException | InvalidAlgorithmParameterException | InvalidKeySpecException | RunningOnMainThreadException e) {
            e.printStackTrace();
            Log.e("private key",e.toString());
        }
    }

    /*private boolean check(String x, String encryptedCheckMessage) {
        if(x.length()!=encryptedCheckMessage.length())
        {
            Log.e("lengths","not equal "+x.length()+" "+encryptedCheckMessage.length());
            return false;
        }
        boolean y = true;
        for(int i=0;i<x.length();i++)
        {
            if(x.charAt(i)!=encryptedCheckMessage.charAt(i))
            {
                y = false;
                Log.e("pos - "+i, String.valueOf(x.charAt(i)+encryptedCheckMessage.charAt(i)));
            }
        }
        return y;
    }*/

    private void show() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Login.this,"Verification successful",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setError("Incorrect password");
            }
        });
    }
}
