# 📍 GeoAttend: AI-Powered Secure Workforce Management

[![Security: Verified](https://img.shields.io/badge/Security-Multi--Layer%20Verified-brightgreen)](https://github.com/yourusername/geoattend)
[![ML: On-Device](https://img.shields.io/badge/AI-On--Device%20TFLite-blue)](https://github.com/yourusername/geoattend)
[![Testing: 100% Pass](https://img.shields.io/badge/Testing-Automated%20Suite-orange)](README_TESTING.md)
[![Status: Production Ready](https://img.shields.io/badge/Status-Go--for--Launch-brightgreen)](https://github.com/yourusername/geoattend)

GeoAttend is a "Zero-Trust" attendance ecosystem designed for the modern hybrid workspace. It leverages **On-Device Machine Learning**, **Precision Geofencing**, and **System Integrity Gates** to eliminate attendance fraud while preserving user privacy.

---

## 🛡️ "Zero-Trust" Security Philosophy
Unlike standard apps, GeoAttend assumes the device environment is hostile until proven otherwise.
*   **Integrity Gate**: Mandatory scan for Root access and USB Debugging (ADB) on startup.
*   **Mock GPS Defense**: Multi-layer risk scoring to identify and block location spoofing.
*   **Biometric Liveness**: Random AI-driven challenges (Blink/Smile/Turn) to prevent deepfake/photo attacks.
*   **Hard-Binding**: High-entropy device fingerprinting to prevent account sharing.

### 📍 Precision Geofencing (OSM)
*   **Adaptive Boundaries**: dynamic circular and polygonal zones using OpenStreetMap (OSMDroid).
*   **Auto-Checkout**: Intelligent background services that trigger checkout if the device exits the secure zone.
*   **Signal Weighting**: A "Confidence Chip" UI that evaluates GPS accuracy before allowing Check-In.

### 🎭 Privacy-First AI
*   **TFLite Embeddings**: Face biometric data is converted into high-dimensional vectors on-device. No actual photos are stored in the cloud, ensuring GDPR/CCPA compliance.
*   **Local Processing**: Zero-latency verification even with poor internet connectivity.

---

## 📸 App Interface (Mockups)

| **Smart Dashboard** | **AI Biometric Scan** | **Admin Control Center** |
|:---:|:---:|:---:|
| <img src="docs/screenshots/dashboard.png" width="250" /> | <img src="docs/screenshots/face_scan.png" width="250" /> | <img src="docs/screenshots/admin_panel.png" width="250" /> |
| *Real-time proximity UI & session timer* | *On-device face verification* | *Global analytics & zone management* |

---

## 🗺️ User Journey & System Flow

```mermaid
sequenceDiagram
    participant U as Employee
    participant APP as GeoAttend App
    participant SEC as Security Manager
    participant AI as ML Engine
    participant FB as Firebase

    U->>APP: Opens App (Splash)
    APP->>SEC: Integrity Check (Root/Mock GPS)
    SEC-->>APP: Device Validated
    APP->>FB: Check Auth & Status
    
    Note over APP,AI: If New User
    U->>APP: Enter Details (Register)
    APP->>FB: Send OTP via SMTP
    U->>APP: Verify OTP
    U->>AI: Biometric Enrollment (Liveness Check)
    AI->>FB: Store Face Embedding

    Note over APP,SEC: Standard Check-In
    U->>APP: Approaches Office
    APP->>SEC: Geofence Pulse (OSM)
    alt Inside Geofence
        APP->>U: Enable Check-In (Confidence Chip)
        U->>AI: Face Verification
        AI->>FB: Log Attendance (Server Timestamp)
    else Outside Geofence
        APP->>U: Show Distance to Office
    end
```

---

## 🏗️ Technical Architecture

```mermaid
graph TD
    subgraph Client [Android Client - Java/TFLite]
        UI[View Layer: XML/ViewBinding]
        VM[Controller: Dashboards/Activities]
        SEC[Security: Mock Detect/Device Binding]
        AI[ML Engine: ML Kit & TFLite]
        MAP[Map Engine: OSMDroid & Geofence]
    end

    subgraph Backend [Firebase Cloud Backend]
        AUTH[Firebase Auth: Secure Token]
        STORE[Firestore: Real-time DB]
        FUNC[Server Logic: Time Validation]
    end

    UI <--> VM
    VM --> SEC
    VM --> AI
    VM --> MAP
    SEC --> AUTH
    VM <--> STORE
    STORE <--> FUNC
```

---

## 🛠️ Tech Stack

*   **Logic**: Java (Android SDK 34 target)
*   **Local AI**: Google ML Kit (Face Detection) + TensorFlow Lite (MobileFaceNet)
*   **Database**: Firebase Firestore (NoSQL)
*   **Maps**: OSMDroid (OpenStreetMap) for open-source map integration
*   **Threading**: RxJava/Concurrency for ML processing
*   **UI/UX**: Material 3 Design System with custom animations (Scale-bounce/Fade)

---

## 📋 Hiring Manager Fast-Track (The "Why Hire Me")
Building this project demonstrated mastery in:
1.  **System Design**: Orchestrating complex interactions between Location APIs, Camera hardware, and Cloud databases.
2.  **Security Engineering**: Thinking like an attacker to build defenses against GPS spoofing and biometric fraud.
3.  **Performance Optimization**: Running ML models and real-time map rendering at 60FPS on-device.
4.  **Product Mindset**: Designing a premium, intuitive UI that balances security with a seamless user experience.

---

## 🚀 Getting Started

1.  Clone this repository.
2.  Add your `google-services.json` to the `app/` folder.
3.  Create a `local.properties` file with your SMTP credentials for the OTP system:
    ```properties
    smtp.email=your-email@example.com
    smtp.password=your-app-password
    ```
4.  Build and run on a physical Android device (Recommended for GPS/Camera accuracy).

---
*Created with a focus on Security, Performance, and Precision.*
