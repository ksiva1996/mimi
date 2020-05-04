package com.leagueofshadows.enc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImagePreview extends AppCompatActivity {

    ZoomageView zoomageView;
    boolean flag = false;
    String path;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        zoomageView = findViewById(R.id.imageView);
        Intent intent = getIntent();
        String name = intent.getStringExtra(Util.name);

        getSupportActionBar().setTitle("Send image to "+name);

        String camera = intent.getStringExtra(Util.camera);
        if(camera!=null)
        {
            flag = true;
            path = intent.getStringExtra(Util.path);
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ExifInterface exifInterface = new ExifInterface(getApplicationContext().getFilesDir()+"/current.jpg");
                final Bitmap correctBitmap;

                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
                switch (orientation)
                {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        correctBitmap = rotateBitmap(bitmap,90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        correctBitmap = rotateBitmap(bitmap,180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        correctBitmap = rotateBitmap(bitmap,270);
                        break;
                    case ExifInterface.ORIENTATION_NORMAL:
                    default: correctBitmap = bitmap;
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(path);
                            correctBitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                zoomageView.setImageBitmap(correctBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            uri = Uri.parse(intent.getStringExtra(Util.uri));
            try {
                zoomageView.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(uri)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flag)
                {
                    Intent data = new Intent();
                    data.putExtra(Util.uri,Uri.fromFile(new File(path)).toString());
                    setResult(RESULT_OK,data);
                    finish();
                }
                else
                {
                    Intent data = new Intent();
                    data.putExtra(Util.uri,uri.toString());
                    setResult(RESULT_OK,data);
                    finish();
                }
            }
        });

    }

    private Bitmap rotateBitmap(Bitmap bitmap, float i) {
        Matrix matrix = new Matrix();
        matrix.setRotate(i);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

}
