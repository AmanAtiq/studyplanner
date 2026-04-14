package com.studyassistant.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.studyassistant.domain.model.*
import com.studyassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*


// ── Weak Area Chip ────────────────────────────────────
@Composable
fun WeakAreaChip(weakArea: WeakArea, modifier: Modifier = Modifier) {
    val accuracy = (weakArea.accuracy * 100).toInt()
    val color = when {
        accuracy < 40 -> WeakAreaRed
        accuracy < 70 -> WarningAmber
        else -> StrongAreaGreen
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(weakArea.topic, style = MaterialTheme.typography.labelSmall, color = color)
            Text("$accuracy%", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}
