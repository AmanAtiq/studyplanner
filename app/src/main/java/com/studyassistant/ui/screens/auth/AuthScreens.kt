package com.studyassistant.ui.screens.auth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.util.Constants
import com.studyassistant.viewmodel.AuthViewModel

// ─── Google Color Palette ─────────────────────────────────────────────────────

private val GoogleBlue    = Color(0xFF4285F4)
private val GoogleRed     = Color(0xFFEA4335)
private val GoogleYellow  = Color(0xFFFBBC05)
private val GoogleGreen   = Color(0xFF34A853)

private val BlueDark      = Color(0xFF1A73E8)
private val BlueLight     = Color(0xFFE8F0FE)
private val BlueMid       = Color(0xFFD2E3FC)

private val SurfaceWhite  = Color(0xFFFFFFFF)
private val BackgroundGray= Color(0xFFF8F9FA)
private val CardGray      = Color(0xFFF1F3F4)
private val BorderGray    = Color(0xFFDADCE0)
private val BorderFocus   = Color(0xFF4285F4)

private val TextPrimary   = Color(0xFF202124)
private val TextSecondary = Color(0xFF5F6368)
private val TextHint      = Color(0xFF9AA0A6)

private val ErrorColor    = Color(0xFFEA4335)
private val SuccessColor  = Color(0xFF34A853)
private val WarningColor  = Color(0xFFFBBC05)

private val GradientBlue = Brush.linearGradient(
    colors = listOf(GoogleBlue, BlueDark),
    start = Offset(0f, 0f),
    end = Offset(400f, 100f)
)

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun TopColorBar() {
    Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleBlue))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleRed))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleYellow))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleGreen))
    }
}

@Composable
private fun BrandHeader(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Google-colored logo dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(GoogleBlue, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(GoogleRed, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(GoogleBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(GoogleYellow, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(GoogleGreen, CircleShape)
            )
        }

        Text(
            text = "StudyAI",
            color = TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = GoogleBlue,
            focusedBorderColor = GoogleBlue,
            unfocusedBorderColor = BorderGray,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedLeadingIconColor = GoogleBlue,
            unfocusedLeadingIconColor = TextHint,
            focusedTrailingIconColor = GoogleBlue,
            unfocusedTrailingIconColor = TextHint,
            focusedLabelColor = GoogleBlue,
            unfocusedLabelColor = TextSecondary,
        )
    )
}

@Composable
private fun PrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    text: String
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoogleBlue,
            contentColor = Color.White,
            disabledContainerColor = BorderGray,
            disabledContentColor = TextHint
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, BorderGray),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = TextPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, BorderGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "G",
                color = GoogleBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Continue with Google",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
    // ← NOTHING HERE. Function ends at the closing brace above.
}

@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
        Text(
            "  or  ",
            color = TextHint,
            fontSize = 13.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCE8E6), RoundedCornerShape(10.dp))
            .border(1.dp, ErrorColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = ErrorColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(message, color = ErrorColor, fontSize = 13.sp)
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = when {
        password.length >= 10 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3
        password.length >= 7 -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    val strengthLabel = listOf("", "Weak", "Fair", "Strong")[strength]
    val strengthColor = listOf(Color.Transparent, ErrorColor, WarningColor, SuccessColor)[strength]

    if (password.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < strength) strengthColor else BorderGray)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                strengthLabel,
                color = strengthColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Step $currentStep of $totalSteps",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                "${((currentStep.toFloat() / totalSteps) * 100).toInt()}% complete",
                color = GoogleBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = GoogleBlue,
            trackColor = BlueLight,
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(15.dp))
        }
        Text(text, color = TextSecondary, fontSize = 13.sp)
    }
}

// ─── Sign In Screen ───────────────────────────────────────────────────────────

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Google color bar
            TopColorBar()

            Spacer(Modifier.height(40.dp))

            // Brand
            BrandHeader(subtitle = "Your AI-powered study companion")

            Spacer(Modifier.height(32.dp))

            // ── Main Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BlueLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Login,
                                null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Sign In",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Welcome back!",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = CardGray)

                    // Email field
                    StyledTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Email, null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // Password field
                    StyledTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Lock, null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Remember me + Forgot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GoogleBlue,
                                    uncheckedColor = BorderGray,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Remember me", color = TextSecondary, fontSize = 13.sp)
                        }
                        TextButton(onClick = { /* forgot password */ }) {
                            Text(
                                "Forgot password?",
                                color = GoogleBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Error banner
                    state.error?.let { ErrorBanner(it) }

                    // Sign In button
                    PrimaryButton(
                        onClick = { viewModel.signIn(email, password) },
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        isLoading = state.isLoading,
                        text = "Sign In"
                    )

                    OrDivider()

                    GoogleSignInButton(onClick = { viewModel.signInWithGoogle(context) })
                }
            }

            Spacer(Modifier.height(24.dp))

            // Trust badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrustBadge(
                    icon = Icons.Filled.People,
                    color = GoogleBlue,
                    label = "50K+ Students",
                    modifier = Modifier.weight(1f)
                )
                TrustBadge(
                    icon = Icons.Filled.Star,
                    color = GoogleYellow,
                    label = "4.9 Rating",
                    modifier = Modifier.weight(1f)
                )
                TrustBadge(
                    icon = Icons.Filled.School,
                    color = GoogleGreen,
                    label = "20+ Exams",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Sign up link
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = TextSecondary, fontSize = 14.sp)
                TextButton(onClick = onNavigateToSignUp) {
                    Text(
                        "Sign up",
                        color = GoogleBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Bottom safe zone color bar
            Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleBlue))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleRed))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleYellow))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleGreen))
            }
        }
    }
}

@Composable
private fun TrustBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

// ─── Sign Up Screen ───────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    onSignedUp: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }

    val exams = Constants.SUPPORTED_EXAMS
    var expanded by remember { mutableStateOf(false) }
    var selectedExam by remember { mutableStateOf(exams.firstOrNull() ?: "Other") }

    val currentStep = when {
        name.isBlank() -> 1
        email.isBlank() -> 2
        password.isBlank() -> 3
        else -> 4
    }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedUp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopColorBar()

            Spacer(Modifier.height(32.dp))

            BrandHeader(subtitle = "Join thousands of students acing their exams")

            Spacer(Modifier.height(24.dp))

            // Progress bar
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                StepProgressBar(currentStep = currentStep, totalSteps = 4)
            }

            Spacer(Modifier.height(16.dp))

            // ── Main Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE6F4EA), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                null,
                                tint = GoogleGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Create Account",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "It's free and takes less than a minute",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = CardGray)

                    // Full Name
                    StyledTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    // Email
                    StyledTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, null, modifier = Modifier.size(20.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // Password
                    StyledTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Password strength
                    if (password.isNotEmpty()) {
                        PasswordStrengthIndicator(password)
                    }

                    // Exam Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedExam,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Exam", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.MenuBook, null, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = GoogleBlue,
                                focusedBorderColor = GoogleBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                focusedLeadingIconColor = GoogleBlue,
                                unfocusedLeadingIconColor = TextHint,
                                focusedTrailingIconColor = GoogleBlue,
                                unfocusedTrailingIconColor = TextHint,
                                focusedLabelColor = GoogleBlue,
                                unfocusedLabelColor = TextSecondary,
                            )
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceWhite, RoundedCornerShape(12.dp))
                        ) {
                            exams.forEach { exam ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.Grade,
                                                null,
                                                tint = if (exam == selectedExam) GoogleBlue else TextHint,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                exam,
                                                color = if (exam == selectedExam) GoogleBlue else TextPrimary,
                                                fontWeight = if (exam == selectedExam) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = { selectedExam = exam; expanded = false },
                                    modifier = Modifier.background(
                                        if (exam == selectedExam) BlueLight else Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // Terms checkbox
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (agreed) GoogleBlue.copy(alpha = 0.4f) else BorderGray,
                                RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { agreed = !agreed },
                        color = if (agreed) BlueLight else CardGray
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GoogleBlue,
                                    uncheckedColor = BorderGray,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    "I agree to the Terms & Conditions",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "and Privacy Policy",
                                    color = GoogleBlue,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Error
                    state.error?.let { ErrorBanner(it) }

                    // Sign Up button
                    PrimaryButton(
                        onClick = { viewModel.signUp(name, email, password) },
                        enabled = agreed && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                        isLoading = state.isLoading,
                        text = "Create Account"
                    )

                    OrDivider()

                    GoogleSignInButton(onClick = { /* google sign up */ })
                }
            }

            Spacer(Modifier.height(20.dp))

            // Feature highlights
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureCard(
                    icon = Icons.Filled.FlashOn,
                    color = GoogleYellow,
                    bg = Color(0xFFFEF7E0),
                    title = "AI Tutor",
                    subtitle = "Instant help",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Filled.BarChart,
                    color = GoogleBlue,
                    bg = BlueLight,
                    title = "Progress",
                    subtitle = "Track growth",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Filled.EmojiEvents,
                    color = GoogleGreen,
                    bg = Color(0xFFE6F4EA),
                    title = "Streaks",
                    subtitle = "Stay on track",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // What you'll get section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "What you'll get",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    InfoRow(Icons.Filled.CheckCircle, GoogleGreen, "Personalized AI study plans")
                    InfoRow(Icons.Filled.CheckCircle, GoogleGreen, "10,000+ practice questions")
                    InfoRow(Icons.Filled.CheckCircle, GoogleGreen, "Real-time performance analytics")
                    InfoRow(Icons.Filled.CheckCircle, GoogleGreen, "Offline access to study material")
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = TextSecondary, fontSize = 14.sp)
                TextButton(onClick = onNavigateToSignIn) {
                    Text(
                        "Sign in",
                        color = GoogleBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleBlue))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleRed))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleYellow))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoogleGreen))
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bg: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}