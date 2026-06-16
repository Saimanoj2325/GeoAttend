# GeoAttend Design Document

## 🎨 Design Philosophy
GeoAttend is designed with a **"Security-First, User-Second"** philosophy. Every UI element and interaction is built to satisfy security requirements while maintaining a professional and accessible enterprise experience.

## 📱 User Interface (UI)

### Visual Style
*   **Color Palette**:
    *   `Primary`: Professional Navy/Blue for stability.
    *   `Accent`: Gold/Amber for biometric status and warnings.
    *   `Success/Error`: Emerald Green and Rose Red for instant feedback.
*   **Material Design**: Uses Material 3 components (Buttons, Cards, Chips) for a modern, native feel.

### Dashboard UX
*   **The Map Centrality**: The dashboard is map-centric to give users immediate visual confirmation of their "Geofence Status".
*   **The Timeline**: A vertical timeline provides a sense of progression through the workday, reducing cognitive load.
*   **Actionable States**: The "Check In" button state is reactive. It changes color and description based on proximity and security status (e.g., turns red if "Mock Location" is detected).

## 🔒 Security Design

### Biometric "Golden Template"
*   During enrollment, the app generates a high-dimensional vector (embedding).
*   This template is stored in a secured Firestore document.
*   **Privacy**: No actual photos of the user are stored for verification—only the mathematical representation, which cannot be easily reversed into a face image.

### Anti-Spoofing (Liveness)
*   The verification flow includes a "Challenge-Response" step.
*   The system randomly requests a **Blink** or a **Head Turn**.
*   This ensures that the person at the camera is a live human and not a static photo or a video recording held up to the lens.

### Geofence Perimeter
*   Uses a Geodesic distance algorithm for accuracy.
*   Supports multiple overlapping or adjacent geofences for large corporate campuses.

## 🛠️ Performance Optimization
*   **Background Processes**: Geofencing is handled via a `BroadcastReceiver` to minimize battery drain when the app is in the background.
*   **On-Device AI**: All face comparison happens locally on the device's NPU/GPU using TensorFlow Lite. This ensures verification works even with spotty internet and keeps biometric data off the public internet during the matching phase.
