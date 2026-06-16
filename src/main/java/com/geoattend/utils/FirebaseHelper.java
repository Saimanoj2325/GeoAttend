package com.geoattend.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseHelper {
    
    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseStorage getStorage() {
        return FirebaseStorage.getInstance();
    }

    public static DocumentReference getUserRef(String uid) {
        return getFirestore().collection("users").document(uid);
    }

    public static CollectionReference getGeofencesRef() {
        return getFirestore().collection("geofences");
    }

    public static CollectionReference getAttendanceRef() {
        return getFirestore().collection("attendance");
    }

    public static StorageReference getAttendancePhotosRef() {
        return getStorage().getReference().child("attendance_photos");
    }

    public static String getCurrentUserId() {
        return getAuth().getCurrentUser() != null ? getAuth().getCurrentUser().getUid() : "test_user";
    }
}
