package com.studyassistant.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.friendlyNoteTitle
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onBack: () -> Unit,
    onTakeQuiz: (String) -> Unit,
    onFlashcards: (String) -> Unit,
    onAskAI: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val note = state.allNotes.firstOrNull { it.id == noteId }
        ?: state.notes.firstOrNull { it.id == noteId }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share button
                    if (note != null) {
                        IconButton(onClick = {
                            val shareText = buildString {
                                append("📚 ${friendlyNoteTitle(note)}\n\n")
                                if (note.summary.isNotBlank()) append("Summary:\n${note.summary}\n\n")
                                if (note.originalContent.isNotBlank()) append("Notes:\n${note.originalContent.take(800)}")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, friendlyNoteTitle(note))
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Note"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        ScreenBackground {
            if (note == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Text("Note not found.", modifier = Modifier.padding(24.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary card
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                friendlyNoteTitle(note),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (note.summary.isNotBlank()) {
                                Text("Summary", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(note.summary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Original content card
                    if (note.originalContent.isNotBlank()) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Original Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(note.originalContent, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Attachment card
                    if (note.fileUrl.isNotBlank()) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Attachment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(note.fileUrl, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Action buttons grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Primary: Ask AI (full width)
                        NoteActionButton(
                            label = "Ask AI About This Note",
                            icon = Icons.Default.SmartToy,
                            color = Color(0xFFB7A1E2),
                            onClick = { onAskAI(note.id) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary row: Quiz + Flashcards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NoteActionButton(
                                label = "Take Quiz",
                                icon = Icons.Default.Quiz,
                                color = Color(0xFF87CEFA),
                                onClick = { onTakeQuiz(note.id) },
                                modifier = Modifier.weight(1f)
                            )
                            NoteActionButton(
                                label = "Flashcards",
                                icon = Icons.Default.Style,
                                color = Color(0xFFFF8BD2),
                                onClick = { onFlashcards(note.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NoteActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.Black)
    }
}
