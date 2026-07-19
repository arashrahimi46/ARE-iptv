package com.arashrahimi46.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreContinueCard
import com.arashrahimi46.iptv.ui.components.AreHero
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreRail
import com.arashrahimi46.iptv.ui.shell.AreIptvAppShell
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AreIptvTheme {
                PlaceholderHomeScreen()
            }
        }
    }
}

/**
 * Phase 0 placeholder screen: the real app shell with a static content area
 * (hero + a couple of dummy rails) so the theme, focus system and component
 * library are visually verifiable. Real navigation/screens land in Phase 1+.
 */
@Composable
fun PlaceholderHomeScreen() {
    var activeNav by remember { mutableStateOf("home") }
    AreIptvAppShell(activeNav = activeNav, onNavSelect = { activeNav = it }) {
        Column(modifier = Modifier.padding(bottom = AreIptvTheme.spacing.sp16)) {
            Box(modifier = Modifier.padding(horizontal = AreIptvTheme.spacing.safeX)) {
                AreHero(
                    title = "The Last Frontier",
                    kicker = "Featured Series",
                    meta = "2026 · Drama · 8 episodes",
                    synopsis = "A gripping survival drama that follows a crew stranded on the edge of known space.",
                    badges = {
                        AreBadge("New", tone = AreBadgeTone.New)
                        AreBadge("4K", tone = AreBadgeTone.Quality)
                    },
                    actions = {
                        AreButton("Play now", onClick = {}, variant = AreButtonVariant.Primary)
                        AreButton("More info", onClick = {}, variant = AreButtonVariant.Secondary)
                    },
                )
            }
            Box(Modifier.padding(top = AreIptvTheme.spacing.sp10))
            AreRail(title = "Continue Watching", smart = true) {
                items(dummyContinueWatching) { name ->
                    AreContinueCard(title = name, onClick = {}, meta = "Movie")
                }
            }
            AreRail(title = "Live Channels") {
                items(dummyChannels) { channel ->
                    AreChannelTile(channel = channel, onClick = {}, number = "10${dummyChannels.indexOf(channel) + 1}", now = "Now playing")
                }
            }
            AreRail(title = "Popular Movies", seeAll = false) {
                items(dummyMovies) { movie ->
                    ArePosterTile(title = movie, onClick = {}, meta = "2024 · Drama")
                }
            }
        }
    }
}

private val dummyContinueWatching = listOf("Oppenheimer", "The Bear", "Dune Part Two")
private val dummyChannels = listOf("Sky Sports HD", "BBC One", "CNN International", "Discovery HD")
private val dummyMovies = listOf("Dune Part Two", "Poor Things", "The Zone of Interest", "Past Lives")

@Preview(widthDp = 1920, heightDp = 1080, showBackground = true)
@Composable
private fun PlaceholderHomeScreenPreview() {
    AreIptvTheme {
        PlaceholderHomeScreen()
    }
}
