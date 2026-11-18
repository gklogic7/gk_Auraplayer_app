package com.example.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Generates ultra-lightweight offline playable WAV audio files and extracts normalized
 * 32-bar amplitude waveforms so the player operates 100% offline with low memory & battery impact.
 */
object OfflineAudioSynthesizer {

    private const val SAMPLE_RATE = 11025 // Low-power mono PCM for tiny file size & instant loading

    suspend fun ensureTrackWavFile(
        context: Context,
        fileName: String,
        baseFreqHz: Double,
        chordRatios: DoubleArray,
        durationSeconds: Int
    ): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "offline_audio").apply { mkdirs() }
        val file = File(dir, fileName)
        if (file.exists() && file.length() > 1024L) {
            return@withContext Uri.fromFile(file)
        }

        val totalSamples = SAMPLE_RATE * durationSeconds
        val dataSize = totalSamples * 2 // 16-bit mono

        BufferedOutputStream(FileOutputStream(file), 16 * 1024).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // AudioFormat = PCM
            header.putShort(1) // NumChannels = 1 (Mono)
            header.putInt(SAMPLE_RATE)
            header.putInt(SAMPLE_RATE * 2) // ByteRate
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val chunkSamples = 2048
            val sampleBuffer = ByteBuffer.allocate(chunkSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            var sampleIndex = 0
            val twoPiOverRate = (2.0 * PI) / SAMPLE_RATE

            while (sampleIndex < totalSamples) {
                sampleBuffer.clear()
                val limit = minOf(chunkSamples, totalSamples - sampleIndex)
                for (i in 0 until limit) {
                    val globalSample = sampleIndex + i
                    val timeSec = globalSample.toDouble() / SAMPLE_RATE
                    val chordIdx = ((timeSec / 2.5).toInt()) % chordRatios.size
                    val freq = baseFreqHz * chordRatios[chordIdx]

                    // Warm acoustic-like envelope with gentle pulse
                    val beatPhase = (timeSec % 0.75) / 0.75
                    val envelope = (0.35 + 0.65 * (1.0 - beatPhase * 0.7))
                    val wave = (
                        0.60 * sin(globalSample * freq * twoPiOverRate) +
                            0.25 * sin(globalSample * (freq * 1.5) * twoPiOverRate) +
                            0.15 * sin(globalSample * (freq * 2.0) * twoPiOverRate)
                        ) * envelope

                    val pcmValue = (wave * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
                    sampleBuffer.putShort(pcmValue)
                }
                out.write(sampleBuffer.array(), 0, limit * 2)
                sampleIndex += limit
            }
            out.flush()
        }

        Uri.fromFile(file)
    }

    /**
     * Computes a deterministic, musical 32-bar waveform profile from track metadata or file bytes.
     */
    suspend fun extractOrGenerateWaveform(
        context: Context,
        uri: Uri,
        seedText: String,
        barCount: Int = 32
    ): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(16384)
                val readBytes = input.read(buffer)
                if (readBytes > 256) {
                    val bars = FloatArray(barCount)
                    val step = (readBytes - 44).coerceAtLeast(barCount) / barCount
                    for (b in 0 until barCount) {
                        var sum = 0f
                        val start = 44 + b * step
                        val count = minOf(step, readBytes - start).coerceAtLeast(1)
                        for (j in 0 until count) {
                            sum += abs(buffer[start + j].toInt()) / 128f
                        }
                        val normalized = (sum / count).coerceIn(0.18f, 0.98f)
                        bars[b] = normalized
                    }
                    // Normalize dynamic range so visual peaks are crisp
                    val minVal = bars.minOrNull() ?: 0.2f
                    val maxVal = (bars.maxOrNull() ?: 0.9f).coerceAtLeast(minVal + 0.1f)
                    return@withContext bars.joinToString(",") { raw ->
                        val scaled = 0.20f + 0.78f * ((raw - minVal) / (maxVal - minVal))
                        "%.2f".format(java.util.Locale.US, scaled.coerceIn(0.18f, 0.98f))
                    }
                }
            }
        } catch (_: Exception) {
            // Fall back to deterministic musical envelope below
        }

        val hash = seedText.hashCode()
        (0 until barCount).joinToString(",") { idx ->
            val phase = (idx.toFloat() / barCount) * PI.toFloat()
            val arch = sin(phase) * 0.55f
            val detail = (((abs(hash + idx * 37) % 100) / 100f) * 0.38f)
            val amp = (0.18f + arch + detail).coerceIn(0.18f, 0.96f)
            "%.2f".format(java.util.Locale.US, amp)
        }
    }
}
