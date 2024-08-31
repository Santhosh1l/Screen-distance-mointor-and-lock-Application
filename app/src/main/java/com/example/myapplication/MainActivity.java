package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;

import androidx.camera.core.CameraSelector;
import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final float KNOWN_WIDTH = 160f;  // Average width of a face in mm
    private static final float FOCAL_LENGTH = 600f;  // Needs to be calibrated
    private static final float DANGER_DISTANCE = 300f;  // Distance in mm to warn user
    private static final float BRIGHTNESS_THRESHOLD = 100f;  // Brightness threshold
    private static final long ON_DURATION = TimeUnit.MINUTES.toMillis(10);  // 10 minutes
    private static final long OFF_DURATION = TimeUnit.SECONDS.toMillis(10);  // 10 seconds
    private static float LOCK_DISTANCE_THRESHOLD;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable distanceCheckRunnable;
    private FaceDetector detector; // Declare detector as a class member
    private TextureView cameraPreview;
    private TextView statusMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LOCK_DISTANCE_THRESHOLD = getResources().getDimension(R.dimen.lock_distance_threshold);

        cameraPreview = findViewById(R.id.camera_preview);
        statusMessage = findViewById(R.id.status_message);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCameraCycle();
        }
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
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            statusMessage.setText("Camera off. Waiting...");
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    protected void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
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

        detector = FaceDetection.getClient(options); // Initialize detector
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            @androidx.camera.core.ExperimentalGetImage
            ImageProxy mediaImage = image;
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage.getImage(), image.getImageInfo().getRotationDegrees());

                // Check brightness
                float avgBrightness = getAverageBrightness(mediaImage);
                if (avgBrightness < BRIGHTNESS_THRESHOLD) {
                    // Adjust brightness (you'll need to implement increaseBrightness)
                    statusMessage.setTextColor(getResources().getColor(R.color.default_color));
                    statusMessage.setText("Increasing brightness...");
                }

                detector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            for (Face face : faces) {
                                float faceWidth = face.getBoundingBox().width();
                                float distance = distanceToCamera(KNOWN_WIDTH, FOCAL_LENGTH, faceWidth);

                                if (distance < DANGER_DISTANCE) {
                                    statusMessage.setTextColor(getResources().getColor(R.color.warning_color));
                                    statusMessage.setText("Go back! Your face is too close.");

                                    // Schedule a distance check after the warning message
                                    if (distanceCheckRunnable != null) {
                                        handler.removeCallbacks(distanceCheckRunnable);
                                    }
                                    distanceCheckRunnable = () -> {
                                        // Recheck the distance after a delay
                                        checkDistanceAgain(inputImage);
                                    };
                                    handler.postDelayed(distanceCheckRunnable, 5000); // 5 seconds delay
                                    lockDevice();
                                    return;
                                } else {
                                    statusMessage.setTextColor(getResources().getColor(R.color.default_color));
                                    statusMessage.setText("Safe distance: " + distance + "mm");
                                }
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

    private void checkDistanceAgain(InputImage inputImage) {
        // Reprocess the image to measure the distance again
        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    for (Face face : faces) {
                        float faceWidth = face.getBoundingBox().width();
                        float distance = distanceToCamera(KNOWN_WIDTH, FOCAL_LENGTH, faceWidth);

                        statusMessage.setTextColor(distance < DANGER_DISTANCE
                                ? getResources().getColor(R.color.warning_color)
                                : getResources().getColor(R.color.default_color));
                        statusMessage.setText(distance < DANGER_DISTANCE
                                ? "Go back! Your face is too close."
                                : "Safe distance: " + distance + "mm");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FaceDetection", "Face detection failed", e);
                });
    }

    protected void lockDevice() {
        // Lock the device screen (requires SYSTEM_ALERT_WINDOW permission or device admin privileges)
        // This is just an example, actual implementation may vary
        // Use an appropriate method to lock the screen or display a lock screen
    }

    private float distanceToCamera(float knownWidth, float focalLength, float perWidth) {
        return (knownWidth * focalLength) / perWidth;
    }

    private float getAverageBrightness(ImageProxy image) {
        // Implement a method to calculate the average brightness of the image
        // Placeholder value, implement the actual brightness calculation here
        return 120f;
    }
}
