package com.dreamweaver.ai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dreamweaver.ai.ai.RunAnywhereHelper
import com.dreamweaver.ai.ai.SpeechRecognizerHelper
import com.dreamweaver.ai.ai.SpeechResult
import com.dreamweaver.ai.data.StoryMessage
import com.dreamweaver.ai.firebase.FirestoreHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreHelper = FirestoreHelper()
    private val runAnywhereHelper = RunAnywhereHelper(application)
    private val speechRecognizerHelper = SpeechRecognizerHelper(application)

    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Onboarding)
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<StoryMessage>>(emptyList())
    val messages: StateFlow<List<StoryMessage>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isVoiceNarrationEnabled = MutableStateFlow(true)
    val isVoiceNarrationEnabled: StateFlow<Boolean> = _isVoiceNarrationEnabled.asStateFlow()

    private val _modelStatus = MutableStateFlow<String>("Checking for local AI model...")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int>(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var currentSessionId: String = ""
    private val contextMemory = mutableListOf<String>() // Store last 10 messages
    private val maxContextSize = 10
    private var isModelLoaded = false

    companion object {
        private const val TAG = "StoryViewModel"

        // Using smaller model for faster download and testing (119 MB instead of 1.2 GB)
        private const val QWEN_MODEL_ID =
            "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf"
    }

    init {
        initializeApp()
        // Try to load AI model in background
        viewModelScope.launch(Dispatchers.IO) {
            downloadAndLoadAIModel()
        }
    }

    private fun initializeApp() {
        viewModelScope.launch {
            try {
                // Initialize TTS in background (non-blocking)
                Log.d(TAG, "Starting TTS initialization...")
                val success = runAnywhereHelper.initializeTTS()
                if (success) {
                    Log.d(TAG, "TTS initialized successfully")
                } else {
                    Log.w(TAG, "TTS initialization failed - app will work without voice")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TTS (app will continue without voice)", e)
            }
        }
    }

    /**
     * Download and load the AI model
     */
    private suspend fun downloadAndLoadAIModel() {
        try {
            Log.i(TAG, "🤖 Starting AI model setup...")
            _modelStatus.value = "Initializing AI..."

            // Wait for SDK initialization
            delay(3000)

            // Step 1: Scan for downloaded models
            Log.i(TAG, "📡 Scanning for existing models...")
            _modelStatus.value = "Scanning for models..."

            try {
                val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
                val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
                val runAnywhereInstance = instanceField.get(null)

                val scanMethod = runAnywhereClass.getDeclaredMethod(
                    "scanForDownloadedModels",
                    kotlin.coroutines.Continuation::class.java
                )

                withContext(Dispatchers.IO) {
                    kotlin.coroutines.suspendCoroutine<Unit> { continuation ->
                        scanMethod.invoke(runAnywhereInstance, continuation)
                    }
                }
                Log.i(TAG, "✅ Scan complete")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Scan failed: ${e.message}", e)
            }

            // Step 2: Check if model exists, if not download it
            Log.i(TAG, "🔍 Checking if model needs to be downloaded...")
            _modelStatus.value = "Checking model availability..."

            val modelExists = checkIfModelExists()

            if (!modelExists) {
                Log.i(TAG, "📥 Model not found, starting download...")
                _modelStatus.value = "Downloading AI model..."
                _isDownloading.value = true

                val downloaded = downloadModel(QWEN_MODEL_ID)

                _isDownloading.value = false

                if (!downloaded) {
                    Log.w(TAG, "⚠️ Download failed, using fallback mode")
                    _modelStatus.value = "Using fallback mode"
                    return
                }

                Log.i(TAG, "✅ Model downloaded successfully")
            } else {
                Log.i(TAG, "✅ Model already exists")
            }

            // Step 3: Load the model
            Log.i(TAG, "📥 Loading AI model into memory...")
            _modelStatus.value = "Loading AI model..."

            val success = runAnywhereHelper.loadModel(QWEN_MODEL_ID)

            if (success) {
                Log.i(TAG, "🎉 AI model loaded successfully!")
                _modelStatus.value = "✅ Local AI Active"
                isModelLoaded = true
            } else {
                Log.w(TAG, "⚠️ Model failed to load, using fallback mode")
                _modelStatus.value = "Using fallback mode"
                isModelLoaded = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ AI model setup failed: ${e.message}", e)
            _modelStatus.value = "Using fallback mode"
            isModelLoaded = false
        }
    }

    /**
     * Check if model file exists locally
     */
    private suspend fun checkIfModelExists(): Boolean {
        return try {
            val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
            val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
            val runAnywhereInstance = instanceField.get(null)

            // Get available models method
            val extensionsClass =
                Class.forName("com.runanywhere.sdk.public.extensions.ModelRegistrationExtensionsKt")
            val listModelsMethod = extensionsClass.getDeclaredMethod(
                "listAvailableModels",
                kotlin.coroutines.Continuation::class.java
            )

            val models = withContext(Dispatchers.IO) {
                kotlin.coroutines.suspendCoroutine<List<*>> { continuation ->
                    listModelsMethod.invoke(null, continuation)
                }
            }

            // Check if any model is downloaded
            for (model in models) {
                if (model == null) continue

                try {
                    val isDownloadedField = model.javaClass.getDeclaredField("isDownloaded")
                    isDownloadedField.isAccessible = true
                    val isDownloaded = isDownloadedField.get(model) as Boolean

                    if (isDownloaded) {
                        Log.d(TAG, "Found downloaded model: $model")
                        return@checkIfModelExists true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not check model download status: ${e.message}")
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model existence: ${e.message}", e)
            false
        }
    }

    /**
     * Download a model with progress tracking
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun downloadModel(modelUrl: String): Boolean {
        return try {
            Log.i(TAG, "🌐 Starting model download...")
            Log.i(TAG, "   URL: $modelUrl")

            val runAnywhereClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
            val instanceField = runAnywhereClass.getDeclaredField("INSTANCE")
            val runAnywhereInstance = instanceField.get(null)

            val downloadMethod = runAnywhereClass.getDeclaredMethod(
                "downloadModel",
                String::class.java,
                kotlin.coroutines.Continuation::class.java
            )

            withContext(Dispatchers.IO) {
                kotlin.coroutines.suspendCoroutine { continuation ->
                    try {
                        // Invoke downloadModel with continuation callback
                        downloadMethod.invoke(
                            runAnywhereInstance,
                            modelUrl,
                            object : kotlin.coroutines.Continuation<Any> {
                                override val context = continuation.context

                                override fun resumeWith(result: Result<Any>) {
                                    result.fold(
                                        onSuccess = { flow ->
                                            // Flow returned successfully
                                            if (flow == null) {
                                                Log.e(TAG, "❌ Download returned null flow")
                                                continuation.resumeWith(Result.success(false))
                                                return@fold
                                            }

                                            viewModelScope.launch(Dispatchers.IO) {
                                                try {
                                                    // Collect the flow
                                                    val collectMethod =
                                                        flow.javaClass.getDeclaredMethod(
                                                            "collect",
                                                            Any::class.java,
                                                            kotlin.coroutines.Continuation::class.java
                                                        )

                                                    // Create a collector
                                                    val collectorClass =
                                                        Class.forName("kotlinx.coroutines.flow.FlowCollector")
                                                    val collector =
                                                        java.lang.reflect.Proxy.newProxyInstance(
                                                            collectorClass.classLoader,
                                                            arrayOf(collectorClass)
                                                        ) { _, method, args ->
                                                            if (method.name == "emit") {
                                                                val progress = args[0] as Float
                                                                val percentage =
                                                                    (progress * 100).toInt()
                                                                _downloadProgress.value = percentage
                                                                Log.d(
                                                                    TAG,
                                                                    "📊 Download progress: $percentage%"
                                                                )

                                                                // Return continuation result
                                                                (args[1] as kotlin.coroutines.Continuation<Unit>).resumeWith(
                                                                    Result.success(Unit)
                                                                )
                                                            }
                                                            null
                                                        }

                                                    kotlin.coroutines.suspendCoroutine<Unit> { flowCont ->
                                                        collectMethod.invoke(
                                                            flow,
                                                            collector,
                                                            flowCont
                                                        )
                                                    }

                                                    Log.i(TAG, "✅ Download completed!")
                                                    continuation.resumeWith(Result.success(true))
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        TAG,
                                                        "❌ Error collecting download flow: ${e.message}",
                                                        e
                                                    )
                                                    continuation.resumeWith(Result.success(false))
                                                }
                                            }
                                        },
                                        onFailure = { error ->
                                            Log.e(TAG, "❌ Download failed: ${error.message}", error)
                                            continuation.resumeWith(Result.success(false))
                                        }
                                    )
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Download invocation failed: ${e.message}", e)
                        continuation.resumeWith(Result.success(false))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download setup failed: ${e.message}", e)
            false
        }
    }

    /**
     * Start a new story session
     */
    fun startNewStory() {
        viewModelScope.launch {
            try {
                _uiState.value = StoryUiState.Loading
                Log.d(TAG, "Starting new story session...")

                // Generate session ID immediately
                try {
                    currentSessionId = firestoreHelper.createSession()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase unavailable, using local session", e)
                }

                // If Firebase fails, generate a local session ID
                if (currentSessionId.isEmpty()) {
                    currentSessionId = "local_${System.currentTimeMillis()}"
                    Log.d(TAG, "Using local session ID: $currentSessionId")
                }

                _messages.value = emptyList()
                contextMemory.clear()

                // IMMEDIATELY transition to Chat state - don't wait for anything
                _uiState.value = StoryUiState.Chat
                Log.d(TAG, "Story session started successfully - Chat UI active")

                // Add welcome message from AI (non-blocking)
                val welcomeMessage =
                    "Welcome to DreamWeaver! Start telling your story, and I'll continue it with you. What adventure shall we create today?"
                addAiMessage(welcomeMessage)

                // Speak in background (non-blocking)
                viewModelScope.launch {
                    try {
                        if (_isVoiceNarrationEnabled.value) {
                            runAnywhereHelper.speakText(welcomeMessage)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS not available", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting new story", e)
                // Even if there's an error, try to proceed with a basic session
                currentSessionId = "local_${System.currentTimeMillis()}"
                _messages.value = emptyList()
                contextMemory.clear()
                _uiState.value = StoryUiState.Chat

                val welcomeMessage = "Welcome to DreamWeaver! Let's create a story together."
                viewModelScope.launch {
                    addAiMessage(welcomeMessage)
                }
            }
        }
    }

    /**
     * Load last story session
     */
    fun loadLastStory() {
        viewModelScope.launch {
            try {
                _uiState.value = StoryUiState.Loading

                val lastSession = firestoreHelper.getLastSession()
                if (lastSession != null && lastSession.id.isNotEmpty()) {
                    currentSessionId = lastSession.id

                    // Load messages from Firestore
                    val messages = firestoreHelper.getSessionMessages(currentSessionId)
                    _messages.value = messages

                    // Rebuild context memory
                    contextMemory.clear()
                    messages.takeLast(maxContextSize).forEach { msg ->
                        contextMemory.add(msg.text)
                    }

                    _uiState.value = StoryUiState.Chat
                } else {
                    // No previous session, start new one
                    startNewStory()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading last story", e)
                startNewStory()
            }
        }
    }

    /**
     * Send text message
     */
    fun sendMessage(text: String) {
        println("=== DREAMWEAVER: sendMessage called with: $text ===")
        Log.e(TAG, "=== sendMessage called with text: $text ===")

        if (text.isBlank()) {
            Log.w(TAG, "Text is blank, returning")
            return
        }

        // Filter out conversational pauses and interruptions
        if (isConversationalPause(text)) {
            Log.d(TAG, "Detected conversational pause/interruption: $text")
            // Don't add to messages or generate response
            // Just acknowledge
            viewModelScope.launch {
                val acknowledgment = when {
                    text.lowercase()
                        .contains("wait") -> "I'm listening! Take your time. What would you like to add to the story?"

                    text.lowercase().contains("hold on") || text.lowercase()
                        .contains("hold") -> "Sure, I'll wait. Ready when you are!"

                    text.lowercase().contains("stop") -> "Okay, I've stopped. What's next?"
                    text.lowercase().contains("hmm") || text.lowercase()
                        .contains("umm") -> "Yes? I'm here! What are you thinking?"

                    else -> "I'm listening! Go ahead."
                }

                // Only speak if voice is enabled
                if (_isVoiceNarrationEnabled.value) {
                    runAnywhereHelper.speakText(acknowledgment)
                }
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding user message...")
                // Add user message
                addUserMessage(text)

                // Generate AI response
                Log.d(TAG, "Setting AI thinking to true...")
                _isAiThinking.value = true

                Log.d(TAG, "Calling generateStory...")
                val aiResponse = runAnywhereHelper.generateStoryUsingSDK(
                    userPrompt = text,
                    context = contextMemory.toList()
                )

                Log.d(TAG, "AI Response received: $aiResponse")
                // Add AI message
                addAiMessage(aiResponse)

                Log.d(TAG, "Speaking AI response...")
                // Speak AI response only if voice narration is enabled
                if (_isVoiceNarrationEnabled.value) {
                    runAnywhereHelper.speakText(aiResponse)
                } else {
                    Log.d(TAG, "Voice narration disabled, skipping speech")
                }

                _isAiThinking.value = false
                Log.d(TAG, "Message processing complete!")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                e.printStackTrace()
                _isAiThinking.value = false
                addAiMessage("I'm having trouble continuing the story. Could you try again?")
            }
        }
    }

    /**
     * Toggle voice narration on/off
     */
    fun toggleVoiceNarration() {
        _isVoiceNarrationEnabled.value = !_isVoiceNarrationEnabled.value
        Log.d(TAG, "Voice narration: ${_isVoiceNarrationEnabled.value}")

        if (!_isVoiceNarrationEnabled.value) {
            // Stop any current speech
            runAnywhereHelper.stopSpeaking()
        }
    }

    /**
     * Start voice input
     */
    fun startVoiceInput() {
        if (_isListening.value) return

        viewModelScope.launch {
            try {
                _isListening.value = true

                speechRecognizerHelper.startListening().collect { result ->
                    when (result) {
                        is SpeechResult.Success -> {
                            _isListening.value = false
                            sendMessage(result.text)
                        }

                        is SpeechResult.Error -> {
                            _isListening.value = false
                            Log.e(TAG, "Speech recognition error: ${result.message}")
                        }

                        is SpeechResult.EndOfSpeech -> {
                            // Speech ended, waiting for results
                        }

                        else -> {
                            // Handle other states if needed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during voice input", e)
                _isListening.value = false
            }
        }
    }

    /**
     * Stop voice input
     */
    fun stopVoiceInput() {
        speechRecognizerHelper.cancelListening()
        _isListening.value = false
    }

    /**
     * Stop AI speaking
     */
    fun stopSpeaking() {
        runAnywhereHelper.stopSpeaking()
    }

    /**
     * Add user message
     */
    private suspend fun addUserMessage(text: String) {
        Log.d(TAG, "addUserMessage: Starting with text: $text")
        val message = StoryMessage(
            sessionId = currentSessionId,
            text = text,
            isUser = true,
            timestamp = Timestamp.now()
        )

        Log.d(TAG, "addUserMessage: Message object created")
        // Add to local list
        _messages.value = _messages.value + message
        Log.d(
            TAG,
            "addUserMessage: Message added to local list. Total messages: ${_messages.value.size}"
        )

        // Add to context memory
        addToContext(text)
        Log.d(TAG, "addUserMessage: Added to context memory")

        // Try to save to Firestore (non-blocking - launch in background)
        viewModelScope.launch {
            try {
                firestoreHelper.saveMessage(message)
                Log.d(TAG, "addUserMessage: Saved to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save message to Firebase (continuing anyway)", e)
            }
        }
        Log.d(TAG, "addUserMessage: Complete")
    }

    /**
     * Add AI message
     */
    private suspend fun addAiMessage(text: String) {
        Log.d(TAG, "addAiMessage: Starting with text: $text")
        val message = StoryMessage(
            sessionId = currentSessionId,
            text = text,
            isUser = false,
            timestamp = Timestamp.now()
        )

        Log.d(TAG, "addAiMessage: Message object created")
        // Add to local list
        _messages.value = _messages.value + message
        Log.d(
            TAG,
            "addAiMessage: Message added to local list. Total messages: ${_messages.value.size}"
        )

        // Add to context memory
        addToContext(text)
        Log.d(TAG, "addAiMessage: Added to context memory")

        // Try to save to Firestore (non-blocking - launch in background)
        viewModelScope.launch {
            try {
                firestoreHelper.saveMessage(message)
                Log.d(TAG, "addAiMessage: Saved to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save message to Firebase (continuing anyway)", e)
            }
        }
        Log.d(TAG, "addAiMessage: Complete")
    }

    /**
     * Add text to context memory (keep last 10 messages)
     */
    private fun addToContext(text: String) {
        contextMemory.add(text)
        if (contextMemory.size > maxContextSize) {
            contextMemory.removeAt(0)
        }
    }

    /**
     * Check if the input is a conversational pause/interruption
     */
    private fun isConversationalPause(text: String): Boolean {
        val lowercaseText = text.lowercase().trim()

        // Patterns that indicate pauses or interruptions
        val pausePatterns = listOf(
            "wait",
            "hold on",
            "hold up",
            "stop",
            "pause",
            "hmm",
            "umm",
            "uh",
            "er",
            "let me think"
        )

        // Check if the entire message is just repetitions of pause words
        val words = lowercaseText.split(Regex("\\s+"))
        val uniqueWords = words.distinct()

        // If all words are the same pause word repeated (like "wait wait wait")
        if (uniqueWords.size == 1 && pausePatterns.any { uniqueWords[0].contains(it) }) {
            return true
        }

        // If it's very short and contains only pause words
        if (words.size <= 3 && words.all { word ->
                pausePatterns.any { pattern -> word.contains(pattern) }
            }) {
            return true
        }

        return false
    }

    override fun onCleared() {
        super.onCleared()
        runAnywhereHelper.shutdown()
        speechRecognizerHelper.stopListening()
    }
}

/**
 * UI State for the app
 */
sealed class StoryUiState {
    object Onboarding : StoryUiState()
    object Loading : StoryUiState()
    object Chat : StoryUiState()
    data class Error(val message: String) : StoryUiState()
}
