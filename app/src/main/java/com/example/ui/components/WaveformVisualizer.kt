package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackStatus
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.AmberWaveform
import com.example.ui.theme.WarmSandDim
import kotlin.math.sin

/**
 * Compact 6-bar waveform icon matching the top-right waveform badge in the reference image.
 */
@Composable
fun MiniWaveformBadge(
    playbackStatus: PlaybackStatus,
    waveformPhase: Float,
    accentColor: Color = AmberWaveform,
    modifier: Modifier = Modifier
) {
    val activeWeight by animateFloatAsState(
        targetValue = if (playbackStatus == PlaybackStatus.PLAYING) 1f else 0.25f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "mini_waveform_weight"
    )

    Canvas(modifier = modifier.size(width = 22.dp, height = 16.dp)) {
        val barCount = 6
        val spacing = size.width * 0.08f
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val baseHeights = floatArrayOf(0.35f, 0.75f, 0.55f, 0.95f, 0.45f, 0.30f)

        for (i in 0 until barCount) {
            val dynamicMod = if (playbackStatus == PlaybackStatus.PLAYING) {
                0.28f * sin(waveformPhase + i * 0.9f)
            } else {
                0f
            }
            val normalizedHeight = (baseHeights[i] * (0.45f + 0.55f * activeWeight) + dynamicMod)
                .coerceIn(0.22f, 1.0f)
            val barHeight = size.height * normalizedHeight
            val top = (size.height - barHeight) / 2f
            val left = i * (barWidth + spacing)

            drawRoundRect(
                color = if (playbackStatus == PlaybackStatus.PLAYING && (i == 1 || i == 3)) {
                    accentColor
                } else {
                    AlabasterWhite.copy(alpha = 0.78f)
                },
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Interactive 32-bar waveform scrubber & visualizer inside the capsule control deck.
 * Supports tap and horizontal drag to seek through the track.
 */
@Composable
fun InteractiveWaveformVisualizer(
    amplitudes: List<Float>,
    progressFraction: Float,
    playbackStatus: PlaybackStatus,
    waveformPhase: Float,
    accentColor: Color,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val energyScale by animateFloatAsState(
        targetValue = when (playbackStatus) {
            PlaybackStatus.PLAYING -> 1.0f
            PlaybackStatus.PAUSED -> 0.72f
            PlaybackStatus.STOPPED -> 0.38f
        },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "waveform_energy_scale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .testTag("interactive_waveform_canvas")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        onSeekFraction((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        onSeekFraction((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
            }
    ) {
        val bars = if (amplitudes.isNotEmpty()) amplitudes else listOf(0.4f, 0.7f, 0.5f, 0.8f)
        val count = bars.size
        val gap = (size.width * 0.011f).coerceAtLeast(2f)
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)

        for (index in 0 until count) {
            val barPositionFraction = index.toFloat() / count.toFloat()
            val isPlayed = barPositionFraction <= progressFraction
            val isPlayhead = kotlin.math.abs(barPositionFraction - progressFraction) < (1.2f / count)

            val liveRipple = if (playbackStatus == PlaybackStatus.PLAYING) {
                0.14f * sin(waveformPhase * 1.4f + index * 0.45f)
            } else {
                0f
            }

            val rawAmp = (bars[index] * energyScale + liveRipple).coerceIn(0.16f, 1.0f)
            val barHeight = size.height * rawAmp
            val left = index * (barWidth + gap)
            val top = (size.height - barHeight) / 2f

            val barColor = when {
                isPlayhead -> accentColor
                isPlayed -> AlabasterWhite.copy(alpha = 0.92f)
                else -> WarmSandDim.copy(alpha = 0.42f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
