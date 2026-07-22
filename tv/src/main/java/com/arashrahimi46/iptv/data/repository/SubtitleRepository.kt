package com.arashrahimi46.iptv.data.repository

import android.content.Context
import android.net.Uri
import com.arashrahimi46.iptv.data.parser.OnlineSubtitle
import com.arashrahimi46.iptv.data.parser.OpenSubtitlesClient
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Online subtitle search + download against OpenSubtitles, using the user's own key/account from
 * [UserSettings]. Search needs only the API key; download needs the account login (its token draws
 * on the daily quota) so it's requested lazily and cached in memory, re-issued once on expiry.
 * Downloaded files land in the app cache and are handed back as a local [Uri] the player sideloads.
 */
class SubtitleRepository(context: Context) {
    private val settings = UserSettings(context)
    private val cacheDir = File(context.cacheDir, "subtitles").apply { mkdirs() }

    @Volatile private var cachedBearer: String? = null

    /** A downloaded subtitle ready to sideload into ExoPlayer. */
    data class SideloadedSub(val uri: Uri, val mimeType: String, val languageCode: String)

    /** Searches for subtitles matching a movie ([year]) or episode ([season]/[episode]) in [languageCode]. */
    suspend fun search(
        query: String,
        languageCode: String,
        year: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): Result<List<OnlineSubtitle>> {
        val cred = settings.openSubsCredential.first()
            ?: return Result.failure(Exception("Connect OpenSubtitles in Settings first."))
        return Result.success(OpenSubtitlesClient.search(cred, query, languageCode, year, season, episode))
    }

    /** Downloads [sub], writing it to the cache and returning a sideloadable local file. */
    suspend fun download(sub: OnlineSubtitle): Result<SideloadedSub> {
        val cred = settings.openSubsCredential.first()
            ?: return Result.failure(Exception("Connect OpenSubtitles in Settings first."))
        val user = settings.openSubsUsername.first()
        val phrase = settings.openSubsPhrase.first()
        if (user == null || phrase == null) {
            return Result.failure(Exception("Sign in to your OpenSubtitles account in Settings to download."))
        }

        val link = try {
            val bearer = cachedBearer ?: loginOrThrow(cred, user, phrase)
            try {
                OpenSubtitlesClient.requestDownloadLink(cred, bearer, sub.fileId)
            } catch (e: OpenSubtitlesClient.UnauthorizedException) {
                // Token missing/expired -- re-login once and retry.
                OpenSubtitlesClient.requestDownloadLink(cred, loginOrThrow(cred, user, phrase), sub.fileId)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        } ?: return Result.failure(Exception("Couldn't get a download link -- your daily quota may be used up."))

        val bytes = OpenSubtitlesClient.fetchBytes(link)
            ?: return Result.failure(Exception("Subtitle download failed -- try again."))
        val ext = extensionOf(link)
        val file = File(cacheDir, "os_${sub.fileId}.$ext")
        file.writeBytes(bytes)
        return Result.success(SideloadedSub(Uri.fromFile(file), mimeFor(ext), sub.language))
    }

    private suspend fun loginOrThrow(cred: String, user: String, phrase: String): String =
        when (val r = OpenSubtitlesClient.login(cred, user, phrase)) {
            is OpenSubtitlesClient.LoginResult.Success -> r.bearer.also { cachedBearer = it }
            is OpenSubtitlesClient.LoginResult.Failure -> {
                cachedBearer = null
                throw Exception(r.message)
            }
        }

    private fun extensionOf(link: String): String =
        link.substringAfterLast('.', "").substringBefore('?').lowercase()
            .takeIf { it in setOf("srt", "vtt", "ass", "ssa", "sub") } ?: "srt"

    private fun mimeFor(ext: String): String = when (ext) {
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        else -> "application/x-subrip"
    }
}
