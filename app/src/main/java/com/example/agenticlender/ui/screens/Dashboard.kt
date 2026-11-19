package com.example.agenticlender.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

// All possible loan application stages and their badge colors
enum class AppWorkflowStatus(val label: String, val color: Color, val description: String) {
    DRAFT("Draft", Color(0xFF757575), "Application is being prepared."),
    APPLIED("Applied", Color(0xFF1E88E5), "Application has been submitted."),
    AI_DELIBERATE("AI Deliberate", Color(0xFF8E24AA), "AI has reviewed the application."),
    CONDITIONALLY_APPROVED("Conditionally Approved", Color(0xFFFB8C00), "Loan parameters are approved, but additional documents are required for full approval."),
    UNDERWRITER_APPROVED("Underwriter Approved", Color(0xFF0288D1), "Lender has reviewed financial information and has decided that the borrower meets the loan criteria."),
    FINAL_APPROVAL("Final Approval", Color(0xFF43A047), "All necessary approvals are complete and the loan funds have been disbursed."),
    CLOSED("Closed", Color(0xFF2E7D32), "Mortgage process is finalised and the borrower is now responsible for making payments."),
    DECLINED("Declined", Color(0xFFE53935), "Lender has reviewed the application and has chosen not to approve it.")
}

// Borrower dashboard view showing stats, progress, AI valuation, and broker messages
@Composable
fun UserDashboard(
    onNavigateToApplication: () -> Unit = {},
    onNavigateToStartNewApplication: () -> Unit = {},
    onNavigateToPersonalDetails: () -> Unit = {},
    onNavigateToLoanRequest: () -> Unit = {},
    onNavigateToPropertyDetails: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    // Holds dashboard state variables for numbers and text
    var fetchedName by remember { mutableStateOf("") }
    var applicationType by remember { mutableStateOf("Mortgage") }
    var applicationStatus by remember { mutableStateOf(AppWorkflowStatus.DRAFT) }
    var hasActiveApplication by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var showActiveAppAlert by remember { mutableStateOf(false) }

    var borrowingCapacityText by remember { mutableStateOf("€0.00") }
    var monthlyRepaymentText by remember { mutableStateOf("€0.00 / mo") }
    var ltiRatioText by remember { mutableStateOf("-") }
    var dtiRatioText by remember { mutableStateOf("-") }
    var ltvRatioText by remember { mutableStateOf("-") }
    var riskLevel by remember { mutableStateOf("Pending Input") }
    var riskColor by remember { mutableStateOf(Color(0xFF757575)) }

    var fairValuationText by remember { mutableStateOf("€0.00") }
    var approvalLikelihoodText by remember { mutableStateOf("Pending User Input") }
    var aiExplanationText by remember {
        mutableStateOf(
            "Enter your financial and property details in the application form to compute live AI valuation and approval likelihood."
        )
    }
    var conditionalNotes by remember { mutableStateOf("• Complete application steps to proceed.") }
    var loanNoteRef by remember { mutableStateOf("-") }

    var progressVal by remember { mutableFloatStateOf(0.0f) }
    var latestMessageText by remember { mutableStateOf("No recent messages from your broker") }
    var unreadMessageCount by remember { mutableStateOf(0) }

    // Fetch user profile and application data from Firestore
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        // Fetch user profile info with real-time listener
        db.collection("users").document(uid).addSnapshotListener { document, _ ->
            if (document != null && document.exists()) {
                val first = document.getString("firstName")
                val full = document.getString("fullName")

                applicationType = document.getString("applicationType") ?: "Mortgage"

                val statusStr = document.getString("applicationStatus")
                if (!statusStr.isNullOrBlank()) {
                    applicationStatus = try {
                        AppWorkflowStatus.valueOf(
                            statusStr.uppercase().replace(" ", "_")
                        )
                    } catch (_: Exception) {
                        AppWorkflowStatus.DRAFT
                    }
                }

                fetchedName = when {
                    !first.isNullOrBlank() -> first
                    !full.isNullOrBlank() -> full.substringBefore(" ")
                    else -> currentUser.email?.substringBefore("@")
                        ?.replaceFirstChar { it.uppercase() } ?: ""
                }
            } else if (currentUser.email != null) {
                fetchedName = currentUser.email!!.substringBefore("@")
                    .replaceFirstChar { it.uppercase() }
            }
        }

        // Fetch application data and calculate real metrics
        db.collection("applications").document(uid).get()
            .addOnSuccessListener { appDoc ->
                if (appDoc.exists()) {
                    hasActiveApplication = true

                    val loanAmtStr = appDoc.getString("loanAmount") ?: appDoc.getString("requestedCreditLimit")
                    val priceStr = appDoc.getString("purchasePrice") ?: appDoc.getString("estimatedHomeValue")
                    val incomeStr = appDoc.getString("grossAnnualIncome")
                    val debtStr = appDoc.getString("rentOrMortgage") ?: appDoc.getString("existingLoanRepayment")

                    val loanAmt = loanAmtStr?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
                    val priceVal = priceStr?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
                    val incomeVal = incomeStr?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
                    val debtVal = debtStr?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0

                    if (incomeVal != null && incomeVal > 0) {
                        val capacity = incomeVal * 4.0
                        borrowingCapacityText = String.format(Locale.getDefault(), "€%,.2f", capacity)
                    } else {
                        borrowingCapacityText = "€0.00"
                    }

                    if (loanAmt != null && loanAmt > 0) {
                        val monthlyEst = (loanAmt / 360.0) * 1.35
                        monthlyRepaymentText = String.format(Locale.getDefault(), "€%,.2f / mo", monthlyEst)

                        if (incomeVal != null && incomeVal > 0) {
                            val lti = loanAmt / incomeVal
                            ltiRatioText = String.format(Locale.getDefault(), "%.1fx", lti)

                            val dti = ((debtVal + monthlyEst) / (incomeVal / 12.0)) * 100.0
                            dtiRatioText = String.format(Locale.getDefault(), "%.1f%%", dti)

                            if (priceVal != null && priceVal > 0) {
                                val ltv = (loanAmt / priceVal) * 100.0
                                ltvRatioText = String.format(Locale.getDefault(), "%.1f%%", ltv)

                                if (ltv <= 80 && lti <= 3.5 && dti <= 35) {
                                    riskLevel = "Low Risk"
                                    riskColor = Color(0xFF43A047)
                                    approvalLikelihoodText = "95% (High Odds)"
                                    aiExplanationText = "Strong financial profile. Ratios are well within regulatory guidelines."
                                } else if (ltv <= 90 && lti <= 4.5 && dti <= 45) {
                                    riskLevel = "Medium Risk"
                                    riskColor = Color(0xFFFB8C00)
                                    approvalLikelihoodText = "78% (Moderate Odds)"
                                    aiExplanationText = "Moderate financial risk profile."
                                } else {
                                    riskLevel = "High Risk"
                                    riskColor = Color(0xFFE53935)
                                    approvalLikelihoodText = "55% (Requires Co-signer)"
                                    aiExplanationText = "High loan-to-income or debt-to-income ratio detected."
                                }
                            }
                        }
                    }

                    if (priceVal != null && priceVal > 0) {
                        fairValuationText = String.format(Locale.getDefault(), "€%,.2f", priceVal * 0.96)
                    }

                    val appRef = appDoc.getString("applicationId") ?: "APP-${uid.take(6).uppercase()}"
                    loanNoteRef = "LOAN-NOTE-$appRef"

                    // Connect progress bar directly to completed application steps
                    var completedSteps = 0
                    if (!appDoc.getString("firstName").isNullOrBlank()) completedSteps++
                    if (!appDoc.getString("employerName").isNullOrBlank()) completedSteps++
                    if (!appDoc.getString("rentOrMortgage").isNullOrBlank()) completedSteps++
                    if (!appDoc.getString("primaryBank").isNullOrBlank()) completedSteps++
                    if (!appDoc.getString("loanAmount").isNullOrBlank() || !appDoc.getString("requestedCreditLimit").isNullOrBlank()) completedSteps++
                    if (!appDoc.getString("propertyAddress").isNullOrBlank()) completedSteps++

                    val submitted = appDoc.getBoolean("isApplicationSubmitted") == true
                    isSubmitted = submitted || applicationStatus != AppWorkflowStatus.DRAFT
                    if (submitted) {
                        completedSteps = 8
                        progressVal = 1.0f
                        if (applicationStatus == AppWorkflowStatus.DRAFT) {
                            applicationStatus = AppWorkflowStatus.APPLIED
                        }
                    } else {
                        progressVal = completedSteps.toFloat() / 8.0f
                    }

                    // Check for missing or rejected documents to highlight on dashboard
                    db.collection("users")
                        .document(uid)
                        .collection("documents")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null && !snapshot.isEmpty) {
                                val missingOrRejected = snapshot.documents.filter {
                                    val status = it.getString("status") ?: ""
                                    status == "MISSING" ||
                                            status == "REJECTED" ||
                                            status == "ADDITIONAL_EVIDENCE_REQUIRED"
                                }

                                if (missingOrRejected.isNotEmpty()) {
                                    conditionalNotes = missingOrRejected.joinToString("\n") { document ->
                                        val title = document.getString("title") ?: document.id
                                        val reason = document.getString("rejectionReason")

                                        if (!reason.isNullOrBlank()) {
                                            "• $title: Rejected ($reason)"
                                        } else {
                                            "• $title (Action Required)"
                                        }
                                    }
                                }
                            }
                        }

                    // Fetch latest message snippet from broker
                    db.collection("chats")
                        .document(uid)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null && !snapshot.isEmpty) {
                                val latestDoc = snapshot.documents[0]
                                val text = latestDoc.getString("text") ?: ""

                                if (text.isNotBlank()) {
                                    latestMessageText = text
                                    unreadMessageCount = snapshot.size()
                                }
                            }
                        }
                }
            }
    }

    val displayName = fetchedName.ifBlank { "User" }

    // Main dashboard scrollable view layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top greeting header with user avatar and application status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Hello, $displayName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$applicationType Loan Application",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = applicationStatus.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = applicationStatus.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = applicationStatus.color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Active application card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSubmitted) "Application Review Process" else "Application Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (!isSubmitted) {
                        Text(
                            text = "${(progressVal * 100).toInt()}% Complete",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Surface(
                            color = applicationStatus.color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = applicationStatus.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = applicationStatus.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isSubmitted) {
                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current State: ${applicationStatus.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = applicationStatus.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToApplication,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSubmitted) "View Review Status" else "Open Application Hub")
                    }

                    OutlinedButton(
                        onClick = {
                            if (hasActiveApplication &&
                                applicationStatus != AppWorkflowStatus.CLOSED &&
                                applicationStatus != AppWorkflowStatus.DECLINED
                            ) {
                                showActiveAppAlert = true
                            } else {
                                onNavigateToStartNewApplication()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            if (hasActiveApplication) "Start New" else "New App"
                        )
                    }
                }
            }
        }

        // Financial ratios & risk level
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Borrowing Capacity & Ratios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = riskColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = riskLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = riskColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Capacity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            borrowingCapacityText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Est. Repayment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            monthlyRepaymentText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "LTI Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            ltiRatioText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "DTI Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            dtiRatioText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "LTV Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            ltvRatioText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // AI insights & property valuation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "AI Valuation & Approval Odds",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "• Approval Odds: $approvalLikelihoodText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "• Predicted Fair Property Valuation: $fairValuationText",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    aiExplanationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action required banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                    alpha = 0.25f
                )
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = onNavigateToDocuments
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Action Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Text(
                        conditionalNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Application shortcuts
        Text(
            "Application Steps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutCard(
                title = "Personal",
                subtitle = "Step 1",
                icon = Icons.Default.Person,
                onClick = onNavigateToPersonalDetails,
                modifier = Modifier.weight(1f)
            )

            ShortcutCard(
                title = "Loan & Rate",
                subtitle = "Step 2",
                icon = Icons.AutoMirrored.Filled.Assignment,
                onClick = onNavigateToLoanRequest,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutCard(
                title = "Property Details",
                subtitle = "Step 3",
                icon = Icons.Default.HomeWork,
                onClick = onNavigateToPropertyDetails,
                modifier = Modifier.weight(1f)
            )

            ShortcutCard(
                title = "Documents",
                subtitle = "Step 4",
                icon = Icons.Default.Description,
                onClick = onNavigateToDocuments,
                modifier = Modifier.weight(1f)
            )
        }

        // Broker chat card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = onNavigateToMessages
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Broker Chat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (unreadMessageCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))

                            Badge {
                                Text("$unreadMessageCount New")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        latestMessageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(onClick = onNavigateToMessages) {
                    Text("Chat")
                }
            }
        }
    }

    // Alert dialog if active application exists when user tries to start a new one
    if (showActiveAppAlert) {
        AlertDialog(
            onDismissRequest = { showActiveAppAlert = false },
            title = { Text("Active Application Exists") },
            text = {
                Text(
                    "You already have an active $applicationType loan application in progress " +
                            "(${applicationStatus.label}).\n\n" +
                            "You cannot start a second application until your current application is closed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActiveAppAlert = false
                        onNavigateToApplication()
                    }
                ) {
                    Text("Continue Active App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActiveAppAlert = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Shortcut card helper
@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
