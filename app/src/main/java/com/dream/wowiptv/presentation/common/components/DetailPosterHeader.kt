package com.dream.wowiptv.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dream.wowiptv.presentation.common.theme.LocalAccentPalette

fun posterContentScaleFromKey(key: String): ContentScale = when (key) {
    "fit" -> ContentScale.Fit
    "inside" -> ContentScale.Inside
    "fill_width" -> ContentScale.FillWidth
    "fill_height" -> ContentScale.FillHeight
    else -> ContentScale.Crop
}

private fun showsSideGlow(contentScale: ContentScale): Boolean =
    contentScale != ContentScale.Crop && contentScale != ContentScale.FillBounds

@Composable
fun DetailPosterHeader(
    model: Any?,
    contentDescription: String,
    contentScale: ContentScale,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 320.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )
        if (showsSideGlow(contentScale)) {
            val accent = LocalAccentPalette.current
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.vibrant.copy(alpha = 0.38f),
                                accent.vibrant.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.vibrant.copy(alpha = 0.10f),
                                accent.vibrant.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}