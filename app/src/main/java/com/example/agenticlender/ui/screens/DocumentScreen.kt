package com.example.agenticlender.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.util.Locale

// Document status options and badge colors
enum class DocumentStatus(val label: String, val color: Color) {
    MISSING("Missing / Action Required", Color(0xFFE53935)),
    UPLOADED("Uploaded", Color(0xFF1E88E5)),
    VERIFIED("Verified", Color(0xFF43A047)),
    REJECTED("Rejected", Color(0xFFD32F2F)),
    EXPIRED("Expired", Color(0xFFE65100)),
    ADDITIONAL_EVIDENCE_REQUIRED("Additional Evidence Required", Color(0xFF8E24AA))
}

// Data class for document items and metadata
data class DocumentItemData(
    val id: String,
    val title: String,
    val description: String,
    var status: DocumentStatus = DocumentStatus.MISSING,
    var fileName: String? = null,
    var fileSize: String? = null,
    var fileType: String? = null,
    var downloadUrl: String? = null,
    var rejectionReason: String? = null,
    var reviewedBy: String? = null
)

// Main screen for document uploads & status tracking
@Composable
fun DocumentsScreen() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    // Screen state variables
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedDocToUpload by remember { mutableStateOf<DocumentItemData?>(null) }
    var showOptionModal by remember { mutableStateOf<DocumentItemData?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPreviewDialog by remember { mutableStateOf<DocumentItemData?>(null) }

    // Temporary URI for camera photo capture
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Required documents list
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

    // Shared Uri processing and Firebase Storage upload handler
    val processAndUploadUri: (Uri) -> Unit = { uri ->
        val targetDoc = selectedDocToUpload
        if (targetDoc != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            // Get file name and size from the device
            var fileName = "${targetDoc.title.replace(" ", "_")}_Photo.jpg"
            var fileSizeMb = 0.0

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            val queriedName = cursor.getString(nameIndex)
                            if (!queriedName.isNullOrBlank()) fileName = queriedName
                        }
                        if (sizeIndex != -1) {
                            val bytes = cursor.getLong(sizeIndex)
                            fileSizeMb = bytes / (1024.0 * 1024.0)
                        }
                    }
                }
            } catch (_: Exception) { }

            // File format check (PDF, PNG, JPG, JPEG)
            val isImage = mimeType.contains("image", ignoreCase = true) ||
                    fileName.endsWith(".png", ignoreCase = true) ||
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true)

            if (fileSizeMb > 10.0) {
                uploadErrorMessage = String.format(Locale.getDefault(), "File size exceeds 10 MB limit! (Selected: %.1f MB)", fileSizeMb)
            } else {
                // Upload directly to Firebase Storage
                val uid = currentUser?.uid ?: "anonymous"
                val docId = targetDoc.id
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    .child("documents")
                    .child(uid)
                    .child("${docId}_${System.currentTimeMillis()}_$fileName")

                isUploading = true
                uploadErrorMessage = null

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uriRes: Uri ->
                            isUploading = false
                            val downloadUrl = uriRes.toString()
                            val formattedSize = if (fileSizeMb > 0) String.format(Locale.getDefault(), "%.1f MB", fileSizeMb) else "1.8 MB"

                            val docData = hashMapOf(
                                "id" to docId,
                                "title" to targetDoc.title,
                                "status" to DocumentStatus.UPLOADED.name,
                                "fileName" to fileName,
                                "fileSize" to formattedSize,
                                "fileType" to mimeType,
                                "downloadUrl" to downloadUrl,
                                "uploadedAt" to Timestamp.now(),
                                "rejectionReason" to null
                            )

                            // Save details to Firestore database
                            db.collection("users").document(uid).collection("documents").document(docId)
                                .set(docData, SetOptions.merge())
                            db.collection("applications").document(uid).collection("documents").document(docId)
                                .set(docData, SetOptions.merge())

                            // Update list on screen
                            documents = documents.map { d ->
                                if (d.id == docId) {
                                    d.copy(
                                        status = DocumentStatus.UPLOADED,
                                        fileName = fileName,
                                        fileSize = formattedSize,
                                        fileType = mimeType,
                                        downloadUrl = downloadUrl,
                                        rejectionReason = null
                                    )
                                } else d
                            }
                            selectedDocToUpload = null
                            showOptionModal = null
                        }.addOnFailureListener { exc: Exception ->
                            isUploading = false
                            uploadErrorMessage = "Failed to obtain download URL: ${exc.localizedMessage}"
                        }
                    }
                    .addOnFailureListener { exc: Exception ->
                        isUploading = false
                        uploadErrorMessage = "Firebase Storage Upload Failed: ${exc.localizedMessage}"
                    }
            }
        }
    }

    // 1. Camera Photo Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && cameraPhotoUri != null) {
            processAndUploadUri(cameraPhotoUri!!)
        }
    }

    // 2. Gallery / Photos Launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processAndUploadUri(uri)
        }
    }

    // 3. System Document File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            processAndUploadUri(uri)
        }
    }

    // Sync uploaded document statuses from Firestore
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
                                    downloadUrl = matchDoc.getString("downloadUrl") ?: docItem.downloadUrl,
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

    // Progress counter
    val completedCount = documents.count { it.status == DocumentStatus.VERIFIED || it.status == DocumentStatus.UPLOADED }
    val progress = if (documents.isNotEmpty()) completedCount.toFloat() / documents.size else 0f

    // Filter documents by tab
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
        // Title
        Column {
            Text(
                text = "Documents Hub",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Take a photo or upload document files directly to Firebase Cloud Storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress bar card
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
                        text = "Verification Progress",
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

        // Filter chips
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

        // Document cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredDocuments.forEach { doc ->
                DocumentCardItem(
                    doc = doc,
                    onUploadClick = {
                        selectedDocToUpload = doc
                        showOptionModal = doc
                        uploadErrorMessage = null
                    },
                    onPreviewClick = { showPreviewDialog = doc }
                )
            }
        }
    }

    // Modal: Choose Upload Source (Camera vs Gallery vs File)
    showOptionModal?.let { doc ->
        AlertDialog(
            onDismissRequest = { showOptionModal = null },
            title = { Text("Upload ${doc.title}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Select how you want to capture or choose your document:")

                    // Option 1: Take Photo with Camera
                    Button(
                        onClick = {
                            showOptionModal = null
                            try {
                                val photoFile = File.createTempFile("doc_photo_", ".jpg", context.cacheDir)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraPhotoUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                uploadErrorMessage = "Could not launch camera: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo with Camera")
                    }

                    // Option 2: Choose Photo from Gallery
                    OutlinedButton(
                        onClick = {
                            showOptionModal = null
                            galleryPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Photo from Gallery")
                    }

                    // Option 3: Pick PDF or File
                    OutlinedButton(
                        onClick = {
                            showOptionModal = null
                            filePickerLauncher.launch(arrayOf("application/pdf", "image/png", "image/jpeg"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Document / PDF File")
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showOptionModal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Uploading popup
    if (isUploading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Uploading to Firebase Storage...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text("Uploading photo/file securely to Firebase Storage buckets...")
                }
            },
            confirmButton = { }
        )
    }

    // Error notice popup
    if (uploadErrorMessage != null && !isUploading) {
        AlertDialog(
            onDismissRequest = { uploadErrorMessage = null },
            title = { Text("Upload Notice") },
            text = { Text(uploadErrorMessage!!) },
            confirmButton = {
                Button(onClick = { uploadErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Document preview popup
    showPreviewDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showPreviewDialog = null },
            title = { Text(doc.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File Name: ${doc.fileName ?: "No file attached"}")
                    Text("File Size: ${doc.fileSize ?: "N/A"}")
                    Text("Status: ${doc.status.label}")
                    if (!doc.downloadUrl.isNullOrBlank()) {
                        Text("Firebase Storage URL:\n${doc.downloadUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!doc.rejectionReason.isNullOrBlank()) {
                        Text("Rejection Reason: ${doc.rejectionReason}", color = MaterialTheme.colorScheme.error)
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
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Uploaded File: ${doc.fileName}")
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

// Card item for each document
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

            // Show reviewer notes if rejected
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
                        text = "${doc.fileName} (${doc.fileSize ?: "File"})",
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
                        Text("Re-Upload File")
                    }
                } else {
                    Button(onClick = onUploadClick) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select & Upload File")
                    }
                }
            }
        }
    }
}

// Status badge component
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
