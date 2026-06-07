package com.studyassistant.ui.screens.flashcards

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyassistant.ui.components.LoadingIndicator
import com.studyassistant.ui.components.ScreenBackground
import com.studyassistant.viewmodel.FlashcardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    noteId: String,
    onBack: () -> Unit,
    viewModel: FlashcardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(noteId) { viewModel.loadFlashcards(noteId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val total = uiState.cards.size
                    val current = if (total == 0) 0 else uiState.currentIndex + 1
                    Text(
                        if (total == 0) "Flashcards" else "Card $current / $total",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.cards.isNotEmpty()) {
                        if (!uiState.isSaved) {
                            IconButton(onClick = { viewModel.saveFlashcards() }, enabled = !uiState.isSaving) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Save Flashcards", tint = Color(0xFFFF8BD2))
                                }
                            }
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = "Saved", modifier = Modifier.padding(8.dp), tint = Color(0xFF4CAF50))
                        }
                        IconButton(onClick = { viewModel.regenerate(noteId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        ScreenBackground {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> LoadingIndicator("Generating flashcards...")

                    uiState.error != null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadFlashcards(noteId) }) { Text("Retry") }
                    }

                    uiState.isFinished -> FinishedState(
                        total = uiState.cards.size,
                        onRestart = { viewModel.restart() },
                        onBack = onBack
                    )

                    uiState.cards.isNotEmpty() -> {
                        val card = uiState.cards[uiState.currentIndex]
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Progress dots
                            LinearProgressIndicator(
                                progress = { (uiState.currentIndex + 1f) / uiState.cards.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Color(0xFFFF8BD2),
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )

                            Spacer(Modifier.weight(0.3f))

                            // Flip card
                            FlipCard(
                                front = card.front,
                                back = card.back,
                                isFlipped = uiState.isFlipped,
                                onClick = { viewModel.flip() }
                            )

                            Text(
                                text = if (uiState.isFlipped) "Showing answer — tap to see question" else "Tap the card to reveal answer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )

                            Spacer(Modifier.weight(1f))

                            // Navigation buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.previous() },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = uiState.currentIndex > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Previous")
                                }
                                Button(
                                    onClick = { viewModel.next() },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8BD2), contentColor = Color.Black)
                                ) {
                                    Text(if (uiState.currentIndex + 1 >= uiState.cards.size) "Finish" else "Next")
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    front: String,
    back: String,
    isFlipped: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    val isFrontVisible = rotation < 90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, if (isFrontVisible) Color(0xFF87CEFA) else Color(0xFFFF8BD2), RoundedCornerShape(24.dp))
            .background(if (isFrontVisible) Color(0xFF87CEFA).copy(alpha = 0.12f) else Color(0xFFFF8BD2).copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isFrontVisible) {
            CardFace(label = "QUESTION", text = front, color = Color(0xFF87CEFA))
        } else {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }.fillMaxSize(), contentAlignment = Alignment.Center) {
                CardFace(label = "ANSWER", text = back, color = Color(0xFFFF8BD2))
            }
        }
    }
}

@Composable
private fun CardFace(label: String, text: String, color: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun FinishedState(total: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("🎉", style = MaterialTheme.typography.displayMedium)
        Text("All Done!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "You reviewed all $total flashcards",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8BD2), contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Review Again")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back to Note")
        }
    }
}
