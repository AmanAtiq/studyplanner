package com.studyassistant.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.viewmodel.HomeViewModel
import com.studyassistant.domain.model.friendlyNoteTitle
import com.studyassistant.ui.components.ScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onBack: () -> Unit,
    onTakeQuiz: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val note = state.notes.firstOrNull { it.id == noteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lecture Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ScreenBackground {
            if (note == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
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
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val displayTitle = friendlyNoteTitle(note)
                            Text(displayTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (note.summary.isNotBlank()) note.summary else "Summary will appear here after upload.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Original Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(note.originalContent.ifBlank { "No text content available." }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (note.fileUrl.isNotBlank()) {
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Attachment saved locally", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(note.fileUrl, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Button(
                        onClick = { onTakeQuiz(note.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Quiz", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}