package com.arashrahimi46.iptv.mobile.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Touch-first movie detail: poster, title, year/category/rating meta, IMDb/RT scores, genre,
 * director, cast and storyline (all lazily enriched by [MovieDetailViewModel] via :core's
 * [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureMetadataLoaded], same source
 * :tv's Detail screen uses), plus Play + Favorite actions. :tv's DetailScreen equivalent also
 * carries a D-pad season rail for series -- that's out of scope here, series stay on
 * [com.arashrahimi46.iptv.mobile.ui.series.SeriesDetailScreen].
 */
@Composable
fun MovieDetailScreen(
    vodTitleId: Long,
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = viewModel(
        factory = MovieDetailViewModel.factory(LocalContext.current.applicationContext as android.app.Application, vodTitleId),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val colors = AreIptvTheme.colors

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.title == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_not_found), style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
        else -> MovieDetailContent(
            title = state.title!!,
            isFavorite = isFavorite,
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
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f * 1.6f)) {
                AsyncImage(
                    model = title.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = title.name, style = AreIptvTheme.typography.h2, color = colors.textPrimary)

                val meta = listOfNotNull(title.year, title.categoryName, title.genre).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(text = meta, style = AreIptvTheme.typography.body, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                }

                val ratings = listOfNotNull(
                    title.rating?.let { "★ $it" },
                    title.imdbRating?.let { "IMDb $it" },
                    title.rtRating?.let { "RT $it" },
                ).joinToString("   ")
                if (ratings.isNotBlank()) {
                    Text(text = ratings, style = AreIptvTheme.typography.caption, color = colors.textTertiary, modifier = Modifier.padding(top = 4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AreButton(
                        text = stringResource(R.string.detail_play),
                        onClick = onPlay,
                        icon = Icons.Filled.PlayArrow,
                        variant = AreButtonVariant.Primary,
                    )
                    AreButton(
                        text = stringResource(if (isFavorite) R.string.detail_remove_from_favorites else R.string.detail_add_to_favorites),
                        onClick = onToggleFavorite,
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        variant = AreButtonVariant.Secondary,
                    )
                }

                title.plot?.let { plot ->
                    Text(
                        text = stringResource(R.string.detail_storyline),
                        style = AreIptvTheme.typography.label,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                    Text(text = plot, style = AreIptvTheme.typography.body, color = colors.textPrimary)
                }
                title.director?.let { director ->
                    Text(
                        text = "${stringResource(R.string.detail_director_label)} $director",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                title.castList?.let { cast ->
                    Text(
                        text = "${stringResource(R.string.detail_cast_label)} $cast",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
