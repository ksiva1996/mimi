package com.leagueofshadows.enc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Items.Message;

import java.util.HashMap;
import java.util.Map;

public class Test extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        findViewById(R.id.put).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                putMessage();
            }
        });

        /*AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences = getSharedPreferences(Util.preferences,MODE_PRIVATE);
                    String base64 = preferences.getString(Util.PublicKeyString,null);
                    String id = preferences.getString(Util.userId,null);
                    User currentUser = new User(id,"siva",id,base64);

                    File file = new File(Util.imagesPath+"login.html");
                    File outFile = new File(Util.imagesPath+"out");
                    FileInputStream fileInputStream = new FileInputStream(file);
                    FileInputStream fileInputStream1 = new FileInputStream(file);
                    FileOutputStream fileOutputStream = new FileOutputStream(outFile);
                    App app = (App) getApplication();

                    User[] users =new User[]{currentUser};
                    AESHelper2 aesHelper2 = new AESHelper2(Test.this);
                    String cipherText = aesHelper2.encryptFile(fileInputStream,fileInputStream1,fileOutputStream,app.getPrivateKey(),users,"login.html");

                    fileInputStream = new FileInputStream(outFile);
                    fileOutputStream = new FileOutputStream(Util.imagesPath+"newout");

                    aesHelper2.decryptFile(fileInputStream,fileOutputStream,app.getPrivateKey(),currentUser,new File(Util.imagesPath+"newout"),cipherText,id);

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (RunningOnMainThreadException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (MalFormedFileException e) {
                    e.printStackTrace();
                }
            }
        });
    */
    }

    private String getTableName(@NonNull String otherUserId) {
        otherUserId = otherUserId.replaceAll("[^a-zA-z\\d]","");
        return "TABLE"+otherUserId.substring(1);
    }

    void putMessage()
    {
        Map<String, Message> messagesMap = new HashMap<>();
        String id1 = "+919440186376";
        String id2 = "+917032523251";
        String id3 = "+919491080376";

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Test");
        String key = databaseReference.push().getKey();

        Message message1 = new Message(0,key,id1,id2,"message1"," "," time",1,null,null,null,5);
        Message message2 = new Message(0,key,id2,id3,"message1"," "," time",1,null,null,null,5);
        Message message3 = new Message(0,key,id3,id1,"message1"," "," time",1,null,null,null,5);

        messagesMap.put(id1,message1);
        messagesMap.put(id2,message2);
        messagesMap.put(id3,message3);

        databaseReference.child(key).setValue(messagesMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(Test.this,"success",Toast.LENGTH_SHORT).show();
            }
        });
    }

}
