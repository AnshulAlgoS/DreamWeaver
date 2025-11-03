package com.dreamweaver.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamweaver.ai.ui.ChatScreen
import com.dreamweaver.ai.ui.OnboardingScreen
import com.dreamweaver.ai.ui.theme.DreamWeaverTheme
import com.dreamweaver.ai.viewmodel.StoryUiState
import com.dreamweaver.ai.viewmodel.StoryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StoryViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(
                this,
                R.string.permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DreamWeaverTheme {
                DreamWeaverApp(
                    viewModel = viewModel,
                    onRequestPermission = { checkAndRequestPermission() }
                )
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startVoiceInput()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    this,
                    R.string.permission_required,
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@Composable
fun DreamWeaverApp(
    viewModel: StoryViewModel,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val isVoiceNarrationEnabled by viewModel.isVoiceNarrationEnabled.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0F0F1E)
    ) {
        when (uiState) {
            is StoryUiState.Onboarding -> {
                OnboardingScreen(
                    onStartStory = { viewModel.startNewStory() }
                )
            }

            is StoryUiState.Loading -> {
                LoadingScreen()
            }

            is StoryUiState.Chat -> {
                ChatScreen(
                    messages = messages,
                    isListening = isListening,
                    isAiThinking = isAiThinking,
                    onSendMessage = { text -> viewModel.sendMessage(text) },
                    onStartVoiceInput = onRequestPermission,
                    onStopVoiceInput = { viewModel.stopVoiceInput() },
                    onNewStory = { viewModel.startNewStory() },
                    isVoiceNarrationEnabled = isVoiceNarrationEnabled,
                    onToggleVoiceNarration = { viewModel.toggleVoiceNarration() }
                )
            }

            is StoryUiState.Error -> {
                ErrorScreen(
                    message = (uiState as StoryUiState.Error).message,
                    onRetry = { viewModel.startNewStory() }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF9C27B0)
            )
            Text(
                text = "Preparing your story...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Text("Try Again")
            }
        }
    }
}
