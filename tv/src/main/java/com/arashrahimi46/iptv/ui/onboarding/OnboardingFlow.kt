package com.arashrahimi46.iptv.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreStepIndicator
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

private val stepLabels = listOf("Source", "Credentials", "EPG", "Confirm")
private val stepRoutes = listOf("source", "credentials", "epg", "confirm")

/**
 * Standalone onboarding wizard (Onboarding.jsx). Full-bleed -- lives outside
 * [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell], per app.jsx (onboarding
 * renders before the persistent shell exists). Uses a real Compose Navigation
 * back stack for step navigation, with wizard state hoisted in
 * [OnboardingViewModel] above the nav graph.
 *
 * @param onFinished called once a source has been successfully added (or the
 *   user chooses "Skip for now"), with the new source id if one was created.
 */
@Composable
fun OnboardingFlow(onFinished: (sourceId: Long?) -> Unit) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(context.applicationContext as android.app.Application),
    )
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val colors = AreIptvTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 40.dp, vertical = 56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.accent, RoundedCornerShape(AreIptvTheme.radius.sm)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "A", style = AreIptvTheme.typography.h2, color = colors.accentFg)
                }
                Text(text = "ARE iptv", style = AreIptvTheme.typography.h2, color = colors.textPrimary)
            }
            Box(Modifier.height(18.dp))
            Text(text = "Add a playlist", style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.height(6.dp))
            Text(
                text = "Bring your own IPTV subscription. ARE iptv never stores your credentials off-device.",
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            Box(Modifier.height(36.dp))

            val currentRoute = stepRoutes.getOrElse(currentStepIndex(navController)) { "source" }
            AreStepIndicator(steps = stepLabels, current = stepRoutes.indexOf(currentRoute).coerceAtLeast(0))

            Box(Modifier.height(40.dp))

            Box(modifier = Modifier.heightIn(min = 260.dp)) {
                NavHost(navController = navController, startDestination = "source") {
                    composable("source") {
                        SourceStep(selected = uiState.sourceType, onSelect = viewModel::setSourceType)
                    }
                    composable("credentials") {
                        CredentialsStep(
                            state = uiState,
                            onChange = { portalName, serverUrl, username, password, m3uUrl ->
                                viewModel.updateCredentials(portalName, serverUrl, username, password, m3uUrl)
                            },
                        )
                    }
                    composable("epg") {
                        EpgStep(
                            epgAuto = uiState.epgAuto,
                            epgUrl = uiState.epgUrl,
                            onEpgAutoChange = { viewModel.setEpg(auto = it) },
                            onEpgUrlChange = { viewModel.setEpg(url = it) },
                        )
                    }
                    composable("confirm") {
                        ConfirmStep(state = uiState)
                    }
                }
            }

            Box(Modifier.height(36.dp))
            val stepIndex = stepRoutes.indexOf(currentRoute).coerceAtLeast(0)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (stepIndex > 0) {
                    AreButton("Back", onClick = { navController.popBackStack() }, variant = AreButtonVariant.Ghost, size = AreButtonSize.Large)
                }
                Box(Modifier.weight(1f))
                AreButton("Skip for now", onClick = { onFinished(null) }, variant = AreButtonVariant.Ghost, size = AreButtonSize.Large)
                if (stepIndex < stepRoutes.lastIndex) {
                    val canContinue = when (currentRoute) {
                        "credentials" -> if (uiState.sourceType == OnboardingSourceType.XTREAM) {
                            uiState.serverUrl.isNotBlank() && uiState.username.isNotBlank() && uiState.password.isNotBlank()
                        } else {
                            uiState.m3uUrl.isNotBlank()
                        }
                        else -> true
                    }
                    AreButton(
                        "Continue",
                        onClick = { navController.navigate(stepRoutes[stepIndex + 1]) },
                        size = AreButtonSize.Large,
                        disabled = !canContinue,
                    )
                } else {
                    AreButton(
                        text = if (uiState.result != null) "Go to Home" else "Add playlist",
                        onClick = {
                            if (uiState.result != null) {
                                onFinished(null)
                            } else {
                                viewModel.submit(onDone = { onFinished(null) })
                            }
                        },
                        size = AreButtonSize.Large,
                        disabled = uiState.isSubmitting,
                    )
                }
            }
        }
    }
}

/** Best-effort current step index derived from the nav back stack for the [AreStepIndicator]. */
@Composable
private fun currentStepIndex(navController: NavHostController): Int {
    val entry = navController.currentBackStackEntryAsState()
    val route = entry.value?.destination?.route
    return stepRoutes.indexOf(route).coerceAtLeast(0)
}
