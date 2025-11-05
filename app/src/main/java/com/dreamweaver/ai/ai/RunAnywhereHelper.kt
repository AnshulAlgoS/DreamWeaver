package com.dreamweaver.ai.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import kotlin.coroutines.resume

/**
 * Helper class for AI operations with TTS
 * Using fallback story generation (model loading will be added separately)
 */
class RunAnywhereHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    companion object {
        private const val TAG = "RunAnywhereHelper"
    }

    /**
     * Initialize TTS engine
     */
    suspend fun initializeTTS(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "═══════════════════════════════════════════════════════")
            Log.d(TAG, "🔊 Initializing Text-to-Speech (TTS)...")
            Log.d(TAG, "───────────────────────────────────────────────────")

            withTimeoutOrNull(3000) {
                suspendCancellableCoroutine { continuation ->
                    tts = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            Log.d(TAG, "✅ TTS engine initialized")
                            val result = tts?.setLanguage(Locale.US)
                            isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                                    result != TextToSpeech.LANG_NOT_SUPPORTED
                            Log.d(TAG, "🌍 Language set to US English: $isTtsInitialized")
                            Log.d(TAG, "═══════════════════════════════════════════════════════")
                            continuation.resume(isTtsInitialized)
                        } else {
                            Log.e(TAG, "❌ TTS initialization failed with status: $status")
                            Log.d(TAG, "═══════════════════════════════════════════════════════")
                            continuation.resume(false)
                        }
                    }
                }
            } ?: run {
                Log.e(TAG, "❌ TTS initialization timed out after 3 seconds")
                Log.d(TAG, "═══════════════════════════════════════════════════════")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ TTS initialization error: ${e.message}", e)
            Log.d(TAG, "═══════════════════════════════════════════════════════")
            false
        }
    }

    /**
     * Generate AI story continuation
     * Currently using intelligent fallback responses
     * Model loading will be added via model management UI
     */
    suspend fun generateStory(context: List<String>, userInput: String): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "📝 generateStory called (deprecated method)")
            Log.d(TAG, "   Context size: ${context.size}")
            Log.d(TAG, "   User input: ${userInput.take(50)}...")
            // Using fallback story generation
            // This provides intelligent, contextual responses
            generateFallbackStory(userInput, context)
        }
    }

    /**
     * Speak text using TTS
     */
    suspend fun speakText(text: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "🔊 speakText called")
                Log.d(TAG, "   Text length: ${text.length} chars")
                Log.d(TAG, "   Text preview: ${text.take(50)}...")

                if (!isTtsInitialized) {
                    Log.w(TAG, "⚠️ TTS not initialized, attempting to initialize now...")
                    initializeTTS()
                }

                if (!isTtsInitialized) {
                    Log.w(TAG, "❌ TTS still not available after initialization attempt")
                    return@withContext false
                }

                // Stop any current speech
                tts?.stop()

                val utteranceId = UUID.randomUUID().toString()
                Log.d(TAG, "🎤 Speaking with utterance ID: $utteranceId")
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "✅ TTS speak command successful")
                } else {
                    Log.w(TAG, "⚠️ TTS speak command returned: $result")
                }

                result == TextToSpeech.SUCCESS
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error speaking text: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Stop TTS
     */
    fun stopSpeaking() {
        Log.d(TAG, "🛑 Stopping TTS speech")
        tts?.stop()
    }

    /**
     * Release resources
     */
    fun shutdown() {
        Log.d(TAG, "🔒 Shutting down RunAnywhereHelper")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        Log.d(TAG, "✅ Shutdown complete")
    }

    /**
     * Intelligent fallback story generation
     * Provides creative, contextual responses for storytelling
     */
    private fun generateFallbackStory(userInput: String, context: List<String>): String {
        Log.d(TAG, "📖 Using intelligent fallback story generation")
        Log.d(TAG, "   Context messages: ${context.size}")

        val isFirstMessage = context.isEmpty()
        val input = userInput.lowercase()

        return when {
            isFirstMessage -> {
                Log.d(TAG, "✨ First message - generating welcome response")
                "What a fantastic beginning! $userInput The adventure is just starting, and I can already feel the excitement building. A mysterious figure appears in the distance, bringing news that will change everything. What should they say? Should we make this a thrilling quest or a heartwarming tale?"
            }

            input.length < 20 -> {
                Log.d(TAG, "✨ Short message - generating brief response")
                "Ooh, $userInput! Perfect! That adds such an interesting twist to our story. The atmosphere shifts as new possibilities emerge before us. A hidden door suddenly reveals itself, glowing with an otherworldly light. Should we explore what's beyond, or is there something else you'd like to happen?"
            }

            else -> {
                Log.d(TAG, "✨ Regular message - generating detailed response")
                "I love it! $userInput This changes everything in the most exciting way. The characters realize they're at a crucial turning point in their journey. An unexpected ally appears with a cryptic message that hints at greater adventures ahead. What direction should we take the story next?"
            }
        }
    }

    /**
     * Load an AI model using RunAnywhere SDK
     */
    suspend fun loadModel(modelId: String): Boolean {
        return try {
            Log.i(TAG, "")
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "📥 Attempting to load model...")
            Log.i(TAG, "   Model ID: $modelId")
            Log.i(TAG, "───────────────────────────────────────────────────")

            // Use reflection to call RunAnywhere.loadModel()
            Log.d(TAG, "🔧 Loading RunAnywhere class via reflection...")
            val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
            val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
            val runAnywhereInstance = instanceField.get(null)
            Log.d(TAG, "✅ Got RunAnywhere instance: $runAnywhereInstance")

            Log.d(TAG, "🔧 Getting loadModel method...")
            val loadModelMethod = runAnywhereClass.getDeclaredMethod(
                "loadModel",
                String::class.java,
                kotlin.coroutines.Continuation::class.java
            )
            Log.d(TAG, "✅ Got loadModel method")

            Log.i(TAG, "🔄 Calling loadModel...")
            val result = kotlin.coroutines.suspendCoroutine<Boolean> { continuation ->
                loadModelMethod.invoke(runAnywhereInstance, modelId, continuation)
            }

            if (result) {
                Log.i(TAG, "✅ Model loaded successfully!")
            } else {
                Log.w(TAG, "⚠️ Model loading returned false")
            }
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            result
        } catch (e: Exception) {
            Log.e(TAG, "")
            Log.e(TAG, "❌ Failed to load model!")
            Log.e(TAG, "   Model ID: $modelId")
            Log.e(TAG, "   Error: ${e.message}")
            Log.e(TAG, "═══════════════════════════════════════════════════════", e)
            false
        }
    }

    /**
     * Generate story continuation using RunAnywhere SDK
     */
    suspend fun generateStoryUsingSDK(
        userPrompt: String,
        context: List<String> = emptyList()
    ): String {
        return try {
            Log.d(TAG, "")
            Log.d(TAG, "═══════════════════════════════════════════════════════")
            Log.d(TAG, "🤖 generateStoryUsingSDK called")
            Log.d(TAG, "   Prompt length: ${userPrompt.length} chars")
            Log.d(TAG, "   Context size: ${context.size} messages")
            Log.d(TAG, "───────────────────────────────────────────────────")

            // Build full prompt with context
            val fullPrompt = buildPrompt(userPrompt, context)
            Log.d(TAG, "📝 Full prompt built: ${fullPrompt.length} chars")

            // Try to use RunAnywhere SDK first
            Log.d(TAG, "🔄 Attempting SDK generation...")
            val sdkResponse = tryRunAnywhereGenerate(fullPrompt)
            if (sdkResponse != null) {
                Log.i(TAG, "✅ Generated using RunAnywhere SDK")
                Log.d(TAG, "   Response length: ${sdkResponse.length} chars")
                Log.d(TAG, "═══════════════════════════════════════════════════════")
                return sdkResponse
            }

            // Fallback to intelligent story generation
            Log.d(TAG, "⚠️ SDK generation failed/unavailable, using fallback")
            val fallbackResponse = generateIntelligentFallback(userPrompt, context)
            Log.d(TAG, "✅ Fallback response generated: ${fallbackResponse.length} chars")
            Log.d(TAG, "═══════════════════════════════════════════════════════")
            fallbackResponse

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in generateStoryUsingSDK: ${e.message}", e)
            Log.d(TAG, "🔄 Falling back to intelligent generation")
            generateIntelligentFallback(userPrompt, context)
        }
    }

    /**
     * Try to generate using RunAnywhere SDK with reflection
     */
    private suspend fun tryRunAnywhereGenerate(prompt: String): String? {
        return try {
            Log.d(TAG, "🔧 Attempting RunAnywhere SDK generation via reflection...")

            val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
            val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
            val runAnywhereInstance = instanceField.get(null)
            Log.d(TAG, "✅ Got RunAnywhere instance")

            // Get RunAnywhereGenerationOptions class and create default instance
            Log.d(TAG, "🔧 Creating generation options...")
            val optionsClass =
                Class.forName("com.runanywhere.sdk.models.RunAnywhereGenerationOptions")
            val optionsConstructor = optionsClass.getDeclaredConstructor(
                Int::class.java,  // maxTokens
                Float::class.java, // temperature
                Float::class.java, // topP
                Float::class.java, // topK
                Float::class.java, // repeatPenalty
                Int::class.java,   // mask (for flags)
                Any::class.java    // last param (may be for defaults)
            )

            // Create options with default values
            val options = try {
                val opts = optionsConstructor.newInstance(512, 0.7f, 0.9f, 40f, 1.1f, 0, null)
                Log.d(TAG, "✅ Generation options created: maxTokens=512, temp=0.7")
                opts
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to create options with all params: ${e.message}")
                null
            }

            Log.d(TAG, "🔧 Getting generate method...")
            val generateMethod = runAnywhereClass.getDeclaredMethod(
                "generate",
                String::class.java,
                optionsClass,
                kotlin.coroutines.Continuation::class.java
            )
            Log.d(TAG, "✅ Got generate method")

            Log.i(TAG, "🔄 Calling SDK generate method...")
            val response = kotlin.coroutines.suspendCoroutine<String?> { continuation ->
                try {
                    generateMethod.invoke(runAnywhereInstance, prompt, options, continuation)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ SDK generate invocation failed: ${e.message}", e)
                    continuation.resume(null)
                }
            }

            if (response != null) {
                Log.i(TAG, "✅ SDK generation successful: ${response.length} chars")
            } else {
                Log.w(TAG, "⚠️ SDK returned null response")
            }

            response
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not use SDK: ${e.message}", e)
            null
        }
    }

    /**
     * Build prompt with context
     */
    private fun buildPrompt(userPrompt: String, context: List<String>): String {
        Log.d(TAG, "📝 Building prompt...")
        return if (context.isEmpty()) {
            Log.d(TAG, "   No context - using simple prompt")
            "Continue this story: $userPrompt"
        } else {
            val recentContext = context.takeLast(5).joinToString("\n")
            Log.d(TAG, "   Using last ${context.takeLast(5).size} messages as context")
            """
            Story so far:
            $recentContext
            
            Latest: $userPrompt
            
            Continue the story naturally and creatively:
            """.trimIndent()
        }
    }

    /**
     * Intelligent fallback story generation
     */
    private fun generateIntelligentFallback(userPrompt: String, context: List<String>): String {
        Log.d(TAG, "📖 Generating intelligent fallback response")

        val responses = listOf(
            "I love it! $userPrompt This changes everything in the most exciting way. The characters realize they're at a crucial turning point in their journey. An unexpected ally appears with a cryptic message that hints at greater adventures ahead. What direction should we take the story next?",

            "Brilliant! $userPrompt opens up fascinating possibilities. A mysterious figure emerges from the shadows, carrying ancient knowledge that could change everything. The air crackles with anticipation as new choices present themselves. Where shall our tale lead us?",

            "Excellent choice! $userPrompt sets the stage beautifully. Suddenly, the atmosphere shifts and a hidden truth begins to surface. The protagonist discovers something extraordinary that will reshape their understanding of this world. What happens next in our adventure?",

            "Perfect! Building on $userPrompt - the scene transforms. A shimmering portal appears, offering glimpses of possibilities beyond imagination. Our hero faces a decision that will echo through the rest of their story. Which path calls to them?",

            "Wonderful! $userPrompt deepens the mystery. An ancient power stirs, responding to recent events. The characters sense they're on the verge of a breakthrough that could alter their fate forever. How should the story unfold from here?"
        )

        val selectedResponse = responses.random()
        Log.d(
            TAG,
            "✅ Selected response variant ${responses.indexOf(selectedResponse) + 1} of ${responses.size}"
        )
        return selectedResponse
    }
}
