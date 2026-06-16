package com.geoattend.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.geoattend.R;
import com.geoattend.utils.FaceRecognitionProcessor;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@androidx.camera.core.ExperimentalGetImage
public class FaceEnrollmentActivity extends AppCompatActivity {
    private FaceDetector detector;
    private FaceRecognitionProcessor faceProcessor;
    private int currentStep = 1;
    private boolean isCapturing = false;
    
    private TextView tvStepIndicator, tvInstruction;
    private LinearProgressIndicator progressBar;
    private androidx.camera.view.PreviewView previewView;
    
    private final String[] instructions = {
        "Look directly into the scanner",
        "Turn your head slightly LEFT",
        "Turn your head slightly RIGHT",
        "Tilt your head UPWARDS",
        "Tilt your head DOWNWARDS"
    };

    private final Map<String, List<Double>> faceTemplates = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_enrollment);

        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        tvInstruction = findViewById(R.id.tv_instruction);
        progressBar = findViewById(R.id.progressBar);
        previewView = findViewById(R.id.previewView);

        try {
            faceProcessor = new FaceRecognitionProcessor(this);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load face recognition model", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        detector = FaceDetection.getClient(options);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for biometric enrollment", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("Enrollment", "Camera failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll(); // Clear any existing bindings

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            if (isCapturing) {
                image.close();
                return;
            }

            processImage(image);
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImage(ImageProxy image) {
        android.media.Image mediaImage = image.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
            detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        validateAndCapture(faces.get(0), image);
                    } else {
                        image.close();
                    }
                })
                .addOnFailureListener(e -> image.close());
        } else {
            image.close();
        }
    }

    private void validateAndCapture(Face face, ImageProxy image) {
        boolean correctPose = false;
        String poseKey = "";

        float eulerY = face.getHeadEulerAngleY();
        float eulerX = face.getHeadEulerAngleX();

        switch (currentStep) {
            case 1: // Front
                if (Math.abs(eulerY) < 10 && Math.abs(eulerX) < 10) { correctPose = true; poseKey = "front"; }
                break;
            case 2: // Left
                if (eulerY > 20) { correctPose = true; poseKey = "left"; }
                break;
            case 3: // Right
                if (eulerY < -20) { correctPose = true; poseKey = "right"; }
                break;
            case 4: // Up
                if (eulerX > 15) { correctPose = true; poseKey = "up"; }
                break;
            case 5: // Down
                if (eulerX < -15) { correctPose = true; poseKey = "down"; }
                break;
        }

        if (correctPose) {
            isCapturing = true;
            Bitmap bitmap = image.toBitmap();
            float[] embedding = faceProcessor.getEmbedding(bitmap, face.getBoundingBox());
            if (embedding != null) {
                faceTemplates.put(poseKey, FaceRecognitionProcessor.floatArrayToList(embedding));
                runOnUiThread(() -> processStep());
            } else {
                isCapturing = false;
            }
        }
        image.close();
    }

    private void processStep() {
        progressBar.setProgress(currentStep * 20, true);
        
        if (currentStep < 5) {
            currentStep++;
            tvStepIndicator.setText("PHASE 0" + currentStep + " / 05");
            tvInstruction.setText(instructions[currentStep - 1]);
            new Handler().postDelayed(() -> isCapturing = false, 1500);
        } else {
            completeEnrollment();
        }
    }

    private void completeEnrollment() {
        String uid = FirebaseHelper.getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("faceRegistered", true);
        updates.put("faceTemplates", faceTemplates);

        FirebaseHelper.getUserRef(uid).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Biometric Profile Secured", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save biometrics", Toast.LENGTH_SHORT).show();
                isCapturing = false;
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
        if (faceProcessor != null) faceProcessor.close();
    }
}
