package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final float KNOWN_WIDTH = 160f;  // Average width of a face in mm
    private static final float FOCAL_LENGTH = 600f;  // Needs to be calibrated
    private static final float DANGER_DISTANCE = 300f;  // Distance in mm to warn user
    private static final float BRIGHTNESS_THRESHOLD = 100f;  // Brightness threshold
    private static final long ON_DURATION = TimeUnit.MINUTES.toMillis(10);  // 10 minutes
    private static final long OFF_DURATION = TimeUnit.SECONDS.toMillis(10);  // 10 seconds
    private static final long WARNING_DURATION = 10000; // 10 seconds

    private TextureView cameraPreview;
    private TextView statusMessage;
    private boolean isUserClose = false; // Flag to check if user is close

    private ProcessCameraProvider cameraProvider; // Add this field to manage the camera lifecycle
    private MediaPlayer warningSoundPlayer; // MediaPlayer for the warning sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        cameraPreview = findViewById(R.id.cameraPreview);
        statusMessage = findViewById(R.id.statusMessage);

        // Existing code for Device Admin and other setups
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
        warningSoundPlayer = MediaPlayer.create(this, R.raw.warning_sound); // Make sure you have a warning sound file
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraCycle();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    protected void startCameraCycle() {
        new Thread(() -> {
            while (true) {
                runOnUiThread(this::startCamera);
                try {
                    Thread.sleep(ON_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(this::stopCamera);
                try {
                    Thread.sleep(OFF_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            statusMessage.setText("Resart...");
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
                                    statusMessage.setTextColor(ContextCompat.getColor(this, android.R.color.white)); // Normal message in white

                                    // Stop the warning sound if the user is at a safe distance
                                    if (warningSoundPlayer.isPlaying()) {
                                        warningSoundPlayer.stop();
                                        warningSoundPlayer.prepareAsync();  // Prepare it for future use
                                    }
                                }
                            }

                            // Update the flag based on the user's current distance
                            isUserClose = userStillClose;

                            // Play warning sound if the user is close
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
        // Show the warning message
        statusMessage.setText("Your face is too close! The device will lock in 10 seconds if you stay too close...");
        statusMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light)); // Warning message in red

        // Play the warning sound
        if (!warningSoundPlayer.isPlaying()) {
            warningSoundPlayer.start();
        }

        // Set the flag to true indicating the user is currently too close
        isUserClose = true;

        // Check the user's distance every second during the warning duration
        Handler handler = new Handler();
        Runnable checkDistanceRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUserClose) {
                    // Stop the warning sound immediately if the user moves back
                    if (warningSoundPlayer.isPlaying()) {
                        warningSoundPlayer.stop();
                        warningSoundPlayer.prepareAsync();  // Prepare it for future use
                    }
                    statusMessage.setText("You moved back. Safe distance maintained.");
                    statusMessage.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white)); // Normal message in white
                } else {
                    // Continue checking the distance if the user is still close
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.postDelayed(checkDistanceRunnable, 1000); // Start checking after 1 second

        // Delay to lock the device after the full warning duration if the user stays too close
        new Handler().postDelayed(() -> {
            if (isUserClose) { // Check if the user is still close after the warning duration
                stopCamera();  // Turn off the camera before locking the device

                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

                if (devicePolicyManager != null && devicePolicyManager.isAdminActive(componentName)) {
                    devicePolicyManager.lockNow();  // Lock the device

                    // Stop the warning sound when the device is locked
                    if (warningSoundPlayer.isPlaying()) {
                        warningSoundPlayer.stop();
                        warningSoundPlayer.prepareAsync();  // Prepare it for future use
                    }
                } else {
                    Log.e("LockDevice", "Device Admin not active or null");
                    // Notify the user or take alternative actions
                }
            }
        }, WARNING_DURATION);
    }


    private float distanceToCamera(float knownWidth, float focalLength, float perWidth) {
        return (knownWidth * focalLength) / perWidth;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // Device admin enabled successfully
                Toast.makeText(this, "Device Admin enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Device admin not enabled
                Toast.makeText(this, "Failed to enable Device Admin", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (warningSoundPlayer != null) {
            warningSoundPlayer.release(); // Release the MediaPlayer resources
        }
    }
}
