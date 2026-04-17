package com.studyassistant.ui.screens.quizzes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.ui.components.QuizCard
import com.studyassistant.viewmodel.QuizzesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesListScreen(
    onBack: () -> Unit,
    onOpenQuiz: (String, Boolean) -> Unit,
    viewModel: QuizzesViewModel = hiltViewModel()
) {
    val quizzes = viewModel.quizzes.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (quizzes.isEmpty()) {
                item {
                    Text("No completed quiz attempts yet. Take a quiz to see results here.", modifier = Modifier.padding(8.dp))
                }
            }
            items(quizzes) { quiz ->
                QuizCard(
                    quiz = quiz,
                    onOpenAttempt = { onOpenQuiz(quiz.noteId, false) },
                    onTakeQuiz = { onOpenQuiz(quiz.noteId, true) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
