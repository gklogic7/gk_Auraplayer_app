package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioOutputRoute
import com.example.data.TrackEntity
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.AmberWaveform
import com.example.ui.theme.CoralPinAccent
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SageEcoAccent
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.WarmSandMuted
import com.example.viewmodel.CapsuleMode
import com.example.viewmodel.CapsulePopover
import com.example.viewmodel.PlayerUiState
import kotlin.math.roundToInt

/**
 * Transparent floating workspace over the Android Home Screen wallpaper.
 * Renders ONLY the floating music capsule, pinned floating mini-cards, and compact floating pills—
 * never an opaque full-screen background.
 */
@Composable
fun SpatialBackdropCanvas(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSetCapsuleMode: (CapsuleMode) -> Unit,
    onCollapseToEdgePill: (Float) -> Unit,
    onExpandFromEdgePill: () -> Unit,
    onEnterPipMode: () -> Unit,
    onUpdateCapsuleOffset: (Float, Float, Float, Float) -> Unit,
    onResizeCapsuleScale: (Float) -> Unit,
    onSetScalePreset: (Float) -> Unit,
    onResetCapsulePosition: () -> Unit,
    onToggleWaveformStrip: () -> Unit,
    onTogglePopover: (CapsulePopover) -> Unit,
    onDismissPopover: () -> Unit,
    onSelectAudioRoute: (AudioOutputRoute) -> Unit,
    onTogglePinCanvas: (TrackEntity) -> Unit,
    onUpdateCanvasPinPosition: (TrackEntity, Float, Float) -> Unit,
    onPinShortcutHome: (TrackEntity) -> Unit,
    onPinWidgetHome: () -> Unit,
    onSelectTrackIndex: (Int) -> Unit,
    onPlayTrackById: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onScanMusicFolder: () -> Unit,
    onPickAudioFiles: () -> Unit,
    onToggleDoNotDisturb: () -> Unit,
    modifier: Modifier = Modifier
) {
    // In Picture-in-Picture mode, render strictly the compact floating PiP capsule
    if (uiState.isInPipMode) {
        DraggableMusicCapsule(
            uiState = uiState,
            onTogglePlayPause = onTogglePlayPause,
            onStopPlayback = onStopPlayback,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSeekFraction = onSeekFraction,
            onSetCapsuleMode = onSetCapsuleMode,
            onExpandFromEdgePill = onExpandFromEdgePill,
            onEnterPipMode = onEnterPipMode,
            onDragCapsule = { _, _ -> },
            onResizeCapsuleScale = onResizeCapsuleScale,
            onSetScalePreset = onSetScalePreset,
            onResetCapsulePosition = onResetCapsulePosition,
            onToggleWaveformStrip = onToggleWaveformStrip,
            onTogglePopover = onTogglePopover,
            onSelectAudioRoute = onSelectAudioRoute,
            onTogglePinCanvas = onTogglePinCanvas,
            onPinShortcutHome = onPinShortcutHome,
            onPinWidgetHome = onPinWidgetHome,
            onSelectTrackIndex = onSelectTrackIndex,
            onSearchQueryChange = onSearchQueryChange,
            onScanMusicFolder = onScanMusicFolder,
            onPickAudioFiles = onPickAudioFiles,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    val activeTrack = uiState.currentTrack

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("spatial_backdrop_canvas")
            // Outside-tap detector: collapses floating panel to Edge Dock Pill when tapping empty wallpaper area
            .pointerInput(uiState.capsuleMode, uiState.activePopover) {
                detectTapGestures {
                    when {
                        uiState.activePopover != CapsulePopover.NONE -> onDismissPopover()
                        uiState.capsuleMode == CapsuleMode.QUEUE_DRAWER ||
                            uiState.capsuleMode == CapsuleMode.REFERENCE_CARD -> {
                            val maxAbsX = (size.width * 0.32f).coerceAtLeast(80f)
                            onCollapseToEdgePill(maxAbsX)
                        }
                        uiState.capsuleMode == CapsuleMode.EDGE_DOCK_PILL -> {
                            onExpandFromEdgePill()
                        }
                    }
                }
            }
    ) {
        val density = LocalDensity.current
        val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val maxDragX = (canvasWidthPx * 0.36f).coerceAtLeast(80f)
        val maxDragY = (canvasHeightPx * 0.34f).coerceAtLeast(100f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // 1. Floating Clock & Date Pill ("4:22  Sun 24 May") — visible in Reference Card mode
            AnimatedVisibility(
                visible = uiState.capsuleMode == CapsuleMode.REFERENCE_CARD,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xB31F1814))
                        .border(0.6.dp, GlassBorderSubtle, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .testTag("ambient_clock_header")
                ) {
                    Text(
                        text = "4:22",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = AlabasterWhite.copy(alpha = 0.94f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sun 24 May",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = AlabasterWhite.copy(alpha = 0.88f)
                    )
                }
            }

            // 2. Draggable Pinned Track Mini-Cards Floating Over the Wallpaper
            uiState.pinnedCanvasTracks.forEach { pinnedTrack ->
                val isPlayingThis = activeTrack?.id == pinnedTrack.id
                PinnedCanvasTrackBadge(
                    track = pinnedTrack,
                    isActiveTrack = isPlayingThis,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onTapPlay = { onPlayTrackById(pinnedTrack.id) },
                    onUnpin = { onTogglePinCanvas(pinnedTrack) },
                    onUpdateNormalizedPosition = { nx, ny ->
                        onUpdateCanvasPinPosition(pinnedTrack, nx, ny)
                    }
                )
            }

            // 3. Always-Floating Resizable & Draggable Music Capsule
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(
                            uiState.capsuleOffsetX.roundToInt(),
                            uiState.capsuleOffsetY.roundToInt()
                        )
                    }
                    // Consume taps inside the capsule so they don't trigger the outside-tap collapse
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    }
            ) {
                DraggableMusicCapsule(
                    uiState = uiState,
                    onTogglePlayPause = onTogglePlayPause,
                    onStopPlayback = onStopPlayback,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekFraction = onSeekFraction,
                    onSetCapsuleMode = onSetCapsuleMode,
                    onExpandFromEdgePill = onExpandFromEdgePill,
                    onEnterPipMode = onEnterPipMode,
                    onDragCapsule = { dx, dy ->
                        onUpdateCapsuleOffset(dx, dy, maxDragX, maxDragY)
                    },
                    onResizeCapsuleScale = onResizeCapsuleScale,
                    onSetScalePreset = onSetScalePreset,
                    onResetCapsulePosition = onResetCapsulePosition,
                    onToggleWaveformStrip = onToggleWaveformStrip,
                    onTogglePopover = onTogglePopover,
                    onSelectAudioRoute = onSelectAudioRoute,
                    onTogglePinCanvas = onTogglePinCanvas,
                    onPinShortcutHome = onPinShortcutHome,
                    onPinWidgetHome = onPinWidgetHome,
                    onSelectTrackIndex = onSelectTrackIndex,
                    onSearchQueryChange = onSearchQueryChange,
                    onScanMusicFolder = onScanMusicFolder,
                    onPickAudioFiles = onPickAudioFiles
                )
            }

            // 4. Floating Feedback Toast Pill
            AnimatedVisibility(
                visible = !uiState.feedbackMessage.isNullOrBlank(),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 58.dp)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(SmokedGlassElevated)
                        .border(0.8.dp, AmberWaveform.copy(alpha = 0.65f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("feedback_toast_pill")
                ) {
                    Text(
                        text = uiState.feedbackMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterWhite
                    )
                }
            }

            // 5. Bottom Floating Pills ("☾ Do Not Disturb" | "Tracks" | "Float PiP")
            AnimatedVisibility(
                visible = uiState.capsuleMode != CapsuleMode.EDGE_DOCK_PILL,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xCC241C17))
                            .border(0.6.dp, GlassBorderSubtle, CircleShape)
                            .clickable { onToggleDoNotDisturb() }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .testTag("do_not_disturb_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = if (uiState.isDoNotDisturbActive) {
                                AlabasterWhite.copy(alpha = 0.88f)
                            } else {
                                SageEcoAccent
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (uiState.isDoNotDisturbActive) {
                                stringResource(R.string.do_not_disturb)
                            } else {
                                stringResource(R.string.battery_optimized_active)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AlabasterWhite.copy(alpha = 0.90f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xCC241C17))
                            .border(0.6.dp, GlassBorderSubtle, CircleShape)
                            .clickable {
                                val target = if (uiState.capsuleMode == CapsuleMode.QUEUE_DRAWER) {
                                    CapsuleMode.REFERENCE_CARD
                                } else {
                                    CapsuleMode.QUEUE_DRAWER
                                }
                                onSetCapsuleMode(target)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("quick_library_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = AmberWaveform,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${uiState.tracks.size} Tracks",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AlabasterWhite.copy(alpha = 0.90f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xCC241C17))
                            .border(0.6.dp, AmberWaveform.copy(alpha = 0.45f), CircleShape)
                            .clickable { onEnterPipMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("bottom_float_pip_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = stringResource(R.string.cd_float_pip),
                            tint = AmberWaveform,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Float PiP",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AlabasterWhite.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating, draggable quick-play track pin placed directly over the transparent Home Screen window.
 */
@Composable
private fun PinnedCanvasTrackBadge(
    track: TrackEntity,
    isActiveTrack: Boolean,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    onTapPlay: () -> Unit,
    onUnpin: () -> Unit,
    onUpdateNormalizedPosition: (Float, Float) -> Unit
) {
    var posX by remember(track.id, track.canvasPinXFraction) {
        mutableFloatStateOf(track.canvasPinXFraction * canvasWidthPx)
    }
    var posY by remember(track.id, track.canvasPinYFraction) {
        mutableFloatStateOf(track.canvasPinYFraction * canvasHeightPx)
    }

    val shape = RoundedCornerShape(16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .widthIn(max = 172.dp)
            .shadow(14.dp, shape)
            .clip(shape)
            .background(Color(0xEB2A211B))
            .border(
                width = 0.9.dp,
                color = if (isActiveTrack) AmberWaveform else GlassBorderSpecular,
                shape = shape
            )
            .pointerInput(track.id) {
                detectDragGestures(
                    onDragEnd = {
                        onUpdateNormalizedPosition(
                            (posX / canvasWidthPx).coerceIn(0.02f, 0.76f),
                            (posY / canvasHeightPx).coerceIn(0.08f, 0.84f)
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        posX = (posX + dragAmount.x).coerceIn(8f, canvasWidthPx * 0.78f)
                        posY = (posY + dragAmount.y).coerceIn(48f, canvasHeightPx * 0.85f)
                    }
                )
            }
            .clickable { onTapPlay() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("canvas_pin_${track.id}")
    ) {
        TrackArtworkView(
            track = track,
            cornerRadius = 10.dp,
            showOverlayTypography = false,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = CoralPinAccent,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AlabasterWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = WarmSandMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable { onUnpin() }
                .testTag("unpin_canvas_${track.id}")
        ) {
            Icon(
                imageVector = if (isActiveTrack) Icons.Default.PlayArrow else Icons.Default.Close,
                contentDescription = stringResource(R.string.unpin_from_canvas),
                tint = if (isActiveTrack) AmberWaveform else WarmSandMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
