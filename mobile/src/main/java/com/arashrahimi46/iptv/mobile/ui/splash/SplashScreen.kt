package com.arashrahimi46.iptv.mobile.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.R

/**
 * Cold-start splash-equivalent for the blank frame [com.arashrahimi46.iptv.mobile.MainActivity]
 * shows while it checks for an existing source (`hasSource == null`). The brand's own portrait
 * splash artwork, drawn edge to edge -- mark, wordmark and backdrop are all baked into the one
 * asset, so there is nothing to lay out. Deliberately static: this frame is up for a DB check,
 * typically a handful of frames, so any reveal animation would barely be visible.
 *
 * The artwork is dark in both themes by design -- it is the brand's splash, not a themed surface.
 */
@Composable
fun MobileSplashScreen() {
    Image(
        painter = painterResource(R.drawable.splash_background),
        contentDescription = stringResource(CoreR.string.splash_logo_content_description),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}
