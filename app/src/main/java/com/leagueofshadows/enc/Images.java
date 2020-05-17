package com.leagueofshadows.enc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import static com.leagueofshadows.enc.Util.getMessageContent;

public class Images extends AppCompatActivity {

    ArrayList<Message> images;
    String messageId;
    Message currentMessage;
    ViewPager2 viewPager2;
    String otherUserId;
    User otherUser;
    DatabaseManager2 databaseManager2;
    private CustomImageAdapter customImageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        viewPager2 = findViewById(R.id.viewPager);
        images = new ArrayList<>();
        Intent intent = getIntent();
        otherUserId = intent.getStringExtra(Util.userId);
        messageId = intent.getStringExtra(Util.messageId);

        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager2 = DatabaseManager2.getInstance();

        otherUser = databaseManager2.getUser(otherUserId);
        currentMessage = databaseManager2.getMessage(messageId,otherUserId);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        customImageAdapter = new CustomImageAdapter(images,this,otherUserId);
        viewPager2.setAdapter(customImageAdapter);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Message message = images.get(position);
                String title;
                if(otherUserId.equals(message.getFrom()))
                    title = otherUser.getName();
                else
                    title = "You";
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle(getMessageContent(message.getContent()));
            }
        });
        //get messages which contain images from local database
        getImages();
    }

    private void getImages() {
        images.clear();
        ArrayList<Message> messages = databaseManager2.getImages(otherUserId);
        for (Message message:messages) {
            String path;
            if(message.getFrom().equals(otherUserId))
                path = Util.imagesPath+getMessageContent(message.getContent());
            else
                path = Util.sentImagesPath+getMessageContent(message.getContent());

            File file = new File(path);
            if(file.exists())
                images.add(message);
        }
        customImageAdapter.notifyDataSetChanged();
        viewPager2.setCurrentItem(images.indexOf(currentMessage),false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_options,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final int position = viewPager2.getCurrentItem();
        final Message message = images.get(position);

        String path;
        if(message.getFrom().equals(otherUserId))
            path = Util.imagesPath+getMessageContent(message.getContent());
        else
            path = Util.sentImagesPath+getMessageContent(message.getContent());

        final File file = new File(path);
        String name = file.getName();
        String mimeType = "image/*";

        switch (id)
        {
            case R.id.share:{
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case R.id.details:{
                AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
                builder.setView(getLayoutInflater().inflate(R.layout.image_info,null));
                builder.setCancelable(false);
                builder.create().show();
                //TODO add the details
                break;
            }
            case R.id.delete:{

                AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
                builder.setMessage("Delete image "+file.getName());
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean x = file.delete();
                        if(x) {
                            Toast.makeText(Images.this, "Image deleted", Toast.LENGTH_SHORT).show();
                            images.remove(message);
                            customImageAdapter.notifyItemRangeRemoved(position,1);
                        }
                    }
                }).create().show();
                break;
            }
            case R.id.send:{
                Intent intent = new Intent(this,ContactsActivity.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case R.id.openInGallery:{
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file),mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    static class CustomImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {

        ArrayList<Message> files;
        Context context;
        String otherUserId;

        CustomImageAdapter(ArrayList<Message> files,Context context,String otherUserId) {
            this.context = context;
            this.files = files;
            this.otherUserId = otherUserId;
        }

        static class Image extends RecyclerView.ViewHolder {
            ZoomageView zoomageView;
            Image(View view) {
                super(view);
                zoomageView = view.findViewById(R.id.imageView);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_item, parent, false);
            return new Image(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message= files.get(position);
            Image image = (Image) holder;
            String path;
            if(message.getFrom().equals(otherUserId))
                path = Util.imagesPath+getMessageContent(message.getContent());
            else
                path = Util.sentImagesPath+getMessageContent(message.getContent());
            Glide.with(context).load(path).into(image.zoomageView);
        }

        @Override
        public int getItemCount() {
            return files.size();
        }
    }
}
