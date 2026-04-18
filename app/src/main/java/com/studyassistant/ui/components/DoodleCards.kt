/**
 * Doodle Button Components
 *
 * Professional soft and doodle-styled button components
 * with black text, black borders, and white backgrounds
 */

package com.studyassistant.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyassistant.ui.theme.AccentPurple

/**
 * Doodle Button - Primary Style
 * Purple button with black text
 */
@Suppress("unused")
@Composable
fun DoodleButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(
                width = 2.dp,
                color = Color(0xFF000000),  // Black border
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFFFFF), // Changed to white
            disabledContainerColor = Color(0xFFE0E0E0).copy(alpha = 0.5f) // Changed to light gray
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000)  // Black text
        )
    }
}

/**
 * Doodle Button - Accent Style
 * Purple button with black text and black border
 */
@Suppress("unused")
@Composable
fun DoodleButtonAccent(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(
                width = 2.dp,
                color = Color(0xFF000000),  // Black border
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPurple,
            disabledContainerColor = AccentPurple.copy(alpha = 0.5f),
            contentColor = Color.Black,
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000)  // Black text
        )
    }
}
