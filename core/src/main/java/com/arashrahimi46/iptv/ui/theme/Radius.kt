package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radius scale, mirroring tokens/radius.css. */
data class AreIptvRadius(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val xxl: Dp = 36.dp,
    val pill: Dp = 999.dp,
)

val AreIptvRadiusDefault = AreIptvRadius()
