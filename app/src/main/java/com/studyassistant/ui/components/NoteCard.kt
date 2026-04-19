package com.studyassistant.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.studyassistant.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

// ── Note Card ────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    Card(
        onClick = onTap,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = Color.Black, shape = RoundedCornerShape(16.dp))
            .background(Color.White), // Explicitly set background to pure white
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), // Ensure container color is white
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Remove any shadow to ensure flat white background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White), // Updated to white background
                        contentAlignment = Alignment.Center
                    ) {
                        // Choose icon, tint and size per file type. PDF gets a pure red, slightly larger icon.
                        val (iv, tint, sizeDp) = when (note.fileType) {
                            FileType.PDF -> Triple(Icons.Default.PictureAsPdf, Color(0xFFFF0000), 26.dp)
                            FileType.IMAGE -> Triple(Icons.Default.Image, MaterialTheme.colorScheme.primary, 22.dp)
                            else -> Triple(Icons.AutoMirrored.Filled.Article, MaterialTheme.colorScheme.primary, 22.dp)
                        }

                        Icon(
                            imageVector = iv,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(sizeDp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayTitle = friendlyNoteTitle(note)
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Black // Set text color to black
                        )
                        Text(
                            text = dateFormat.format(note.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (note.summary.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onQuiz,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Take Quiz", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
