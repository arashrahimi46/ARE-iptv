package com.arashrahimi46.iptv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.settings.ExternalPlayerChoice
import com.arashrahimi46.iptv.data.settings.PinHasher
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the real Settings screen (Phase 4): every flow here is a live read
 * off [UserSettings]' DataStore, and every setter writes straight back to it
 * (off the main thread, via [UserSettings]' own `dataStore.edit`). Theme and
 * reduced-motion take effect immediately in the running app because
 * [com.arashrahimi46.iptv.MainActivity] collects [UserSettings.isDarkTheme] /
 * [UserSettings.isReducedMotion] directly and feeds them into
 * [com.arashrahimi46.iptv.ui.theme.AreIptvTheme] at the composition root --
 * see report for how this was verified.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = UserSettings(app)

    private fun <T> flowState(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val isDarkTheme: StateFlow<Boolean> = flowState(settings.isDarkTheme, true)
    val isReducedMotion: StateFlow<Boolean> = flowState(settings.isReducedMotion, false)
    val isHardwareDecoding: StateFlow<Boolean> = flowState(settings.isHardwareDecoding, true)
    val isAutoplayNextEpisode: StateFlow<Boolean> = flowState(settings.isAutoplayNextEpisode, true)
    val isPictureInPicture: StateFlow<Boolean> = flowState(settings.isPictureInPicture, false)
    val externalPlayer: StateFlow<ExternalPlayerChoice> = flowState(settings.externalPlayer, ExternalPlayerChoice.BUILT_IN)
    val isParentalLockEnabled: StateFlow<Boolean> = flowState(settings.isParentalLockEnabled, false)
    val hasPinSet: StateFlow<Boolean> = flowState(settings.parentalPinHash.map { it != null }, false)

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settings.setDarkTheme(enabled) }
    fun setReducedMotion(enabled: Boolean) = viewModelScope.launch { settings.setReducedMotion(enabled) }
    fun setHardwareDecoding(enabled: Boolean) = viewModelScope.launch { settings.setHardwareDecoding(enabled) }
    fun setAutoplayNextEpisode(enabled: Boolean) = viewModelScope.launch { settings.setAutoplayNextEpisode(enabled) }

    /** Storage-only -- see [UserSettings.isPictureInPicture] doc comment. */
    fun setPictureInPicture(enabled: Boolean) = viewModelScope.launch { settings.setPictureInPicture(enabled) }

    fun setExternalPlayer(choice: ExternalPlayerChoice) = viewModelScope.launch { settings.setExternalPlayer(choice) }

    /** Turning the lock ON never needs a PIN check; the caller (SettingsScreen) only calls this after a PIN exists. */
    fun setParentalLockEnabled(enabled: Boolean) = viewModelScope.launch { settings.setParentalLockEnabled(enabled) }

    /** Hashes+salts [pin] via [PinHasher] -- the raw PIN is never persisted. */
    fun setPin(pin: String) {
        viewModelScope.launch {
            val salt = PinHasher.newSalt()
            settings.setParentalPin(PinHasher.hash(pin, salt), salt)
        }
    }

    /** Suspend so the PIN-entry dialog can await a real DataStore read before showing success/failure. */
    suspend fun verifyPin(pin: String): Boolean {
        val hash = settings.parentalPinHash.first() ?: return false
        val salt = settings.parentalPinSalt.first() ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(app) as T
            }
    }
}
