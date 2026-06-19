package com.geoattend.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Map;

public class AttendanceRecord {
    private String id;
    private String userId;
    private String userName;
    private Timestamp timestamp;
    
    @ServerTimestamp
    private Timestamp serverTimestamp;
    private String type; // IN, OUT, AUTO_OUT
    private String geofenceId;
    private String geofenceName;
    private String photoUrl;
    
    // Security and Audit Fields
    private String deviceId;
    private double latitude;
    private double longitude;
    private float accuracy;
    private float distanceToOffice;
    private boolean isMockLocation;
    private boolean isRooted;
    private boolean isUsbDebugging;
    private String integrityVerdict;
    private String livenessResult;
    private int riskScore; // 0 (Safe) to 100 (Critical)
    private String status; // "VERIFIED", "FLAGGED", "REJECTED"
    private String failureReason;

    public AttendanceRecord() {}

    public AttendanceRecord(String id, String userId, String userName, Timestamp timestamp, String type, String geofenceId, String geofenceName, String photoUrl) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.timestamp = timestamp;
        this.type = type;
        this.geofenceId = geofenceId;
        this.geofenceName = geofenceName;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public Timestamp getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(Timestamp serverTimestamp) { this.serverTimestamp = serverTimestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGeofenceId() { return geofenceId; }
    public void setGeofenceId(String geofenceId) { this.geofenceId = geofenceId; }
    public String getGeofenceName() { return geofenceName; }
    public void setGeofenceName(String geofenceName) { this.geofenceName = geofenceName; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
    public float getDistanceToOffice() { return distanceToOffice; }
    public void setDistanceToOffice(float distanceToOffice) { this.distanceToOffice = distanceToOffice; }
    public boolean isMockLocation() { return isMockLocation; }
    public void setMockLocation(boolean mockLocation) { isMockLocation = mockLocation; }
    public boolean isRooted() { return isRooted; }
    public void setRooted(boolean rooted) { isRooted = rooted; }
    public boolean isUsbDebugging() { return isUsbDebugging; }
    public void setUsbDebugging(boolean usbDebugging) { isUsbDebugging = usbDebugging; }
    public String getIntegrityVerdict() { return integrityVerdict; }
    public void setIntegrityVerdict(String integrityVerdict) { this.integrityVerdict = integrityVerdict; }
    public String getLivenessResult() { return livenessResult; }
    public void setLivenessResult(String livenessResult) { this.livenessResult = livenessResult; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
