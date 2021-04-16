package com.example.pixelmanipulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ImageView image;
    private Bitmap imgBitmap;
    private Button btnSelect, btnProcess;
    private SeekBar sbWindow, sbLevel, sbDepth;
    private TextView tvWindow, tvLevel, tvDepth;
    private LinearLayout llWindow, llLevel, llDepth;
    private ArrayList<ArrayList<Integer>> buffers;
    private int W, H;
    private static final int ALMACENAMIENTO_EXTERNO = 3;
    private static final int IMAGE_PICKER_REQUEST = 4;
    private static final int FILE_PICKER_REQUEST = 5;
    private static boolean accessAlm = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = findViewById(R.id.imgView);
        tvWindow = findViewById(R.id.tvProgressW);
        tvLevel = findViewById(R.id.tvProgressL);
        tvDepth = findViewById(R.id.tvProgressDepth);
        llWindow = findViewById(R.id.llWindow);
        llLevel = findViewById(R.id.lllevel);
        llDepth = findViewById(R.id.llDepth);
        btnSelect = findViewById(R.id.btnSeleccionar);
        btnProcess = findViewById(R.id.btnProcesar);
        sbWindow = findViewById(R.id.sbWindow);
        sbLevel = findViewById(R.id.sbLevel);
        sbDepth = findViewById(R.id.sbDepth);

        W = -1;
        H = -1;

        buffers = new ArrayList<ArrayList<Integer>>();

        image.setDrawingCacheEnabled(true);

        llWindow.setVisibility(View.INVISIBLE);
        llLevel.setVisibility(View.INVISIBLE);
        llDepth.setVisibility(View.INVISIBLE);
        btnProcess.setVisibility(View.INVISIBLE);

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessAlm = requestPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE, "Permission to Access Gallery", ALMACENAMIENTO_EXTERNO);
                if(accessAlm){
                    usePermissionApplication();
                }
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Bitmap bitmapIntent = image.getDrawingCache();
                bitmapIntent.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                Intent intent = new Intent(view.getContext(), CanvaImageView.class);
                intent.putExtra("BitmapImage", byteArray);
                startActivity(intent);
            }
        });

        sbWindow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateWindow();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sbLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateLevel();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sbDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateDepth();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private boolean requestPermission(Activity context, String permit, String justification, int id){
        if(ContextCompat.checkSelfPermission(context, permit) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(context, permit)){
                Toast.makeText(this, justification, Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(context, new String[]{permit}, id);
            return false;
        } else {
            return true;
        }
    }

    private void usePermissionApplication(){
        Intent appIntent = new Intent(Intent.ACTION_GET_CONTENT);
        appIntent.setType("*/*");
        startActivityForResult(appIntent, FILE_PICKER_REQUEST);
    }

    private void usePermissionImage(){
        Intent pictureIntent = new Intent(Intent.ACTION_PICK);
        pictureIntent.setType("image/*");
        startActivityForResult(pictureIntent, IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case IMAGE_PICKER_REQUEST: {
                if(resultCode == RESULT_OK){
                    try{
                        final Uri imageUri = data.getData();
                        final InputStream is = getContentResolver().openInputStream(imageUri);
                        imgBitmap = BitmapFactory.decodeStream(is);
                        image.setImageBitmap(imgBitmap);
                    }catch(FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
                break;
            }

            case FILE_PICKER_REQUEST: {
                if(resultCode == RESULT_OK){

                    /**
                     * https://firebase.google.com/docs/storage/android/download-files?hl=es
                     * https://stackoverflow.com/questions/38581575/android-not-able-to-select-file-from-internal-storage
                     * https://developer.android.com/training/data-storage/app-specific?hl=es-419
                     * https://developer.android.com/studio/debug/device-file-explorer
                     * https://developer.android.com/training/data-storage
                     */

                    final Uri fileUri = data.getData();
                    String path = getPathFromUri(this, fileUri);
                    String name = getFileName(fileUri);
                    //path = path.replace(name, "");
                    Log.i("Files", "Name: " + name);
                    Log.i("Files", "Path: " + path);
                    //insertIntoInternalStorage(name, path);
                    //Toast.makeText(this, "Added file: " + getFilesDir() + "/" + name, Toast.LENGTH_LONG).show();

                    try {
                        String line;
                        BufferedReader br = new BufferedReader(new FileReader(path));
                        /*while((line = br.readLine()) != null){
                            Log.i("Files", line);
                        }*/
                        Log.i("Files", "Leyó archivo");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //WRITE TO INTERNAL STORAGE
                    /*String text = "Hello World! \n Test file \n For saving files in the internal storage of Android";
                    String file_name = "test.txt";
                    FileOutputStream fos = null;
                    try {
                        fos = openFileOutput(file_name, MODE_PRIVATE);
                        fos.write(text.getBytes());
                        Toast.makeText(this, "Saved to: " + getFilesDir() + "/" + file_name, Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(fos != null){
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }*/


                    //READ FROM INTERNAL STORAGE
                    /*try {
                        String line;
                        FileInputStream fis = null;
                        fis = openFileInput("test.txt");
                        InputStreamReader isr = new InputStreamReader(fis);
                        BufferedReader br = new BufferedReader(isr);
                        while((line = br.readLine()) != null){
                            Log.i("Files", line);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    //Log.i("Ruta", "" + convertMHD("/data/user/0/com.example.pixelmanipulation/files/test.txt", "radius_ulna_raw.raw"));
                    //showSeekBars();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){

            case ALMACENAMIENTO_EXTERNO: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    usePermissionImage();
                } else {
                    Toast.makeText(getApplicationContext(), "Access denied to image gallery", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    public void updateWindow() {

        tvWindow.setText("" + sbWindow.getProgress());
        new Thread(new Runnable() {
            @Override
            public void run() {
                image.post(new Runnable() {
                    @Override
                    public void run() {
                        wlAlgorithm();
                    }
                });
            }
        }).start();
    }

    public void updateLevel() {
        tvLevel.setText("" + sbLevel.getProgress());
        new Thread(new Runnable() {
            @Override
            public void run() {
                image.post(new Runnable() {
                    @Override
                    public void run() {
                        wlAlgorithm();
                    }
                });
            }
        }).start();
    }

    public void updateDepth(){
        tvDepth.setText("" + sbDepth.getProgress());
        new Thread(new Runnable() {
            @Override
            public void run() {
                image.post(new Runnable() {
                    @Override
                    public void run() {
                        getBuffer(sbDepth.getProgress());
                    }
                });
            }
        }).start();
    }

    public void getBuffer(int bufferIndex) {

        //Se obtiene el buffer del indice bufferIndex
        ArrayList<Integer> index = buffers.get(bufferIndex);
        int[] buffer = new int[index.size()];
        for(int i = 0; i < index.size(); i++){
            buffer[i] = index.get(i);
        }

        for(int i = 0; i < buffer.length; i++){
            int color = (255 & 0xff) << 24 | (buffer[i] & 0xff) << 16 | (buffer[i] & 0xff) << 8 | (buffer[i] & 0xff);
            buffer[i] = color;
        }

        imgBitmap = Bitmap.createBitmap(buffer, W, H, Bitmap.Config.ARGB_8888);
        image.setImageBitmap(imgBitmap);
    }

    public void wlAlgorithm(){
        int W = imgBitmap.getWidth();
        int H = imgBitmap.getHeight();
        Bitmap imgWL;
        imgWL = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);

        for(int i = 0; i < W; i++){
            for(int j = 0; j < H; j++){
                int color = imgWL.getPixel(i, j);
                int indColor = (color >> 16) & 0xff;
                double slope = getSlope(indColor);
                if(slope > 255){
                    slope = 255;
                } else if(slope < 0) {
                    slope = 0;
                }
                int defColor = Color.argb(255, (int) slope, (int) slope, (int) slope);
                imgWL.setPixel(i, j, defColor);
            }
        }

        image.setImageBitmap(imgWL);
    }

    public double getSlope(int color){

        //Points
        double x1 = sbLevel.getProgress() - (sbWindow.getProgress() / 2);
        double y1 = 0;
        double x2 = sbLevel.getProgress() + (sbWindow.getProgress() / 2);
        double y2 = 255;
        //Get the slope of the curve
        double m = (y2 - y1) / (x2 - x1);
        //Get the Y-axis interception of the curve => Y = mX + b  =>  b = Y - mX
        double b = y2 - (m * x2);
        return m * color + b;
    }

    public String getPathFromUri(Context context, Uri uri){
        String realPath;
        // SDK < API11
        if (Build.VERSION.SDK_INT < 11) {
            realPath = getRealPathFromURI_BelowAPI11(context, uri);
        }
        // SDK >= 11 && SDK < 19
        else if (Build.VERSION.SDK_INT < 19) {
            realPath = getRealPathFromURI_API11to18(context, uri);
        }
        // SDK > 19 (Android 4.4) and up
        else {
            realPath = getRealPathFromURI_API19(context, uri);
        }
        return realPath;
    }

    public String getFileName(Uri uri){
        String result = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try{
                if (cursor != null && cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }

        if(result == null){
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if(cut != -1){
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void insertIntoInternalStorage(String name, String path){

        try {
            FileOutputStream fos = openFileOutput(name, MODE_PRIVATE);
            File file = new File(path);
            byte[] bytes = getBytesFromFile(file);
            fos.write(bytes);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getBytesFromFile(File file){
        byte[] data = new byte[0];
        try {
            data = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void showSeekBars(){
        sbWindow.setMax(255);
        sbWindow.setProgress(255);
        sbLevel.setMax(255);
        sbLevel.setProgress(128);
        sbDepth.setMax(buffers.size());
        sbDepth.setProgress(0);
        tvWindow.setText("" + sbWindow.getProgress());
        tvLevel.setText("" + sbLevel.getProgress());
        tvDepth.setText("" + sbDepth.getProgress());

        llWindow.setVisibility(View.VISIBLE);
        llLevel.setVisibility(View.VISIBLE);
        llDepth.setVisibility(View.VISIBLE);
        btnProcess.setVisibility(View.VISIBLE);
    }

    public native ArrayList<ArrayList<Integer>> convertMHD(String mhdFile, String rawFile);
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    //----------------------------------------------------UTILS METHODS TO OBTAIN FILE PATHS

    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
        }
        return result;
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = 0;
        String result = "";
        if (cursor != null) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
            return result;
        }
        return result;
    }

    public static String getRealPathFromURI_API19(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}