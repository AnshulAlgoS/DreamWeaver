package com.dreamweaver.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamweaver.ai.R
import com.dreamweaver.ai.data.StoryMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<StoryMessage>,
    isListening: Boolean,
    isAiThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onNewStory: () -> Unit,
    isVoiceNarrationEnabled: Boolean,
    onToggleVoiceNarration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DreamWeaver",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (isAiThinking) {
                            Text(
                                text = stringResource(id = R.string.ai_thinking),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9C27B0)
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Voice narration toggle
                    IconButton(onClick = onToggleVoiceNarration) {
                        Icon(
                            imageVector = if (isVoiceNarrationEnabled)
                                Icons.Default.VolumeUp
                            else
                                Icons.Default.VolumeOff,
                            contentDescription = if (isVoiceNarrationEnabled)
                                "Disable voice narration"
                            else
                                "Enable voice narration",
                            tint = if (isVoiceNarrationEnabled) Color(0xFF9C27B0) else Color.Gray
                        )
                    }

                    IconButton(onClick = onNewStory) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.new_story),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F1E),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message
                    )
                }

                // AI Thinking Indicator
                if (isAiThinking) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Input Area
            ChatInputArea(
                textInput = textInput,
                onTextInputChange = { textInput = it },
                isListening = isListening,
                onSendMessage = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                },
                onStartVoiceInput = onStartVoiceInput,
                onStopVoiceInput = onStopVoiceInput,
                enabled = !isAiThinking,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: StoryMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            color = if (message.isUser) Color(0xFF2196F3) else Color(0xFF9C27B0),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF9C27B0).copy(alpha = 0.5f),
            modifier = Modifier.widthIn(max = 80.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    AnimatedDot(delay = index * 150)
                }
            }
        }
    }
}

@Composable
fun AnimatedDot(delay: Int, modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(delay.toLong())
            isVisible = true
            kotlinx.coroutines.delay(600)
            isVisible = false
            kotlinx.coroutines.delay(600)
        }
    }

    Surface(
        shape = CircleShape,
        color = if (isVisible) Color.White else Color.White.copy(alpha = 0.3f),
        modifier = modifier.size(8.dp)
    ) {}
}

@Composable
fun ChatInputArea(
    textInput: String,
    onTextInputChange: (String) -> Unit,
    isListening: Boolean,
    onSendMessage: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1A1A2E),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text Input Field
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (isListening) {
                            stringResource(id = R.string.listening)
                        } else {
                            stringResource(id = R.string.type_message)
                        },
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF9C27B0),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color(0xFF9C27B0)
                ),
                enabled = enabled && !isListening,
                maxLines = 4
            )

            // Voice Input Button
            FilledIconButton(
                onClick = {
                    if (isListening) {
                        onStopVoiceInput()
                    } else {
                        onStartVoiceInput()
                    }
                },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isListening) Color(0xFFE91E63) else Color(0xFF9C27B0)
                ),
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = stringResource(id = R.string.speak),
                    tint = Color.White
                )
            }

            // Send Button
            if (textInput.isNotBlank() && !isListening) {
                FilledIconButton(
                    onClick = onSendMessage,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = stringResource(id = R.string.send),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
