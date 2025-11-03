package com.dreamweaver.ai.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class StoryMessage(
    @DocumentId
    val id: String = "",
    val sessionId: String = "",
    val text: String = "",
    val isUser: Boolean = true,
    val timestamp: Timestamp = Timestamp.now()
)

data class StorySession(
    @DocumentId
    val id: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val messageCount: Int = 0
)
