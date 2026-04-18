package com.studyassistant.ui.screens.quizzes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studyassistant.ui.adapter.QuizAdapter
import com.studyassistant.viewmodel.QuizzesViewModel
import androidx.compose.foundation.layout.*
import com.studyassistant.ui.components.ScreenBackground

/**
 * QuizzesListScreen using RecyclerView (instead of LazyColumn)
 * This demonstrates the integration of traditional Android RecyclerView
 * with Jetpack Compose
 *
 * WHERE RECYCLERVIEW IS USED:
 * Location: QuizzesListScreenWithRecyclerView - Quiz History Screen
 *
 * This screen uses:
 * - RecyclerView (from androidx.recyclerview.widget)
 * - QuizAdapter (custom adapter at ui/adapter/QuizAdapter.kt)
 * - item_quiz_card.xml (layout at res/layout/item_quiz_card.xml)
 * - DiffUtil for efficient updates
 * - LinearLayoutManager for vertical scrolling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesListScreenWithRecyclerView(
    onBack: () -> Unit,
    onOpenQuiz: (String, Boolean) -> Unit,
    viewModel: QuizzesViewModel = hiltViewModel()
) {
    val quizzes = viewModel.quizzes.collectAsState().value

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quiz History",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        ScreenBackground {
            // Embed RecyclerView inside Compose
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                factory = { context ->
                    RecyclerView(context).apply {
                        layoutManager = LinearLayoutManager(context)

                        // Create and set adapter
                        adapter = QuizAdapter(
                            onOpenAttempt = { quiz ->
                                onOpenQuiz(quiz.noteId, false)
                            },
                            onTakeQuiz = { quiz ->
                                onOpenQuiz(quiz.noteId, true)
                            }
                        )
                    }
                },
                update = { recyclerView ->
                    // Update the adapter data when quizzes change
                    (recyclerView.adapter as? QuizAdapter)?.submitList(quizzes)
                }
            )
        }
    }
}
