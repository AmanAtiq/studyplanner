package com.studyassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyassistant.domain.model.Quiz
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.CalendarView
import android.widget.ProgressBar
import android.content.Context
import android.view.ViewGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCard(quiz: Quiz, onOpenAttempt: () -> Unit, onTakeQuiz: () -> Unit) {
    val percentage = if (quiz.questions.isNotEmpty()) (quiz.score * 100) / quiz.questions.size else 0

    var showDetails by remember { mutableStateOf(false) }
    var selectedDateText by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val titleText = quiz.title.ifBlank {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(quiz.createdAt).let { "Lecture • $it" }
                    }
                    Text(text = titleText, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Completed attempt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(text = "${quiz.questions.size} questions • $percentage%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    // Linear progress bar inside card showing the percentage
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                }

                // Info / details button
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Details")
                }
            }

            Text(
                text = "Score: ${quiz.score}/${quiz.questions.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenAttempt, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open Attempt")
                }
                Button(onClick = onTakeQuiz, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Take Quiz")
                }
            }
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Quiz Details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(quiz.createdAt)}")
                    Text("Score: ${quiz.score}/${quiz.questions.size} ($percentage%)")

                    // Android CalendarView embedded
                    AndroidView(factory = { ctx ->
                        CalendarView(ctx).apply {
                            // set minimal params, show selected date when changed
                            setOnDateChangeListener { _, year, month, dayOfMonth ->
                                selectedDateText = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            }
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                    }, modifier = Modifier.fillMaxWidth())

                    // Android ProgressBar showing score progress horizontally
                    AndroidView(factory = { ctx: Context ->
                        ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                            max = if (quiz.questions.size > 0) quiz.questions.size else 1
                            progress = quiz.score
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                    }, modifier = Modifier.fillMaxWidth())

                    // Show selected date from calendar if user picked one
                    if (selectedDateText.isNotBlank()) {
                        Text("Selected date: $selectedDateText", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}
