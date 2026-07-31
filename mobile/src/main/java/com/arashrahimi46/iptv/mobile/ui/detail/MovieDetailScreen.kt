package com.arashrahimi46.iptv.mobile.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreBadge
import com.arashrahimi46.iptv.mobile.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreLoadingState
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.glassSurface

/** Height of the edge-to-edge hero. 4:5 keeps the artwork generous without eating the whole page. */
private const val HeroAspect = 4f / 5f

/** Room the sticky action bar needs at the bottom of the scroll. */
private val ActionBarReserve = 112.dp

/**
 * Touch-first movie detail: an edge-to-edge collapsing hero, meta/ratings/storyline/credits, and a
 * sticky bottom action bar (Play + Favourite) placed for one-handed reach.
 *
 * Series keep their own screen ([com.arashrahimi46.iptv.mobile.ui.series.SeriesDetailScreen]) —
 * this one has no episode concern.
 */
@Composable
fun MovieDetailScreen(
    vodTitleId: Long,
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = viewModel(
        factory = MovieDetailViewModel.factory(
            LocalContext.current.applicationContext as android.app.Application,
            vodTitleId,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val title = state.title

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AreLoadingState(message = stringResource(CoreR.string.detail_loading))
        }

        title == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AreEmptyState(
                title = stringResource(CoreR.string.detail_not_found),
                actionLabel = stringResource(CoreR.string.action_back),
                onAction = onBack,
            )
        }

        else -> MovieDetailContent(
            title = title,
            isFavorite = isFavorite,
            hasResumePoint = state.resumeMs > 0L,
            onPlay = { onPlay(vodTitleId) },
            onToggleFavorite = viewModel::toggleFavorite,
            onBack = onBack,
        )
    }
}

@Composable
private fun MovieDetailContent(
    title: VodTitle,
    isFavorite: Boolean,
    hasResumePoint: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val listState = rememberLazyListState()

    // The bar only becomes glass once the hero has effectively scrolled away; until then the back
    // button floats on its own glass disc so it stays legible over a bright poster in either theme.
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
        }
    }
    val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, label = "movie-detail-bar")

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ActionBarReserve),
        ) {
            item(contentType = "hero") { Hero(title) }
            item(contentType = "body") { Body(title) }
        }

        // Top bar: transparent over the hero, glass once collapsed.
        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(barAlpha)
                    .glassSurface(RectangleShape, elevated = false, shadow = false),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AreIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreR.string.action_back),
                    onClick = onBack,
                    variant = AreIconButtonVariant.Glass,
                )
                Text(
                    text = title.name,
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(barAlpha),
                )
            }
        }

        // Sticky action bar — thumb-reachable, above the gesture bar.
        ProvideOnGlass(true) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .glassSurface(RectangleShape, elevated = true, shadow = false)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AreButton(
                    // The player already seeks to the stored position on load, so the button was
                    // resuming while promising "Play" -- the action was right and only the label
                    // lied. Both strings already ship in all 24 locales (the tile action sheet
                    // uses them), so this costs no translation work.
                    text = stringResource(
                        if (hasResumePoint) CoreR.string.tile_action_resume else CoreR.string.detail_play,
                    ),
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    variant = AreButtonVariant.Primary,
                    size = AreButtonSize.Large,
                    icon = Icons.Filled.PlayArrow,
                    full = true,
                )
                AreIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) {
                            CoreR.string.detail_remove_from_favorites
                        } else {
                            CoreR.string.detail_add_to_favorites
                        },
                    ),
                    onClick = onToggleFavorite,
                    variant = AreIconButtonVariant.Glass,
                    size = AreIconButtonSize.Large,
                    active = isFavorite,
                )
            }
        }
    }
}

/** Edge-to-edge artwork with a bottom scrim into the page background, so the body reads as one sheet. */
@Composable
private fun Hero(title: VodTitle) {
    val colors = AreIptvTheme.colors
    // Two scrims, not one. The bottom gradient blends artwork into the page; the top one is for
    // legibility -- the status-bar icons and the floating Back button are drawn straight onto this
    // image, and Back is a translucent glass disc, so over a bright poster (an overcast sky, a
    // snow scene) both vanished completely. It read as though the screen had no back button at all.
    val scrim = remember(colors.bgBase) {
        Brush.verticalGradient(0.45f to Color.Transparent, 1f to colors.bgBase)
    }
    val topScrim = remember {
        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.28f to Color.Transparent)
    }
    Box(Modifier.fillMaxWidth().aspectRatio(HeroAspect)) {
        AsyncImage(
            model = title.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(colors.surface1),
        )
        Box(Modifier.matchParentSize().background(scrim))
        Box(Modifier.matchParentSize().background(topScrim))
    }
}

@Composable
private fun Body(title: VodTitle) {
    val colors = AreIptvTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(text = title.name, style = AreIptvTheme.typography.h1, color = colors.textPrimary)

        val meta = listOfNotNull(title.year, title.categoryName, title.genre).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        val ratings = listOfNotNull(
            title.rating?.let { "★ $it" },
            title.imdbRating?.let { "IMDb $it" },
            title.rtRating?.let { "RT $it" },
        )
        if (ratings.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ratings.forEach { AreBadge(text = it, tone = AreBadgeTone.Neutral) }
            }
        }

        title.plot?.takeIf { it.isNotBlank() }?.let { plot ->
            AreSectionHeader(
                text = stringResource(CoreR.string.detail_storyline),
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(text = plot, style = AreIptvTheme.typography.body, color = colors.textPrimary)
        }

        title.director?.takeIf { it.isNotBlank() }?.let { director ->
            Text(
                text = "${stringResource(CoreR.string.detail_director_label)} $director",
                style = AreIptvTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        title.castList?.takeIf { it.isNotBlank() }?.let { cast ->
            Text(
                text = "${stringResource(CoreR.string.detail_cast_label)} $cast",
                style = AreIptvTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
