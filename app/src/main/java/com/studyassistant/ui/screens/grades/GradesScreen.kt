package com.studyassistant.ui.screens.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.GradeEntry
import com.studyassistant.domain.model.gradeColor
import com.studyassistant.ui.components.LoadingIndicator
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.ui.theme.StrongAreaGreen
import com.studyassistant.ui.theme.WarningAmber
import com.studyassistant.ui.theme.WeakAreaRed
import com.studyassistant.viewmodel.GradesViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    onBack: () -> Unit,
    onNavigateToTimeline: () -> Unit = {},
    viewModel: GradesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Grades", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToTimeline) {
                        Icon(Icons.Default.Timeline, contentDescription = "Progress Timeline")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        ScreenBackground {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator("Loading grades...")
                }
                return@ScreenBackground
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview card
                item {
                    GradeOverviewCard(
                        averageGrade = uiState.averageGrade,
                        averagePct = uiState.averagePct,
                        total = uiState.grades.size
                    )
                }

                // Grade distribution chips
                if (uiState.gradeCounts.isNotEmpty()) {
                    item {
                        GradeDistributionRow(counts = uiState.gradeCounts)
                    }
                }

                // Empty state
                if (uiState.grades.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.School, contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                                Text("No grades yet", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text("Complete a quiz to earn your first grade",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                // Section header
                if (uiState.grades.isNotEmpty()) {
                    item {
                        Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Grade entries
                items(uiState.grades, key = { it.id }) { entry ->
                    GradeEntryCard(entry = entry, dateText = dateFormat.format(entry.createdAt))
                }
            }
        }
    }
}

@Composable
private fun GradeOverviewCard(averageGrade: String, averagePct: Int, total: Int) {
    val gradeColor = Color(gradeColor(averageGrade))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = gradeColor.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, gradeColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = averageGrade,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = gradeColor
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Average Grade", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Text("$averagePct%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$total quiz${if (total != 1) "zes" else ""} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun GradeDistributionRow(counts: Map<String, Int>) {
    val order = listOf("A+", "A", "B", "C", "D", "F")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Grade Distribution", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(order.filter { counts.containsKey(it) }) { grade ->
                val count = counts[grade] ?: 0
                val col = Color(gradeColor(grade))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = col.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(grade, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = col)
                        Text("×$count", style = MaterialTheme.typography.labelSmall,
                            color = col.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeEntryCard(entry: GradeEntry, dateText: String) {
    val gradeCol = Color(gradeColor(entry.grade))
    val scoreColor = when {
        entry.percentage >= 70 -> StrongAreaGreen
        entry.percentage >= 50 -> WarningAmber
        else -> WeakAreaRed
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape)
                    .background(gradeCol.copy(alpha = 0.12f))
                    .border(1.5.dp, gradeCol.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.grade, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black, color = gradeCol)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.noteTitle.ifBlank { "Quiz" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(dateText, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${entry.score}/${entry.total}", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = scoreColor)
                Text("${entry.percentage}%", style = MaterialTheme.typography.labelSmall,
                    color = scoreColor.copy(alpha = 0.8f))
            }
        }
    }
}
