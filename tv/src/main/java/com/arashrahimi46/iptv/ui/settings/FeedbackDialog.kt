package com.arashrahimi46.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreChipSize
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Feedback delivery with NO backend of our own: a Google Form is the store. The TV UI here is
 * 100% native (nobody sees Google) — on submit we fire the same hidden POST the form's own web
 * page fires ([FeedbackConfig.responseUrl] with `entry.<id>` fields). The QR panel points a phone
 * at the branded Google Form for anyone who wants to type a longer message on a real keyboard.
 *
 * SETUP (only the app owner can do this): create a Google Form with three questions — a 1..5
 * linear scale, a multiple-choice matching [FeedbackCategory.formValue] EXACTLY (English), and a
 * paragraph field — then paste the four ids below. Get the form id from the public
 * `/forms/d/e/<ID>/viewform` URL; get each `entry.<id>` from the form's pre-filled-link URL
 * (⋮ menu → Get pre-filled link → fill dummy answers → Get link).
 */
private object FeedbackConfig {
    const val FORM_ID = "1FAIpQLSdxmmP_5GBRVY3gVWMQh4a_W4DrVfLy-vaxfBkPM29N94Cr-A"
    const val RATING_ENTRY = "entry.1708683984"
    const val CATEGORY_ENTRY = "entry.1194707559"
    const val MESSAGE_ENTRY = "entry.1623099748"

    val formUrl get() = "https://docs.google.com/forms/d/e/$FORM_ID/viewform"
    val responseUrl get() = "https://docs.google.com/forms/d/e/$FORM_ID/formResponse"
    val isConfigured get() = FORM_ID != "PASTE_FORM_ID"
}

/** Category options. [formValue] is the stable English string the Google Form's multiple-choice
 *  question must contain verbatim; [labelRes] is what the user sees (translated). */
private enum class FeedbackCategory(val formValue: String, val labelRes: Int) {
    PLAYBACK("Playback", R.string.feedback_cat_playback),
    GUIDE("EPG / Guide", R.string.feedback_cat_guide),
    LAYOUT("Layout", R.string.feedback_cat_layout),
    FAVORITES("Favorites", R.string.feedback_cat_favorites),
    MISSING("Missing feature", R.string.feedback_cat_missing),
    OTHER("Other", R.string.feedback_cat_other),
}

private sealed interface SubmitState {
    data object Idle : SubmitState
    data object Sending : SubmitState
    data object Sent : SubmitState
    data object Error : SubmitState
}

private val feedbackHttp by lazy { OkHttpClient() }

/**
 * The feedback modal. Caller renders it inside a real [androidx.compose.ui.window.Dialog] window
 * (focus trap, per the project's TV dialog convention) — see [SettingsScreen]'s usage.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    val scope = rememberCoroutineScope()

    var rating by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf<FeedbackCategory?>(null) }
    var message by remember { mutableStateOf("") }
    var submit by remember { mutableStateOf<SubmitState>(SubmitState.Idle) }

    AreDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.feedback_title),
        // Wider so the form (left) and the QR hand-off (right) sit side by side -- keeps the modal
        // short enough to fit the screen without scrolling (the QR used to overflow off the bottom).
        width = 880.dp,
        actions = {
            AreButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                variant = AreButtonVariant.Ghost,
                size = AreButtonSize.Large,
            )
            if (submit != SubmitState.Sent) {
                val sending = submit == SubmitState.Sending
                AreButton(
                    text = if (sending) stringResource(R.string.feedback_sending)
                        else stringResource(R.string.feedback_send),
                    // Nothing to send without at least a rating; also guard the unconfigured build.
                    disabled = sending || rating == 0 || !FeedbackConfig.isConfigured,
                    variant = AreButtonVariant.Primary,
                    size = AreButtonSize.Large,
                    onClick = {
                        submit = SubmitState.Sending
                        scope.launch {
                            submit = postFeedback(rating, category, message)
                        }
                    },
                )
            }
        },
    ) {
        if (submit == SubmitState.Sent) {
            // Thank-you state replaces the form once delivered.
            Text(
                text = stringResource(R.string.feedback_thanks),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            return@AreDialog
        }

        Text(
            text = stringResource(R.string.feedback_subtitle),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
        Box(Modifier.size(16.dp))

        // Two columns: the form on the left, the phone hand-off (QR) on the right, so the modal
        // stays short enough to fit without scrolling. If the form is unconfigured the right column
        // is dropped and the form takes the full width.
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1f)) {
                // Star rating.
                Text(stringResource(R.string.feedback_rating_label), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                Box(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        val filled = i <= rating
                        TvFocusable(
                            onClick = { rating = i },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(AreIptvTheme.radius.sm),
                        ) { _, _ ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.feedback_star_cd, i),
                                    tint = if (filled) colors.accent else colors.textTertiary,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                    }
                }
                Box(Modifier.size(20.dp))

                // Category chips (single-select, optional).
                Text(stringResource(R.string.feedback_category_label), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                Box(Modifier.size(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeedbackCategory.entries.forEach { cat ->
                        AreChip(
                            text = stringResource(cat.labelRes),
                            selected = cat == category,
                            onClick = { category = if (category == cat) null else cat },
                            size = AreChipSize.Small,
                        )
                    }
                }
                Box(Modifier.size(20.dp))

                // Optional message (short — long messages belong on the phone form via the QR).
                AreTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = stringResource(R.string.feedback_message_label),
                    placeholder = stringResource(R.string.feedback_message_placeholder),
                    activateOnClick = true,
                )

                if (submit == SubmitState.Error) {
                    Box(Modifier.size(10.dp))
                    Text(stringResource(R.string.feedback_error), style = AreIptvTheme.typography.caption, color = colors.danger)
                }
            }

            // Phone hand-off: scan to write a longer message on a real keyboard.
            if (FeedbackConfig.isConfigured) {
                Column(Modifier.width(200.dp)) {
                    QrCode(FeedbackConfig.formUrl)
                    Box(Modifier.size(10.dp))
                    Text(stringResource(R.string.feedback_qr_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                    Box(Modifier.size(4.dp))
                    Text(stringResource(R.string.feedback_qr_desc), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
                }
            }
        }
    }
}

/** Fire the form's own POST. Runs off the main thread; maps any failure to [SubmitState.Error]. */
private suspend fun postFeedback(rating: Int, category: FeedbackCategory?, message: String): SubmitState =
    withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add(FeedbackConfig.RATING_ENTRY, rating.toString())
                .apply {
                    category?.let { add(FeedbackConfig.CATEGORY_ENTRY, it.formValue) }
                    if (message.isNotBlank()) add(FeedbackConfig.MESSAGE_ENTRY, message.trim())
                }
                .build()
            val req = Request.Builder().url(FeedbackConfig.responseUrl).post(body).build()
            feedbackHttp.newCall(req).execute().use { resp ->
                // Google Forms answers a good submission with 200; it 4xx's on a field-id mismatch.
                if (resp.isSuccessful) SubmitState.Sent else SubmitState.Error
            }
        } catch (_: Exception) {
            SubmitState.Error
        }
    }

/** Static QR of [content] on a white quiet-zone card (white bg + dark modules scans in any theme). */
@Composable
private fun QrCode(content: String) {
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0, 0,
                mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
            )
        }.getOrNull()
    }
    val dark = Color(0xFF0B0B0B)
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(AreIptvTheme.radius.md))
            .padding(10.dp),
    ) {
        Canvas(Modifier.size(140.dp)) {
            val m = matrix ?: return@Canvas
            val cell = size.width / m.width
            for (x in 0 until m.width) {
                for (y in 0 until m.height) {
                    if (m.get(x, y)) {
                        drawRect(color = dark, topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
                    }
                }
            }
        }
    }
}
