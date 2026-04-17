package com.studyassistant.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.*
import com.studyassistant.ui.components.*
import com.studyassistant.ui.theme.*
import com.studyassistant.viewmodel.QuizViewModel

// ── Quiz Screen ───────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    noteId: String,
    forceRefresh: Boolean = false,
    onBack: () -> Unit,
    onFinish: (score: Int, total: Int) -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.quiz
    val isReviewMode = quiz?.completed == true && uiState.isReviewMode

    LaunchedEffect(noteId, forceRefresh) { viewModel.loadQuiz(noteId, forceRefresh) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            val total = uiState.quiz?.questions?.size ?: 0
            onFinish(uiState.score, total)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val total = quiz?.questions?.size ?: 0
                    val current = uiState.currentQuestionIndex + 1
                    Text(
                        if (isReviewMode) {
                            if (uiState.language == AppLanguage.URDU) "مکمل کوئز" else "Quiz Review"
                        } else {
                            if (uiState.language == AppLanguage.URDU)
                                "سوال $current / $total" else "Question $current / $total"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit quiz")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    LoadingIndicator(
                        if (uiState.language == AppLanguage.URDU)
                            "AI سوالات بنا رہا ہے..." else "AI is generating your quiz..."
                    )
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(uiState.error!!, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error)
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
            }
            quiz != null -> {
                if (isReviewMode) {
                    QuizReviewContent(
                        quiz = quiz,
                        onBack = onBack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    val question = quiz.questions.getOrNull(uiState.currentQuestionIndex)
                    if (question != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress bar
                        val progress = (uiState.currentQuestionIndex + 1).toFloat() / quiz.questions.size
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                            strokeCap = StrokeCap.Round
                        )

                        // Score chip
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "✓ ${uiState.score}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Question card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Q${uiState.currentQuestionIndex + 1}.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = question.question,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Answer options
                        question.options.forEachIndexed { index, option ->
                            AnswerOptionButton(
                                text = option,
                                index = index,
                                selectedIndex = uiState.selectedAnswerIndex,
                                correctIndex = if (uiState.isAnswerRevealed) question.correctAnswerIndex else -1,
                                isRevealed = uiState.isAnswerRevealed,
                                onClick = { viewModel.selectAnswer(index) }
                            )
                        }

                        // Explanation
                        AnimatedVisibility(
                            visible = uiState.isAnswerRevealed && question.explanation.isNotEmpty()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp))
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Action button
                        Button(
                            onClick = {
                                if (!uiState.isAnswerRevealed) viewModel.revealAnswer()
                                else viewModel.nextQuestion()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = uiState.selectedAnswerIndex != -1
                        ) {
                            Text(
                                text = when {
                                    !uiState.isAnswerRevealed ->
                                        if (uiState.language == AppLanguage.URDU)
                                            "جواب چیک کریں" else "Check Answer"
                                    uiState.currentQuestionIndex + 1 >= (uiState.quiz?.questions?.size ?: 0) ->
                                        if (uiState.language == AppLanguage.URDU) "نتیجہ دیکھیں" else "See Results"
                                    else ->
                                        if (uiState.language == AppLanguage.URDU) "اگلا سوال" else "Next Question"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
private fun QuizReviewContent(
    quiz: Quiz,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = quiz.questions.size
    val percentage = if (total > 0) (quiz.score * 100) / total else 0
    val scoreColor = when {
        percentage >= 70 -> StrongAreaGreen
        percentage >= 50 -> WarningAmber
        else -> WeakAreaRed
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Completed Attempt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${quiz.score}/$total correct", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scoreColor)
                Text("${percentage}% score", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Correct",
                value = "${quiz.score}",
                icon = Icons.Default.CheckCircle,
                tint = StrongAreaGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Wrong",
                value = "${total - quiz.score}",
                icon = Icons.Default.Cancel,
                tint = WeakAreaRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total",
                value = "$total",
                icon = Icons.Default.Quiz,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Question Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        quiz.questions.forEachIndexed { index, question ->
            QuizReviewQuestionCard(questionNumber = index + 1, question = question)
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back")
        }
    }
}

@Composable
private fun QuizReviewQuestionCard(
    questionNumber: Int,
    question: QuizQuestion
) {
    val isCorrect = question.selectedAnswerIndex == question.correctAnswerIndex
    val selectedText = question.options.getOrNull(question.selectedAnswerIndex)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Q$questionNumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isCorrect) StrongAreaGreen.copy(alpha = 0.15f) else WeakAreaRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCorrect) "Correct" else "Wrong",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCorrect) StrongAreaGreen else WeakAreaRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(question.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

            question.options.forEachIndexed { index, option ->
                val optionColor = when {
                    index == question.correctAnswerIndex -> StrongAreaGreen.copy(alpha = 0.14f)
                    index == question.selectedAnswerIndex && !isCorrect -> WeakAreaRed.copy(alpha = 0.14f)
                    index == question.selectedAnswerIndex -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                val borderColor = when {
                    index == question.correctAnswerIndex -> StrongAreaGreen
                    index == question.selectedAnswerIndex && !isCorrect -> WeakAreaRed
                    index == question.selectedAnswerIndex -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = optionColor,
                    border = BorderStroke(1.2.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = listOf("A", "B", "C", "D").getOrElse(index) { "?" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = borderColor
                            )
                        }
                        Text(option, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }

            val selectedLabel = selectedText ?: "Not answered"
            Text(
                text = "Your answer: $selectedLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            question.explanation.takeIf { it.isNotBlank() }?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionButton(
    text: String,
    index: Int,
    selectedIndex: Int,
    correctIndex: Int,
    isRevealed: Boolean,
    onClick: () -> Unit
) {
    val optionLabels = listOf("A", "B", "C", "D")
    val isSelected = index == selectedIndex
    val isCorrect = index == correctIndex && isRevealed
    val isWrong = isSelected && isRevealed && index != correctIndex

    val containerColor = when {
        isCorrect -> StrongAreaGreen.copy(alpha = 0.15f)
        isWrong -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isCorrect -> StrongAreaGreen
        isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.5.dp, borderColor),
        enabled = !isRevealed
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCorrect -> Icon(Icons.Default.Check, contentDescription = null,
                        tint = StrongAreaGreen, modifier = Modifier.size(18.dp))
                    isWrong -> Icon(Icons.Default.Close, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    else -> Text(optionLabels.getOrElse(index) { "?" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = borderColor)
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Quiz Result Screen ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    score: Int,
    total: Int,
    onBack: () -> Unit
) {
    val percentage = if (total > 0) (score * 100) / total else 0
    val grade = when {
        percentage >= 90 -> "A+"
        percentage >= 80 -> "A"
        percentage >= 70 -> "B"
        percentage >= 60 -> "C"
        percentage >= 50 -> "D"
        else -> "F"
    }
    val message = when {
        percentage >= 80 -> "Excellent! 🎉"
        percentage >= 60 -> "Good job! Keep it up 👍"
        percentage >= 40 -> "Keep studying! 📚"
        else -> "Don't give up! Review your notes 💪"
    }
    val scoreColor = when {
        percentage >= 70 -> StrongAreaGreen
        percentage >= 50 -> WarningAmber
        else -> WeakAreaRed
    }

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "score_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Score circle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    trackColor = scoreColor.copy(alpha = 0.1f),
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = grade,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
                    )
                    Text(
                        text = "$score/$total",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Correct",
                    value = "$score",
                    icon = Icons.Default.CheckCircle,
                    tint = StrongAreaGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Wrong",
                    value = "${total - score}",
                    icon = Icons.Default.Cancel,
                    tint = WeakAreaRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total",
                    value = "$total",
                    icon = Icons.Default.Quiz,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Home", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}