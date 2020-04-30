package com.leagueofshadows.enc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.jsibbold.zoomage.ZoomageView;

import java.io.FileNotFoundException;

public class ImagePreview extends AppCompatActivity {

    ZoomageView zoomageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        zoomageView = findViewById(R.id.imageView);
        Uri uri = Uri.parse(getIntent().getStringExtra(Util.uri));

        try {
            zoomageView.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(uri)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
