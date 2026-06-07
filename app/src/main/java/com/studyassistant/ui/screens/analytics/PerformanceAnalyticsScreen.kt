package com.studyassistant.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.PerformanceAnalyticsData
import com.studyassistant.domain.model.PerformanceTrend
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.viewmodel.PerformanceAnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: PerformanceAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ScreenBackground {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            uiState.error ?: "Error loading analytics",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.analytics != null && uiState.analytics!!.totalQuizzesTaken > 0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { OverallStatsCard(uiState.analytics!!) }
                        item { PerformanceTrendCard(uiState.analytics!!.trends) }
                        item { BestWorstSubjectsCard(uiState.analytics!!) }
                        item { 
                            Text(
                                "Subject Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.analytics!!.subjectStats) { subjectStat ->
                            SubjectStatCard(subjectStat)
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No quiz data yet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Complete some quizzes to see your performance analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallStatsCard(analytics: PerformanceAnalyticsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Overall Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Average Score",
                    value = String.format("%.1f%%", analytics.overallAverageScore),
                    iconTint = Color(0xFF4CAF50)
                )
                StatItem(
                    icon = Icons.Default.Quiz,
                    label = "Quizzes Taken",
                    value = analytics.totalQuizzesTaken.toString(),
                    iconTint = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun PerformanceTrendCard(trends: List<PerformanceTrend>) {
    if (trends.isEmpty()) return
    
    val dateFormatter = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Quiz Performance History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 24.dp, end = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barWidth = width / (trends.size.coerceAtLeast(1) * 2)
                    val spacing = width / trends.size
                    
                    // Draw Y axis lines (0, 50, 100)
                    for (i in 0..2) {
                        val y = height - (i * 0.5f * height)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    trends.forEachIndexed { index, trend ->
                        val barHeight = (trend.score / 100).toFloat() * height
                        val x = index * spacing + (spacing / 2) - (barWidth / 2)
                        
                        drawRect(
                            color = when {
                                trend.score >= 90 -> Color(0xFF4CAF50)
                                trend.score >= 70 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF6B6B)
                            },
                            topLeft = Offset(x, height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
                
                // Date labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Show only few labels to avoid crowding
                    val step = (trends.size / 4).coerceAtLeast(1)
                    trends.forEachIndexed { index, trend ->
                        if (index % step == 0 || index == trends.size - 1) {
                            Text(
                                text = dateFormatter.format(trend.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.width(30.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.width(0.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestWorstSubjectsCard(analytics: PerformanceAnalyticsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Best & Worst Subjects",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (analytics.bestSubject != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Best", style = MaterialTheme.typography.labelSmall)
                        Text(
                            analytics.bestSubject.subjectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        String.format("%.1f%%", analytics.bestSubject.averageScore),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (analytics.worstSubject != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFFF6B6B).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Needs Work", style = MaterialTheme.typography.labelSmall)
                        Text(
                            analytics.worstSubject.subjectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        String.format("%.1f%%", analytics.worstSubject.averageScore),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectStatCard(subjectStat: com.studyassistant.domain.model.QuizPerformanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                Column {
                    Text(
                        subjectStat.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${subjectStat.totalQuizzes} quizzes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Text(
                    String.format("%.1f%%", subjectStat.averageScore),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Progress bar
            LinearProgressIndicator(
                progress = (subjectStat.averageScore / 100).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    subjectStat.averageScore >= 90 -> Color(0xFF4CAF50)
                    subjectStat.averageScore >= 70 -> Color(0xFFFFC107)
                    else -> Color(0xFFFF6B6B)
                }
            )
            
            // Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Low: ${String.format("%.1f%%", subjectStat.lowestScore)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "High: ${String.format("%.1f%%", subjectStat.highestScore)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Trend
            if (subjectStat.improvementTrend != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (subjectStat.improvementTrend > 0) 
                            Icons.Default.TrendingUp 
                        else 
                            Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (subjectStat.improvementTrend > 0) 
                            Color(0xFF4CAF50) 
                        else 
                            Color(0xFFFF6B6B)
                    )
                    Text(
                        "${String.format("%.1f%%", kotlin.math.abs(subjectStat.improvementTrend))} ${
                            if (subjectStat.improvementTrend > 0) "improving" else "declining"
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (subjectStat.improvementTrend > 0) 
                            Color(0xFF4CAF50) 
                        else 
                            Color(0xFFFF6B6B)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconTint
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
