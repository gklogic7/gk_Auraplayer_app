package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val durationMs: Long,
    val audioUriString: String,
    val artworkResName: String? = null,
    val artworkUriString: String? = null,
    val primaryColorHex: Long = 0xFF5A4232,
    val secondaryColorHex: Long = 0xFF241A14,
    val accentColorHex: Long = 0xFFF5B041,
    val waveformCsv: String,
    val isPinnedToCanvas: Boolean = false,
    val canvasPinXFraction: Float = 0.08f,
    val canvasPinYFraction: Float = 0.14f,
    val isPinnedToHome: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    fun parsedWaveform(): List<Float> {
        if (waveformCsv.isBlank()) return DefaultWaveform
        return waveformCsv.split(",")
            .mapNotNull { it.trim().toFloatOrNull()?.coerceIn(0.12f, 1.0f) }
            .ifEmpty { DefaultWaveform }
    }

    companion object {
        val DefaultWaveform = listOf(
            0.28f, 0.45f, 0.62f, 0.38f, 0.78f, 0.92f, 0.54f, 0.40f,
            0.68f, 0.85f, 0.96f, 0.72f, 0.48f, 0.60f, 0.88f, 0.74f,
            0.52f, 0.35f, 0.66f, 0.90f, 0.82f, 0.58f, 0.44f, 0.76f,
            0.94f, 0.64f, 0.50f, 0.70f, 0.84f, 0.56f, 0.36f, 0.24f
        )
    }
}
