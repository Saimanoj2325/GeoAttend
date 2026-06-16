# GeoAttend Walkthrough Guide

This guide walks you through the core workflows of the GeoAttend application, from onboarding to daily attendance.

## 1. Onboarding & Enrollment

### 🔐 Initial Sign In
*   Users sign in via the **LoginActivity**.
*   New employees can register through **RegisterActivity**, which initializes their profile in Firestore.

### 📱 Device Binding
*   Upon first login, the app identifies the unique hardware ID.
*   The `DeviceBindingManager` registers this ID. Subsequent logins from different devices will be flagged or blocked to prevent account sharing.

### 👤 Face Registration
*   The user must register their face template.
*   The app captures a high-quality face embedding, which is encrypted and stored in Firebase. This serves as the "Golden Template" for all future verifications.

## 2. The Daily Attendance Workflow

### 📍 Step 1: Geofence Validation
*   When the employee opens the **EmployeeDashboardActivity**, the map shows their current location.
*   The `GeofenceHelper` checks if the user is within the assigned radius of the office.
*   **Safety Check**: The `SecurityManager` simultaneously checks for Mock Location providers. If "Fake GPS" is detected, the Check-In button is disabled.

### 🎭 Step 2: Face & Liveness Verification
*   Clicking "Check In" launches **FaceVerificationActivity**.
*   **The Challenge**: The user is asked to perform a random action (e.g., "Blink your eyes" or "Turn your head").
*   **The Match**: ML Kit detects the face, and the TFLite model compares the live embedding with the stored Golden Template.
*   Only if **Liveness + Match** are successful will the record be submitted.

### 📝 Step 3: Record Submission
*   A new `AttendanceRecord` is created with:
    *   Verified Status
    *   Server-side Timestamp
    *   GPS Accuracy & Distance
    *   Device Integrity Metadata

## 3. Monitoring & Management

### 🕒 History & Timeline
*   The **Timeline** on the dashboard shows real-time status (In/Out).
*   **AttendanceHistoryActivity** allows users to browse past records and filter by date using the calendar.

### 🛠 Admin Features
*   **Add Geofence**: Admins can tap on a map to define new office locations and set a radius.
*   **Security Center**: A high-level overview of security alerts (e.g., users who attempted to use mock locations or rooted devices).

## 4. Automatic Features

### 🚪 Auto Check-Out
*   The app registers a `GeofenceBroadcastReceiver`.
*   If an employee leaves the office perimeter without manually checking out, the system triggers an "AUTO_OUT" event in the background to ensure work hours are captured correctly.

---
*Tip: Always ensure GPS and Camera permissions are granted for the best experience.*
