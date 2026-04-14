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


// ── Language Toggle ───────────────────────────────────
@Composable
fun LanguageToggle(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "EN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentLanguage == AppLanguage.ENGLISH) FontWeight.Bold else FontWeight.Normal,
                color = if (currentLanguage == AppLanguage.ENGLISH)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text("|", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            Text(
                text = "اردو",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentLanguage == AppLanguage.URDU) FontWeight.Bold else FontWeight.Normal,
                color = if (currentLanguage == AppLanguage.URDU)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}