package com.dreamweaver.ai

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DreamWeaverApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d("DreamWeaver", "Application starting...")

        // Initialize Firebase in background (non-blocking)
        applicationScope.launch {
            try {
                FirebaseApp.initializeApp(this@DreamWeaverApplication)
                Log.d("DreamWeaver", "Firebase initialized")

                // Test Firestore connection (non-blocking)
                testFirestoreConnection()
            } catch (e: Exception) {
                Log.w("DreamWeaver", "Firebase initialization failed (app will work without it)", e)
            }
        }

        Log.d("DreamWeaver", "Application initialized - ready to use")
    }

    private fun testFirestoreConnection() {
        try {
            val db = FirebaseFirestore.getInstance()
            val testData = hashMapOf(
                "test" to "DreamWeaver initialized",
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            db.collection("app_status")
                .add(testData)
                .addOnSuccessListener {
                    Log.d("DreamWeaver", "Firestore connection successful!")
                }
                .addOnFailureListener { e ->
                    Log.w("DreamWeaver", "Firestore connection failed (app works without it)", e)
                }
        } catch (e: Exception) {
            Log.w("DreamWeaver", "Firestore test failed", e)
        }
    }
}
