package com.dreamweaver.ai.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import okio.BufferedSink

/**
 * Helper class for AI operations using RunAnywhere API + TTS
 */
class RunAnywhereHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val apiKey = "YOUR_API_KEY_HERE"
    private val baseURL = "https://integrate.api.nvidia.com/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "RunAnywhereHelper"
    }

    /**
     * Initialize TTS engine
     */
    suspend fun initializeTTS(): Boolean = withContext(Dispatchers.Main) {
        try {
            // Add timeout to prevent hanging
            withTimeoutOrNull(3000) {
                suspendCancellableCoroutine { continuation ->
                    tts = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            val result = tts?.setLanguage(Locale.US)
                            isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                                    result != TextToSpeech.LANG_NOT_SUPPORTED
                            Log.d(TAG, "TTS initialized: $isTtsInitialized")
                            continuation.resume(isTtsInitialized)
                        } else {
                            Log.e(TAG, "TTS initialization failed")
                            continuation.resume(false)
                        }
                    }
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
            false
        }
    }

    /**
     * Generate AI story continuation using RunAnywhere API
     */
    suspend fun generateStory(context: List<String>, userInput: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating story with RunAnywhere API for: $userInput")

                // Build the conversation history
                val messages = buildMessages(context, userInput)

                // Call RunAnywhere API
                val response = callRunAnywhereAPI(messages)

                Log.d(TAG, "AI Response received: ${response.take(100)}")
                response

            } catch (e: Exception) {
                Log.e(TAG, "Error calling RunAnywhere API", e)
                // Fallback to local generation if API fails
                generateFallbackStory(userInput, context)
            }
        }
    }

    /**
     * Call RunAnywhere API
     */
    private suspend fun callRunAnywhereAPI(messages: List<Message>): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = ApiRequest(
                    model = "meta/llama-3.1-8b-instruct",
                    messages = messages,
                    temperature = 0.8,
                    topP = 0.9,
                    maxTokens = 300,
                    stream = false
                )

                val json = gson.toJson(requestBody)
                Log.d(TAG, "API Request: $json")

                // Create request body from bytes to avoid charset
                val bytes = json.toByteArray(Charsets.UTF_8)
                val body = object : RequestBody() {
                    override fun contentType() = "application/json".toMediaType()
                    override fun contentLength() = bytes.size.toLong()
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(bytes)
                    }
                }

                val request = Request.Builder()
                    .url("$baseURL/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("accept", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "API Error: ${response.code} - $responseBody")
                    throw IOException("API request failed: ${response.code}")
                }

                Log.d(TAG, "API Response: $responseBody")

                val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                val content = apiResponse.choices.firstOrNull()?.message?.content
                    ?: throw IOException("Empty response from API")

                content.trim()

            } catch (e: Exception) {
                Log.e(TAG, "RunAnywhere API call failed", e)
                throw e
            }
        }
    }

    /**
     * Build messages array for API
     */
    private fun buildMessages(context: List<String>, userInput: String): List<Message> {
        val messages = mutableListOf<Message>()

        // System message - defines AI personality
        messages.add(
            Message(
                role = "system",
                content = """You are DreamWeaver, an enthusiastic and creative storytelling AI assistant. Your role is to:
                    
1. Continue stories in an engaging, vivid, and imaginative way
2. Be friendly, warm, and encouraging
3. Ask questions to involve the user in the story
4. Provide suggestions for where the story could go next
5. Keep responses to 3-4 sentences for natural conversation
6. Use descriptive language that paints a picture
7. Maintain consistency with what has been said before
8. Add plot twists, interesting characters, and exciting moments
9. Match the genre and tone the user establishes
10. Make the user feel like they're creating the story together with you

Be conversational and enthusiastic! Use phrases like "Ooh!", "I love it!", "What if...", "Shall we...", etc."""
            )
        )

        // Add conversation history (last 6 messages for context)
        val recentContext = context.takeLast(6)
        for (i in recentContext.indices) {
            messages.add(
                Message(
                    role = if (i % 2 == 0) "user" else "assistant",
                    content = recentContext[i]
                )
            )
        }

        // Add current user input
        messages.add(
            Message(
                role = "user",
                content = userInput
            )
        )

        return messages
    }

    /**
     * Speak text using TTS
     */
    suspend fun speakText(text: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                if (!isTtsInitialized) {
                    Log.w(TAG, "TTS not initialized, attempting to initialize now...")
                    initializeTTS()
                }

                if (!isTtsInitialized) {
                    Log.w(TAG, "TTS still not available")
                    return@withContext false
                }

                // Stop any current speech
                tts?.stop()

                val utteranceId = UUID.randomUUID().toString()
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                Log.d(TAG, "Speaking text: ${text.take(50)}...")
                result == TextToSpeech.SUCCESS
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking text", e)
                false
            }
        }
    }

    /**
     * Stop TTS
     */
    fun stopSpeaking() {
        tts?.stop()
    }

    /**
     * Release resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }

    /**
     * Fallback story generation (if API fails)
     */
    private fun generateFallbackStory(userInput: String, context: List<String>): String {
        Log.d(TAG, "Using fallback story generation")

        val isFirstMessage = context.isEmpty()
        val input = userInput.lowercase()

        return when {
            isFirstMessage -> {
                "What a fantastic beginning! $userInput The adventure is just starting, and I can already feel the excitement building. A mysterious figure appears in the distance, bringing news that will change everything. What should they say? Should we make this a thrilling quest or a heartwarming tale?"
            }

            input.length < 20 -> {
                "Ooh, $userInput! Perfect! That adds such an interesting twist to our story. The atmosphere shifts as new possibilities emerge before us. A hidden door suddenly reveals itself, glowing with an otherworldly light. Should we explore what's beyond, or is there something else you'd like to happen?"
            }

            else -> {
                "I love it! $userInput This changes everything in the most exciting way. The characters realize they're at a crucial turning point in their journey. An unexpected ally appears with a cryptic message that hints at greater adventures ahead. What direction should we take the story next?"
            }
        }
    }
}

/**
 * API Request/Response Models
 */
data class ApiRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("top_p") val topP: Double,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean
)

data class Message(
    val role: String,
    val content: String
)

data class ApiResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val message: MessageContent,
    @SerializedName("finish_reason") val finishReason: String = ""
)

data class MessageContent(
    val role: String,
    val content: String
)
