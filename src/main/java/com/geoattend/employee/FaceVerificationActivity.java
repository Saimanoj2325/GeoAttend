package com.geoattend.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
import com.geoattend.model.AttendanceRecord;
import com.geoattend.model.User;
import com.geoattend.utils.FaceRecognitionProcessor;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SecurityUtils;
import com.geoattend.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@androidx.camera.core.ExperimentalGetImage
public class FaceVerificationActivity extends AppCompatActivity {
    private FaceDetector detector;
    private FaceRecognitionProcessor faceProcessor;
    private boolean isProcessing = false;
    private boolean isSubmitting = false;
    private int livenessStep = 0;
    private List<String> challenges = new ArrayList<>();
    private User currentUser;
    private String attendanceType = "IN";
    
    private TextView tvInstruction;
    private ProgressBar progressBar;
    private androidx.camera.view.PreviewView previewView;
    
    private static final double MATCH_THRESHOLD = 0.8; // Lowered from 1.0 for better sensitivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);

        tvInstruction = findViewById(R.id.tvInstruction);
        progressBar = findViewById(R.id.progressBar);
        previewView = findViewById(R.id.previewView);

        if (getIntent().hasExtra("type")) {
            attendanceType = getIntent().getStringExtra("type");
        }

        try {
            faceProcessor = new FaceRecognitionProcessor(this);
        } catch (IOException e) {
            finish();
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        detector = FaceDetection.getClient(options);

        setupChallenges();
        fetchUserData();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 3001);
        }
    }

    private void setupChallenges() {
        challenges.add("BLINK BOTH EYES");
        challenges.add("SMILE WIDELY");
        challenges.add("TURN HEAD LEFT");
        Collections.shuffle(challenges);
        challenges = challenges.subList(0, 2); // Pick 2 random
    }

    private void fetchUserData() {
        FirebaseHelper.getUserRef(FirebaseHelper.getCurrentUserId()).get().addOnSuccessListener(doc -> {
            currentUser = doc.toObject(User.class);
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceVer", "Camera failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            if (isProcessing || currentUser == null) {
                image.close();
                return;
            }
            processImage(image);
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImage(ImageProxy imageProxy) {
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        handleLivenessAndMatch(faces.get(0), imageProxy);
                    } else {
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(e -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void handleLivenessAndMatch(Face face, ImageProxy imageProxy) {
        if (livenessStep < challenges.size()) {
            String currentChallenge = challenges.get(livenessStep);
            runOnUiThread(() -> {
                tvInstruction.setText(currentChallenge);
                tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_gold));
            });

            boolean passed = false;
            switch (currentChallenge) {
                case "BLINK BOTH EYES":
                    if (face.getLeftEyeOpenProbability() != null && face.getLeftEyeOpenProbability() < 0.2) passed = true;
                    break;
                case "SMILE WIDELY":
                    if (face.getSmilingProbability() != null && face.getSmilingProbability() > 0.8) passed = true;
                    break;
                case "TURN HEAD LEFT":
                    if (face.getHeadEulerAngleY() > 20) passed = true;
                    break;
            }

            if (passed) {
                livenessStep++;
                if (livenessStep >= challenges.size()) {
                    runOnUiThread(() -> {
                        tvInstruction.setText("MATCHING BIOMETRICS...");
                        tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_blue));
                    });
                }
            }
            imageProxy.close();
        } else {
            // Liveness Passed, perform Face Match
            isProcessing = true;
            Bitmap bitmap = imageProxy.toBitmap();
            float[] currentEmbedding = faceProcessor.getEmbedding(bitmap, face.getBoundingBox());
            
            if (currentEmbedding != null) {
                double minDistance = verifyMatch(currentEmbedding);
                if (minDistance < MATCH_THRESHOLD) {
                    runOnUiThread(() -> {
                        tvInstruction.setText("VERIFIED");
                        tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_emerald));
                        progressBar.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(this::submitAttendance, 1000);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Face Match Failed", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    });
                }
            } else {
                isProcessing = false;
            }
            imageProxy.close();
        }
    }

    private double verifyMatch(float[] currentEmbedding) {
        if (currentUser == null || currentUser.getFaceTemplates() == null) return Double.MAX_VALUE;
        
        double minDistance = Double.MAX_VALUE;
        for (Map.Entry<String, List<Double>> entry : currentUser.getFaceTemplates().entrySet()) {
            List<Double> templateList = entry.getValue();
            float[] templateArr = new float[templateList.size()];
            for (int i = 0; i < templateList.size(); i++) {
                templateArr[i] = templateList.get(i).floatValue();
            }
            
            double distance = FaceRecognitionProcessor.calculateDistance(currentEmbedding, templateArr);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private void submitAttendance() {
        if (isSubmitting) return;
        isSubmitting = true;
        
        Log.d("FaceVerification", "Starting attendance submission for type: " + attendanceType);
        
        runOnUiThread(() -> {
            tvInstruction.setText("SECURING...");
            tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_gold));
            progressBar.setVisibility(View.VISIBLE);
        });

        String uid = FirebaseHelper.getCurrentUserId();
        String geofenceId = getIntent().getStringExtra("geofenceId");
        String geofenceName = getIntent().getStringExtra("geofenceName");
        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);
        float acc = getIntent().getFloatExtra("acc", 0f);
        float dist = getIntent().getFloatExtra("dist", 0f);

        AttendanceRecord record = new AttendanceRecord(
                null, uid, currentUser.getName(), Timestamp.now(), attendanceType,
                geofenceId, geofenceName, null
        );
        
        record.setLatitude(lat);
        record.setLongitude(lng);
        record.setAccuracy(acc);
        record.setDistanceToOffice(dist);
        record.setDeviceId(SecurityUtils.getAppSpecificDeviceId(this));
        record.setStatus("VERIFIED");
        record.setMockLocation(false); 

        // Final duplicate check before adding - Using a simplified query to avoid missing index error
        // Filter by userId only and check the date in-memory
        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener(query -> {
                boolean alreadyRecorded = false;
                if (query != null && !query.isEmpty()) {
                    long startOfDay = getStartOfDay(new Date()).getTime();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("timestamp");
                        String type = doc.getString("type");
                        
                        if (ts != null && ts.toDate().getTime() >= startOfDay) {
                            if (attendanceType.equals(type)) {
                                alreadyRecorded = true;
                                break;
                            }
                        }
                    }
                }

                if (alreadyRecorded) {
                    Log.d("FaceVerification", "Duplicate " + attendanceType + " detected. Aborting.");
                    Toast.makeText(this, "Attendance already recorded for today", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                
                Log.d("FaceVerification", "Duplicate check passed. Adding record...");
                FirebaseHelper.getAttendanceRef().add(record).addOnSuccessListener(doc -> {
                    Log.d("FaceVerification", "Record added successfully with ID: " + doc.getId());
                    Toast.makeText(this, "Attendance Secured", Toast.LENGTH_LONG).show();
                    finish();
                }).addOnFailureListener(e -> {
                    isSubmitting = false;
                    Log.e("FaceVerification", "Failed to add record", e);
                    runOnUiThread(() -> {
                        tvInstruction.setText("FAILED TO SECURE");
                        progressBar.setVisibility(View.GONE);
                    });
                    Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            })
            .addOnFailureListener(e -> {
                isSubmitting = false;
                Log.e("FaceVerification", "Duplicate check failed", e);
                runOnUiThread(() -> {
                    tvInstruction.setText("VERIFICATION ERROR");
                    progressBar.setVisibility(View.GONE);
                });
                Toast.makeText(this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private Date getStartOfDay(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
        if (faceProcessor != null) faceProcessor.close();
    }
}
