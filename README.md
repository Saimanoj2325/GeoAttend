# GeoAttend: Next-Gen Geofencing & Biometric Attendance System

GeoAttend is a secure, enterprise-grade attendance tracking solution that combines **Geofencing**, **Face Recognition (Biometrics)**, and **Advanced Security Checks** to ensure foolproof employee attendance.

## 🚀 Key Features

### 📍 Intelligent Geofencing
*   **Virtual Perimeters**: Define precise office boundaries using OpenStreetMap (OSMDroid).
*   **Proximity Alerts**: Real-time distance calculation from the center or boundary.
*   **Auto Check-Out**: Automatically records exit when an employee leaves the geofenced area.

### 🎭 Biometric Verification
*   **AI Face Detection**: Uses Google ML Kit for real-time face detection.
*   **Feature Matching**: Local TFLite models for secure face embedding comparison.
*   **Liveness Detection**: Anti-spoofing challenges (blink, smile, turn head) to prevent photo/video-based fraud.

### 🛡️ Multi-Layer Security
*   **Mock Location Detection**: Identifies and blocks GPS spoofing apps.
*   **Device Binding**: Ensures attendance is marked only from an enrolled, authorized device.
*   **Root/Integrity Checks**: Prevents operation on compromised or rooted devices.
*   **Secure Timestamps**: Uses Firebase Server Timestamps to prevent local clock manipulation.

### 📊 Dashboard & Insights
*   **Employee Timeline**: Chronological view of today's movements.
*   **Attendance Analytics**: Monthly percentage and current attendance streaks.
*   **Admin Control**: Manage geofences, view global logs, and handle security alerts.

## 🛠️ Tech Stack

*   **Language**: Java / Android SDK
*   **Database**: Google Firebase Firestore
*   **Authentication**: Firebase Auth
*   **Storage**: Firebase Storage (for Biometric Templates)
*   **Maps**: OSMDroid (OpenStreetMap)
*   **AI/ML**: Google ML Kit (Face Detection) & TensorFlow Lite (Embeddings)
*   **Location**: Google Play Services Location API

## 📋 Setup Instructions

### 1. Firebase Configuration
1.  Create a project in the [Firebase Console](https://console.firebase.google.com/).
2.  Add an Android app with the package name `com.geoattend`.
3.  Download `google-services.json` and place it in the `app/` directory.
4.  Enable **Anonymous** or **Email/Password** Authentication.
5.  Create a **Firestore** database in test mode (or configure rules).
6.  Create a **Firebase Storage** bucket.

### 2. Local Environment
1.  Open the project in **Android Studio (Ladybug or newer)**.
2.  Wait for Gradle Sync to complete.
3.  Ensure you have a physical device for testing (Emulators may have limited GPS/Camera support for biometrics).

### 3. Firestore Indexes
The app uses in-memory sorting for basic operations to avoid initial setup friction. However, for large-scale production, creating a composite index on the `attendance` collection is recommended:
*   Fields: `userId (Ascending)`, `timestamp (Descending)`

## 📁 Project Structure
*   `com.geoattend.employee`: Dashboard, Face Verification, and Profile activities.
*   `com.geoattend.admin`: Geofence management and Security center.
*   `com.geoattend.model`: Data models (AttendanceRecord, GeofenceItem, User).
*   `com.geoattend.utils`: Core logic for Firebase, Security, and AI processing.

---
*Developed for secure and reliable workforce management.*
