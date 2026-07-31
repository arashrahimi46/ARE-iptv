package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.ui.components.AreChoiceRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreSwitchRow

/** `settings/playback` -- hardware decoding, preferred audio language, autoplay-next delay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val isHardwareDecoding by viewModel.isHardwareDecoding.collectAsState()
    val preferredAudioLang by viewModel.preferredAudioLanguage.collectAsState()
    val autoplayNextDelay by viewModel.autoplayNextDelaySeconds.collectAsState()

    val autoLabel = stringResource(CoreR.string.settings_audio_auto)

    AreScreenScaffold(
        title = stringResource(CoreR.string.settings_section_playback),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_playback)) }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_hardware_decoding),
                    supporting = stringResource(CoreR.string.settings_hardware_decoding_desc),
                    checked = isHardwareDecoding,
                    onCheckedChange = viewModel::setHardwareDecoding,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_preferred_audio_title),
                    options = AudioLanguageCodes,
                    selected = AudioLanguageCodes.firstOrNull { it == preferredAudioLang } ?: "",
                    label = { if (it.isEmpty()) autoLabel else languageCodeLabel(it) },
                    onSelect = viewModel::setPreferredAudioLanguage,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_autoplay_next),
                    options = AutoplayDelayOptions,
                    selected = AutoplayDelayOptions.firstOrNull { it == autoplayNextDelay } ?: 0L,
                    label = { stringResource(autoplayDelayLabelRes(it)) },
                    onSelect = viewModel::setAutoplayNextDelaySeconds,
                )
            }
        }
    }
}
