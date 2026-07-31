package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** One supported UI language, keyed by its BCP-47 tag (matches `UserSettings.languageTag`). */
data class AreLanguageOption(val tag: String, val nativeNameRes: Int)

/** All 24 supported languages, in display order -- native display names only. */
val AreLanguageOptions: List<AreLanguageOption> = listOf(
    AreLanguageOption("en", CoreR.string.language_name_en),
    AreLanguageOption("es", CoreR.string.language_name_es),
    AreLanguageOption("fr", CoreR.string.language_name_fr),
    AreLanguageOption("de", CoreR.string.language_name_de),
    AreLanguageOption("it", CoreR.string.language_name_it),
    AreLanguageOption("pt-BR", CoreR.string.language_name_pt_br),
    AreLanguageOption("pt-PT", CoreR.string.language_name_pt_pt),
    AreLanguageOption("ru", CoreR.string.language_name_ru),
    AreLanguageOption("tr", CoreR.string.language_name_tr),
    AreLanguageOption("az", CoreR.string.language_name_az),
    AreLanguageOption("pl", CoreR.string.language_name_pl),
    AreLanguageOption("uk", CoreR.string.language_name_uk),
    AreLanguageOption("nl", CoreR.string.language_name_nl),
    AreLanguageOption("ro", CoreR.string.language_name_ro),
    AreLanguageOption("sv", CoreR.string.language_name_sv),
    AreLanguageOption("da", CoreR.string.language_name_da),
    AreLanguageOption("nb", CoreR.string.language_name_nb),
    AreLanguageOption("fi", CoreR.string.language_name_fi),
    AreLanguageOption("cs", CoreR.string.language_name_cs),
    AreLanguageOption("el", CoreR.string.language_name_el),
    AreLanguageOption("hu", CoreR.string.language_name_hu),
    AreLanguageOption("bg", CoreR.string.language_name_bg),
    AreLanguageOption("fa", CoreR.string.language_name_fa),
    AreLanguageOption("ar", CoreR.string.language_name_ar),
)

/**
 * The language picker as a LIST, not a chip `FlowRow`. 24 locales in native script do not fit as
 * chips on a handset -- that layout was a TV affordance and it wrapped into five rows on a phone.
 */
@Composable
fun AreLanguageList(
    selectedTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = contentPadding) {
        items(AreLanguageOptions, key = { it.tag }) { option ->
            val selected = option.tag.equals(selectedTag, ignoreCase = true)
            AreListRow(
                title = stringResource(option.nativeNameRes),
                onClick = { onSelect(option.tag) },
                trailing = {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = AreIptvTheme.colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )
        }
    }
}
