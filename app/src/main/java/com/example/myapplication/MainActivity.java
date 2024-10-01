package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final float LOCK_DISTANCE_THRESHOLD = 50.0f;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final float KNOWN_WIDTH = 160f;  // Average width of a face in mm
    private static final float FOCAL_LENGTH = 600f;  // Needs to be calibrated
    private static final float DANGER_DISTANCE = 300f;  // Distance in mm to warn user
    private static final long WARNING_DURATION = 10000; // 10 seconds

    private TextView statusMessage;
    private boolean isUserClose = false; // Flag to check if user is close
    private ProcessCameraProvider cameraProvider;
    private MediaPlayer warningSoundPlayer;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        TextureView cameraPreview = findViewById(R.id.cameraPreview);
        statusMessage = findViewById(R.id.statusMessage);

        // Initialize Device Policy Manager for locking the device
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (!devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Explanation about why you need this permission");
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        }

        // Check for camera permission and start the camera cycle
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraCycle();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Initialize the MediaPlayer for warning sound
        warningSoundPlayer = MediaPlayer.create(this, R.raw.warning_sound);  // Ensure a warning sound is available
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraCycle();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void startCameraCycle() {
        new Thread(() -> {
            while (true) {
                runOnUiThread(this::startCamera);
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(10)); // Camera on duration
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(this::stopCamera);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10)); // Camera off duration
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            statusMessage.setText("Camera stopped...");
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    protected void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();  // Assign cameraProvider here
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    protected void bindCamera(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            ImageProxy mediaImage = image;
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage.getImage(), image.getImageInfo().getRotationDegrees());
                detector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            boolean userStillClose = false;
                            for (Face face : faces) {
                                float faceWidth = face.getBoundingBox().width();
                                float distance = distanceToCamera(KNOWN_WIDTH, FOCAL_LENGTH, faceWidth);
                                if (distance < DANGER_DISTANCE) {
                                    userStillClose = true;
                                    lockDeviceIfStillClose();
                                } else {
                                    statusMessage.setText("Safe distance: " + distance + "mm");
                                    statusMessage.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                                    if (warningSoundPlayer.isPlaying()) {
                                        warningSoundPlayer.stop();
                                        warningSoundPlayer.prepareAsync();  // Prepare for future use
                                    }
                                }
                            }
                            isUserClose = userStillClose;
                            if (isUserClose && !warningSoundPlayer.isPlaying()) {
                                warningSoundPlayer.start();
                            }
                            mediaImage.close();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FaceDetection", "Face detection failed", e);
                            mediaImage.close();
                        });
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
    }

    protected void lockDeviceIfStillClose() {
        statusMessage.setText("Your face is too close! Device will lock in 10 seconds...");
        statusMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));

        if (!warningSoundPlayer.isPlaying()) {
            warningSoundPlayer.start();
        }

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (isUserClose) {
                stopCamera();
                if (devicePolicyManager.isAdminActive(componentName)) {
                    devicePolicyManager.lockNow();
                    if (warningSoundPlayer.isPlaying()) {
                        warningSoundPlayer.stop();
                        warningSoundPlayer.prepareAsync();
                    }
                } else {
                    Log.e("LockDevice", "Device Admin not active or null");
                }
            }
        }, WARNING_DURATION);
    }

    private float distanceToCamera(float knownWidth, float focalLength, float perWidth) {
        return (knownWidth * focalLength) / perWidth;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (warningSoundPlayer != null) {
            warningSoundPlayer.release();  // Release MediaPlayer resources
        }
    }
}
