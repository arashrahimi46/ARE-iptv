package com.arashrahimi46.iptv.ui.player

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer

/**
 * Positive audio delay for the player's "Audio sync" control. Shifts audio LATER than video by
 * [delayMs] milliseconds by inserting that much silence ahead of the stream; decreasing the delay
 * drops already-inserted audio so video catches back up. [delayMs] is read live off the playback
 * thread so the HUD stepper can retune it mid-stream without rebuilding the player.
 *
 * // monolean: positive-only by construction. Making audio play EARLIER than video (a negative
 * // delay) would mean delaying the VIDEO renderer, which an AudioProcessor can't reach -- so the
 * // UI clamps to 0..+N ms (see AUDIO_DELAY_MIN_MS in SubtitleMenu.kt). A true bidirectional,
 * // frame-accurate A/V offset is out of scope for this seam.
 */
class DelayAudioProcessor : BaseAudioProcessor() {
    /** Target delay in ms; set from the UI thread, read on the audio thread. */
    @Volatile
    var delayMs: Int = 0

    /** Silence bytes currently inserted ahead of the stream (always frame-aligned). */
    private var insertedBytes: Int = 0

    // Delay never changes the PCM format -- only the timeline. Output format == input format.
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    /** [delayMs] expressed as whole-frame bytes for the current format (0 when unconfigured). */
    private fun targetDelayBytes(): Int {
        val fmt = inputAudioFormat
        if (fmt == AudioProcessor.AudioFormat.NOT_SET) return 0
        val target = delayMs.toLong().coerceAtLeast(0L) * fmt.sampleRate * fmt.bytesPerFrame / 1000L
        // Frame-align so we only ever insert/drop whole audio frames.
        return (target - target % fmt.bytesPerFrame).toInt()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val target = targetDelayBytes()
        when {
            insertedBytes < target -> {
                // Delay grew (or these are the first frames): emit the missing silence, then input.
                val silence = target - insertedBytes
                val out = replaceOutputBuffer(silence + remaining)
                out.put(ByteArray(silence)) // zero-filled -> silence
                out.put(inputBuffer)
                insertedBytes = target
                out.flip()
            }
            insertedBytes > target -> {
                // Delay shrank: drop up to the surplus from this input so video catches back up.
                val drop = minOf(insertedBytes - target, remaining)
                inputBuffer.position(inputBuffer.position() + drop)
                insertedBytes -= drop
                val rest = inputBuffer.remaining()
                val out = replaceOutputBuffer(rest)
                out.put(inputBuffer)
                out.flip()
            }
            else -> {
                val out = replaceOutputBuffer(remaining)
                out.put(inputBuffer)
                out.flip()
            }
        }
    }

    // A flush/seek discards the inserted silence, so re-insert it from scratch afterwards.
    override fun onFlush() {
        insertedBytes = 0
    }

    override fun onReset() {
        insertedBytes = 0
    }
}

/**
 * A [DefaultRenderersFactory] that routes audio through [delayProcessor] so the "Audio sync" control
 * can shift audio relative to video at runtime. Only the audio sink is customised; everything else is
 * stock. The user processor runs BEFORE Media3's own silence-skip + speed (Sonic) chain, so playback
 * speed still works unchanged.
 */
class DelayRenderersFactory(
    context: Context,
    private val delayProcessor: DelayAudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink =
        DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(delayProcessor))
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
}
