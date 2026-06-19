# 📍 GeoAttend: AI-Powered Secure Workforce Management

[![Security: Verified](https://img.shields.io/badge/Security-Multi--Layer%20Verified-brightgreen)](https://github.com/Saimanoj2325/GeoAttend)
[![ML: On-Device](https://img.shields.io/badge/AI-On--Device%20TFLite-blue)](https://github.com/Saimanoj2325/GeoAttend)
[![Status: Go-for-Launch](https://img.shields.io/badge/Status-Production%20Ready-brightgreen)](https://github.com/Saimanoj2325/GeoAttend)

GeoAttend is a "Zero-Trust" attendance ecosystem designed for the modern hybrid workspace. It leverages **On-Device Machine Learning**, **Precision Geofencing**, and **System Integrity Gates** to eliminate attendance fraud while preserving user privacy.

---

## 📽️ Product Showcase (Demo)

<div align="center">
  <img src="1000299198.gif" width="300" alt="GeoAttend Demo Walkthrough" />
  <p><i>Full workflow: Integrity Scan → Secure Login → Biometric Verification → Attendance Logged</i></p>
</div>

---

## 🛡️ "Zero-Trust" Security Philosophy
Unlike standard apps, GeoAttend assumes the device environment is hostile until proven otherwise.
*   **Integrity Gate**: Mandatory scan for Root access and USB Debugging (ADB) on startup.
*   **Mock GPS Defense**: Multi-layer risk scoring to identify and block location spoofing.
*   **Biometric Liveness**: Random AI-driven liveness challenges (Blink/Smile/Turn) to prevent deepfake/photo attacks.
*   **Hard-Binding**: High-entropy device fingerprinting to ensure account integrity.

### 📍 Precision Geofencing (OSM)
*   **Adaptive Boundaries**: Dynamic circular and polygonal zones using OpenStreetMap (OSMDroid).
*   **Auto-Checkout**: Intelligent background services that trigger checkout if the device exits the secure zone.
*   **Signal Weighting**: A "Confidence Chip" UI that evaluates GPS accuracy before allowing Check-In.

### 🎭 Privacy-First AI
*   **TFLite Embeddings**: Face biometric data is converted into high-dimensional vectors on-device. No actual photos are stored in the cloud.
*   **Local Processing**: Zero-latency biometric verification even with poor internet connectivity.

---

## 🗺️ System Workflow

```mermaid
graph TD
    Start((App Open)) --> Scan[System Integrity Scan]
    Scan --> Integrity{Environment Safe?}
    
    Integrity -- No --> Block[Show Security Violation]
    Integrity -- Yes --> Auth[Check Auth Status]
    
    Auth --> NewUser{New User?}
    
    NewUser -- Yes --> Reg[Register & OTP Verify]
    Reg --> Enroll[AI Biometric Enrollment]
    Enroll --> Login
    
    NewUser -- No --> Login[Access Dashboard]
    
    Login --> Geo[Geofence Pulse]
    Geo --> Zone{Inside Zone?}
    
    Zone -- No --> Dist[Show Distance to Office]
    Zone -- Yes --> Face[AI Face Verification]
    
    Face --> Success[Mark Attendance & Sync]
    Success --> End((Done))

    style Integrity fill:#f96,stroke:#333
    style Zone fill:#f96,stroke:#333
    style NewUser fill:#f96,stroke:#333
    style Success fill:#1EC98E,stroke:#333
    style Block fill:#EF4444,stroke:#333
```

---

## 🏗️ Technical Architecture

```mermaid
graph TD
    subgraph Client ["📱 Android Client (Java)"]
        direction TB
        UI["🎨 View Layer<br/>(XML / ViewBinding)"]
        VM["🎮 Controller Layer<br/>(Activities / Logic)"]
        
        subgraph Core ["🛠️ Core Engines"]
            direction LR
            AI["🧠 ML Engine<br/>(ML Kit & TFLite)"]
            SEC["🛡️ Security Manager<br/>(Integrity & Mock Detect)"]
            MAP["📍 Map Engine<br/>(OSM & Geofencing)"]
        end
        
        UI <--> VM
        VM --> AI
        VM --> SEC
        VM --> MAP
    end

    subgraph Backend ["🔥 Firebase Backend"]
        direction TB
        AUTH["🔑 Firebase Auth<br/>(Identity Management)"]
        DB["🗄️ Firestore DB<br/>(Real-time Data)"]
        Audit["📋 Audit Logic<br/>(Server-side Timestamps)"]
    end

    SEC -.-> AUTH
    VM <--> DB
    DB --- Audit
```

---

## 🛠️ Tech Stack

*   **Logic**: Java (Android SDK 34 target)
*   **Local AI**: Google ML Kit (Face Detection) + TensorFlow Lite (MobileFaceNet)
*   **Database**: Firebase Firestore (NoSQL)
*   **Maps**: OSMDroid (OpenStreetMap)
*   **Threading**: RxJava/Concurrency for ML processing
*   **UI/UX**: Material 3 Design System with custom animations

---

## 📝 Technical Reviewer Notes
During the development, several architectural decisions were made to prioritize security:
*   **State Machine Verification**: The Face Verification process uses a strict state machine to prevent race conditions during frame analysis.
*   **Environment Guarding**: A mandatory integrity scan prevents usage on compromised (Rooted/ADB-enabled) devices.
*   **On-Device Priority**: Biometric matching is performed locally to ensure data privacy and zero-latency performance.

---

## 🚀 Getting Started

1.  Clone this repository.
2.  Add your `google-services.json` to the `app/` folder.
3.  Build and run on a **physical Android device** (GPS and Camera accuracy is required).

---
*Created with a focus on Security, Performance, and Precision.*
