package com.arashrahimi46.iptv.mobile.ui.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreLanguageOptions
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * First-run language picker: shown once, right after Splash and before Onboarding -- gated in
 * [com.arashrahimi46.iptv.mobile.MainActivity] by [UserSettings.hasSelectedLanguage].
 *
 * 24 locales in native script never fit as a chip `FlowRow` on a handset (that was a TV
 * affordance, and it wrapped into five rows), so this is a real scrolling list of native names
 * with a check mark, a filter field on top, and the Continue button pinned to the bottom edge
 * where a thumb can reach it. Both the selection and the filter survive rotation.
 */
@Composable
fun LanguageSelectScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(deviceDefaultLanguageTag()) }
    var query by rememberSaveable { mutableStateOf("") }
    var applying by remember { mutableStateOf(false) }
    val colors = AreIptvTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(CoreR.string.language_select_title),
                style = AreIptvTheme.typography.h1,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(CoreR.string.language_select_subtitle),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            AreTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(CoreR.string.action_search),
                leadingIcon = Icons.Filled.Search,
            )
        }

        LanguageList(
            selectedTag = selected,
            onSelect = { selected = it },
            query = query.trim(),
            modifier = Modifier.weight(1f),
        )

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            AreButton(
                text = stringResource(CoreR.string.action_continue),
                onClick = {
                    if (applying) return@AreButton
                    applying = true
                    scope.launch {
                        settings.setLanguageTag(selected)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected))
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                size = AreButtonSize.Large,
                full = true,
                loading = applying,
            )
        }
    }
}

/**
 * Filtered variant of `AreLanguageList`. It is local to this screen because the shared component
 * takes no filter and the match has to run against the *translated* native name, which is only
 * resolvable inside composition -- see the NAV/THEME note in the handoff.
 */
@Composable
private fun LanguageList(
    selectedTag: String,
    onSelect: (String) -> Unit,
    query: String,
    modifier: Modifier = Modifier,
) {
    val accent = AreIptvTheme.colors.accent
    // The native name has to be resolved in composition, so the whole 24-entry list is materialised
    // with its labels once and filtered on both the label and the BCP-47 tag.
    val named = AreLanguageOptions.map { it.tag to stringResource(it.nativeNameRes) }
    val visible = if (query.isEmpty()) {
        named
    } else {
        named.filter { (tag, name) -> name.contains(query, ignoreCase = true) || tag.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(visible, key = { it.first }) { (tag, name) ->
            LanguageRow(tag, name, selectedTag, accent, onSelect)
        }
    }
}

@Composable
private fun LanguageRow(
    tag: String,
    name: String,
    selectedTag: String,
    accent: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit,
) {
    val isSelected = tag.equals(selectedTag, ignoreCase = true)
    AreListRow(
        title = name,
        onClick = { onSelect(tag) },
        trailing = {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    )
}

/**
 * Device locale matched to one of the 24 shipped languages, else English. `pt` needs the region to
 * disambiguate pt-BR from pt-PT; everything else matches on the ISO language subtag alone.
 */
internal fun deviceDefaultLanguageTag(): String {
    val locale = Locale.getDefault()
    val language = locale.language.lowercase()
    if (language == "pt") {
        return if (locale.country.equals("PT", ignoreCase = true)) "pt-PT" else "pt-BR"
    }
    // Historic Java language codes: Locale normalises he/yi/id, but nb/no still varies by OEM.
    val normalized = if (language == "no") "nb" else language
    return AreLanguageOptions.firstOrNull { it.tag.equals(normalized, ignoreCase = true) }?.tag ?: "en"
}
