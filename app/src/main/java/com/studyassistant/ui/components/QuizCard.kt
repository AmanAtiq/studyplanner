package com.studyassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyassistant.domain.model.Quiz

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCard(quiz: Quiz, onTake: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Quiz for Note: ${quiz.noteId}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Questions: ${quiz.questions.size}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onTake) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Take Quiz")
            }
        }
    }
}

