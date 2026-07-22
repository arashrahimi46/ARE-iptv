package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.R

/** One of the six languages Round 1 supports, keyed by its BCP-47 tag (matches
 * [com.arashrahimi46.iptv.data.settings.UserSettings.languageTag]). */
data class AreLanguageOption(val tag: String, val nativeNameRes: Int)

/** All supported languages, in display order -- native display names only (LTR; RTL is a future round). */
val AreLanguageOptions: List<AreLanguageOption> = listOf(
    AreLanguageOption("en", R.string.language_name_en),
    AreLanguageOption("es", R.string.language_name_es),
    AreLanguageOption("fr", R.string.language_name_fr),
    AreLanguageOption("de", R.string.language_name_de),
    AreLanguageOption("it", R.string.language_name_it),
    AreLanguageOption("pt-BR", R.string.language_name_pt_br),
    AreLanguageOption("pt-PT", R.string.language_name_pt_pt),
    AreLanguageOption("ru", R.string.language_name_ru),
    AreLanguageOption("tr", R.string.language_name_tr),
    AreLanguageOption("az", R.string.language_name_az),
    AreLanguageOption("pl", R.string.language_name_pl),
    AreLanguageOption("uk", R.string.language_name_uk),
    AreLanguageOption("nl", R.string.language_name_nl),
    AreLanguageOption("ro", R.string.language_name_ro),
    AreLanguageOption("sv", R.string.language_name_sv),
    AreLanguageOption("da", R.string.language_name_da),
    AreLanguageOption("nb", R.string.language_name_nb),
    AreLanguageOption("fi", R.string.language_name_fi),
    AreLanguageOption("cs", R.string.language_name_cs),
    AreLanguageOption("el", R.string.language_name_el),
    AreLanguageOption("hu", R.string.language_name_hu),
    AreLanguageOption("bg", R.string.language_name_bg),
)

/**
 * Shared language picker (native display names, e.g. "Español", "Français") -- a wrapping row of
 * [AreChip]s so it behaves like every other filter/picker in the app (D-pad focus travel, no
 * separate open/close state to manage) and is reusable from both the first-run language screen and
 * Settings without duplicating the list or the visual treatment.
 */
@Composable
fun AreLanguageSelector(
    selectedTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        // Vertical gap clears the focused chip's glow halo + 1.06x focus scale so the glow doesn't
        // bleed onto the chip in the wrapped row below.
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AreLanguageOptions.forEach { option ->
            AreChip(
                text = stringResource(option.nativeNameRes),
                selected = option.tag.equals(selectedTag, ignoreCase = true),
                onClick = { onSelect(option.tag) },
            )
        }
    }
}
