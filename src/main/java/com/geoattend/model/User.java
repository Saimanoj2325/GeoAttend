package com.geoattend.model;

import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role; // "admin" or "employee"
    private String profileImageUrl;
    private String employeeId;
    
    // Security Fields
    private String registeredDeviceId;
    private boolean isFaceRegistered;
    private boolean isDeviceEnrolled;
    private Map<String, Object> deviceInfo;
    private String assignedGeofenceId;
    private String department;
    private String managerName;
    private Map<String, List<Double>> faceTemplates;
    private String status;
    private String otp;

    public User() {}

    public User(String uid, String name, String email, String role, String profileImageUrl, String employeeId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.employeeId = employeeId;
        this.isFaceRegistered = false;
        this.isDeviceEnrolled = false;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getRegisteredDeviceId() { return registeredDeviceId; }
    public void setRegisteredDeviceId(String registeredDeviceId) { this.registeredDeviceId = registeredDeviceId; }
    public boolean isFaceRegistered() { return isFaceRegistered; }
    public void setFaceRegistered(boolean faceRegistered) { isFaceRegistered = faceRegistered; }
    public boolean isDeviceEnrolled() { return isDeviceEnrolled; }
    public void setDeviceEnrolled(boolean deviceEnrolled) { isDeviceEnrolled = deviceEnrolled; }
    public Map<String, Object> getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(Map<String, Object> deviceInfo) { this.deviceInfo = deviceInfo; }
    public String getAssignedGeofenceId() { return assignedGeofenceId; }
    public void setAssignedGeofenceId(String assignedGeofenceId) { this.assignedGeofenceId = assignedGeofenceId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public Map<String, List<Double>> getFaceTemplates() { return faceTemplates; }
    public void setFaceTemplates(Map<String, List<Double>> faceTemplates) { this.faceTemplates = faceTemplates; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
