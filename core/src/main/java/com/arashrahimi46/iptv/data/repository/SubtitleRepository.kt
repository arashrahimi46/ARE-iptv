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

    /**
     * Downloads [sub], writing it to the cache and returning a sideloadable local file. Signs in
     * automatically using the saved account (and re-signs once if the cached token was rejected), and
     * returns an actionable failure message for every case the user can fix (quota, rate limit, creds).
     */
    suspend fun download(sub: OnlineSubtitle): Result<SideloadedSub> {
        val cred = settings.openSubsCredential.first()
            ?: return Result.failure(Exception("Connect your OpenSubtitles API key in Settings ▸ Subtitles first."))
        val user = settings.openSubsUsername.first()
        val phrase = settings.openSubsPhrase.first()
        if (user == null || phrase == null) {
            return Result.failure(Exception("Sign in to your OpenSubtitles account in Settings ▸ Subtitles to download."))
        }

        // Reuse the cached token; sign in automatically if we don't have one yet.
        var bearer = cachedBearer ?: when (val r = OpenSubtitlesClient.login(cred, user, phrase)) {
            is OpenSubtitlesClient.LoginResult.Success -> r.bearer.also { cachedBearer = it }
            is OpenSubtitlesClient.LoginResult.Failure -> return Result.failure(Exception(signInHelp(r.message)))
        }

        var link = OpenSubtitlesClient.requestDownloadLink(cred, bearer, sub.fileId)
        if (link is OpenSubtitlesClient.LinkResult.Unauthorized) {
            // Token expired/dropped -- sign in again automatically and retry once.
            bearer = when (val r = OpenSubtitlesClient.login(cred, user, phrase)) {
                is OpenSubtitlesClient.LoginResult.Success -> r.bearer.also { cachedBearer = it }
                is OpenSubtitlesClient.LoginResult.Failure -> {
                    cachedBearer = null
                    return Result.failure(Exception(signInHelp(r.message)))
                }
            }
            link = OpenSubtitlesClient.requestDownloadLink(cred, bearer, sub.fileId)
        }

        val url = when (val l = link) {
            is OpenSubtitlesClient.LinkResult.Ok -> l.link
            is OpenSubtitlesClient.LinkResult.Quota -> return Result.failure(Exception(l.message))
            is OpenSubtitlesClient.LinkResult.RateLimited ->
                return Result.failure(Exception("OpenSubtitles is rate-limiting requests -- wait a few seconds and try again."))
            is OpenSubtitlesClient.LinkResult.Unauthorized -> {
                cachedBearer = null
                return Result.failure(Exception("Your OpenSubtitles session keeps being rejected -- re-check your username/password in Settings ▸ Subtitles."))
            }
            is OpenSubtitlesClient.LinkResult.Error -> return Result.failure(Exception(l.message))
        }

        val bytes = OpenSubtitlesClient.fetchBytes(url)
            ?: return Result.failure(Exception("Subtitle download failed -- try again."))
        val ext = extensionOf(url)
        val file = File(cacheDir, "os_${sub.fileId}.$ext")
        file.writeBytes(bytes)
        return Result.success(SideloadedSub(Uri.fromFile(file), mimeFor(ext), sub.language))
    }

    private fun signInHelp(message: String): String = "$message Update your account in Settings ▸ Subtitles."

    private fun extensionOf(link: String): String =
        link.substringAfterLast('.', "").substringBefore('?').lowercase()
            .takeIf { it in setOf("srt", "vtt", "ass", "ssa", "sub") } ?: "srt"

    private fun mimeFor(ext: String): String = when (ext) {
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        else -> "application/x-subrip"
    }
}
