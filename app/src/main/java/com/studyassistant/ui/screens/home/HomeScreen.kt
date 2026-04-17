package com.studyassistant.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.*
import com.studyassistant.ui.components.*
import com.studyassistant.ui.theme.*
import com.studyassistant.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.currentUser?.name?.let { name ->
                            Text(
                                text = "Welcome, $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToQuizzes) {
                        Icon(Icons.Default.List, contentDescription = "Quizzes", modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile",
                            modifier = Modifier.size(30.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToUpload,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Error banner
            if (uiState.error != null) {
                item {
                    ErrorBanner(message = uiState.error!!, onDismiss = viewModel::clearError)
                }
            }

            // Quick action cards
            item {
                QuickActionsRow(
                    onUpload = onNavigateToUpload,
                    onPlanner = onNavigateToPlanner
                )
            }

            // Weak areas section
            if (uiState.weakAreas.isNotEmpty()) {
                item {
                    WeakAreasSection(
                        weakAreas = uiState.weakAreas
                    )
                }
            }

            // Notes section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Notes (${uiState.notes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Loading
            if (uiState.isLoading) {
                item { LoadingIndicator(message = "Loading notes...") }
            }

            // Empty state
            if (!uiState.isLoading && uiState.notes.isEmpty()) {
                item { EmptyNotesState(onAdd = onNavigateToUpload) }
            }

            // Notes list
            items(uiState.notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onTap = { onNavigateToNoteDetail(note.id) },
                    onDelete = { viewModel.deleteNote(note.id) },
                    onQuiz = { onNavigateToQuiz(note.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onUpload: () -> Unit,
    onPlanner: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            title = "Upload Note",
            subtitle = "AI Summary",
            icon = Icons.Default.Upload,
            gradient = Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3B82F6))),
            onClick = onUpload,
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            title = "Study Planner",
            subtitle = "AI Plan",
            icon = Icons.Default.CalendarMonth,
            gradient = Brush.linearGradient(listOf(Color(0xFF1DB954), Color(0xFF0F6E56))),
            onClick = onPlanner,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column {
                Icon(icon, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(26.dp))
                Spacer(Modifier.weight(1f))
                Text(title, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun WeakAreasSection(weakAreas: List<WeakArea>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Weak Areas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(weakAreas) { area ->
                WeakAreaChip(weakArea = area)
            }
        }
    }
}

@Composable
private fun EmptyNotesState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = "No notes yet!\nAdd your first note to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Note")
        }
    }
}