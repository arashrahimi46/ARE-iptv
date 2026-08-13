package com.arashrahimi46.iptv.mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.SourceType
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreErrorState
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme

/**
 * Single-screen "add a source" form -- a phone-appropriate collapse of :tv's multi-step wizard
 * (Source -> Credentials -> EPG -> Confirm) into one scrollable form, reusing the same
 * [com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository] calls and the same `CoreR` copy so
 * the concepts read identically between the two apps.
 *
 * Touch mechanics: the source-type picker is an [AreTabs] segmented control (the old full-width
 * chips-as-radios stack was a D-pad affordance); every field chains `ImeAction.Next` to
 * `FocusManager.moveFocus(Down)` with `Done` on the last one; the scroll column carries
 * `imePadding()` so the keyboard can never cover the submit button.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = AreIptvTheme.colors
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.added) {
        if (state.added) onDone()
    }

    val next = KeyboardOptions(imeAction = ImeAction.Next)
    val nextActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val submit: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        viewModel.submit()
    }
    val doneActions = KeyboardActions(onDone = { submit() })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(CoreR.string.onboarding_title),
            style = AreIptvTheme.typography.h1,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(CoreR.string.onboarding_subtitle),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )

        AreTabs(
            options = SourceType.entries,
            selected = state.type,
            label = { stringResource(it.titleRes()) },
            onSelect = viewModel::setType,
            modifier = Modifier.fillMaxWidth(),
            scrollable = false,
        )
        Text(
            text = stringResource(state.type.descriptionRes()),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
        )

        AreTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(CoreR.string.onboarding_portal_name_label),
            placeholder = stringResource(CoreR.string.onboarding_portal_name_placeholder),
            isError = OnboardingFieldError.NAME in state.fieldErrors,
            supportingText = if (OnboardingFieldError.NAME in state.fieldErrors) {
                stringResource(CoreR.string.onboarding_invalid_name)
            } else {
                null
            },
            keyboardOptions = next,
            keyboardActions = nextActions,
        )

        val urlIsInvalid = OnboardingFieldError.URL in state.fieldErrors
        val urlSupporting = if (urlIsInvalid) stringResource(CoreR.string.onboarding_invalid_url) else null

        when (state.type) {
            SourceType.XTREAM -> {
                AreTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_server_url_label),
                    placeholder = stringResource(CoreR.string.onboarding_server_url_placeholder),
                    isError = urlIsInvalid,
                    supportingText = urlSupporting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = nextActions,
                )
                AreTextField(
                    value = state.username,
                    onValueChange = viewModel::setUsername,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_username_label),
                    keyboardOptions = next,
                    keyboardActions = nextActions,
                )
                AreTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_password_label),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = nextActions,
                )
            }

            SourceType.M3U -> {
                AreTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_m3u_url_label),
                    placeholder = stringResource(CoreR.string.onboarding_m3u_url_placeholder),
                    isError = urlIsInvalid,
                    supportingText = urlSupporting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = nextActions,
                )
            }

            SourceType.STALKER -> {
                AreTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_portal_url_label),
                    isError = urlIsInvalid,
                    supportingText = urlSupporting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = nextActions,
                )
                val macIsInvalid = OnboardingFieldError.MAC in state.fieldErrors
                AreTextField(
                    value = state.mac,
                    onValueChange = viewModel::setMac,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.onboarding_mac_label),
                    placeholder = stringResource(CoreR.string.onboarding_mac_placeholder),
                    isError = macIsInvalid,
                    supportingText = stringResource(
                        if (macIsInvalid) CoreR.string.onboarding_mac_invalid else CoreR.string.onboarding_mac_helper,
                    ),
                    monospace = true,
                    keyboardOptions = next,
                    keyboardActions = nextActions,
                )
            }
        }

        AreTextField(
            value = state.epgUrl,
            onValueChange = viewModel::setEpgUrl,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(CoreR.string.onboarding_epg_url_label),
            placeholder = stringResource(CoreR.string.onboarding_epg_url_placeholder),
            supportingText = stringResource(CoreR.string.onboarding_epg_custom_helper),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = doneActions,
        )

        state.error?.let { message ->
            AreErrorState(
                message = message,
                modifier = Modifier.fillMaxWidth(),
                onRetry = submit,
            )
        }

        Spacer(Modifier.height(4.dp))

        AreButton(
            text = stringResource(CoreR.string.onboarding_add_playlist),
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
            variant = AreButtonVariant.Primary,
            size = AreButtonSize.Large,
            full = true,
            loading = state.isSubmitting,
        )
        AreButton(
            text = stringResource(CoreR.string.onboarding_skip_for_now),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            variant = AreButtonVariant.Ghost,
            full = true,
            enabled = !state.isSubmitting,
        )
    }
}

private fun SourceType.titleRes(): Int = when (this) {
    SourceType.XTREAM -> CoreR.string.onboarding_xtream_title
    SourceType.M3U -> CoreR.string.onboarding_m3u_title
    SourceType.STALKER -> CoreR.string.onboarding_stalker_title
}

private fun SourceType.descriptionRes(): Int = when (this) {
    SourceType.XTREAM -> CoreR.string.onboarding_xtream_desc
    SourceType.M3U -> CoreR.string.onboarding_m3u_desc
    SourceType.STALKER -> CoreR.string.onboarding_stalker_desc
}
