package com.geoattend.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
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
    
    private static final double MATCH_THRESHOLD = 0.95; // Balanced threshold for MobileFaceNet
    private int matchFailureCount = 0;
    private static final int MAX_MATCH_ATTEMPTS = 5;

    private double minMatchDistance = Double.MAX_VALUE;
    private List<String> passedChallenges = new ArrayList<>();

    private enum State {
        CHALLENGE,
        MATCHING,
        VERIFIED,
        FAILED
    }
    private State currentState = State.CHALLENGE;

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
            Toast.makeText(this, "Model init failed", Toast.LENGTH_SHORT).show();
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
        challenges = challenges.subList(0, 2); 
    }

    private void fetchUserData() {
        FirebaseHelper.getUserRef(FirebaseHelper.getCurrentUserId()).get().addOnSuccessListener(doc -> {
            currentUser = doc.toObject(User.class);
            if (currentUser == null || currentUser.getFaceTemplates() == null || currentUser.getFaceTemplates().isEmpty()) {
                Toast.makeText(this, "Biometric profile not found. Please enroll first.", Toast.LENGTH_LONG).show();
                finish();
            }
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
            if (isProcessing || isSubmitting || currentUser == null || currentState == State.VERIFIED || currentState == State.FAILED) {
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
        if (currentState == State.CHALLENGE) {
            if (livenessStep < challenges.size()) {
                String currentChallenge = challenges.get(livenessStep);
                runOnUiThread(() -> {
                    tvInstruction.setText(currentChallenge);
                    tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_gold));
                });

                boolean passed = false;
                switch (currentChallenge) {
                    case "BLINK BOTH EYES":
                        if (face.getLeftEyeOpenProbability() != null && face.getLeftEyeOpenProbability() < 0.25) passed = true;
                        break;
                    case "SMILE WIDELY":
                        if (face.getSmilingProbability() != null && face.getSmilingProbability() > 0.75) passed = true;
                        break;
                    case "TURN HEAD LEFT":
                        if (face.getHeadEulerAngleY() > 20) passed = true;
                        break;
                }

                if (passed) {
                    passedChallenges.add(currentChallenge);
                    livenessStep++;
                    if (livenessStep >= challenges.size()) {
                        currentState = State.MATCHING;
                        runOnUiThread(() -> {
                            tvInstruction.setText("IDENTIFYING...");
                            tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_blue));
                        });
                    }
                }
                imageProxy.close();
            }
        } else if (currentState == State.MATCHING) {
            isProcessing = true;
            Bitmap bitmap = imageProxy.toBitmap();
            float[] currentEmbedding = faceProcessor.getEmbedding(bitmap, face.getBoundingBox());
            
            if (currentEmbedding != null) {
                minMatchDistance = verifyMatch(currentEmbedding);
                Log.d("FaceVerification", "Min distance: " + minMatchDistance);

                if (minMatchDistance < MATCH_THRESHOLD) {
                    currentState = State.VERIFIED;
                    runOnUiThread(() -> {
                        tvInstruction.setText("IDENTITY VERIFIED");
                        tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_emerald));
                        
                        // Victory Pop Animation
                        tvInstruction.setScaleX(0.5f);
                        tvInstruction.setScaleY(0.5f);
                        tvInstruction.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                            .withEndAction(() -> tvInstruction.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start())
                            .start();

                        progressBar.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(this::submitAttendance, 500);
                    });
                } else {
                    matchFailureCount++;
                    Log.d("FaceVerification", "Match attempt failed: " + matchFailureCount);
                    
                    runOnUiThread(() -> {
                        tvInstruction.setText("IDENTIFYING... (" + matchFailureCount + ")");
                        tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.accent_gold));
                    });

                    if (matchFailureCount >= MAX_MATCH_ATTEMPTS) {
                        currentState = State.FAILED;
                        runOnUiThread(() -> {
                            tvInstruction.setText("FACE MATCH FAILED");
                            tvInstruction.setTextColor(ContextCompat.getColor(this, com.geoattend.R.color.error));
                            Toast.makeText(this, "Biometric mismatch. Please ensure good lighting.", Toast.LENGTH_LONG).show();
                            new Handler().postDelayed(this::finish, 2500);
                        });
                    } else {
                        // Small delay before next attempt to allow user to adjust
                        new Handler().postDelayed(() -> {
                            isProcessing = false;
                        }, 400);
                    }
                }
            } else {
                isProcessing = false;
            }
            imageProxy.close();
        } else {
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
        
        // Populate Security & Liveness fields
        record.setLivenessResult("VERIFIED_CHALLENGES: " + String.join(" | ", passedChallenges));
        record.setRooted(com.geoattend.utils.SecurityManager.isDeviceCompromised());
        record.setUsbDebugging(com.geoattend.utils.SecurityManager.isUsbDebuggingEnabled(this));
        record.setIntegrityVerdict(com.geoattend.utils.SecurityManager.getIntegrityVerdict());
        
        // Advanced Risk Assessment
        Location loc = new Location("verified_gps");
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        int mockRisk = com.geoattend.utils.SecurityManager.getMockRiskScore(loc, this);
        record.setMockLocation(mockRisk >= 50);
        
        // Final risk score aggregation
        int finalRisk = mockRisk;
        if (record.isRooted()) finalRisk += 40;
        if (record.isUsbDebugging()) finalRisk += 25;
        if (minMatchDistance > 0.85) finalRisk += 15;
        record.setRiskScore(Math.min(100, finalRisk));
        
        record.setStatus(finalRisk > 70 ? "FLAGGED" : "VERIFIED");
        record.setFailureReason("Liveness: OK | Match Dist: " + String.format(Locale.getDefault(), "%.4f", minMatchDistance));

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
