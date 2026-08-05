package com.dream.wowiptv.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun rememberIsTablet(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 840
}
