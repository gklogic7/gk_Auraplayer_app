package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build

enum class PlaybackStatus {
    PLAYING,
    PAUSED,
    STOPPED
}

data class AudioOutputRoute(
    val id: Int,
    val label: String,
    val isBluetooth: Boolean
)

/**
 * Battery-optimized offline audio playback engine wrapping MediaPlayer.
 * Releases active hardware locks immediately when STOPPED to preserve battery.
 */
class OfflineAudioController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var preparedUri: String? = null
    private var isPrepared: Boolean = false
    var onTrackCompleted: (() -> Unit)? = null

    fun prepareAndSeek(uriString: String, startPositionMs: Long, playImmediately: Boolean): Boolean {
        if (uriString.isBlank()) return false
        try {
            if (preparedUri != uriString || mediaPlayer == null || !isPrepared) {
                releasePlayer()
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    player.setDataSource(uri.path ?: return false)
                } else {
                    player.setDataSource(context, uri)
                }
                player.prepare()
                player.setOnCompletionListener {
                    onTrackCompleted?.invoke()
                }
                mediaPlayer = player
                preparedUri = uriString
                isPrepared = true
            }

            mediaPlayer?.let { player ->
                val safeSeek = startPositionMs.coerceIn(0L, player.duration.toLong().coerceAtLeast(1L))
                player.seekTo(safeSeek.toInt())
                if (playImmediately && !player.isPlaying) {
                    player.start()
                } else if (!playImmediately && player.isPlaying) {
                    player.pause()
                }
            }
            return true
        } catch (_: Exception) {
            isPrepared = false
            return false
        }
    }

    fun play(uriString: String, currentPositionMs: Long): Boolean {
        val player = mediaPlayer
        if (player != null && isPrepared && preparedUri == uriString) {
            return try {
                player.seekTo(currentPositionMs.toInt())
                player.start()
                true
            } catch (_: Exception) {
                prepareAndSeek(uriString, currentPositionMs, playImmediately = true)
            }
        }
        return prepareAndSeek(uriString, currentPositionMs, playImmediately = true)
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Stops playback, resets playback position to 0:00, and releases hardware decoder resources
     * for zero idle battery consumption.
     */
    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        } catch (_: Exception) {
        } finally {
            releasePlayer()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            if (isPrepared) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
        } catch (_: Exception) {
        }
    }

    fun currentPositionMs(): Long? {
        return try {
            if (isPrepared && mediaPlayer != null) {
                mediaPlayer?.currentPosition?.toLong()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getAvailableOutputRoutes(): List<AudioOutputRoute> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val routes = mutableListOf<AudioOutputRoute>()
        if (audioManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                val (label, isBt) = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio"
                        name to true
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "Wired Headphones" to false
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device Stereo Speaker" to false
                    else -> continue
                }
                if (routes.none { it.label == label }) {
                    routes.add(AudioOutputRoute(device.id, label, isBt))
                }
            }
        }
        if (routes.none { it.isBluetooth }) {
            routes.add(0, AudioOutputRoute(-2, "Studio Spatial AirPods / BT", true))
        }
        if (routes.none { !it.isBluetooth }) {
            routes.add(AudioOutputRoute(-1, "Internal Hi-Res Speaker", false))
        }
        return routes
    }

    fun releasePlayer() {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            preparedUri = null
            isPrepared = false
        }
    }
}
