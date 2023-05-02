package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.camerax.presenter.IPlatePresenter;
import com.example.camerax.presenter.PlatePresenter;
import com.example.camerax.view.IPlateView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


//saus: https://codelabs.developers.google.com/codelabs/camerax-getting-started/

public class MainActivity extends AppCompatActivity implements IPlateView {

    private static final int GALLERY_REQUEST_CODE = 1;
    private final int REQUEST_CODE_PERMISSIONS = 10; //arbitrary number, can be changed accordingly
    private final String[] REQUIRED_PERMISSIONS =
            new String[]{"android.permission.CAMERA"
            ,"android.permission.WRITE_EXTERNAL_STORAGE"
            ,"android.permission.READ_EXTERNAL_STORAGE"}; //array w/ permissions from manifest
    TextureView txView;
    String imagePath;
    ImageView imageView;
    Button btnGallery, submitBtn;
    private IPlatePresenter platePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        txView = findViewById(R.id.view_finder);
        btnGallery = findViewById(R.id.galery_button);
        submitBtn = findViewById(R.id.submit_button);

        txView.setOnClickListener((v)  -> {
            startCamera();
        });

        imageView.setOnClickListener((v)  -> {
            startCamera();
        });

        btnGallery.setOnClickListener(v -> {
            imageView.setVisibility(View.VISIBLE);
            txView.setVisibility(View.GONE);
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        });

        setBtnSubmit();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    GALLERY_REQUEST_CODE);
        } else {
            // Permission is already granted
            // Perform the read operation here
        }

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }

    private void setBtnSubmit(){
        submitBtn.setOnClickListener(item->{
            ImageView imageView = findViewById(R.id.imageView);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            System.out.println("1 hehee----------------------------------");
            //Chuyển đổi Bitmap thành đối tượng byte[] (mảng byte)
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            System.out.println("2 hehee----------------------------------");
            // dong goi du lieu
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "plate.jpg", requestBody);
            System.out.println("3 hehee----------------------------------");
            platePresenter = new PlatePresenter(this);
            platePresenter.sendImage(imagePart);

            System.out.println("4 hehee----------------------------------");

        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }
    void setImage(Bitmap bitmap){
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        txView.setSurfaceTexture(surfaceTexture);
        Canvas canvas = txView.lockCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        txView.unlockCanvasAndPost(canvas);
    }

    private void startCamera() {
        //make sure there isn't another camera instance running before starting
        CameraX.unbindAll();
        imageView.setVisibility(View.GONE);
        txView.setVisibility(View.VISIBLE);

        /* start preview */
        int aspRatioW = txView.getWidth(); //get width of screen
        int aspRatioH = txView.getHeight(); //get height
        Rational asp = new Rational (aspRatioW, aspRatioH); //aspect ratio
        Size screen = new Size(aspRatioW, aspRatioH); //size of the screen

        //config obj for preview/viewfinder thingy.
        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(asp).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig); //lets build it

        //to update the surface texture we have to destroy it first, then re-add it
        preview.setOnPreviewOutputUpdateListener(
                output -> {
                    ViewGroup parent = (ViewGroup) txView.getParent();
                    parent.removeView(txView);
                    parent.addView(txView, 0);
                    txView.setSurfaceTexture(output.getSurfaceTexture());
                    updateTransform();
                });
        //config obj, selected capture mode
        ImageCaptureConfig imgCapConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imgCapConfig);

        findViewById(R.id.capture_button).setOnClickListener(v -> {

            File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".jpg");
            imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                @Override
                public void onImageSaved(@NonNull File file) {
                    String msg = "Photo capture succeeded: " + file.getAbsolutePath();
                    imagePath =  file.getAbsolutePath();
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    setImage(bitmap);
                    Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                    String msg = "Photo capture failed: " + message;
                    Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    if(cause != null){
                        cause.printStackTrace();
                    }
                }
            });
        });



        /* image analyser */

        ImageAnalysisConfig imgAConfig = new ImageAnalysisConfig.Builder().setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE).build();
        ImageAnalysis analysis = new ImageAnalysis(imgAConfig);

        analysis.setAnalyzer(
                (image, rotationDegrees) -> {
                });

        CameraX.bindToLifecycle(this, analysis, imgCap, preview);
    }

    private void updateTransform(){
        /*
        * compensates the changes in orientation for the viewfinder, bc the rest of the layout stays in portrait mode.
        * methinks :thonk:
        * imgCap does this already, this class can be commented out or be used to optimise the preview
        */
        Matrix mx = new Matrix();
        float w = txView.getMeasuredWidth();
        float h = txView.getMeasuredHeight();

        float centreX = w / 2f; //calc centre of the viewfinder
        float centreY = h / 2f;

        int rotationDgr;
        int rotation = (int)txView.getRotation(); //cast to int bc switches don't like floats

        switch(rotation){ //correct output to account for display rotation
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, centreX, centreY);
        txView.setTransform(mx); //apply transformations to textureview
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){
        //check if req permissions have been granted
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onComplete(String isSuccessful) {
        System.out.printf("1. callback api upload images ");
        System.out.printf(isSuccessful);
        imageView = findViewById(R.id.imageView);
        imageView.setImageDrawable(null);
        startCamera();
    }

    @Override
    public void onError(String message) {
        System.out.printf("2. callback api upload images error %s", message);
    }
}
