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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private var currentSessionId: String = ""
    private val contextMemory = mutableListOf<String>() // Store last 10 messages
    private val maxContextSize = 10

    companion object {
        private const val TAG = "StoryViewModel"
    }

    init {
        initializeApp()
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
                val aiResponse = runAnywhereHelper.generateStory(
                    context = contextMemory.toList(),
                    userInput = text
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
