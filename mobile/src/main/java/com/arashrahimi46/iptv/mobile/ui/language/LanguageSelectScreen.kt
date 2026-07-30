package com.arashrahimi46.iptv.mobile.ui.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreLanguageSelector
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * First-run language picker: shown once, right after Splash and before Onboarding -- gated in
 * [com.arashrahimi46.iptv.mobile.MainActivity] by [UserSettings.hasSelectedLanguage], same as
 * :tv's LanguageSelectScreen. Reuses :core's [AreLanguageSelector] (already platform-neutral --
 * chip picker, no D-pad-specific bits) and the same [UserSettings.setLanguageTag] write.
 */
@Composable
fun LanguageSelectScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(deviceDefaultLanguageTag()) }
    val colors = AreIptvTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Text(text = stringResource(R.string.language_select_title), style = AreIptvTheme.typography.h1, color = colors.textPrimary)
            Box(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.language_select_subtitle),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            Box(Modifier.height(28.dp))
            AreLanguageSelector(selectedTag = selected, onSelect = { selected = it })
            Box(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f))
                AreButton(
                    text = stringResource(R.string.action_continue),
                    onClick = {
                        scope.launch {
                            settings.setLanguageTag(selected)
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected))
                            onDone()
                        }
                    },
                    size = AreButtonSize.Large,
                )
            }
        }
    }
}

/** Device locale matched to one of the supported languages, else English -- mirrors :tv's
 * deviceDefaultLanguageTag exactly. */
private fun deviceDefaultLanguageTag(): String {
    val locale = Locale.getDefault()
    return when (locale.language) {
        "es" -> "es"
        "fr" -> "fr"
        "de" -> "de"
        "it" -> "it"
        "fa" -> "fa"
        "ar" -> "ar"
        "pt" -> if (locale.country.equals("BR", ignoreCase = true)) "pt-BR" else "en"
        else -> "en"
    }
}
