package com.studyassistant.ui.screens.planner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.domain.model.*
import com.studyassistant.ui.components.*
import com.studyassistant.ui.theme.*
import com.studyassistant.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clip
import com.studyassistant.ui.components.ScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onBack: () -> Unit,
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ScreenBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Error
                uiState.error?.let { error ->
                    item { ErrorBanner(message = error, onDismiss = viewModel::clearError) }
                }

                // Generate plan button / loading
                item {
                    GeneratePlanSection(
                        isGenerating = uiState.isGenerating,
                        hasPlan = uiState.studyPlan != null,
                        onGenerate = viewModel::generatePlan
                    )
                }

                // Plan content
                if (uiState.studyPlan != null) {
                    val plan = uiState.studyPlan!!

                    // DEBUG: Show plan source (AI or FALLBACK)
                    item {
                        DebugSourceBanner(source = plan.source)
                    }

                    // Plan header (show target exam from UI state)
                    item {
                        PlanHeader(plan = plan, targetExam = uiState.targetExam)
                    }

                    // Progress summary
                    item {
                        PlanProgressCard(plan = plan)
                    }

                    // Tasks grouped by date
                    val grouped = plan.tasks.groupBy {
                        SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(it.dueDate)
                    }

                    grouped.forEach { (date, tasks) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(tasks, key = { it.id }) { task ->
                            StudyTaskItem(
                                task = task,
                                onToggle = { viewModel.toggleTaskCompletion(task.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Empty state
                if (uiState.studyPlan == null && !uiState.isGenerating) {
                    item { PlannerEmptyState(onGenerate = viewModel::generatePlan) }
                }
            }
        }
    }
}

@Composable
private fun GeneratePlanSection(
    isGenerating: Boolean,
    hasPlan: Boolean,
    onGenerate: () -> Unit,
    generatingStep: String = ""
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isGenerating,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Building your plan...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasPlan) "Regenerate AI Plan" else "Generate AI Study Plan",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Show detailed step when generating
        if (isGenerating && generatingStep.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            generatingStep,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        "This may take 1-3 minutes depending on your quiz history size.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanHeader(plan: StudyPlan, targetExam: String?) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(plan.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                // If the UI provides a targetExam, display it as a subtitle
                if (targetExam?.isNotBlank() == true) {
                    Text(
                        "Target exam: $targetExam",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Text(
                    "${dateFormat.format(plan.startDate)} → ${dateFormat.format(plan.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PlanProgressCard(plan: StudyPlan) {
    val completed = plan.tasks.count { it.isCompleted }
    val total = plan.tasks.size
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Progress", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("$completed/$total tasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun StudyTaskItem(
    task: StudyTask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priority) {
        Priority.HIGH -> WeakAreaRed
        Priority.MEDIUM -> WarningAmber
        Priority.LOW -> StrongAreaGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }
            }
            // Priority badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = priorityColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = task.priority.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlannerEmptyState(onGenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text(
            "No study plan yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Generate an AI-powered plan based on your quiz performance and weak areas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onGenerate, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Generate Plan")
        }
    }
}

@Composable
private fun DebugSourceBanner(source: PlanSource) {
    val (backgroundColor, borderColor, textColor, icon, label) = when (source) {
        PlanSource.AI -> {
            Tuple5(
                Color(0xFF2E7D32).copy(alpha = 0.1f),  // Green
                Color(0xFF2E7D32),
                Color(0xFF1B5E20),
                "✓ AI Generated",
                "Generated using Gemini API (Optimized)"
            )
        }
        PlanSource.FALLBACK -> {
            Tuple5(
                Color(0xFFF57C00).copy(alpha = 0.1f),  // Orange
                Color(0xFFF57C00),
                Color(0xFFE65100),
                "⚠ Fallback Mode",
                "Generated using local method (API unavailable)"
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontSize = 16.sp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Helper data class for tuple
data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
