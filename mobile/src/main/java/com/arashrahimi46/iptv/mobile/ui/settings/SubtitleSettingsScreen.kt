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
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.mobile.ui.components.AreChoiceRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader

/** `settings/subtitles` -- language, size, edge style, colour, font. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val subtitleLanguage by viewModel.subtitleLanguage.collectAsState()
    val subtitleTextScale by viewModel.subtitleTextScale.collectAsState()
    val subtitleEdge by viewModel.subtitleEdge.collectAsState()
    val subtitleColor by viewModel.subtitleColor.collectAsState()
    val subtitleFont by viewModel.subtitleFont.collectAsState()

    AreScreenScaffold(
        title = stringResource(CoreR.string.settings_section_subtitles),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_subtitles)) }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_subtitle_lang_title),
                    options = SubtitleLanguageCodes,
                    selected = SubtitleLanguageCodes.firstOrNull { it == subtitleLanguage },
                    label = { languageCodeLabel(it) },
                    onSelect = viewModel::setSubtitleLanguage,
                )
            }
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_subtitle_appearance)) }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_sub_size_title),
                    options = SubtitleTextScale.entries,
                    selected = subtitleTextScale,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setSubtitleTextScale,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_sub_style_title),
                    options = SubtitleEdge.entries,
                    selected = subtitleEdge,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setSubtitleEdge,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_sub_color_title),
                    options = SubtitleColorChoice.entries,
                    selected = subtitleColor,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setSubtitleColor,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_sub_font_title),
                    options = SubtitleFontChoice.entries,
                    selected = subtitleFont,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setSubtitleFont,
                )
            }
        }
    }
}
