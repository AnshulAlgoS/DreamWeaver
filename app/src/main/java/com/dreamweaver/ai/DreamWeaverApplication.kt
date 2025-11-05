package com.dreamweaver.ai

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class DreamWeaverApplication : Application() {

    companion object {
        private const val TAG = "DreamWeaver"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "═══════════════════════════════════════════════════════")
        Log.d(TAG, "🚀 DreamWeaver Application Starting...")
        Log.d(TAG, "═══════════════════════════════════════════════════════")

        // Initialize RunAnywhere SDK asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeRunAnywhereSDK()
        }

        // Initialize Firebase in background
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "📱 Initializing Firebase...")
                FirebaseApp.initializeApp(this@DreamWeaverApplication)
                Log.d(TAG, "✅ Firebase initialized successfully")
                testFirestoreConnection()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Firebase initialization failed (app will work without it)", e)
            }
        }
    }

    private suspend fun initializeRunAnywhereSDK() {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            try {
                Log.i(TAG, "")
                Log.i(TAG, "🔧 Starting RunAnywhere SDK initialization...")
                Log.i(TAG, "───────────────────────────────────────────────────")

                // Step 1: Get RunAnywhere singleton instance
                Log.d(TAG, "📦 Step 1: Loading RunAnywhere class...")
                val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
                Log.d(TAG, "✅ RunAnywhere class loaded successfully")

                Log.d(TAG, "📦 Getting INSTANCE field...")
                val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
                val runAnywhereInstance = instanceField.get(null)
                Log.d(TAG, "✅ RunAnywhere instance obtained: $runAnywhereInstance")

                // Get SDKEnvironment.DEVELOPMENT
                Log.d(TAG, "📦 Loading SDKEnvironment class...")
                val sdkEnvironmentClass =
                    Class.forName("com.runanywhere.sdk.data.models.SDKEnvironment")
                val developmentEnv = sdkEnvironmentClass.getDeclaredField("DEVELOPMENT").get(null)
                Log.d(TAG, "✅ SDK Environment set to: $developmentEnv")

                // Call initialize: (Context, String apiKey, String baseURL, SDKEnvironment, Continuation)
                Log.d(TAG, "📦 Getting initialize method...")
                val initializeMethod = runAnywhereClass.getDeclaredMethod(
                    "initialize",
                    android.content.Context::class.java,
                    String::class.java,
                    String::class.java,
                    sdkEnvironmentClass,
                    kotlin.coroutines.Continuation::class.java
                )
                Log.d(TAG, "✅ Initialize method obtained")

                Log.i(TAG, "")
                Log.i(TAG, "🔄 Calling SDK initialize...")
                Log.i(TAG, "   API Key: dev")
                Log.i(TAG, "   Base URL: https://api.runanywhere.ai")
                Log.i(TAG, "   Environment: DEVELOPMENT")

                val initContinuation = object : kotlin.coroutines.Continuation<Unit> {
                    override val context: kotlin.coroutines.CoroutineContext = continuation.context

                    override fun resumeWith(result: Result<Unit>) {
                        result.fold(
                            onSuccess = {
                                Log.i(TAG, "")
                                Log.i(TAG, "✅ SDK initialized successfully!")
                                Log.i(TAG, "───────────────────────────────────────────────────")
                                GlobalScope.launch(Dispatchers.IO) {
                                    try {
                                        // Step 2: Register LlamaCpp Service Provider
                                        Log.i(TAG, "")
                                        Log.i(
                                            TAG,
                                            "🔧 Step 2: Registering LlamaCpp Service Provider..."
                                        )
                                        val llamaCppProviderClass =
                                            Class.forName("com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider")
                                        val registerMethod =
                                            llamaCppProviderClass.getDeclaredMethod("register")
                                        registerMethod.invoke(null)
                                        Log.i(
                                            TAG,
                                            "✅ LlamaCpp Service Provider registered successfully"
                                        )

                                        // Step 3: Register Models
                                        Log.i(TAG, "")
                                        Log.i(TAG, "🔧 Step 3: Registering models...")
                                        registerModels()

                                        // Step 4: Scan for downloaded models
                                        Log.i(TAG, "")
                                        Log.i(TAG, "🔧 Step 4: Scanning for downloaded models...")
                                        val scanMethod = runAnywhereClass.getDeclaredMethod(
                                            "scanForDownloadedModels",
                                            kotlin.coroutines.Continuation::class.java
                                        )
                                        kotlin.coroutines.suspendCoroutine<Unit> { scanCont ->
                                            scanMethod.invoke(runAnywhereInstance, scanCont)
                                        }
                                        Log.i(TAG, "✅ Model scan completed")

                                        Log.i(TAG, "")
                                        Log.i(TAG, "🎉 SDK initialization complete!")
                                        Log.i(
                                            TAG,
                                            "═══════════════════════════════════════════════════════"
                                        )
                                        continuation.resume(Unit)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed post-init steps: ${e.message}", e)
                                        e.printStackTrace()
                                        continuation.resume(Unit) // Continue anyway
                                    }
                                }
                            },
                            onFailure = { error ->
                                Log.e(TAG, "")
                                Log.e(TAG, "❌ SDK initialization failed!")
                                Log.e(TAG, "   Error: ${error.message}")
                                Log.e(
                                    TAG,
                                    "───────────────────────────────────────────────────",
                                    error
                                )
                                error.printStackTrace()
                                continuation.resume(Unit) // Continue anyway
                            }
                        )
                    }
                }

                initializeMethod.invoke(
                    runAnywhereInstance,
                    this@DreamWeaverApplication,
                    "dev",
                    "https://api.runanywhere.ai",
                    developmentEnv,
                    initContinuation
                )

            } catch (e: Exception) {
                Log.e(TAG, "")
                Log.e(TAG, "❌ SDK initialization setup failed!")
                Log.e(TAG, "   Error: ${e.message}")
                Log.e(TAG, "───────────────────────────────────────────────────", e)
                e.printStackTrace()
                continuation.resume(Unit) // Continue anyway
            }
        }
    }

    private suspend fun registerModels() {
        try {
            Log.i(TAG, "📋 Registering Qwen 2.5 1.5B model...")
            Log.d(
                TAG,
                "   URL: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q6_k.gguf"
            )

            // Get the addModelFromURL extension function
            val extensionsClass =
                Class.forName("com.runanywhere.sdk.public.extensions.ModelRegistrationExtensionsKt")
            val addModelMethod = extensionsClass.getDeclaredMethod(
                "addModelFromURL",
                String::class.java,  // url
                String::class.java,  // name
                String::class.java,  // type
                kotlin.coroutines.Continuation::class.java
            )

            // Register Qwen 2.5 1.5B (already downloaded on device)
            kotlin.coroutines.suspendCoroutine<Unit> { continuation ->
                addModelMethod.invoke(
                    null,
                    "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q6_k.gguf",
                    "Qwen 2.5 1.5B Instruct Q6_K",
                    "LLM",
                    continuation
                )
            }
            Log.i(TAG, "✅ Qwen 2.5 1.5B model registered")

            // Register smaller model as backup
            Log.i(TAG, "📋 Registering SmolLM2 360M model...")
            Log.d(
                TAG,
                "   URL: https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf"
            )
            kotlin.coroutines.suspendCoroutine<Unit> { continuation ->
                addModelMethod.invoke(
                    null,
                    "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
                    "SmolLM2 360M Q8_0",
                    "LLM",
                    continuation
                )
            }
            Log.i(TAG, "✅ SmolLM2 360M model registered")
            Log.i(TAG, "✅ All models registered successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register models: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun testFirestoreConnection() {
        try {
            Log.d(TAG, "🔍 Testing Firestore connection...")
            val db = FirebaseFirestore.getInstance()
            val testData = hashMapOf(
                "test" to "DreamWeaver initialized",
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            db.collection("app_status")
                .add(testData)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Firestore connection successful!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "⚠️ Firestore connection failed (app works without it)", e)
                }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Firestore test failed", e)
        }
    }
}
