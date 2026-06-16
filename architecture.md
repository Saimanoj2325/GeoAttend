# GeoAttend Architecture Documentation

The GeoAttend application follows a modular, layer-based architecture designed for security, scalability, and performance on mobile devices.

## 🏗️ High-Level Architecture

The project is divided into three primary layers:

### 1. Presentation Layer (`com.geoattend`)
*   **Activities**: Responsible for UI rendering and user interaction.
    *   `employee`: Contains activities for the employee dashboard, face verification, and history.
    *   `admin`: Contains tools for geofence management and security monitoring.
*   **Adapters**: Handle the population of List/RecyclerView components (e.g., `TimelineAdapter`, `HistoryAdapter`).
*   **View Binding**: Used throughout the project to ensure type-safe access to UI components.

### 2. Logic & Utility Layer (`com.geoattend.utils`)
*   **`FaceRecognitionProcessor`**: Handles the interface with ML Kit and TensorFlow Lite. It extracts face embeddings and performs the mathematical comparison.
*   **`GeofenceHelper`**: Encapsulates the logic for distance calculation and boundary checks.
*   **`SecurityManager`**: The core security engine. It checks for:
    *   Mock Locations (GPS Spoofing).
    *   Device Rooting/Integrity status.
    *   Unique Device Binding.
*   **`FirebaseHelper`**: A centralized singleton for accessing Firestore collections, Auth, and Storage.

### 3. Data Layer (`com.geoattend.model`)
*   **POJOs (Plain Old Java Objects)**: Represent the data structures stored in Firestore.
    *   `AttendanceRecord`: Detailed logs of every check-in/out attempt.
    *   `User`: Profile information, including the biometric "Golden Template".
    *   `GeofenceItem`: Geographic coordinates and radius for office locations.

## 🔄 Data Flow: The Attendance Cycle

1.  **Trigger**: User clicks "Check In" on `EmployeeDashboardActivity`.
2.  **Location Check**: `GeofenceHelper` validates the user is inside a permitted zone. `SecurityManager` validates location integrity.
3.  **Biometric Capture**: `FaceVerificationActivity` captures camera frames.
4.  **AI Processing**:
    *   **ML Kit** detects the face and handles liveness challenges.
    *   **TFLite** generates an embedding for the detected face.
5.  **Verification**: The embedding is compared against the user's "Golden Template" stored in Firestore.
6.  **Submission**: If all checks pass, a new `AttendanceRecord` is pushed to Firestore.
7.  **Sync**: The Dashboard's `SnapshotListener` detects the new record and updates the UI timeline in real-time.

## 📡 External Integrations

*   **Google Firebase**: Real-time DB, Auth, and template storage.
*   **OpenStreetMap (OSMDroid)**: Tile rendering and map interactions without requiring a Google Maps API Key.
*   **TensorFlow Lite**: On-device machine learning for privacy and speed.
