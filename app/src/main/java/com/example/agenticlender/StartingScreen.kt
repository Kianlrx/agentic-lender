package com.example.agenticlender

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun LandingScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Agentic Lender Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Agentic Lender",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The first step in your home journey.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Apply, track your progress, check a property's fair price and speak with your broker in one secure app.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Create an Account")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "I already have an account")
        }
    }
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToResetPassword: () -> Unit = {},
    onNavigateToVerifyEmail: () -> Unit = {}
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isDuplicateEmail by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Borrower Account",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Get started with your loan application",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; isDuplicateEmail = false },
                label = { Text("First Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it; isDuplicateEmail = false },
                label = { Text("Last Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; isDuplicateEmail = false },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; isDuplicateEmail = false },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; isDuplicateEmail = false },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isDuplicateEmail) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign In")
                }
                OutlinedButton(
                    onClick = onNavigateToResetPassword,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Password")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    errorMessage = "Please fill in all mandatory fields."
                    isDuplicateEmail = false
                    return@Button
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    errorMessage = "Please enter a valid email address."
                    isDuplicateEmail = false
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters long."
                    isDuplicateEmail = false
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Password and confirm password do not match."
                    isDuplicateEmail = false
                    return@Button
                }

                isLoading = true
                errorMessage = null
                isDuplicateEmail = false

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            // user?.sendEmailVerification() // Commented out for  testing

                            val userId = user?.uid ?: ""
                            val userMap = hashMapOf(
                                "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(),
                                "fullName" to "${firstName.trim()} ${lastName.trim()}",
                                "email" to email.trim(),
                                "role" to "borrower",
                                "isEmailVerified" to true,
                                "createdAt" to Timestamp.now()
                            )
                            if (userId.isNotEmpty()) {
                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .set(userMap)
                            }
                            isLoading = false
                            onNavigateToVerifyEmail()
                        } else {
                            isLoading = false
                            val ex = task.exception
                            if (ex is FirebaseAuthUserCollisionException) {
                                errorMessage = "An account with this email address already exists. Would you like to log in or reset your password?"
                                isDuplicateEmail = true
                            } else {
                                errorMessage = ex?.localizedMessage ?: "Registration failed."
                            }
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Create Account")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(text = "Already registered? Sign in")
        }
    }
}

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = "Verify Email",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Verify Your Email Address",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A verification email has been sent to:\n${user?.email ?: "your registered email"}",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please select the verification link in your email to activate your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (message != null) {
            Text(
                text = message!!,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                isLoading = true
                message = null
                errorMessage = null

                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    isLoading = false
                    val refreshedUser = auth.currentUser
                    if (refreshedUser != null && refreshedUser.isEmailVerified) {
                        refreshedUser.uid.let { uid ->
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update("isEmailVerified", true)
                        }
                        onVerified()
                    } else {
                        errorMessage = "Email is not verified yet. Please click the link in your email and try again."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "I Have Verified My Email")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                isLoading = true
                message = null
                errorMessage = null

                auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        message = "A new verification email has been sent!"
                    } else {
                        errorMessage = task.exception?.localizedMessage ?: "Failed to resend email."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(text = "Resend Verification Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text(text = "Back to Sign In")
        }
    }
}

@Composable
fun SelectApplicationTypeScreen(
    onTypeSelected: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val savedType = doc.getString("applicationType")
                        if (!savedType.isNullOrBlank()) {
                            selectedType = savedType
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Loan Type",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please choose the application option you wish to apply for to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Mortgage
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedType == "Mortgage") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (selectedType == "Mortgage") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            onClick = {
                selectedType = "Mortgage"
                errorMessage = null
            }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🏡  Mortgage Loan",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selectedType == "Mortgage") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "For purchasing a new home, moving home, or refinancing an existing mortgage.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedType == "HELOC") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (selectedType == "HELOC") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            onClick = {
                selectedType = "HELOC"
                errorMessage = null
            }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🏦  Home Equity Line of Credit (HELOC)",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selectedType == "HELOC") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A revolving line of credit secured against the equity in your home.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedType == null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Prompt",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Please select a loan type above (Mortgage or HELOC) to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (selectedType == null) {
                    errorMessage = "Please select a loan type above to continue."
                    return@Button
                }

                val user = FirebaseAuth.getInstance().currentUser
                val userId = user?.uid
                if (userId != null) {
                    val uniqueAppId = "APP-${System.currentTimeMillis()}"
                    val now = Timestamp.now()

                    val appRecord = hashMapOf(
                        "applicationId" to uniqueAppId,
                        "borrowerUid" to userId,
                        "borrowerEmail" to (user.email ?: ""),
                        "applicationType" to selectedType,
                        "status" to "Draft",
                        "createdAt" to now,
                        "updatedAt" to now
                    )

                    val userRecord = hashMapOf(
                        "activeApplicationId" to uniqueAppId,
                        "applicationType" to selectedType,
                        "applicationStatus" to "Draft",
                        "applicationTypeSelectedAt" to now
                    )

                    val db = FirebaseFirestore.getInstance()
                    db.collection("applications").document(uniqueAppId).set(appRecord, SetOptions.merge())
                    db.collection("applications").document(userId).set(appRecord, SetOptions.merge())
                    db.collection("users").document(userId).set(userRecord, SetOptions.merge())
                }
                onTypeSelected()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedType != null
        ) {
            Text(text = "Next: Begin Application Form")
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToResetPassword: () -> Unit = {},
    onNavigateToVerifyEmail: () -> Unit = {},
    onNavigateToSelectApplicationType: () -> Unit = {},
    onNavigateToSignIn: (String) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to continue to your account",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToResetPassword,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Forgot password?")
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter both email address and password."
                    return@Button
                }
                isLoading = true
                errorMessage = null

                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val uid = user?.uid ?: ""

                            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    isLoading = false
                                    val appType = doc.getString("applicationType")
                                    if (appType.isNullOrBlank()) {
                                        onNavigateToSelectApplicationType()
                                    } else {
                                        val role = doc.getString("role") ?: "borrower"
                                        onNavigateToSignIn(role)
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    onNavigateToSignIn("borrower")
                                }
                        } else {
                            isLoading = false
                            errorMessage = task.exception?.localizedMessage ?: "Invalid email or password."
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Sign in Securely")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text(text = "New to Agentic Lender? Create an account")
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter your registered email address below. We will send you a link to reset your password.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (message != null) {
            Text(
                text = message!!,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    errorMessage = "Please enter a valid registered email address."
                    message = null
                    return@Button
                }

                isLoading = true
                errorMessage = null
                message = null

                FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            message = "Password reset link sent! Check your inbox to complete reset."
                        } else {
                            errorMessage = task.exception?.localizedMessage ?: "Failed to send password reset email."
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Send Password Reset Link")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(text = "Back to Sign In")
        }
    }
}
