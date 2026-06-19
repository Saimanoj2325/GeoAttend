package com.geoattend.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
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
    private int currentStepIndex = 0;
    private boolean isCapturing = false;
    
    private TextView tvStepIndicator, tvInstruction, tvProgressPercent;
    private LinearProgressIndicator progressBar;
    private androidx.camera.view.PreviewView previewView;
    
    private View[] stepContainers;
    private ImageView[] stepIcons;
    private TextView[] stepTexts;

    private interface PoseValidator {
        boolean isValid(float eulerX, float eulerY);
    }

    private static class EnrollmentStep {
        final String key;
        final String instruction;
        final PoseValidator validator;

        EnrollmentStep(String key, String instruction, PoseValidator validator) {
            this.key = key;
            this.instruction = instruction;
            this.validator = validator;
        }
    }

    private final EnrollmentStep[] enrollmentSteps = {
        new EnrollmentStep("front", "Look directly into the scanner", (x, y) -> Math.abs(y) < 10 && Math.abs(x) < 10),
        new EnrollmentStep("left", "Turn your head slightly LEFT", (x, y) -> y > 20),
        new EnrollmentStep("right", "Turn your head slightly RIGHT", (x, y) -> y < -20),
        new EnrollmentStep("up", "Tilt your head UPWARDS", (x, y) -> x > 15),
        new EnrollmentStep("down", "Tilt your head DOWNWARDS", (x, y) -> x < -15)
    };

    private final Map<String, List<Double>> faceTemplates = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_enrollment);

        initViews();
        setupClickListeners();
        initProcessors();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
        
        updateStepUI();
    }

    private void initViews() {
        tvStepIndicator = findViewById(R.id.tv_step_indicator_top);
        tvInstruction = findViewById(R.id.tv_instruction);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        progressBar = findViewById(R.id.progressBar);
        previewView = findViewById(R.id.previewView);

        stepContainers = new View[]{
            findViewById(R.id.step1_container),
            findViewById(R.id.step2_container),
            findViewById(R.id.step3_container),
            findViewById(R.id.step4_container),
            findViewById(R.id.step5_container)
        };

        stepIcons = new ImageView[]{
            findViewById(R.id.step1_icon),
            findViewById(R.id.step2_icon),
            findViewById(R.id.step3_icon),
            findViewById(R.id.step4_icon),
            findViewById(R.id.step5_icon)
        };

        stepTexts = new TextView[]{
            findViewById(R.id.step1_text),
            findViewById(R.id.step2_text),
            findViewById(R.id.step3_text),
            findViewById(R.id.step4_text),
            findViewById(R.id.step5_text)
        };
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initProcessors() {
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
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
        if (currentStepIndex >= enrollmentSteps.length) {
            image.close();
            return;
        }

        EnrollmentStep currentStep = enrollmentSteps[currentStepIndex];
        float eulerY = face.getHeadEulerAngleY();
        float eulerX = face.getHeadEulerAngleX();

        if (currentStep.validator.isValid(eulerX, eulerY)) {
            isCapturing = true;
            Bitmap bitmap = image.toBitmap();
            float[] embedding = faceProcessor.getEmbedding(bitmap, face.getBoundingBox());
            
            if (embedding != null) {
                faceTemplates.put(currentStep.key, FaceRecognitionProcessor.floatArrayToList(embedding));
                runOnUiThread(this::proceedToNextStep);
            } else {
                isCapturing = false;
            }
        }
        image.close();
    }

    private void proceedToNextStep() {
        currentStepIndex++;
        updateStepUI();
        
        if (currentStepIndex < enrollmentSteps.length) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> isCapturing = false, 1500);
        } else {
            completeEnrollment();
        }
    }

    private void updateStepUI() {
        int totalSteps = enrollmentSteps.length;
        int displayStep = Math.min(currentStepIndex + 1, totalSteps);
        
        if (tvStepIndicator != null) {
            tvStepIndicator.setText(String.format(Locale.getDefault(), "Step %d of %d", displayStep, totalSteps));
        }
        
        int progress = (int) (((float) currentStepIndex / totalSteps) * 100);
        if (progressBar != null) {
            progressBar.setProgress(progress, true);
        }
        if (tvProgressPercent != null) {
            tvProgressPercent.setText(String.format(Locale.getDefault(), "%d%%", progress));
        }

        if (currentStepIndex < totalSteps && tvInstruction != null) {
            String newInstruction = enrollmentSteps[currentStepIndex].instruction;
            if (!tvInstruction.getText().toString().equals(newInstruction)) {
                tvInstruction.animate().alpha(0).setDuration(200).withEndAction(() -> {
                    tvInstruction.setText(newInstruction);
                    tvInstruction.animate().alpha(1).setDuration(200).start();
                }).start();
            }
        }

        // Update the list of steps
        for (int i = 0; i < stepContainers.length; i++) {
            if (stepContainers[i] == null) continue;

            final int index = i;
            String state = (String) stepIcons[i].getTag();
            if (i < currentStepIndex) {
                // Completed
                if (stepIcons[i] != null && !"completed".equals(state)) {
                    stepIcons[i].setTag("completed");
                    stepIcons[i].animate().scaleX(0.5f).scaleY(0.5f).setDuration(150).withEndAction(() -> {
                        stepIcons[index].setImageResource(R.drawable.ic_check_circle);
                        stepIcons[index].setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_gold)));
                        stepIcons[index].animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction(() -> 
                            stepIcons[index].animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        ).start();
                    }).start();
                }
                if (stepTexts[i] != null) {
                    stepTexts[i].setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    stepTexts[i].setTypeface(null, Typeface.NORMAL);
                }
                stepContainers[i].animate().alpha(0.6f).setDuration(300).start();
            } else if (i == currentStepIndex) {
                // Current
                if (stepIcons[i] != null && !"active".equals(state)) {
                    stepIcons[i].setTag("active");
                    stepIcons[i].setImageResource(R.drawable.dot_active);
                    stepIcons[i].setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_gold)));
                    stepIcons[i].animate().scaleX(1.3f).scaleY(1.3f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                }
                if (stepTexts[i] != null) {
                    stepTexts[i].setTextColor(ContextCompat.getColor(this, R.color.accent_gold));
                    stepTexts[i].setTypeface(null, Typeface.BOLD);
                }
                stepContainers[i].animate().alpha(1.0f).scaleX(1.02f).scaleY(1.02f).setDuration(300).start();
            } else {
                // Future
                if (stepIcons[i] != null) {
                    stepIcons[i].setTag("future");
                    stepIcons[i].setImageResource(R.drawable.dot_inactive);
                    stepIcons[i].setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
                    stepIcons[i].setScaleX(1.0f);
                    stepIcons[i].setScaleY(1.0f);
                }
                if (stepTexts[i] != null) {
                    stepTexts[i].setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    stepTexts[i].setTypeface(null, Typeface.NORMAL);
                }
                stepContainers[i].animate().alpha(0.4f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            }
        }
    }

    private void completeEnrollment() {
        if (tvInstruction != null) {
            tvInstruction.animate().alpha(0).setDuration(200).withEndAction(() -> {
                tvInstruction.setText("Securing Profile...");
                tvInstruction.animate().alpha(1).setDuration(200).start();
            }).start();
        }
        if (progressBar != null) progressBar.setProgress(100, true);
        if (tvProgressPercent != null) tvProgressPercent.setText(String.format(Locale.getDefault(), "%d%%", 100));

        String uid = FirebaseHelper.getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFaceRegistered", true);
        updates.put("faceTemplates", faceTemplates);
        updates.put("status", "ACTIVE");

        FirebaseHelper.getUserRef(uid).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Biometric Profile Secured", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save biometrics", Toast.LENGTH_SHORT).show();
                isCapturing = false;
                if (currentStepIndex > 0) currentStepIndex--; // Allow retry of last step
                updateStepUI();
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
        if (faceProcessor != null) faceProcessor.close();
    }
}
