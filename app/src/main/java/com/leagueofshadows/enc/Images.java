package com.leagueofshadows.enc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.util.ArrayList;

public class Images extends AppCompatActivity {

    ArrayList<File> images;
    int currentPosition = -1;
    ViewPager2 viewPager2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        viewPager2 = findViewById(R.id.viewPager);
        images = new ArrayList<>();
        Intent intent = getIntent();
        String path = intent.getStringExtra(Util.path);
        String currentFocus = intent.getStringExtra(Util.currentFocus);
        String name = intent.getStringExtra(Util.name);
        if(name==null)
        {
            name = "Aditya";
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getSupportActionBar().setTitle(name);


        try{
            assert path != null;
            File file = new File(path);
            if(file.exists())
            {
                File[] files = file.listFiles();
                for (int i=0;i<files.length;i++) {
                    File f = files[i];
                    if (!f.isDirectory())
                    images.add(f);

                    if(f.getName().equals(currentFocus))
                        currentPosition = i;
                }
                if(currentPosition == -1)
                    currentPosition = images.size()-1;

                CustomImageAdapter customImageAdapter = new CustomImageAdapter(images,this);
                viewPager2.setAdapter(customImageAdapter);

            }
            else {
                finish();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            finish();
        }
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                String name = images.get(position).getName();
                getSupportActionBar().setSubtitle(name);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_options,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.share:{

            }
            case R.id.details:{

            }
            case R.id.delete:{

            }
            case R.id.send:{

            }
            case R.id.openInGallery:{

            }
        }
        return true;
    }

    static class CustomImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {

        ArrayList<File> files;
        Context context;

        CustomImageAdapter(ArrayList<File> files,Context context) {
            this.context = context;
            this.files = files;
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
            File file = files.get(position);
            Image image = (Image) holder;
            Glide.with(context).load(file).into(image.zoomageView);
        }

        @Override
        public int getItemCount() {
            return files.size();
        }
    }
}
