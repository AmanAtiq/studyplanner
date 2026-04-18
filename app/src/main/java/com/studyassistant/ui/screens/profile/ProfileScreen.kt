/**
 * Profile Screen Module
 *
 * Provides comprehensive user profile management including:
 * - View/Edit profile information (name, bio, email)
 * - Profile picture upload and management
 * - Quiz statistics and performance analytics
 * - Weak area detection and improvement suggestions
 * - User preferences management (exam, language)
 * - Sign out functionality
 *
 * @author Study Assistant Team
 * @version 1.0
 */

package com.studyassistant.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.studyassistant.ui.components.*
import com.studyassistant.ui.theme.*
import com.studyassistant.util.Constants
import com.studyassistant.viewmodel.ProfileViewModel

/**
 * Profile Screen Composable
 *
 * Main screen for user profile management with dual-mode UI:
 * - View Mode: Display profile info, stats, and weak areas
 * - Edit Mode: Edit profile details and upload profile picture
 *
 * @param onBack Callback when back button is pressed
 * @param onSignOut Callback when user signs out
 * @param viewModel ProfileViewModel instance (injected via Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: android.net.Uri? ->
            uri?.let {
                viewModel.uploadProfilePhoto(it.toString())
            }
        }
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut(); onSignOut() }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isEditMode) {
                        IconButton(onClick = { viewModel.enableEditMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        ScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feedback messages
                uiState.successMessage?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StrongAreaGreen.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = StrongAreaGreen,
                                modifier = Modifier.size(20.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = StrongAreaGreen)
                        }
                    }
                }
                uiState.error?.let { ErrorBanner(message = it, onDismiss = viewModel::clearMessages) }

                if (uiState.isEditMode) {
                    // Edit Mode UI
                    EditProfileSection(
                        uiState = uiState,
                        onNameChange = { viewModel.updateEditName(it) },
                        onBioChange = { viewModel.updateEditBio(it) },
                        onPhotoClick = { imageLauncher.launch("image/*") },
                        onSave = { viewModel.saveProfileEdits() },
                        onCancel = { viewModel.disableEditMode() },
                        user = uiState.user
                    )
                } else {
                    // View Mode UI
                    ProfileAvatarSection(
                        name = uiState.user?.name ?: "Student",
                        email = uiState.user?.email ?: "",
                        photoUrl = uiState.user?.photoUrl
                    )

                    StatsSection(
                        quizCount = uiState.quizHistory.count { it.completed },
                        avgScore = viewModel.averageScore()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Weak Areas Detection Section
                    val weakAreas = viewModel.detectWeakAreas()
                    if (weakAreas.isNotEmpty()) {
                        WeakAreasSection(weakAreas = weakAreas)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }

                    SectionTitle("Target Exam")
                    ExamSelector(
                        selected = uiState.selectedExam,
                        onSelect = viewModel::onExamChange
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::saveProfile,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Preferences", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarSection(name: String, email: String, photoUrl: String? = null) {
    /**
     * Profile Avatar Section
     * Displays user's profile picture/initial, name, and email
     */
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column {
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(email, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun StatsSection(quizCount: Int, avgScore: Int) {
    /**
     * Statistics Display Section
     * Shows user's quiz completion count and average score in visual cards
     */
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Completed Quizzes",
            value = "$quizCount",
            icon = Icons.Default.Quiz,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Avg Score",
            value = "$avgScore%",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            tint = if (avgScore >= 70) StrongAreaGreen else WarningAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Suppress("SameParameterValue")
@Composable
private fun SectionTitle(title: String) {
    /**
     * Section Title Component
     * Displays a styled section header with consistent formatting
     */
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Composable
private fun EditProfileSection(
    uiState: com.studyassistant.viewmodel.ProfileUiState,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    user: com.studyassistant.domain.model.User?
) {
    /**
     * Edit Profile Section
     * Provides UI for editing user profile including:
     * - Profile picture upload
     * - Name and bio editing
     * - Form validation and submission
     */
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture with Upload Button
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = !uiState.isUploadingPhoto) { onPhotoClick() },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.selectedPhotoUri.isNotEmpty()) {
                AsyncImage(
                    model = uiState.selectedPhotoUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (user?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.isUploadingPhoto) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Upload Photo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = if (uiState.isUploadingPhoto) "Uploading..." else "Tap to Change Photo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Name Field
        OutlinedTextField(
            value = uiState.editName,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        // Bio Field
        OutlinedTextField(
            value = uiState.editBio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5,
            supportingText = {
                Text("${uiState.editBio.length}/500", style = MaterialTheme.typography.labelSmall)
            }
        )

        // Email (Read-only)
        OutlinedTextField(
            value = user?.email ?: "",
            onValueChange = {},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = false,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Save & Cancel Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamSelector(selected: String, onSelect: (String) -> Unit) {
    /**
     * Exam Selector Component
     * Displays available exam options as filter chips for user selection
     */
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Constants.SUPPORTED_EXAMS.forEach { exam ->
            FilterChip(
                selected = selected == exam,
                onClick = { onSelect(exam) },
                label = { Text(exam) },
                leadingIcon = if (selected == exam) {
                    { Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun WeakAreasSection(weakAreas: List<com.studyassistant.domain.model.WeakArea>) {
    /**
     * Weak Areas Display Section
     * Shows detected weak areas with accuracy levels and improvement suggestions
     * Only displayed if weak areas are detected (< 70% accuracy)
     */
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with warning icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = "Weak Areas",
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Areas to Improve (${weakAreas.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WarningAmber
            )
        }

        // Weak areas cards
        weakAreas.forEach { weakArea ->
            WeakAreaCard(weakArea = weakArea)
        }
    }
}

@Composable
private fun WeakAreaCard(weakArea: com.studyassistant.domain.model.WeakArea) {
    /**
     * Weak Area Card Component
     * Displays individual weak area with:
     * - Topic name and accuracy percentage
     * - Visual progress indicator
     * - Attempt count and suggestions
     * - Color-coded severity (red/orange/pink)
     */
    val accuracyPercentage = (weakArea.accuracy * 100).toInt()
    val color = when {
        accuracyPercentage < 40 -> WeakAreaRed
        accuracyPercentage < 60 -> WarningAmber
        else -> Color(0xFFB7A1E2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Topic header with accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weakArea.topic.replace(Regex("[0-9-]+"), "").trim().ifBlank { "General Topic" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Accuracy percentage badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "$accuracyPercentage%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { weakArea.accuracy },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            // Attempts count
            Text(
                text = "Attempted ${weakArea.totalAttempts} time${if (weakArea.totalAttempts != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            // Suggestions
            if (weakArea.suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    weakArea.suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
