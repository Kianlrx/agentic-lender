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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

// Application step progress enum
enum class StepStatus(val label: String, val color: Color) {
    COMPLETED("Completed", Color(0xFF43A047)),
    IN_PROGRESS("In Progress", Color(0xFF1E88E5)),
    NOT_STARTED("Not Started", Color(0xFF757575))
}

// Main Application Hub showing all steps & full application review button
@Composable
fun ApplicationScreen(
    onNavigateToPersonalDetails: () -> Unit = {},
    onNavigateToEmployment: () -> Unit = {},
    onNavigateToDebtsAndCommitments: () -> Unit = {},
    onNavigateToDepositAndAssets: () -> Unit = {},
    onNavigateToLoanRequest: () -> Unit = {},
    onNavigateToPropertyDetails: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToConsent: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToSelectApplicationType: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var applicationType by remember { mutableStateOf("Mortgage") }
    var step1Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step2Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step3Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step4Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step5Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step6Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step7Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }
    var step8Status by remember { mutableStateOf(StepStatus.NOT_STARTED) }

    // Evaluate completed steps from Firestore
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        applicationType = userDoc.getString("applicationType") ?: "Mortgage"
                    }
                }

            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val appType = doc.getString("applicationType")
                        if (!appType.isNullOrBlank()) {
                            applicationType = appType
                        }
                        if (!doc.getString("firstName").isNullOrBlank()) step1Status = StepStatus.COMPLETED
                        if (!doc.getString("employerName").isNullOrBlank()) step2Status = StepStatus.COMPLETED
                        if (!doc.getString("rentOrMortgage").isNullOrBlank()) step3Status = StepStatus.COMPLETED
                        if (!doc.getString("primaryBank").isNullOrBlank()) step4Status = StepStatus.COMPLETED
                        if (!doc.getString("loanAmount").isNullOrBlank() || !doc.getString("requestedCreditLimit").isNullOrBlank()) step5Status = StepStatus.COMPLETED
                        if (!doc.getString("propertyAddress").isNullOrBlank()) step6Status = StepStatus.COMPLETED
                        
                        val isPassportUploaded = doc.getBoolean("isPassportUploaded") == true
                        val isPayslipUploaded = doc.getBoolean("isPayslipUploaded") == true
                        if (isPassportUploaded || isPayslipUploaded) {
                            step7Status = StepStatus.COMPLETED
                        }
                        
                        if (doc.getBoolean("isApplicationSubmitted") == true) {
                            step7Status = StepStatus.COMPLETED
                            step8Status = StepStatus.COMPLETED
                        }
                    }
                }

            db.collection("users").document(uid).collection("documents").get()
                .addOnSuccessListener { snap ->
                    if (snap != null && !snap.isEmpty) {
                        val uploaded = snap.documents.any {
                            val st = it.getString("status") ?: ""
                            st == "UPLOADED" || st == "VERIFIED"
                        }
                        if (uploaded) step7Status = StepStatus.COMPLETED
                    }
                }
        }
    }

    val steps = listOf(
        step1Status, step2Status, step3Status, step4Status,
        step5Status, step6Status, step7Status, step8Status
    )
    val completedCount = steps.count { it == StepStatus.COMPLETED }
    val progressVal = completedCount.toFloat() / steps.size
    val progressPercent = (progressVal * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Application Hub",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$applicationType Application • REF-2025-${currentUser?.uid?.take(4)?.uppercase() ?: "8841"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (step8Status != StepStatus.COMPLETED) {
                OutlinedButton(onClick = onNavigateToSelectApplicationType) {
                    Text("Change Type")
                }
            }
        }

        // Progress bar card & Review Application button
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
                        text = "Application Completion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("$completedCount of 8 Steps ($progressPercent%)")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressVal },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Review Full Application & Summary")
                }
            }
        }

        Text(
            text = "Application Steps",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        ApplicationStepCard(
            stepNumber = 1,
            title = "Personal & Household Details",
            description = "Contact info, home address, dependants & joint applicant details.",
            status = step1Status,
            onClick = onNavigateToPersonalDetails
        )

        ApplicationStepCard(
            stepNumber = 2,
            title = "Employment & Income",
            description = "Employer name, occupation, gross annual base income & net pay.",
            status = step2Status,
            onClick = onNavigateToEmployment
        )

        ApplicationStepCard(
            stepNumber = 3,
            title = "Existing Debts & Commitments",
            description = "Credit cards, personal loan repayments & current rent/mortgage.",
            status = step3Status,
            onClick = onNavigateToDebtsAndCommitments
        )

        ApplicationStepCard(
            stepNumber = 4,
            title = "Deposit & Assets",
            description = "Available deposit, deposit source & bank account details.",
            status = step4Status,
            onClick = onNavigateToDepositAndAssets
        )

        ApplicationStepCard(
            stepNumber = 5,
            title = "Loan Request (${if (applicationType == "HELOC") "HELOC Terms" else "Mortgage Terms"})",
            description = if (applicationType == "HELOC") "Requested HELOC limit, draw period & home equity available." else "Requested loan amount, desired loan term & rate preference.",
            status = step5Status,
            onClick = onNavigateToLoanRequest
        )

        ApplicationStepCard(
            stepNumber = 6,
            title = "Property Details (${if (applicationType == "HELOC") "HELOC Collateral" else "Property Info"})",
            description = if (applicationType == "HELOC") "Existing 1st mortgage lender, property valuation & location." else "Purchase price, property address, property type & occupancy.",
            status = step6Status,
            onClick = onNavigateToPropertyDetails
        )

        ApplicationStepCard(
            stepNumber = 7,
            title = "Document Upload",
            description = "Passport, 3 payslips, 6 months bank statements & utility bill.",
            status = step7Status,
            onClick = onNavigateToUpload
        )

        ApplicationStepCard(
            stepNumber = 8,
            title = "Consent & Declarations",
            description = "Legal declarations, credit check consent & electronic signature.",
            status = step8Status,
            onClick = onNavigateToConsent
        )
    }
}

// Step card item component
@Composable
private fun ApplicationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    status: StepStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == StepStatus.COMPLETED) status.color.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (status == StepStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = status.color,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$stepNumber. $title",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = status.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = status.color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Step",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Sub-screen top header with back arrow
@Composable
fun SubScreenHeader(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

// Step 1: Collects personal details, home address, dependants & joint applicant info
@Composable
fun PersonalDetailsScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(currentUser?.email ?: "") }
    var address by rememberSaveable { mutableStateOf("") }
    var householdStatus by rememberSaveable { mutableStateOf("") }
    var dependants by rememberSaveable { mutableStateOf("") }

    var isJointApplication by rememberSaveable { mutableStateOf(false) }
    var coFirstName by rememberSaveable { mutableStateOf("") }
    var coLastName by rememberSaveable { mutableStateOf("") }
    var coEmail by rememberSaveable { mutableStateOf("") }
    var coPhone by rememberSaveable { mutableStateOf("") }
    var coDob by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isReadOnly by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { uDoc ->
                    if (uDoc.exists()) {
                        firstName = uDoc.getString("firstName") ?: firstName
                        lastName = uDoc.getString("lastName") ?: lastName
                        dob = uDoc.getString("dob") ?: dob
                        phone = uDoc.getString("phone") ?: phone
                        address = uDoc.getString("address") ?: address
                    }
                }

            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        isReadOnly = doc.getBoolean("isApplicationSubmitted") == true
                        firstName = doc.getString("firstName") ?: firstName
                        lastName = doc.getString("lastName") ?: lastName
                        dob = doc.getString("dob") ?: dob
                        phone = doc.getString("phone") ?: phone
                        email = doc.getString("email") ?: email
                        address = doc.getString("address") ?: address
                        householdStatus = doc.getString("householdStatus") ?: householdStatus
                        dependants = doc.getString("dependants") ?: dependants
                        isJointApplication = doc.getBoolean("isJointApplication") ?: isJointApplication
                        coFirstName = doc.getString("coFirstName") ?: coFirstName
                        coLastName = doc.getString("coLastName") ?: coLastName
                        coEmail = doc.getString("coEmail") ?: coEmail
                        coPhone = doc.getString("coPhone") ?: coPhone
                        coDob = doc.getString("coDob") ?: coDob
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "1. Personal Details",
            onNavigateBack = onNavigateBack
        )
        if (isReadOnly) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Read Only",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Application Submitted — Read-Only Mode. This section cannot be modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Text(
            text = "Please provide your personal, contact, and household information.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (DD/MM/YYYY)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Home Address") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = householdStatus,
            onValueChange = { householdStatus = it },
            label = { Text("Household Status (e.g., Homeowner, Renter)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dependants,
            onValueChange = { dependants = it },
            label = { Text("Number of Dependants") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Is this a Joint Application?", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isJointApplication,
                onCheckedChange = { isJointApplication = it },
                enabled = !isReadOnly
            )
        }

        if (isJointApplication) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Co-Applicant Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            OutlinedTextField(
                value = coFirstName,
                onValueChange = { coFirstName = it },
                label = { Text("Co-Applicant First Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = coLastName,
                onValueChange = { coLastName = it },
                label = { Text("Co-Applicant Last Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = coDob,
                onValueChange = { coDob = it },
                label = { Text("Co-Applicant DOB (DD/MM/YYYY)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = coPhone,
                onValueChange = { coPhone = it },
                label = { Text("Co-Applicant Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = coEmail,
                onValueChange = { coEmail = it },
                label = { Text("Co-Applicant Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (successMessage != null) {
            Text(
                text = successMessage!!,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            if (!isReadOnly) {
                OutlinedButton(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            val data = hashMapOf(
                                "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(),
                                "dob" to dob.trim(),
                                "phone" to phone.trim(),
                                "email" to email.trim(),
                                "address" to address.trim(),
                                "householdStatus" to householdStatus.trim(),
                                "dependants" to dependants.trim(),
                                "isJointApplication" to isJointApplication,
                                "coFirstName" to coFirstName.trim(),
                                "coLastName" to coLastName.trim(),
                                "coEmail" to coEmail.trim(),
                                "coPhone" to coPhone.trim(),
                                "coDob" to coDob.trim(),
                                "status" to "Draft",
                                "updatedAt" to Timestamp.now()
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                        }
                        errorMessage = null
                        successMessage = "Draft Saved Successfully ✅"
                    },
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank() || dob.isBlank() || phone.isBlank() || email.isBlank() || address.isBlank() || householdStatus.isBlank() || dependants.isBlank()) {
                            errorMessage = "Please fill in all required personal and household fields."
                            successMessage = null
                            return@Button
                        }
                        if (isJointApplication && (coFirstName.isBlank() || coLastName.isBlank() || coDob.isBlank() || coPhone.isBlank() || coEmail.isBlank())) {
                            errorMessage = "Please fill in all required co-applicant fields for a joint application."
                            successMessage = null
                            return@Button
                        }

                        currentUser?.uid?.let { uid ->
                            val data = hashMapOf(
                                "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(),
                                "dob" to dob.trim(),
                                "phone" to phone.trim(),
                                "email" to email.trim(),
                                "address" to address.trim(),
                                "householdStatus" to householdStatus.trim(),
                                "dependants" to dependants.trim(),
                                "isJointApplication" to isJointApplication,
                                "coFirstName" to coFirstName.trim(),
                                "coLastName" to coLastName.trim(),
                                "coEmail" to coEmail.trim(),
                                "coPhone" to coPhone.trim(),
                                "coDob" to coDob.trim(),
                                "status" to "Draft",
                                "updatedAt" to Timestamp.now()
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                        }

                        errorMessage = null
                        successMessage = "Personal details saved successfully!"
                        onNavigateNext()
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Save & Continue")
                }
            } else {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Next Step")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 2: Collects employer name, occupation, annual income & net take-home pay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmploymentAndIncomeScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var employerName by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var employmentType by rememberSaveable { mutableStateOf("") }
    var yearsAtEmployer by rememberSaveable { mutableStateOf("") }
    var grossAnnualIncome by rememberSaveable { mutableStateOf("") }
    var additionalIncome by rememberSaveable { mutableStateOf("") }
    var netMonthlyIncome by rememberSaveable { mutableStateOf("") }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isReadOnly by rememberSaveable { mutableStateOf(false) }

    val employmentOptions = listOf(
        "Full-time Employed",
        "Part-time Employed",
        "Self-employed",
        "Contractor / Temporary",
        "Retired",
        "Unemployed"
    )

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        isReadOnly = doc.getBoolean("isApplicationSubmitted") == true
                        employerName = doc.getString("employerName") ?: employerName
                        jobTitle = doc.getString("jobTitle") ?: jobTitle
                        employmentType = doc.getString("employmentType") ?: employmentType
                        yearsAtEmployer = doc.getString("yearsAtEmployer") ?: yearsAtEmployer
                        grossAnnualIncome = doc.getString("grossAnnualIncome") ?: grossAnnualIncome
                        additionalIncome = doc.getString("additionalIncome") ?: additionalIncome
                        netMonthlyIncome = doc.getString("netMonthlyIncome") ?: netMonthlyIncome
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("2. Employment & Income", onNavigateBack)
        if (isReadOnly) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Read Only",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Application Submitted — Read-Only Mode. This section cannot be modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Text("Provide details about your employment status and annual income sources.")

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded && !isReadOnly,
            onExpandedChange = { if (!isReadOnly) isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = employmentType,
                onValueChange = {},
                readOnly = true,
                enabled = !isReadOnly,
                label = { Text("Employment Status") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                employmentOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            employmentType = option
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = employerName,
            onValueChange = { employerName = it },
            label = { Text("Employer / Business Name") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = jobTitle,
            onValueChange = { jobTitle = it },
            label = { Text("Job Title / Occupation") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = yearsAtEmployer,
            onValueChange = { yearsAtEmployer = it },
            label = { Text("Time at Current Role (Years / Months)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = grossAnnualIncome,
            onValueChange = { grossAnnualIncome = it },
            label = { Text("Gross Annual Base Income (€)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = additionalIncome,
            onValueChange = { additionalIncome = it },
            label = { Text("Additional Annual Income (Bonus / Overtime)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = netMonthlyIncome,
            onValueChange = { netMonthlyIncome = it },
            label = { Text("Net Monthly Take-Home Pay (€)") },
            singleLine = true,
            enabled = !isReadOnly,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            if (!isReadOnly) {
                OutlinedButton(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            val data = hashMapOf(
                                "employerName" to employerName.trim(),
                                "jobTitle" to jobTitle.trim(),
                                "employmentType" to employmentType.trim(),
                                "yearsAtEmployer" to yearsAtEmployer.trim(),
                                "grossAnnualIncome" to grossAnnualIncome.trim(),
                                "additionalIncome" to additionalIncome.trim(),
                                "netMonthlyIncome" to netMonthlyIncome.trim(),
                                "status" to "Draft",
                                "updatedAt" to Timestamp.now()
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                        }
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = {
                        if (employerName.isBlank() || jobTitle.isBlank() || grossAnnualIncome.isBlank() || netMonthlyIncome.isBlank()) {
                            errorMessage = "Please fill in all employment and income fields."
                            return@Button
                        }

                        currentUser?.uid?.let { uid ->
                            val data = hashMapOf(
                                "employerName" to employerName.trim(),
                                "jobTitle" to jobTitle.trim(),
                                "employmentType" to employmentType.trim(),
                                "yearsAtEmployer" to yearsAtEmployer.trim(),
                                "grossAnnualIncome" to grossAnnualIncome.trim(),
                                "additionalIncome" to additionalIncome.trim(),
                                "netMonthlyIncome" to netMonthlyIncome.trim(),
                                "status" to "Draft",
                                "updatedAt" to Timestamp.now()
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                        }

                        errorMessage = null
                        onNavigateNext()
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Save & Continue")
                }
            } else {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Next Step")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 3: Collects existing debt commitments, loan repayments & rent or mortgage expenses
@Composable
fun DebtsAndCommitmentsScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var creditCardLimit by rememberSaveable { mutableStateOf("") }
    var existingLoanRepayment by rememberSaveable { mutableStateOf("") }
    var rentOrMortgage by rememberSaveable { mutableStateOf("") }
    var otherMonthlyCommitments by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        creditCardLimit = doc.getString("creditCardLimit") ?: creditCardLimit
                        existingLoanRepayment = doc.getString("existingLoanRepayment") ?: existingLoanRepayment
                        rentOrMortgage = doc.getString("rentOrMortgage") ?: rentOrMortgage
                        otherMonthlyCommitments = doc.getString("otherMonthlyCommitments") ?: otherMonthlyCommitments
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("3. Debts & Commitments", onNavigateBack)
        Text("Declare your existing financial commitments and monthly debt repayments.")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = creditCardLimit,
            onValueChange = { creditCardLimit = it },
            label = { Text("Total Credit Card Limits (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = existingLoanRepayment,
            onValueChange = { existingLoanRepayment = it },
            label = { Text("Personal / Car Loan Monthly Repayments (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = rentOrMortgage,
            onValueChange = { rentOrMortgage = it },
            label = { Text("Current Monthly Rent or Existing Mortgage (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = otherMonthlyCommitments,
            onValueChange = { otherMonthlyCommitments = it },
            label = { Text("Other Monthly Expenses (Childcare, Alimony, etc.)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "creditCardLimit" to creditCardLimit.trim(),
                            "existingLoanRepayment" to existingLoanRepayment.trim(),
                            "rentOrMortgage" to rentOrMortgage.trim(),
                            "otherMonthlyCommitments" to otherMonthlyCommitments.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }
                    errorMessage = null
                },
                modifier = Modifier.weight(1.1f)
            ) {
                Text("Save Draft")
            }
            Button(
                onClick = {
                    if (rentOrMortgage.isBlank()) {
                        errorMessage = "Please enter your current rent or mortgage commitment."
                        return@Button
                    }

                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "creditCardLimit" to creditCardLimit.trim(),
                            "existingLoanRepayment" to existingLoanRepayment.trim(),
                            "rentOrMortgage" to rentOrMortgage.trim(),
                            "otherMonthlyCommitments" to otherMonthlyCommitments.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }

                    errorMessage = null
                    onNavigateNext()
                },
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Save & Continue")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 4: Collects available deposit amount, source of deposit & primary bank name
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositAndAssetsScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var depositAmount by rememberSaveable { mutableStateOf("") }
    var depositSource by rememberSaveable { mutableStateOf("") }
    var assetsValue by rememberSaveable { mutableStateOf("") }
    var primaryBank by rememberSaveable { mutableStateOf("") }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val depositSources = listOf(
        "Personal Savings",
        "Gift from Family",
        "First-Time Buyer Grant / Help to Buy",
        "Inheritance",
        "Sale of Asset / Property"
    )

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        depositAmount = doc.getString("depositAmount") ?: depositAmount
                        depositSource = doc.getString("depositSource") ?: depositSource
                        assetsValue = doc.getString("assetsValue") ?: assetsValue
                        primaryBank = doc.getString("primaryBank") ?: primaryBank
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("4. Deposit & Assets", onNavigateBack)
        Text("Provide details on your deposit source and overall asset value.")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = depositAmount,
            onValueChange = { depositAmount = it },
            label = { Text("Available Deposit Amount (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = depositSource,
                onValueChange = {},
                readOnly = true,
                label = { Text("Source of Deposit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                depositSources.forEach { source ->
                    DropdownMenuItem(
                        text = { Text(text = source) },
                        onClick = {
                            depositSource = source
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = assetsValue,
            onValueChange = { assetsValue = it },
            label = { Text("Total Other Assets Value (Stocks, Investments) (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = primaryBank,
            onValueChange = { primaryBank = it },
            label = { Text("Primary Bank Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "depositAmount" to depositAmount.trim(),
                            "depositSource" to depositSource.trim(),
                            "assetsValue" to assetsValue.trim(),
                            "primaryBank" to primaryBank.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }
                    errorMessage = null
                },
                modifier = Modifier.weight(1.1f)
            ) {
                Text("Save Draft")
            }
            Button(
                onClick = {
                    if (depositAmount.isBlank() || primaryBank.isBlank()) {
                        errorMessage = "Please enter deposit amount and primary bank."
                        return@Button
                    }

                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "depositAmount" to depositAmount.trim(),
                            "depositSource" to depositSource.trim(),
                            "assetsValue" to assetsValue.trim(),
                            "primaryBank" to primaryBank.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }

                    errorMessage = null
                    onNavigateNext()
                },
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Save & Continue")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 5: Collects requested loan amount, term, and rate preference for Mortgage or HELOC
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRequestScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var applicationType by remember { mutableStateOf("Mortgage") }

    var loanAmount by rememberSaveable { mutableStateOf("") }
    var loanPurpose by rememberSaveable { mutableStateOf("") }
    var loanTermYears by rememberSaveable { mutableStateOf("") }
    var rateType by rememberSaveable { mutableStateOf("") }

    var existingFirstMortgageBalance by rememberSaveable { mutableStateOf("") }
    var estimatedHomeEquity by rememberSaveable { mutableStateOf("") }
    var requestedCreditLimit by rememberSaveable { mutableStateOf("") }
    var drawPeriodYears by rememberSaveable { mutableStateOf("") }

    var isPurposeExpanded by remember { mutableStateOf(false) }
    var isRateExpanded by remember { mutableStateOf(false) }
    var isDrawPeriodExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val loanPurposes = listOf(
        "First-Time Home Purchase",
        "Move / Next Home Purchase",
        "Refinance / Switcher",
        "Equity Release"
    )

    val rateTypes = listOf(
        "Fixed Rate",
        "Variable Rate",
        "Split Rate"
    )

    val drawPeriods = listOf(
        "5 Years",
        "10 Years",
        "15 Years"
    )

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { uDoc ->
                    if (uDoc.exists()) {
                        applicationType = uDoc.getString("applicationType") ?: "Mortgage"
                    }
                }

            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val savedAppType = doc.getString("applicationType")
                        if (!savedAppType.isNullOrBlank()) {
                            applicationType = savedAppType
                        }
                        loanAmount = doc.getString("loanAmount") ?: loanAmount
                        loanPurpose = doc.getString("loanPurpose") ?: loanPurpose
                        loanTermYears = doc.getString("loanTermYears") ?: loanTermYears
                        rateType = doc.getString("rateType") ?: rateType

                        existingFirstMortgageBalance = doc.getString("existingFirstMortgageBalance") ?: existingFirstMortgageBalance
                        estimatedHomeEquity = doc.getString("estimatedHomeEquity") ?: estimatedHomeEquity
                        requestedCreditLimit = doc.getString("requestedCreditLimit") ?: requestedCreditLimit
                        drawPeriodYears = doc.getString("drawPeriodYears") ?: drawPeriodYears
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("5. Loan Request ($applicationType)", onNavigateBack)
        Text(
            text = if (applicationType == "HELOC") "Specify your HELOC revolving line of credit limit and home equity details."
            else "Specify your requested mortgage loan amount, term, and rate preference.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (applicationType == "HELOC") {
            OutlinedTextField(
                value = existingFirstMortgageBalance,
                onValueChange = { existingFirstMortgageBalance = it },
                label = { Text("Existing 1st Mortgage Balance (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estimatedHomeEquity,
                onValueChange = { estimatedHomeEquity = it },
                label = { Text("Estimated Current Home Equity (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = requestedCreditLimit,
                onValueChange = { requestedCreditLimit = it },
                label = { Text("Requested HELOC Credit Limit (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = isDrawPeriodExpanded,
                onExpandedChange = { isDrawPeriodExpanded = !isDrawPeriodExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = drawPeriodYears,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Draw Period Preference") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDrawPeriodExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isDrawPeriodExpanded,
                    onDismissRequest = { isDrawPeriodExpanded = false }
                ) {
                    drawPeriods.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(text = period) },
                            onClick = {
                                drawPeriodYears = period
                                isDrawPeriodExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = loanAmount,
                onValueChange = { loanAmount = it },
                label = { Text("Requested Loan Amount (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = isPurposeExpanded,
                onExpandedChange = { isPurposeExpanded = !isPurposeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = loanPurpose,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Purpose of Loan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPurposeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isPurposeExpanded,
                    onDismissRequest = { isPurposeExpanded = false }
                ) {
                    loanPurposes.forEach { purpose ->
                        DropdownMenuItem(
                            text = { Text(text = purpose) },
                            onClick = {
                                loanPurpose = purpose
                                isPurposeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = loanTermYears,
                onValueChange = { loanTermYears = it },
                label = { Text("Desired Loan Term (Years)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = isRateExpanded,
                onExpandedChange = { isRateExpanded = !isRateExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = rateType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rate Preference") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRateExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isRateExpanded,
                    onDismissRequest = { isRateExpanded = false }
                ) {
                    rateTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(text = type) },
                            onClick = {
                                rateType = type
                                isRateExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "applicationType" to applicationType,
                            "loanAmount" to loanAmount.trim(),
                            "loanPurpose" to loanPurpose.trim(),
                            "loanTermYears" to loanTermYears.trim(),
                            "rateType" to rateType.trim(),
                            "existingFirstMortgageBalance" to existingFirstMortgageBalance.trim(),
                            "estimatedHomeEquity" to estimatedHomeEquity.trim(),
                            "requestedCreditLimit" to requestedCreditLimit.trim(),
                            "drawPeriodYears" to drawPeriodYears.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }
                    errorMessage = null
                },
                modifier = Modifier.weight(1.1f)
            ) {
                Text("Save Draft")
            }
            Button(
                onClick = {
                    if (applicationType == "HELOC") {
                        if (existingFirstMortgageBalance.isBlank() || requestedCreditLimit.isBlank()) {
                            errorMessage = "Please enter 1st mortgage balance and requested HELOC credit limit."
                            return@Button
                        }
                    } else {
                        if (loanAmount.isBlank() || loanTermYears.isBlank()) {
                            errorMessage = "Please enter requested loan amount and loan term."
                            return@Button
                        }
                    }

                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "applicationType" to applicationType,
                            "loanAmount" to loanAmount.trim(),
                            "loanPurpose" to loanPurpose.trim(),
                            "loanTermYears" to loanTermYears.trim(),
                            "rateType" to rateType.trim(),
                            "existingFirstMortgageBalance" to existingFirstMortgageBalance.trim(),
                            "estimatedHomeEquity" to estimatedHomeEquity.trim(),
                            "requestedCreditLimit" to requestedCreditLimit.trim(),
                            "drawPeriodYears" to drawPeriodYears.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }

                    errorMessage = null
                    onNavigateNext()
                },
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Save & Continue")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 6: Property Details
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailsScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var applicationType by remember { mutableStateOf("Mortgage") }

    var purchasePrice by rememberSaveable { mutableStateOf("") }
    var propertyAddress by rememberSaveable { mutableStateOf("") }
    var propertyType by rememberSaveable { mutableStateOf("") }
    var occupancyType by rememberSaveable { mutableStateOf("") }
    var existingMortgageLender by rememberSaveable { mutableStateOf("") }

    var isTypeExpanded by remember { mutableStateOf(false) }
    var isOccupancyExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val propertyTypes = listOf(
        "Detached House",
        "Semi-Detached",
        "Terraced",
        "Apartment / Flat",
        "Duplex",
        "Land / Site"
    )

    val occupancyTypes = listOf(
        "Primary Residence",
        "Second / Holiday Home",
        "Investment / Buy-to-Let"
    )

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { uDoc ->
                    if (uDoc.exists()) {
                        applicationType = uDoc.getString("applicationType") ?: "Mortgage"
                    }
                }

            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val savedAppType = doc.getString("applicationType")
                        if (!savedAppType.isNullOrBlank()) {
                            applicationType = savedAppType
                        }
                        purchasePrice = doc.getString("purchasePrice") ?: doc.getString("estimatedHomeValue") ?: purchasePrice
                        propertyAddress = doc.getString("propertyAddress") ?: propertyAddress
                        propertyType = doc.getString("propertyType") ?: propertyType
                        occupancyType = doc.getString("occupancyType") ?: occupancyType
                        existingMortgageLender = doc.getString("existingMortgageLender") ?: existingMortgageLender
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("6. Property Details ($applicationType)", onNavigateBack)
        Text(
            text = if (applicationType == "HELOC") "Provide details for the property collateral securing your HELOC line of credit."
            else "Enter the estimated property valuation, type, and location details.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = purchasePrice,
            onValueChange = { purchasePrice = it },
            label = { Text(if (applicationType == "HELOC") "Estimated Home Valuation (€)" else "Purchase Price / Estimated Valuation (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = propertyAddress,
            onValueChange = { propertyAddress = it },
            label = { Text("Property Address / Location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (applicationType == "HELOC") {
            OutlinedTextField(
                value = existingMortgageLender,
                onValueChange = { existingMortgageLender = it },
                label = { Text("Existing 1st Mortgage Lender Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ExposedDropdownMenuBox(
            expanded = isTypeExpanded,
            onExpandedChange = { isTypeExpanded = !isTypeExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = propertyType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Property Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isTypeExpanded,
                onDismissRequest = { isTypeExpanded = false }
            ) {
                propertyTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(text = type) },
                        onClick = {
                            propertyType = type
                            isTypeExpanded = false
                        }
                    )
                }
            }
        }

        if (applicationType == "Mortgage") {
            ExposedDropdownMenuBox(
                expanded = isOccupancyExpanded,
                onExpandedChange = { isOccupancyExpanded = !isOccupancyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = occupancyType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Occupancy Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOccupancyExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isOccupancyExpanded,
                    onDismissRequest = { isOccupancyExpanded = false }
                ) {
                    occupancyTypes.forEach { occupancy ->
                        DropdownMenuItem(
                            text = { Text(text = occupancy) },
                            onClick = {
                                occupancyType = occupancy
                                isOccupancyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "applicationType" to applicationType,
                            "purchasePrice" to purchasePrice.trim(),
                            "estimatedHomeValue" to purchasePrice.trim(),
                            "propertyAddress" to propertyAddress.trim(),
                            "propertyType" to propertyType.trim(),
                            "occupancyType" to occupancyType.trim(),
                            "existingMortgageLender" to existingMortgageLender.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }
                    errorMessage = null
                },
                modifier = Modifier.weight(1.1f)
            ) {
                Text("Save Draft")
            }
            Button(
                onClick = {
                    if (purchasePrice.isBlank() || propertyAddress.isBlank()) {
                        errorMessage = "Please enter estimated property value and address."
                        return@Button
                    }

                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "applicationType" to applicationType,
                            "purchasePrice" to purchasePrice.trim(),
                            "estimatedHomeValue" to purchasePrice.trim(),
                            "propertyAddress" to propertyAddress.trim(),
                            "propertyType" to propertyType.trim(),
                            "occupancyType" to occupancyType.trim(),
                            "existingMortgageLender" to existingMortgageLender.trim(),
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }

                    errorMessage = null
                    onNavigateNext()
                },
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Save & Continue")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 7: Document Upload
@Composable
fun DocumentUploadScreen(
    onNavigateNext: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var isPassportUploaded by rememberSaveable { mutableStateOf(false) }
    var isPayslipUploaded by rememberSaveable { mutableStateOf(false) }
    var isBankStatementUploaded by rememberSaveable { mutableStateOf(false) }
    var isAddressProofUploaded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("7. Document Upload", onNavigateBack)
        Text("Upload required verification documents for your application.")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isPassportUploaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPassportUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Proof of Identity (Passport / Driving License)", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isPassportUploaded) "Uploaded ✅" else "Not uploaded yet",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = { isPassportUploaded = !isPassportUploaded }) {
                    Text(if (isPassportUploaded) "Remove" else "Upload")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isPayslipUploaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPayslipUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Income Proof (3 Recent Payslips)", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isPayslipUploaded) "Uploaded ✅" else "Not uploaded yet",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = { isPayslipUploaded = !isPayslipUploaded }) {
                    Text(if (isPayslipUploaded) "Remove" else "Upload")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isBankStatementUploaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isBankStatementUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("6 Months Certified Bank Statements", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isBankStatementUploaded) "Uploaded ✅" else "Not uploaded yet",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = { isBankStatementUploaded = !isBankStatementUploaded }) {
                    Text(if (isBankStatementUploaded) "Remove" else "Upload")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAddressProofUploaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAddressProofUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Proof of Address (Utility Bill / Tax Doc)", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isAddressProofUploaded) "Uploaded ✅" else "Not uploaded yet",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = { isAddressProofUploaded = !isAddressProofUploaded }) {
                    Text(if (isAddressProofUploaded) "Remove" else "Upload")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = {
                    currentUser?.uid?.let { uid ->
                        val data = hashMapOf(
                            "isPassportUploaded" to isPassportUploaded,
                            "isPayslipUploaded" to isPayslipUploaded,
                            "isBankStatementUploaded" to isBankStatementUploaded,
                            "isAddressProofUploaded" to isAddressProofUploaded,
                            "status" to "Draft",
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("applications").document(uid).set(data, SetOptions.merge())
                    }
                },
                modifier = Modifier.weight(1.1f)
            ) {
                Text("Save Draft")
            }
            Button(
                onClick = onNavigateNext,
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Save & Continue")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Step 8: Consent & Declarations
@Composable
fun ConsentAndDeclarationsScreen(
    onSubmitApplication: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var isDataProcessingConsented by rememberSaveable { mutableStateOf(false) }
    var isCreditCheckConsented by rememberSaveable { mutableStateOf(false) }
    var isValuationConsented by rememberSaveable { mutableStateOf(false) }
    var isAiAnalysisConsented by rememberSaveable { mutableStateOf(false) }
    var isTermsAgreed by rememberSaveable { mutableStateOf(false) }
    var signature by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        isSubmitted = doc.getBoolean("isApplicationSubmitted") == true
                        isDataProcessingConsented = doc.getBoolean("consent_dataProcessing") ?: isDataProcessingConsented
                        isCreditCheckConsented = doc.getBoolean("consent_creditCheck") ?: isCreditCheckConsented
                        isValuationConsented = doc.getBoolean("consent_valuation") ?: isValuationConsented
                        isAiAnalysisConsented = doc.getBoolean("consent_aiAnalysis") ?: isAiAnalysisConsented
                        isTermsAgreed = doc.getBoolean("consent_terms") ?: isTermsAgreed
                        signature = doc.getString("signature") ?: signature
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader("8. Consent & Declarations", onNavigateBack)
        if (isSubmitted) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Read Only",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔒 Application Submitted — Status: Applied. Your application is under review and cannot be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Text("Please review and confirm all mandatory consents to submit your application.")

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isDataProcessingConsented,
                onCheckedChange = { isDataProcessingConsented = it },
                enabled = !isSubmitted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "1. I consent to the collection and processing of my personal & financial data.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isCreditCheckConsented,
                onCheckedChange = { isCreditCheckConsented = it },
                enabled = !isSubmitted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "2. I consent to credit reference checks with authorized credit bureaus.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isValuationConsented,
                onCheckedChange = { isValuationConsented = it },
                enabled = !isSubmitted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "3. I consent to property valuation, desktop appraisal, and physical inspection.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isAiAnalysisConsented,
                onCheckedChange = { isAiAnalysisConsented = it },
                enabled = !isSubmitted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "4. I consent to AI automated eligibility analysis & risk score deliberation.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isTermsAgreed,
                onCheckedChange = { isTermsAgreed = it },
                enabled = !isSubmitted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "5. I agree to the Agentic Lender Terms of Service & Privacy Policy.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = signature,
            onValueChange = { signature = it },
            label = { Text("Electronic Signature (Full Legal Name)") },
            singleLine = true,
            enabled = !isSubmitted,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (isSubmitted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎉 Application Submitted Successfully! Status set to Applied.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            if (!isSubmitted) {
                OutlinedButton(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            val now = Timestamp.now()
                            val data = hashMapOf(
                                "isApplicationSubmitted" to false,
                                "status" to "Draft",
                                "consent_dataProcessing" to isDataProcessingConsented,
                                "consent_creditCheck" to isCreditCheckConsented,
                                "consent_valuation" to isValuationConsented,
                                "consent_aiAnalysis" to isAiAnalysisConsented,
                                "consent_terms" to isTermsAgreed,
                                "signature" to signature.trim(),
                                "updatedAt" to now
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                            db.collection("users").document(uid).set(hashMapOf("applicationStatus" to "Draft"), SetOptions.merge())
                        }
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = {
                        if (!isDataProcessingConsented || !isCreditCheckConsented || !isValuationConsented || !isAiAnalysisConsented || !isTermsAgreed || signature.isBlank()) {
                            errorMessage = "All 5 mandatory consent options and your electronic signature are required."
                            return@Button
                        }

                        currentUser?.uid?.let { uid ->
                            val now = Timestamp.now()
                            val data = hashMapOf(
                                "isApplicationSubmitted" to true,
                                "status" to "Applied",
                                "consent_dataProcessing" to isDataProcessingConsented,
                                "consent_creditCheck" to isCreditCheckConsented,
                                "consent_valuation" to isValuationConsented,
                                "consent_aiAnalysis" to isAiAnalysisConsented,
                                "consent_terms" to isTermsAgreed,
                                "signature" to signature.trim(),
                                "submittedAt" to now,
                                "updatedAt" to now
                            )
                            db.collection("applications").document(uid).set(data, SetOptions.merge())
                            db.collection("users").document(uid).set(hashMapOf("applicationStatus" to "Applied"), SetOptions.merge())
                        }

                        errorMessage = null
                        isSubmitted = true
                        onSubmitApplication()
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Submit App")
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(2f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Submitted 🔒 (Applied)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Review Application Screen (Story: Review Application)
@Composable
fun ReviewApplicationScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToConsent: () -> Unit = {},
    onEditPersonalDetails: () -> Unit = {},
    onEditEmployment: () -> Unit = {},
    onEditDebts: () -> Unit = {},
    onEditDeposit: () -> Unit = {},
    onEditLoanRequest: () -> Unit = {},
    onEditPropertyDetails: () -> Unit = {},
    onEditDocuments: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // Application data state variables
    var nameSummary by remember { mutableStateOf("Pending Input") }
    var emailSummary by remember { mutableStateOf("Pending Input") }
    var phoneSummary by remember { mutableStateOf("Pending Input") }

    var employmentSummary by remember { mutableStateOf("Pending Input") }
    var incomeSummary by remember { mutableStateOf("Pending Input") }

    var debtsSummary by remember { mutableStateOf("Pending Input") }
    var depositSummary by remember { mutableStateOf("Pending Input") }
    var loanSummary by remember { mutableStateOf("Pending Input") }
    var propertySummary by remember { mutableStateOf("Pending Input") }
    var docSummary by remember { mutableStateOf("Pending Input") }

    // Section completion statuses
    var isSec1Complete by remember { mutableStateOf(false) }
    var isSec2Complete by remember { mutableStateOf(false) }
    var isSec3Complete by remember { mutableStateOf(false) }
    var isSec4Complete by remember { mutableStateOf(false) }
    var isSec5Complete by remember { mutableStateOf(false) }
    var isSec6Complete by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("applications").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val fName = doc.getString("firstName") ?: ""
                        val lName = doc.getString("lastName") ?: ""
                        if (fName.isNotBlank()) {
                            nameSummary = "$fName $lName".trim()
                            emailSummary = doc.getString("email") ?: ""
                            phoneSummary = doc.getString("phone") ?: ""
                            isSec1Complete = true
                        }

                        val emp = doc.getString("employerName") ?: ""
                        val inc = doc.getString("grossAnnualIncome") ?: ""
                        if (emp.isNotBlank() && inc.isNotBlank()) {
                            employmentSummary = "$emp (${doc.getString("jobTitle") ?: ""})"
                            incomeSummary = "Gross: €$inc / yr • Net: €${doc.getString("netMonthlyIncome") ?: "0"} / mo"
                            isSec2Complete = true
                        }

                        val rent = doc.getString("rentOrMortgage") ?: ""
                        if (rent.isNotBlank()) {
                            debtsSummary = "Rent/Mortgage: €$rent / mo • Loans: €${doc.getString("existingLoanRepayment") ?: "0"} / mo"
                            isSec3Complete = true
                        }

                        val dep = doc.getString("depositAmount") ?: ""
                        if (dep.isNotBlank()) {
                            depositSummary = "Deposit: €$dep (${doc.getString("depositSource") ?: "Savings"}) • Bank: ${doc.getString("primaryBank") ?: ""}"
                            isSec4Complete = true
                        }

                        val loanAmt = doc.getString("loanAmount") ?: doc.getString("requestedCreditLimit") ?: ""
                        if (loanAmt.isNotBlank()) {
                            loanSummary = "Amount: €$loanAmt • Term: ${doc.getString("loanTermYears") ?: "30"} yrs • Rate: ${doc.getString("rateType") ?: "Fixed"}"
                            isSec5Complete = true
                        }

                        val propVal = doc.getString("purchasePrice") ?: doc.getString("estimatedHomeValue") ?: ""
                        if (propVal.isNotBlank()) {
                            propertySummary = "Valuation: €$propVal • Address: ${doc.getString("propertyAddress") ?: ""}"
                            isSec6Complete = true
                        }
                    }
                }

            db.collection("users").document(uid).collection("documents").get()
                .addOnSuccessListener { snap ->
                    if (snap != null && !snap.isEmpty) {
                        val count = snap.documents.count { (it.getString("status") ?: "") == "UPLOADED" || (it.getString("status") ?: "") == "VERIFIED" }
                        docSummary = "$count of ${snap.size()} Documents Verified"
                    }
                }
        }
    }

    val allComplete = isSec1Complete && isSec2Complete && isSec3Complete && isSec4Complete && isSec5Complete && isSec6Complete

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubScreenHeader("Review Full Application", onNavigateBack)
        Text("Review all application details before final consent and submission. Tap Edit to fix any section.")

        if (!allComplete) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.WarningAmber, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "⚠️ Incomplete Application: Please complete all sections marked 'Action Required' before submitting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Section 1: Personal Details
        ReviewSectionCard(
            sectionTitle = "1. Personal & Household Details",
            isComplete = isSec1Complete,
            detailsText = "Name: $nameSummary\nEmail: $emailSummary\nPhone: $phoneSummary",
            onEditClick = onEditPersonalDetails
        )

        // Section 2: Employment & Income
        ReviewSectionCard(
            sectionTitle = "2. Employment & Income",
            isComplete = isSec2Complete,
            detailsText = "Employer: $employmentSummary\nIncome: $incomeSummary",
            onEditClick = onEditEmployment
        )

        // Section 3: Debts & Commitments
        ReviewSectionCard(
            sectionTitle = "3. Debts & Commitments",
            isComplete = isSec3Complete,
            detailsText = debtsSummary,
            onEditClick = onEditDebts
        )

        // Section 4: Deposit & Assets
        ReviewSectionCard(
            sectionTitle = "4. Deposit & Assets",
            isComplete = isSec4Complete,
            detailsText = depositSummary,
            onEditClick = onEditDeposit
        )

        // Section 5: Loan Request
        ReviewSectionCard(
            sectionTitle = "5. Loan Request",
            isComplete = isSec5Complete,
            detailsText = loanSummary,
            onEditClick = onEditLoanRequest
        )

        // Section 6: Property Details
        ReviewSectionCard(
            sectionTitle = "6. Property Details",
            isComplete = isSec6Complete,
            detailsText = propertySummary,
            onEditClick = onEditPropertyDetails
        )

        // Section 7: Document Upload Status
        ReviewSectionCard(
            sectionTitle = "7. Uploaded Documents",
            isComplete = true,
            detailsText = docSummary,
            onEditClick = onEditDocuments
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNavigateToConsent,
                modifier = Modifier.weight(1.5f),
                enabled = allComplete
            ) {
                Text("Proceed to Submit")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Helper card for review screen section summary with Edit button & status badge
@Composable
private fun ReviewSectionCard(
    sectionTitle: String,
    isComplete: Boolean,
    detailsText: String,
    onEditClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = sectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Surface(
                    color = if (isComplete) Color(0xFF43A047).copy(alpha = 0.15f) else Color(0xFFE53935).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isComplete) "Complete ✅" else "Action Required 🔴",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isComplete) Color(0xFF43A047) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = detailsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}
