package com.shafik.image;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
{
    private FloatingActionButton fab;
    private TextView tv;
    private ImageView iv;
    private String DATA_PATH;
    private final String TESS_DATA="/tessdata";
    private final String FILE_NAME="eng.traineddata";
    private String result="";
    private Bitmap bitmap;
    private Uri imgUri;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        init();
        assign();
        if (!checkPermission())
            requestPermission();

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CropImage.activity().start(MainActivity.this);
            }
        });
    }

    /**
    * initializing view
    * */
    private void init()
    {
        fab=findViewById(R.id.main_fab);
        tv=findViewById(R.id.main_tv);
        iv=findViewById(R.id.main_iv);
    }

    /**
     * assigning instance
     * */
    private void assign()
    {
        Toolbar tb=(Toolbar)findViewById(R.id.main_tb);
        setSupportActionBar(tb);
        getSupportActionBar().setTitle("Image to Text");

        DATA_PATH = Environment.getExternalStorageDirectory() + "/Android/data/"+getPackageName();
    }

    /**
    * create folder method
    * */
    private void createFolder(String filePath)
    {
        File sd = new File(filePath);
        File directory = new File(sd.getAbsolutePath());
        if (!directory.isDirectory())
            directory.mkdirs();
    }

    /**
     * check file exists or not
     * */
    private boolean checkFileExists(String filePath)
    {
        File file = new File(filePath);
        if (file.exists())
            return true;
        else return false;
    }

    /**
    * image selection callback
    * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (!checkFileExists(DATA_PATH + TESS_DATA + "/" + FILE_NAME)) {
                    createFolder(DATA_PATH + TESS_DATA);
                    copyAsset();
                }

                imgUri = result.getUri();
                //iv.setImageURI(imgUri);
                new ConvertTask().execute();
                //longMsg(imgUri.toString()+"\n\n"+imgUri.getPath());
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                longMsg(error.getMessage());
            }
        }
    }

    /**
     * copy tesseract eng.traineddata file and paste in folder
     * */
    private void copyAsset()
    {
        try
        {
            InputStream is = getAssets().open(FILE_NAME);
            OutputStream os = new FileOutputStream(DATA_PATH+TESS_DATA+"/"+FILE_NAME);
            byte[] buffer = new byte[1024];
            while (is.read(buffer) > 0)
            {
                os.write(buffer);
            }
            os.flush();
            os.close();
            is.close();
        } catch (IOException e) { }
    }

    /**
     * convert image into grey scale then convert into text
     * */
    private class ConvertTask extends AsyncTask<String, Void, String>
    {
        TessBaseAPI baseApi;
        ProgressDialog pd;
        Bitmap bitmapGrey=null;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            baseApi = new TessBaseAPI();
            pd= ProgressDialog.show(MainActivity.this, "Processing", "Please wait...", true);
            pd.show();
            if(!checkFileExists(DATA_PATH+TESS_DATA+"/"+FILE_NAME))
            {
                createFolder(DATA_PATH+TESS_DATA);
                copyAsset();
            }
        }

        @Override
        protected String doInBackground(String... strings)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);
            baseApi.init(DATA_PATH, "eng");

            bitmapGrey=toGrayscale(bitmap);
            baseApi.setImage(bitmapGrey);
            result = baseApi.getUTF8Text();
            baseApi.end();
            return result;
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            pd.dismiss();
            tv.setText(s);
            iv.setImageBitmap(bitmapGrey);
        }
    }

    /**
     * convert selected image to gray scale
     * */
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    /**
     * check external read/write permission is enable or not
     * */
    private boolean checkPermission()
    {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] { WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE }, 200);
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 200:
                if (grantResults.length > 0)
                {
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted)
                    {
                        //Toast.makeText(MainActivity.this, "Permission Granted, Now you can access storage.", Toast.LENGTH_SHORT).show();
                        createFolder(DATA_PATH+TESS_DATA);
                        copyAsset();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Permission Denied, App will not work", Toast.LENGTH_SHORT).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))
                            {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                                    requestPermissions(new String[] { WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE }, 200);
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener)
    {
        new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null).create().show();
    }

    private void longMsg(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}