package com.example.agenticlender

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

enum class DocumentStatus(val label: String, val color: Color) {
    MISSING("Missing / Action Required", Color(0xFFE53935)),
    UPLOADED("Uploaded", Color(0xFF1E88E5)),
    VERIFIED("Verified", Color(0xFF43A047)),
    REJECTED("Rejected", Color(0xFFD32F2F)),
    EXPIRED("Expired", Color(0xFFE65100)),
    ADDITIONAL_EVIDENCE_REQUIRED("Additional Evidence Required", Color(0xFF8E24AA))
}

data class DocumentItemData(
    val id: String,
    val title: String,
    val description: String,
    var status: DocumentStatus = DocumentStatus.MISSING,
    var fileName: String? = null,
    var fileSize: String? = null,
    var fileType: String? = null,
    var rejectionReason: String? = null,
    var reviewedBy: String? = null
)

@Composable
fun DocumentsScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    var filterIndex by rememberSaveable { mutableIntStateOf(0) } // 0: All, 1: Action Required, 2: Uploaded, 3: Verified, 4: Rejected

    var documents by remember {
        mutableStateOf(
            listOf(
                DocumentItemData(
                    id = "id_proof",
                    title = "Proof of Identity",
                    description = "Passport, Driving License, or National ID Card",
                    status = DocumentStatus.MISSING
                ),
                DocumentItemData(
                    id = "address_proof",
                    title = "Proof of Address",
                    description = "Recent utility bill or tax document within last 3 months",
                    status = DocumentStatus.MISSING
                ),
                DocumentItemData(
                    id = "payslips",
                    title = "Latest Payslips",
                    description = "Most recent 3 consecutive months payslips",
                    status = DocumentStatus.MISSING
                ),
                DocumentItemData(
                    id = "bank_statements",
                    title = "Bank Statements",
                    description = "6 months certified statements from your primary bank",
                    status = DocumentStatus.MISSING
                ),
                DocumentItemData(
                    id = "deposit_evidence",
                    title = "Deposit Evidence",
                    description = "Savings account statement or Gift Letter for deposit funds",
                    status = DocumentStatus.MISSING
                )
            )
        )
    }

    var documentToUpload by remember { mutableStateOf<DocumentItemData?>(null) }
    var selectedSimulatedFormat by remember { mutableStateOf("PDF") }
    var selectedSimulatedSizeMb by remember { mutableStateOf(3) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPreviewDialog by remember { mutableStateOf<DocumentItemData?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("documents").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val fetchedList = documents.map { docItem ->
                            val matchDoc = snapshot.documents.find { it.id == docItem.id }
                            if (matchDoc != null && matchDoc.exists()) {
                                val statusStr = matchDoc.getString("status") ?: docItem.status.name
                                val status = try { DocumentStatus.valueOf(statusStr) } catch (_: Exception) { docItem.status }
                                docItem.copy(
                                    status = status,
                                    fileName = matchDoc.getString("fileName") ?: docItem.fileName,
                                    fileSize = matchDoc.getString("fileSize") ?: docItem.fileSize,
                                    fileType = matchDoc.getString("fileType") ?: docItem.fileType,
                                    rejectionReason = matchDoc.getString("rejectionReason") ?: docItem.rejectionReason,
                                    reviewedBy = matchDoc.getString("reviewedBy") ?: docItem.reviewedBy
                                )
                            } else {
                                docItem
                            }
                        }
                        documents = fetchedList
                    }
                }
        }
    }

    val completedCount = documents.count { it.status == DocumentStatus.VERIFIED || it.status == DocumentStatus.UPLOADED }
    val progress = if (documents.isNotEmpty()) completedCount.toFloat() / documents.size else 0f

    val filteredDocuments = when (filterIndex) {
        1 -> documents.filter { it.status == DocumentStatus.MISSING || it.status == DocumentStatus.REJECTED || it.status == DocumentStatus.ADDITIONAL_EVIDENCE_REQUIRED }
        2 -> documents.filter { it.status == DocumentStatus.UPLOADED }
        3 -> documents.filter { it.status == DocumentStatus.VERIFIED }
        4 -> documents.filter { it.status == DocumentStatus.REJECTED || it.status == DocumentStatus.ADDITIONAL_EVIDENCE_REQUIRED }
        else -> documents
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Documents Hub",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload clear, valid copies of required documents to process your application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Verification Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("$completedCount / ${documents.size} Ready")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterIndex == 0,
                onClick = { filterIndex = 0 },
                label = { Text("All (${documents.size})") }
            )
            FilterChip(
                selected = filterIndex == 1,
                onClick = { filterIndex = 1 },
                label = { Text("Action Required") }
            )
            FilterChip(
                selected = filterIndex == 2,
                onClick = { filterIndex = 2 },
                label = { Text("Uploaded") }
            )
            FilterChip(
                selected = filterIndex == 3,
                onClick = { filterIndex = 3 },
                label = { Text("Verified") }
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredDocuments.forEach { doc ->
                DocumentCardItem(
                    doc = doc,
                    onUploadClick = { documentToUpload = doc; uploadErrorMessage = null },
                    onPreviewClick = { showPreviewDialog = doc }
                )
            }
        }
    }

    documentToUpload?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToUpload = null },
            title = { Text("Upload ${doc.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Supported file formats: PDF, PNG, JPG (Max size: 10 MB)")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulated File Format:")
                        listOf("PDF", "PNG", "JPG", "EXE (Invalid)").forEach { fmt ->
                            FilterChip(
                                selected = selectedSimulatedFormat == fmt,
                                onClick = { selectedSimulatedFormat = fmt; uploadErrorMessage = null },
                                label = { Text(fmt) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulated File Size:")
                        listOf(2, 5, 12).forEach { sz ->
                            FilterChip(
                                selected = selectedSimulatedSizeMb == sz,
                                onClick = { selectedSimulatedSizeMb = sz; uploadErrorMessage = null },
                                label = { Text("$sz MB") }
                            )
                        }
                    }

                    if (uploadErrorMessage != null) {
                        Text(
                            text = uploadErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Restriction checks
                        if (selectedSimulatedFormat == "EXE (Invalid)") {
                            uploadErrorMessage = "Invalid file format! Uploads are restricted to PDF, PNG, or JPG."
                            return@Button
                        }
                        if (selectedSimulatedSizeMb > 10) {
                            uploadErrorMessage = "File size exceeds 10 MB limit! Please select a smaller file."
                            return@Button
                        }

                        val generatedFileName = "${doc.title.replace(" ", "_")}_Document.${selectedSimulatedFormat.lowercase()}"
                        val newStatus = DocumentStatus.UPLOADED
                        val newDoc = doc.copy(
                            status = newStatus,
                            fileName = generatedFileName,
                            fileSize = "$selectedSimulatedSizeMb.0 MB",
                            fileType = "application/${selectedSimulatedFormat.lowercase()}",
                            rejectionReason = null
                        )

                        currentUser?.uid?.let { uid ->
                            val docData = hashMapOf(
                                "id" to doc.id,
                                "title" to doc.title,
                                "status" to newStatus.name,
                                "fileName" to generatedFileName,
                                "fileSize" to "$selectedSimulatedSizeMb.0 MB",
                                "fileType" to selectedSimulatedFormat.lowercase(),
                                "uploadedAt" to Timestamp.now(),
                                "rejectionReason" to null
                            )
                            db.collection("users").document(uid).collection("documents").document(doc.id)
                                .set(docData, SetOptions.merge())
                            db.collection("applications").document(uid).collection("documents").document(doc.id)
                                .set(docData, SetOptions.merge())
                        }

                        documents = documents.map { if (it.id == doc.id) newDoc else it }
                        documentToUpload = null
                    }
                ) {
                    Text("Confirm Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToUpload = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showPreviewDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showPreviewDialog = null },
            title = { Text(doc.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File Name: ${doc.fileName ?: "No file attached"}")
                    Text("Status: ${doc.status.label}")
                    if (!doc.rejectionReason.isNullOrBlank()) {
                        Text("Rejection/Review Reason: ${doc.rejectionReason}", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📄 Preview of ${doc.fileName}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DocumentCardItem(
    doc: DocumentItemData,
    onUploadClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = doc.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadgeItem(status = doc.status)
            }

            if (!doc.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Reason",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reviewer Notes: ${doc.rejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (!doc.fileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "File Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${doc.fileName} (${doc.fileSize ?: "2 MB"})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (doc.status != DocumentStatus.MISSING) {
                    TextButton(onClick = onPreviewClick) {
                        Text("View File")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onUploadClick) {
                        Text("Re-Upload")
                    }
                } else {
                    Button(onClick = onUploadClick) {
                        Text("Upload Document")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadgeItem(status: DocumentStatus) {
    val icon = when (status) {
        DocumentStatus.VERIFIED -> Icons.Default.CheckCircle
        DocumentStatus.UPLOADED -> Icons.Default.Description
        DocumentStatus.REJECTED -> Icons.Default.Error
        DocumentStatus.EXPIRED -> Icons.Default.HourglassTop
        DocumentStatus.ADDITIONAL_EVIDENCE_REQUIRED -> Icons.Default.Info
        DocumentStatus.MISSING -> Icons.Default.UploadFile
    }

    Surface(
        color = status.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = status.color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = status.color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
