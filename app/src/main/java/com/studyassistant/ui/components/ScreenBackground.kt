package com.studyassistant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.studyassistant.R

@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    // very light scrim left as transparent by default; screens can pass a scrim if needed
    scrimColor: Color = Color.Transparent,
    // fraction of screen width the background image should occupy (0.0 - 1.0)
    imageWidthFraction: Float = 0.9f,
    // how transparent the image should be (0 = invisible, 1 = opaque)
    imageAlpha: Float = 0.50f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Center a slightly larger, faint background image so it won't overwhelm the UI
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.bg_screen),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = imageAlpha,
                modifier = Modifier
                    .fillMaxWidth(imageWidthFraction)
                    .wrapContentSize(align = Alignment.Center)
            )
        }

        // optional subtle scrim layer (default transparent)
        if (scrimColor != Color.Transparent) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
            ) {}
        }

        content()
    }
}
