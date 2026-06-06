package com.studyassistant.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.viewmodel.GradesViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressTimelineScreen(
    onBack: () -> Unit,
    viewModel: GradesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Build activity map: date-string → quiz count
    val activityMap = remember(uiState.grades) {
        uiState.grades.groupBy { grade ->
            val cal = java.util.Calendar.getInstance().apply { time = grade.createdAt }
            "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
        }.mapValues { it.value.size }
    }

    val today = LocalDate.now()
    // 84 days = 12 weeks
    val weeks = 12
    val totalDays = weeks * 7
    val startDate = today.minusDays((totalDays - 1).toLong())

    // Build weeks list: list of 7-day columns
    val dayGrid = (0 until totalDays).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        val key = date.toString()
        val count = activityMap[key] ?: 0
        date to count
    }.chunked(7)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Timeline", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        ScreenBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Summary stats
                item {
                    val totalActive = activityMap.size
                    val totalQuizzes = activityMap.values.sum()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatPill("Active Days", "$totalActive", Color(0xFF87CEFA), Modifier.weight(1f))
                        StatPill("Total Quizzes", "$totalQuizzes", Color(0xFFFF8BD2), Modifier.weight(1f))
                        StatPill("Weeks", "$weeks", Color(0xFFB7A1E2), Modifier.weight(1f))
                    }
                }

                // Legend
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Less", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        HeatCell(count = 0, size = 14.dp)
                        HeatCell(count = 1, size = 14.dp)
                        HeatCell(count = 2, size = 14.dp)
                        HeatCell(count = 3, size = 14.dp)
                        Text("More", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }

                // Day-of-week labels + heatmap
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Quiz Activity — last 12 weeks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))

                        val scrollState = rememberScrollState()
                        Row(modifier = Modifier.horizontalScroll(scrollState)) {
                            // Day labels column
                            Column(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                    Box(modifier = Modifier.size(width = 28.dp, height = 16.dp), contentAlignment = Alignment.CenterEnd) {
                                        Text(day, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                            fontSize = 9.sp)
                                    }
                                }
                            }

                            // Heatmap grid (week columns)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                dayGrid.forEach { week ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        week.forEach { (_, count) ->
                                            HeatCell(count = count, size = 16.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // Month labels below the grid
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Spacer(Modifier.width(32.dp))
                            val monthLabels = dayGrid.mapIndexed { colIdx, week ->
                                val firstDay = week.first().first
                                if (colIdx == 0 || firstDay.dayOfMonth <= 7) {
                                    firstDay.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                } else ""
                            }
                            monthLabels.forEach { label ->
                                Box(modifier = Modifier.width(20.dp)) {
                                    if (label.isNotEmpty()) {
                                        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent activity list
                if (activityMap.isNotEmpty()) {
                    item {
                        Text("Recent Sessions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    items(
                        count = minOf(uiState.grades.size, 10),
                        key = { uiState.grades[it].id }
                    ) { idx ->
                        val grade = uiState.grades[idx]
                        val dateStr = remember(grade.createdAt) {
                            java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(grade.createdAt)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(heatColor(1).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(grade.noteTitle.ifBlank { "Quiz" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("${grade.score}/${grade.total} · ${grade.percentage}%", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            Text(grade.grade, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                color = Color(com.studyassistant.domain.model.gradeColor(grade.grade)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatCell(count: Int, size: Dp) {
    val color = heatColor(count)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(3.dp))
    )
}

private fun heatColor(count: Int): Color = when {
    count == 0 -> Color(0xFFE8E8E8)
    count == 1 -> Color(0xFFFFCCE8)
    count == 2 -> Color(0xFFFF8BD2)
    else       -> Color(0xFFD63BA0)
}

@Composable
private fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}
