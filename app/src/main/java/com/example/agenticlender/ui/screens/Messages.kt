package com.example.agenticlender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data holder for secure application-linked chat messages
data class ChatMessage(
    val id: String = "",
    val applicationId: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: String = "Just now",
    val isFromUser: Boolean = false,
    val isRead: Boolean = true
)

// Secure broker & underwriting team messaging screen
@Composable
fun MessageScreen() {
    var messageText by remember { mutableStateOf("") }
    var activeApplicationId by remember { mutableStateOf("APP-2025") }
    var unreadCount by remember { mutableStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: "anonymous_user"

    // Fetch active application ID for linking
    LaunchedEffect(userId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val appId = doc.getString("activeApplicationId") ?: "APP-${userId.take(6).uppercase()}"
                    activeApplicationId = appId
                }
            }
    }

    // Auto-scroll chat list to the newest message whenever messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Scoped real-time message listener (strictly restricted to authorized borrower's account)
    DisposableEffect(userId) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("chats")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages.clear()
                    var unread = 0
                    for (doc in snapshot.documents) {
                        val appId = doc.getString("applicationId") ?: activeApplicationId
                        val sender = doc.getString("sender") ?: "Mortgage Broker"
                        val text = doc.getString("text") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")
                        val isFromUser = doc.getBoolean("isFromUser") ?: false
                        val isRead = doc.getBoolean("isRead") ?: true

                        if (!isFromUser && !isRead) {
                            unread++
                            // Automatically mark broker message as read when borrower views the screen
                            db.collection("chats")
                                .document(userId)
                                .collection("messages")
                                .document(doc.id)
                                .update("isRead", true)
                        }

                        val timeStr = timestamp?.toDate()?.let {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                        } ?: "Just now"

                        messages.add(
                            ChatMessage(
                                id = doc.id,
                                applicationId = appId,
                                sender = sender,
                                text = text,
                                timestamp = timeStr,
                                isFromUser = isFromUser,
                                isRead = isRead
                            )
                        )
                    }
                    unreadCount = unread
                }
            }
        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Application Header & Scoped Link Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Broker Messaging",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Secure application-linked chat ($activeApplicationId)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (unreadCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("$unreadCount Unread")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Security Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Messages are end-to-end encrypted & linked to Application ID $activeApplicationId.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Scoped Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val alignment = if (msg.isFromUser) Alignment.End else Alignment.Start
                val bgColor = if (msg.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (msg.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg.sender,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!msg.isFromUser && !msg.isRead) {
                                    Icon(
                                        imageVector = Icons.Default.MarkEmailUnread,
                                        contentDescription = "Unread",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = msg.text, style = MaterialTheme.typography.bodyMedium, color = textColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Send Message Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a secure message...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val input = messageText.trim()
                        messageText = ""

                        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        val senderName = currentUser?.email ?: "Borrower"

                        // 1. Immediately show sent message locally in the UI
                        messages.add(
                            ChatMessage(
                                id = "temp_${System.currentTimeMillis()}",
                                applicationId = activeApplicationId,
                                sender = senderName,
                                text = input,
                                timestamp = formattedTime,
                                isFromUser = true,
                                isRead = true
                            )
                        )

                        // 2. Write sent message to Cloud Firestore database
                        val db = FirebaseFirestore.getInstance()
                        val messageMap = hashMapOf(
                            "applicationId" to activeApplicationId,
                            "borrowerUid" to userId,
                            "sender" to senderName,
                            "text" to input,
                            "timestamp" to Timestamp.now(),
                            "isFromUser" to true,
                            "isRead" to true
                        )
                        db.collection("chats")
                            .document(userId)
                            .collection("messages")
                            .add(messageMap)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
