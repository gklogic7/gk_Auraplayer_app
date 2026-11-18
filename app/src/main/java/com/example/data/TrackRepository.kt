package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.audio.OfflineAudioSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrackRepository(
    private val context: Context,
    private val trackDao: TrackDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.observeAllTracks()

    private val palettePool = listOf(
        Triple(0xFF5A4232L, 0xFF221813L, 0xFFF5B041L), // Warm Bronze Sunset (Reference)
        Triple(0xFF3E4C5EL, 0xFF171E26L, 0xFF7FB3D5L), // Midnight Slate Blue
        Triple(0xFF5B3A42L, 0xFF231519L, 0xFFE59866L), // Dusk Terracotta Rose
        Triple(0xFF364E42L, 0xFF15201AL, 0xFF82E0AAL), // Deep Moss Forest
        Triple(0xFF4D3E5CL, 0xFF1D1724L, 0xFFD7BDE2L)  // Velvet Twilight Plum
    )

    suspend fun ensureSeededTracks() = withContext(Dispatchers.IO) {
        val existing = trackDao.getAllTracksOnce()
        if (existing.isNotEmpty()) return@withContext

        val picheTereUri = OfflineAudioSynthesizer.ensureTrackWavFile(
            context = context,
            fileName = "piche_tere_offline.wav",
            baseFreqHz = 220.0,
            chordRatios = doubleArrayOf(1.0, 1.25, 1.5, 1.125),
            durationSeconds = 120 // 2:00 total -> 0:06 elapsed & -1:54 remaining matches reference UI!
        )
        val amberDriftUri = OfflineAudioSynthesizer.ensureTrackWavFile(
            context = context,
            fileName = "amber_drift_offline.wav",
            baseFreqHz = 196.0,
            chordRatios = doubleArrayOf(1.0, 1.2, 1.5, 1.333),
            durationSeconds = 96
        )
        val midnightEchoUri = OfflineAudioSynthesizer.ensureTrackWavFile(
            context = context,
            fileName = "midnight_echo_offline.wav",
            baseFreqHz = 246.94,
            chordRatios = doubleArrayOf(1.0, 1.189, 1.498, 1.335),
            durationSeconds = 108
        )
        val velvetHorizonUri = OfflineAudioSynthesizer.ensureTrackWavFile(
            context = context,
            fileName = "velvet_horizon_offline.wav",
            baseFreqHz = 174.61,
            chordRatios = doubleArrayOf(1.0, 1.25, 1.498, 1.681),
            durationSeconds = 88
        )

        val seeded = listOf(
            TrackEntity(
                title = "Piche Tere",
                artist = "Kunwarr, Dishant",
                album = "Times Music Originals",
                durationMs = 120_000L,
                audioUriString = picheTereUri.toString(),
                artworkResName = "cover_piche_tere",
                primaryColorHex = 0xFF5E4534L,
                secondaryColorHex = 0xFF211812L,
                accentColorHex = 0xFFF5B041L,
                waveformCsv = "0.32,0.54,0.76,0.48,0.88,0.95,0.62,0.44,0.72,0.90,0.84,0.58,0.40,0.68,0.92,0.78,0.52,0.38,0.74,0.96,0.82,0.60,0.46,0.80,0.94,0.66,0.48,0.70,0.86,0.58,0.42,0.28",
                isPinnedToCanvas = true,
                canvasPinXFraction = 0.06f,
                canvasPinYFraction = 0.14f
            ),
            TrackEntity(
                title = "Golden Hour Solitude",
                artist = "Aarav & The Echoes",
                album = "Sunset Sessions",
                durationMs = 96_000L,
                audioUriString = amberDriftUri.toString(),
                artworkResName = "vinyl_sunset",
                primaryColorHex = 0xFF6B4435L,
                secondaryColorHex = 0xFF261712L,
                accentColorHex = 0xFFE59866L,
                waveformCsv = "0.24,0.40,0.58,0.74,0.62,0.46,0.80,0.94,0.88,0.64,0.50,0.72,0.86,0.68,0.44,0.56,0.78,0.92,0.76,0.54,0.38,0.66,0.84,0.70,0.52,0.74,0.88,0.62,0.48,0.60,0.36,0.22",
                isPinnedToCanvas = true,
                canvasPinXFraction = 0.72f,
                canvasPinYFraction = 0.14f
            ),
            TrackEntity(
                title = "Monsoon Glass",
                artist = "Noor Collective",
                album = "Acoustic Nocturnes",
                durationMs = 108_000L,
                audioUriString = midnightEchoUri.toString(),
                artworkResName = "vinyl_slate",
                primaryColorHex = 0xFF3B4D5EL,
                secondaryColorHex = 0xFF161D24L,
                accentColorHex = 0xFF7FB3D5L,
                waveformCsv = "0.20,0.36,0.52,0.44,0.68,0.84,0.92,0.74,0.56,0.42,0.64,0.88,0.96,0.78,0.60,0.48,0.72,0.86,0.66,0.50,0.76,0.90,0.82,0.58,0.40,0.62,0.78,0.68,0.52,0.44,0.30,0.18",
                isPinnedToCanvas = false,
                canvasPinXFraction = 0.06f,
                canvasPinYFraction = 0.74f
            ),
            TrackEntity(
                title = "Velvet Horizon",
                artist = "Kabeer Sen",
                album = "Analog Tape Vol. 4",
                durationMs = 88_000L,
                audioUriString = velvetHorizonUri.toString(),
                artworkResName = "vinyl_moss",
                primaryColorHex = 0xFF385043L,
                secondaryColorHex = 0xFF151F1AL,
                accentColorHex = 0xFF82E0AAL,
                waveformCsv = "0.28,0.50,0.70,0.86,0.64,0.48,0.60,0.82,0.94,0.72,0.54,0.68,0.90,0.76,0.52,0.38,0.62,0.84,0.88,0.66,0.46,0.74,0.92,0.78,0.56,0.42,0.68,0.80,0.60,0.44,0.32,0.22",
                isPinnedToCanvas = false,
                canvasPinXFraction = 0.72f,
                canvasPinYFraction = 0.74f
            )
        )
        trackDao.insertTracks(seeded)
    }

    /**
     * Imports user-picked local audio files via Storage Access Framework.
     */
    suspend fun importAudioFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        for ((index, uri) in uris.withIndex()) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Non-persistable URI; still readable for current session
            }
            if (insertTrackFromUri(uri, index)) {
                importedCount++
            }
        }
        importedCount
    }

    /**
     * Scans a user-selected directory tree (OpenDocumentTree) for offline audio tracks
     * without requiring broad storage permissions, plus queries MediaStore for accessible audio.
     */
    suspend fun scanFolderTreeAndMediaStore(treeUri: Uri?): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        if (treeUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            try {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    var idx = 0
                    while (cursor.moveToNext() && idx < 40) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: ""
                        val mime = cursor.getString(mimeCol) ?: ""
                        val isAudio = mime.startsWith("audio/") ||
                            name.endsWith(".mp3", ignoreCase = true) ||
                            name.endsWith(".wav", ignoreCase = true) ||
                            name.endsWith(".m4a", ignoreCase = true) ||
                            name.endsWith(".ogg", ignoreCase = true) ||
                            name.endsWith(".flac", ignoreCase = true)
                        if (isAudio) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            if (insertTrackFromUri(docUri, idx)) {
                                addedCount++
                            }
                            idx++
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Also query MediaStore.Audio.Media for any accessible device audio files
        addedCount += scanAccessibleMediaStoreAudio()
        addedCount
    }

    suspend fun scanAccessibleMediaStoreAudio(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                var idx = 0
                while (cursor.moveToNext() && idx < 35) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    if (trackDao.getTrackByUri(contentUri.toString()) == null) {
                        val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Track $id"
                        val artist = cursor.getString(artistCol)?.takeIf {
                            it.isNotBlank() && it != "<unknown>"
                        } ?: "Local Artist"
                        val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Local Library"
                        val duration = cursor.getLong(durCol).coerceAtLeast(15_000L)
                        val palette = palettePool[idx % palettePool.size]
                        val waveform = OfflineAudioSynthesizer.extractOrGenerateWaveform(
                            context = context,
                            uri = contentUri,
                            seedText = "$title-$artist"
                        )
                        trackDao.insertTrack(
                            TrackEntity(
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                audioUriString = contentUri.toString(),
                                artworkResName = "vinyl_sunset",
                                primaryColorHex = palette.first,
                                secondaryColorHex = palette.second,
                                accentColorHex = palette.third,
                                waveformCsv = waveform
                            )
                        )
                        count++
                    }
                    idx++
                }
            }
        } catch (_: Exception) {
            // Permission not granted or empty store; handled gracefully
        }
        count
    }

    private suspend fun insertTrackFromUri(uri: Uri, paletteIdx: Int): Boolean {
        val existing = trackDao.getTrackByUri(uri.toString())
        if (existing != null) return false

        var fallbackName = uri.lastPathSegment?.substringAfterLast('/') ?: "Offline Track"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) {
                        fallbackName = c.getString(col) ?: fallbackName
                    }
                }
            }
        } catch (_: Exception) {
        }
        val cleanFallbackTitle = fallbackName.substringBeforeLast('.')

        var title = cleanFallbackTitle
        var artist = "Local Studio Artist"
        var album = "Imported Audio"
        var durationMs = 120_000L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.let { durationMs = it }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val palette = palettePool[paletteIdx % palettePool.size]
        val waveform = OfflineAudioSynthesizer.extractOrGenerateWaveform(
            context = context,
            uri = uri,
            seedText = "$title-$artist"
        )

        trackDao.insertTrack(
            TrackEntity(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                audioUriString = uri.toString(),
                artworkResName = "vinyl_slate",
                primaryColorHex = palette.first,
                secondaryColorHex = palette.second,
                accentColorHex = palette.third,
                waveformCsv = waveform
            )
        )
        return true
    }

    suspend fun setCanvasPinned(trackId: Long, pinned: Boolean, xFraction: Float, yFraction: Float) {
        trackDao.updateCanvasPin(trackId, pinned, xFraction, yFraction)
    }

    suspend fun setHomePinned(trackId: Long, pinned: Boolean) {
        trackDao.updateHomePin(trackId, pinned)
    }
}
