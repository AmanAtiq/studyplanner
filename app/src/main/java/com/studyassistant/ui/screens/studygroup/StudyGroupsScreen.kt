package com.studyassistant.ui.screens.studygroup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.StudyGroup
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.viewmodel.StudyGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGroupsScreen(
    onBack: () -> Unit,
    onSelectGroup: (StudyGroup) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    viewModel: StudyGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var groupToDelete by remember { mutableStateOf<StudyGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Groups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreateGroup) {
                        Icon(Icons.Default.Add, contentDescription = "Create Group")
                    }
                }
            )
        }
    ) { padding ->
        ScreenBackground {
            if (groupToDelete != null) {
                AlertDialog(
                    onDismissRequest = { groupToDelete = null },
                    title = { Text("Delete Group") },
                    text = { Text("Are you sure you want to delete '${groupToDelete?.name}'? Members will still see old chats but won't be able to message.") },
                    confirmButton = {
                        TextButton(onClick = {
                            groupToDelete?.let { viewModel.leaveGroup(it.id) } // This handles the soft delete logic in VM
                            groupToDelete = null
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF8BD2))
                        }
                    }
                    uiState.userGroups.isEmpty() && uiState.publicGroups.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFFF8BD2).copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No study groups yet", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Create or join a study group to collaborate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToCreateGroup,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8BD2), contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Group")
                            }
                            Spacer(Modifier.height(16.dp))
                            JoinGroupPanel(
                                inviteInput = uiState.inviteInput,
                                passwordInput = uiState.passwordInput,
                                onInviteChange = viewModel::updateInviteInput,
                                onPasswordChange = viewModel::updatePasswordInput,
                                onJoin = { viewModel.joinGroup() }
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = onNavigateToCreateGroup,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8BD2), contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Create New Group")
                                    }

                                    JoinGroupPanel(
                                        inviteInput = uiState.inviteInput,
                                        passwordInput = uiState.passwordInput,
                                        onInviteChange = viewModel::updateInviteInput,
                                        onPasswordChange = viewModel::updatePasswordInput,
                                        onJoin = { viewModel.joinGroup() }
                                    )
                                }
                            }

                            if (uiState.userGroups.isNotEmpty()) {
                                item {
                                    Text(
                                        "Your Groups",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8BD2)
                                    )
                                }
                                items(uiState.userGroups) { group ->
                                    StudyGroupCard(
                                        group = group,
                                        isOwner = group.createdBy == viewModel.getCurrentUserId(),
                                        onClick = { onSelectGroup(group) },
                                        onCopyInvite = {
                                            copyToClipboard(context, group.inviteLink)
                                            Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        onDelete = { groupToDelete = group }
                                    )
                                }
                            }

                            if (uiState.publicGroups.isNotEmpty()) {
                                item {
                                    Text(
                                        "Explore Public Groups",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = Color(0xFFFFD166)
                                    )
                                }

                                items(uiState.publicGroups) { group ->
                                    PublicGroupCard(
                                        group = group,
                                        onJoin = { viewModel.joinPublicGroup(group) }
                                    )
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }

                // Error handling
                AnimatedVisibility(
                    visible = uiState.error != null,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearError) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Study Group Invite", text)
    clipboard.setPrimaryClip(clip)
}

@Composable
private fun JoinGroupPanel(
    inviteInput: String,
    passwordInput: String,
    onInviteChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD166).copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Color(0xFFFFD166).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Join with invite", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = inviteInput,
                onValueChange = onInviteChange,
                label = { Text("Invite link") },
                placeholder = { Text("study.group/...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
            )
            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                placeholder = { Text("Only needed for private groups") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
            )
            Button(
                onClick = onJoin,
                enabled = inviteInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD166), contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Join Group")
            }
        }
    }
}

@Composable
private fun PublicGroupCard(
    group: StudyGroup,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (group.topic.isNotBlank()) {
                    Text(group.topic, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                }
                Text(
                    group.description.ifBlank { "Public study group" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
            Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8E6CF), contentColor = Color.Black)) {
                Text("Join")
            }
        }
    }
}

@Composable
private fun StudyGroupCard(
    group: StudyGroup,
    isOwner: Boolean,
    onClick: () -> Unit,
    onCopyInvite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (group.isActive) Color.White else Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (isOwner) {
                            Surface(color = Color(0xFFFF8BD2).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("Owner", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC2185B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    if (group.topic.isNotEmpty()) {
                        Text(group.topic, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }
                if (isOwner && group.isActive) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                }
            }

            if (group.description.isNotEmpty()) {
                Text(group.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 2)
            }

            Surface(
                color = Color(0xFFFFD166).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(0.5.dp, Color(0xFFFFD166).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invite Link", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                        Text(group.inviteLink, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.DarkGray)
                    }
                    IconButton(onClick = onCopyInvite, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", modifier = Modifier.size(18.dp), tint = Color(0xFFE65100))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text("${group.memberCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }

                if (!group.isActive) {
                    Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("Deleted by Owner", style = MaterialTheme.typography.labelSmall, color = Color.Red, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}
