package com.studyassistant.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Small data holder for a slide (title + optional action and subtitle)
data class Slide(
    val title: String,
    val subtitle: String = "",
    val buttonLabel: String = "Open",
    val action: (() -> Unit)? = null
)

@Composable
fun SliderComponent(
    pages: List<Slide>,
    modifier: Modifier = Modifier
) {
    if (pages.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp }
    val itemWidth = screenWidth - 32.dp // honor some horizontal padding

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentPage by derivedStateOf { listState.firstVisibleItemIndex }

    Column(modifier = modifier) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(pages) { index, slide ->
                Card(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFAF8FF), Color(0xFFF4F7FF))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    text = slide.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF1B1B1F)
                                )
                                if (slide.subtitle.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = slide.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF505050),
                                        maxLines = 2
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                val btnLabel = slide.buttonLabel
                                Button(
                                    onClick = {
                                        slide.action?.invoke() ?: run {
                                            // no-op fallback
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8BD2), contentColor = Color.Black)
                                ) {
                                    Text(btnLabel)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Indicators
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.forEachIndexed { idx, _ ->
                val isSelected = idx == currentPage
                val size = if (isSelected) 10.dp else 8.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(size)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        .clickable {
                            coroutineScope.launch { listState.animateScrollToItem(idx) }
                        }
                ) {}
            }
        }
    }
}
