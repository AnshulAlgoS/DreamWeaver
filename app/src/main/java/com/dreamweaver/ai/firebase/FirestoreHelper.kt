package com.dreamweaver.ai.firebase

import android.util.Log
import com.dreamweaver.ai.data.StoryMessage
import com.dreamweaver.ai.data.StorySession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val storiesCollection = db.collection("stories")
    private val sessionsCollection = db.collection("sessions")

    companion object {
        private const val TAG = "FirestoreHelper"
    }

    /**
     * Create a new story session
     */
    suspend fun createSession(): String {
        return try {
            // Add timeout to prevent hanging
            withTimeoutOrNull(2000) {
                val session = StorySession(
                    startTime = Timestamp.now(),
                    lastUpdated = Timestamp.now(),
                    messageCount = 0
                )
                val docRef = sessionsCollection.add(session).await()
                Log.d(TAG, "Session created: ${docRef.id}")
                docRef.id
            } ?: run {
                Log.w(TAG, "Session creation timed out")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            ""
        }
    }

    /**
     * Save a story message to Firestore
     */
    suspend fun saveMessage(message: StoryMessage): Boolean {
        return try {
            val messageData = hashMapOf(
                "sessionId" to message.sessionId,
                "text" to message.text,
                "isUser" to message.isUser,
                "timestamp" to message.timestamp
            )

            storiesCollection.add(messageData).await()

            // Update session last updated time and message count
            updateSession(message.sessionId)

            Log.d(TAG, "Message saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
            false
        }
    }

    /**
     * Update session metadata
     */
    private suspend fun updateSession(sessionId: String) {
        try {
            sessionsCollection.document(sessionId)
                .update(
                    mapOf(
                        "lastUpdated" to Timestamp.now(),
                        "messageCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session", e)
        }
    }

    /**
     * Get messages for a specific session
     */
    suspend fun getSessionMessages(sessionId: String): List<StoryMessage> {
        return try {
            val snapshot = storiesCollection
                .whereEqualTo("sessionId", sessionId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                StoryMessage(
                    id = doc.id,
                    sessionId = doc.getString("sessionId") ?: "",
                    text = doc.getString("text") ?: "",
                    isUser = doc.getBoolean("isUser") ?: true,
                    timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            emptyList()
        }
    }

    /**
     * Get the last session
     */
    suspend fun getLastSession(): StorySession? {
        return try {
            val snapshot = sessionsCollection
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.let { doc ->
                StorySession(
                    id = doc.id,
                    startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                    lastUpdated = doc.getTimestamp("lastUpdated") ?: Timestamp.now(),
                    messageCount = doc.getLong("messageCount")?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last session", e)
            null
        }
    }

    /**
     * Get all sessions
     */
    fun getAllSessionsFlow(): Flow<List<StorySession>> = callbackFlow {
        val listener = sessionsCollection
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to sessions", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    StorySession(
                        id = doc.id,
                        startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                        lastUpdated = doc.getTimestamp("lastUpdated") ?: Timestamp.now(),
                        messageCount = doc.getLong("messageCount")?.toInt() ?: 0
                    )
                } ?: emptyList()

                trySend(sessions)
            }

        awaitClose { listener.remove() }
    }
}
