package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.PlaybackStatus
import com.example.data.TrackEntity
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.AmberWaveform
import com.example.ui.theme.CoralPinAccent
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.SmokedGlassSurface
import com.example.ui.theme.WarmSandDim
import com.example.ui.theme.WarmSandMuted
import com.example.viewmodel.CapsuleMode
import com.example.viewmodel.CapsulePopover
import com.example.viewmodel.PlayerUiState
import kotlin.math.roundToInt

/**
 * Always-floating, resizable, draggable glassmorphic music capsule.
 * Supports 4 tween-animated floating panel states:
 * 1. EDGE_DOCK_PILL (compact draggable edge pill docked to screen edge)
 * 2. MINI_PILL (floating mini bar)
 * 3. REFERENCE_CARD (matches reference UI with square album art + frosted glass deck)
 * 4. QUEUE_DRAWER (floating offline library & importer)
 * Plus a dedicated Picture-in-Picture (PiP) system floating window renderer.
 */
@Composable
fun DraggableMusicCapsule(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSetCapsuleMode: (CapsuleMode) -> Unit,
    onExpandFromEdgePill: () -> Unit,
    onEnterPipMode: () -> Unit,
    onDragCapsule: (Float, Float) -> Unit,
    onResizeCapsuleScale: (Float) -> Unit,
    onSetScalePreset: (Float) -> Unit,
    onResetCapsulePosition: () -> Unit,
    onToggleWaveformStrip: () -> Unit,
    onTogglePopover: (CapsulePopover) -> Unit,
    onSelectAudioRoute: (com.example.audio.AudioOutputRoute) -> Unit,
    onTogglePinCanvas: (TrackEntity) -> Unit,
    onPinShortcutHome: (TrackEntity) -> Unit,
    onPinWidgetHome: () -> Unit,
    onSelectTrackIndex: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onScanMusicFolder: () -> Unit,
    onPickAudioFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = uiState.currentTrack
    val accentColor = Color(track?.accentColorHex ?: 0xFFF5B041L)
    val primaryGlow by animateColorAsState(
        targetValue = Color(track?.primaryColorHex ?: 0xFF5E4534L),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "capsule_halo_glow"
    )

    val progressFraction = remember(uiState.currentPositionMs, track?.durationMs) {
        val dur = (track?.durationMs ?: 120_000L).coerceAtLeast(1L)
        (uiState.currentPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    }

    // If the window is in Android system Picture-in-Picture floating mode, render compact PiP HUD
    if (uiState.isInPipMode) {
        PipFloatingPanelContent(
            track = track,
            playbackStatus = uiState.playbackStatus,
            waveformPhase = uiState.waveformPhase,
            progressFraction = progressFraction,
            accentColor = accentColor,
            onTogglePlayPause = onTogglePlayPause
        )
        return
    }

    // Whole-capsule coordinated tween transition across all 4 floating modes
    val modeTransition = updateTransition(
        targetState = uiState.capsuleMode,
        label = "capsule_mode_tween"
    )

    val baseWidthDp by modeTransition.animateDp(
        transitionSpec = { tween(durationMillis = 360, easing = FastOutSlowInEasing) },
        label = "capsule_base_width"
    ) { mode ->
        when (mode) {
            CapsuleMode.EDGE_DOCK_PILL -> 168.dp
            CapsuleMode.MINI_PILL -> 292.dp
            CapsuleMode.REFERENCE_CARD -> 288.dp
            CapsuleMode.QUEUE_DRAWER -> 324.dp
        }
    }

    val cornerRadiusDp by modeTransition.animateDp(
        transitionSpec = { tween(durationMillis = 360, easing = FastOutSlowInEasing) },
        label = "capsule_corner_radius"
    ) { mode ->
        when (mode) {
            CapsuleMode.EDGE_DOCK_PILL -> 32.dp
            CapsuleMode.MINI_PILL -> 36.dp
            CapsuleMode.REFERENCE_CARD -> 28.dp
            CapsuleMode.QUEUE_DRAWER -> 26.dp
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = uiState.capsuleScale,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "capsule_scale_tween"
    )

    val effectiveWidth = baseWidthDp * animatedScale

    // Horizontal swipe state for switching music left and right
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedSwipeX by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "swipe_card_offset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(effectiveWidth)
            .offset { IntOffset(animatedSwipeX.roundToInt(), 0) }
            // Localized floating warm halo strictly around the capsule so the wallpaper around it stays transparent
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.42f),
                            accentColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.68f
                    )
                )
            }
            .testTag("draggable_music_capsule")
            .pointerInput(track?.id, uiState.capsuleMode) {
                if (uiState.capsuleMode != CapsuleMode.EDGE_DOCK_PILL) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                swipeOffsetX < -85f -> onSkipNext()
                                swipeOffsetX > 85f -> onSkipPrevious()
                            }
                            swipeOffsetX = 0f
                        },
                        onDragCancel = { swipeOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-150f, 150f)
                        }
                    )
                }
            }
    ) {
        // Show floating handle bar in Mini Pill, Reference Card, and Library modes
        AnimatedVisibility(
            visible = uiState.capsuleMode != CapsuleMode.EDGE_DOCK_PILL,
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CapsuleHeaderHandleBar(
                    currentMode = uiState.capsuleMode,
                    onSelectMode = onSetCapsuleMode,
                    onEnterPipMode = onEnterPipMode,
                    onDragDelta = onDragCapsule
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Main Tween-Animated Floating Capsule Body
        modeTransition.AnimatedContent(
            transitionSpec = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        ) { targetMode ->
            when (targetMode) {
                CapsuleMode.EDGE_DOCK_PILL -> {
                    EdgeDockPillContent(
                        track = track,
                        playbackStatus = uiState.playbackStatus,
                        waveformPhase = uiState.waveformPhase,
                        accentColor = accentColor,
                        onTogglePlayPause = onTogglePlayPause,
                        onExpandToCard = onExpandFromEdgePill,
                        onDragDelta = onDragCapsule
                    )
                }

                CapsuleMode.MINI_PILL -> {
                    MiniPillCapsuleContent(
                        track = track,
                        playbackStatus = uiState.playbackStatus,
                        waveformPhase = uiState.waveformPhase,
                        accentColor = accentColor,
                        cornerRadius = cornerRadiusDp,
                        onTogglePlayPause = onTogglePlayPause,
                        onStopPlayback = onStopPlayback,
                        onExpandToCard = { onSetCapsuleMode(CapsuleMode.REFERENCE_CARD) },
                        onDragDelta = onDragCapsule
                    )
                }

                CapsuleMode.REFERENCE_CARD -> {
                    ReferenceCardCapsuleContent(
                        uiState = uiState,
                        track = track,
                        progressFraction = progressFraction,
                        accentColor = accentColor,
                        cornerRadius = cornerRadiusDp,
                        onTogglePlayPause = onTogglePlayPause,
                        onStopPlayback = onStopPlayback,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekFraction = onSeekFraction,
                        onToggleWaveformStrip = onToggleWaveformStrip,
                        onTogglePopover = onTogglePopover,
                        onDragDelta = onDragCapsule,
                        onResizeScaleDelta = onResizeCapsuleScale
                    )
                }

                CapsuleMode.QUEUE_DRAWER -> {
                    QueueDrawerCapsuleContent(
                        uiState = uiState,
                        accentColor = accentColor,
                        cornerRadius = cornerRadiusDp,
                        onSelectTrackIndex = onSelectTrackIndex,
                        onTogglePlayPause = onTogglePlayPause,
                        onStopPlayback = onStopPlayback,
                        onSearchQueryChange = onSearchQueryChange,
                        onScanMusicFolder = onScanMusicFolder,
                        onPickAudioFiles = onPickAudioFiles,
                        onTogglePinCanvas = onTogglePinCanvas,
                        onPinShortcutHome = onPinShortcutHome,
                        onBackToReferenceCard = { onSetCapsuleMode(CapsuleMode.REFERENCE_CARD) },
                        onDragDelta = onDragCapsule,
                        onResizeScaleDelta = onResizeCapsuleScale
                    )
                }
            }
        }

        // Attached Popover Menus (Pinning & Audio Output / Scale)
        AnimatedVisibility(
            visible = uiState.activePopover != CapsulePopover.NONE &&
                uiState.capsuleMode != CapsuleMode.EDGE_DOCK_PILL,
            enter = fadeIn(tween(220)) + expandVertically(tween(260)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
        ) {
            Box(modifier = Modifier.padding(top = 8.dp)) {
                when (uiState.activePopover) {
                    CapsulePopover.PIN_MENU -> {
                        PinOptionsPopover(
                            track = track,
                            onToggleCanvasPin = onTogglePinCanvas,
                            onPinShortcutToHome = onPinShortcutHome,
                            onPinWidgetToHome = onPinWidgetHome
                        )
                    }
                    CapsulePopover.AUDIO_OUTPUT_MENU -> {
                        AudioRouteAndScalePopover(
                            routes = uiState.availableRoutes,
                            selectedRoute = uiState.selectedRoute,
                            currentScale = uiState.capsuleScale,
                            onSelectRoute = onSelectAudioRoute,
                            onSelectScalePreset = onSetScalePreset,
                            onResetCapsulePosition = onResetCapsulePosition
                        )
                    }
                    CapsulePopover.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun CapsuleHeaderHandleBar(
    currentMode: CapsuleMode,
    onSelectMode: (CapsuleMode) -> Unit,
    onEnterPipMode: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xE628201B))
            .border(0.9.dp, GlassBorderSpecular, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("capsule_drag_handle_bar")
    ) {
        // Drag grip pill indicator
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(width = 14.dp, height = 4.dp)
                .clip(CircleShape)
                .background(WarmSandMuted.copy(alpha = 0.6f))
        )

        val modes = listOf(
            CapsuleMode.EDGE_DOCK_PILL to stringResource(R.string.mode_edge_dock),
            CapsuleMode.MINI_PILL to stringResource(R.string.mode_mini_pill),
            CapsuleMode.REFERENCE_CARD to stringResource(R.string.mode_reference_card),
            CapsuleMode.QUEUE_DRAWER to stringResource(R.string.mode_queue_drawer)
        )
        modes.forEach { (mode, label) ->
            val selected = currentMode == mode
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) AlabasterWhite.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
                    .testTag("mode_tab_${mode.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (selected) AlabasterWhite else WarmSandMuted
                )
            }
        }

        // Float Picture-in-Picture System Window button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AmberWaveform.copy(alpha = 0.20f))
                .clickable { onEnterPipMode() }
                .testTag("float_pip_button")
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPictureAlt,
                contentDescription = stringResource(R.string.cd_float_pip),
                tint = AmberWaveform,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * State 0: Collapsible Draggable Edge Dock Pill — tucks neatly to the screen edge over the
 * transparent Android Home Screen wallpaper when tapping outside the main card.
 */
@Composable
private fun EdgeDockPillContent(
    track: TrackEntity?,
    playbackStatus: PlaybackStatus,
    waveformPhase: Float,
    accentColor: Color,
    onTogglePlayPause: () -> Unit,
    onExpandToCard: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, shape)
            .clip(shape)
            .background(Color(0xF0261E19))
            .border(1.dp, GlassBorderSpecular, shape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onExpandToCard() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("edge_dock_pill_view")
    ) {
        TrackArtworkView(
            track = track,
            cornerRadius = 18.dp,
            showOverlayTypography = false,
            modifier = Modifier.size(34.dp)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track?.title ?: "Piche Tere",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AlabasterWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MiniWaveformBadge(
                playbackStatus = playbackStatus,
                waveformPhase = waveformPhase,
                accentColor = accentColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(32.dp)
                .testTag("edge_play_pause_button")
        ) {
            Icon(
                imageVector = if (playbackStatus == PlaybackStatus.PLAYING) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = AlabasterWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Picture-in-Picture (PiP) System Floating Window Renderer.
 * Renders a compact, always-on-top glassmorphic floating card when the app is in Android PiP mode.
 */
@Composable
private fun PipFloatingPanelContent(
    track: TrackEntity?,
    playbackStatus: PlaybackStatus,
    waveformPhase: Float,
    progressFraction: Float,
    accentColor: Color,
    onTogglePlayPause: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(track?.secondaryColorHex ?: 0xFF211812L),
                        Color(0xF22D231C)
                    )
                )
            )
            .border(1.dp, GlassBorderSpecular, shape)
            .clickable { onTogglePlayPause() }
            .padding(10.dp)
            .testTag("pip_floating_panel_view")
    ) {
        TrackArtworkView(
            track = track,
            cornerRadius = 14.dp,
            showOverlayTypography = false,
            modifier = Modifier
                .size(56.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track?.title ?: "Piche Tere",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                MiniWaveformBadge(
                    playbackStatus = playbackStatus,
                    waveformPhase = waveformPhase,
                    accentColor = accentColor
                )
            }
            Text(
                text = track?.artist ?: "Kunwarr, Dishant",
                style = MaterialTheme.typography.labelSmall,
                color = WarmSandMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Compact PiP progress bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                val r = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = WarmSandDim.copy(alpha = 0.4f),
                    size = size,
                    cornerRadius = r
                )
                drawRoundRect(
                    color = accentColor,
                    size = Size(size.width * progressFraction, size.height),
                    cornerRadius = r
                )
            }
        }
    }
}

/**
 * State 1: Compact Mini Pill Capsule.
 */
@Composable
private fun MiniPillCapsuleContent(
    track: TrackEntity?,
    playbackStatus: PlaybackStatus,
    waveformPhase: Float,
    accentColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onExpandToCard: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(22.dp, shape)
            .clip(shape)
            .background(Color(0xF02B221C))
            .border(1.dp, GlassBorderSpecular, shape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("mini_pill_capsule_view")
    ) {
        TrackArtworkView(
            track = track,
            cornerRadius = 22.dp,
            showOverlayTypography = false,
            modifier = Modifier
                .size(44.dp)
                .clickable { onExpandToCard() }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onExpandToCard() }
        ) {
            Text(
                text = track?.title ?: "Piche Tere",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AlabasterWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track?.artist ?: "Kunwarr, Dishant",
                style = MaterialTheme.typography.labelSmall,
                color = WarmSandMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MiniWaveformBadge(
            playbackStatus = playbackStatus,
            waveformPhase = waveformPhase,
            accentColor = accentColor,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(40.dp)
                .minimumInteractiveComponentSize()
                .testTag("mini_play_pause_button")
        ) {
            Icon(
                imageVector = if (playbackStatus == PlaybackStatus.PLAYING) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = AlabasterWhite
            )
        }

        IconButton(
            onClick = onStopPlayback,
            modifier = Modifier
                .size(38.dp)
                .minimumInteractiveComponentSize()
                .testTag("mini_stop_button")
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = stringResource(R.string.cd_stop_playback),
                tint = WarmSandMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * State 2: Reference Card Mode — directly modeled after the user's provided UI reference image,
 * floating cleanly over the transparent Android Home Screen wallpaper.
 */
@Composable
private fun ReferenceCardCapsuleContent(
    uiState: PlayerUiState,
    track: TrackEntity?,
    progressFraction: Float,
    accentColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onToggleWaveformStrip: () -> Unit,
    onTogglePopover: (CapsulePopover) -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onResizeScaleDelta: (Float) -> Unit
) {
    val topArtworkShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = 18.dp,
        bottomEnd = 18.dp
    )
    val bottomDeckShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("reference_card_capsule_view")
    ) {
        // Upper Square Album Cover Card (matches reference photo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(26.dp, topArtworkShape)
        ) {
            TrackArtworkView(
                track = track,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("capsule_artwork_card")
            )
        }

        // Lower Smoked-Glass Control Capsule Deck (matches reference photo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-4).dp)
                .shadow(24.dp, bottomDeckShape)
                .clip(bottomDeckShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEB3B3028),
                            Color(0xF02B221D),
                            Color(0xF5201915)
                        )
                    )
                )
                .border(1.dp, GlassBorderSpecular, bottomDeckShape)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row: Left Pin Button | Centered Track Title & Artist | Right Mini Waveform Icon
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Top-left Pin icon button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onTogglePopover(CapsulePopover.PIN_MENU) }
                            .testTag("capsule_pin_button")
                    ) {
                        Icon(
                            imageVector = if (track?.isPinnedToCanvas == true || track?.isPinnedToHome == true) {
                                Icons.Default.PushPin
                            } else {
                                Icons.Outlined.PushPin
                            },
                            contentDescription = stringResource(R.string.cd_pin_track),
                            tint = if (track?.isPinnedToCanvas == true) CoralPinAccent else WarmSandMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Centered Title ("Piche Tere") & Artist ("Kunwarr, Dishant")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 36.dp)
                    ) {
                        Text(
                            text = track?.title ?: "Piche Tere",
                            style = MaterialTheme.typography.titleMedium,
                            color = AlabasterWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("current_track_title")
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track?.artist ?: "Kunwarr, Dishant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmSandMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("current_track_artist")
                        )
                    }

                    // Top-right Animated Waveform Badge (matches reference UI top-right waveform icon)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onToggleWaveformStrip() }
                            .testTag("waveform_toggle_button")
                    ) {
                        MiniWaveformBadge(
                            playbackStatus = uiState.playbackStatus,
                            waveformPhase = uiState.waveformPhase,
                            accentColor = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expandable Interactive 32-Bar Audio Waveform Strip
                AnimatedVisibility(
                    visible = uiState.isWaveformStripExpanded,
                    enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
                ) {
                    Column {
                        InteractiveWaveformVisualizer(
                            amplitudes = track?.parsedWaveform() ?: TrackEntity.DefaultWaveform,
                            progressFraction = progressFraction,
                            playbackStatus = uiState.playbackStatus,
                            waveformPhase = uiState.waveformPhase,
                            accentColor = accentColor,
                            onSeekFraction = onSeekFraction
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Reference Scrubber Line with Elapsed ("0:06") and Remaining ("-1:54") Timestamps
                ReferenceProgressScrubberRow(
                    currentPositionMs = uiState.currentPositionMs,
                    durationMs = track?.durationMs ?: 120_000L,
                    progressFraction = progressFraction,
                    onSeekFraction = onSeekFraction
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Transport Controls Row: Stop | Rewind | Play/Pause | Fast-Forward | Bluetooth Audio Route
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    IconButton(
                        onClick = onStopPlayback,
                        modifier = Modifier
                            .size(42.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_stop_playback),
                            tint = if (uiState.playbackStatus == PlaybackStatus.STOPPED) {
                                WarmSandDim
                            } else {
                                AlabasterWhite.copy(alpha = 0.88f)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .size(44.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = stringResource(R.string.cd_previous_track),
                            tint = AlabasterWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.playbackStatus == PlaybackStatus.PLAYING) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = AlabasterWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size(44.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = stringResource(R.string.cd_next_track),
                            tint = AlabasterWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = { onTogglePopover(CapsulePopover.AUDIO_OUTPUT_MENU) },
                        modifier = Modifier
                            .size(42.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("audio_output_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothAudio,
                            contentDescription = stringResource(R.string.cd_audio_output),
                            tint = if (uiState.activePopover == CapsulePopover.AUDIO_OUTPUT_MENU) {
                                AmberWaveform
                            } else {
                                WarmSandMuted
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom-Right Corner Freeform Resize Grip Handle
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount.x + dragAmount.y) / 600f
                            onResizeScaleDelta(delta)
                        }
                    }
                    .testTag("capsule_corner_resize_grip")
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val strokeColor = WarmSandMuted.copy(alpha = 0.55f)
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width * 0.35f, size.height),
                        end = Offset(size.width, size.height * 0.35f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width * 0.70f, size.height),
                        end = Offset(size.width, size.height * 0.70f),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

/**
 * Scrubber bar matching the reference image ("0:06 [===o--------] -1:54").
 */
@Composable
private fun ReferenceProgressScrubberRow(
    currentPositionMs: Long,
    durationMs: Long,
    progressFraction: Float,
    onSeekFraction: (Float) -> Unit
) {
    val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = formatTimestamp(currentPositionMs, prefixMinus = false),
            style = MaterialTheme.typography.labelSmall,
            color = WarmSandMuted,
            modifier = Modifier.testTag("elapsed_time_label")
        )

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .padding(horizontal = 8.dp)
                .testTag("progress_scrubber_bar")
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
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                val trackHeight = 3.5.dp.toPx()
                val centerY = size.height / 2f
                val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                drawRoundRect(
                    color = WarmSandDim.copy(alpha = 0.45f),
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = corner
                )

                val fillWidth = (size.width * progressFraction).coerceIn(0f, size.width)
                drawRoundRect(
                    color = AlabasterWhite.copy(alpha = 0.85f),
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(fillWidth, trackHeight),
                    cornerRadius = corner
                )

                drawCircle(
                    color = AlabasterWhite,
                    radius = 4.5.dp.toPx(),
                    center = Offset(fillWidth.coerceIn(4.dp.toPx(), size.width - 4.dp.toPx()), centerY)
                )
            }
        }

        Text(
            text = formatTimestamp(remainingMs, prefixMinus = true),
            style = MaterialTheme.typography.labelSmall,
            color = WarmSandMuted,
            modifier = Modifier.testTag("remaining_time_label")
        )
    }
}

/**
 * State 3: Compact Floating Queue & Offline Library Capsule (No Full-Screen View).
 */
@Composable
private fun QueueDrawerCapsuleContent(
    uiState: PlayerUiState,
    accentColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onSelectTrackIndex: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onScanMusicFolder: () -> Unit,
    onPickAudioFiles: () -> Unit,
    onTogglePinCanvas: (TrackEntity) -> Unit,
    onPinShortcutHome: (TrackEntity) -> Unit,
    onBackToReferenceCard: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onResizeScaleDelta: (Float) -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val activeTrack = uiState.currentTrack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(26.dp, shape)
            .clip(shape)
            .background(Color(0xF22D241E))
            .border(1.dp, GlassBorderSpecular, shape)
            .padding(14.dp)
            .testTag("queue_drawer_capsule_view")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SmokedGlassSurface)
                    .clickable { onBackToReferenceCard() }
                    .padding(8.dp)
            ) {
                TrackArtworkView(
                    track = activeTrack,
                    cornerRadius = 12.dp,
                    showOverlayTypography = false,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTrack?.title ?: "Piche Tere",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeTrack?.artist ?: "Kunwarr, Dishant",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmSandMuted,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.playbackStatus == PlaybackStatus.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = AlabasterWhite
                    )
                }
                IconButton(
                    onClick = onStopPlayback,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.cd_stop_playback),
                        tint = WarmSandMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onScanMusicFolder,
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWaveform.copy(alpha = 0.18f),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_folder_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.cd_scan_media),
                            tint = AmberWaveform,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.scan_device_library),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AlabasterWhite,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    onClick = onPickAudioFiles,
                    shape = RoundedCornerShape(12.dp),
                    color = AlabasterWhite.copy(alpha = 0.12f),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_files_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = stringResource(R.string.cd_pick_files),
                            tint = AlabasterWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.import_audio_files),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AlabasterWhite,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(0.8.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = WarmSandMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.searchQuery.isEmpty()) {
                        Text(
                            text = "Filter offline tracks...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmSandDim
                        )
                    }
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlabasterWhite),
                        cursorBrush = SolidColor(AmberWaveform),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("queue_search_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .testTag("capsule_track_list")
            ) {
                itemsIndexed(
                    items = uiState.filteredTracks,
                    key = { _, item -> item.id }
                ) { _, item ->
                    val actualIndex = uiState.tracks.indexOfFirst { it.id == item.id }
                    val isCurrent = item.id == activeTrack?.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) AmberWaveform.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .clickable {
                                if (actualIndex >= 0) {
                                    onSelectTrackIndex(actualIndex)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("track_row_${item.id}")
                    ) {
                        TrackArtworkView(
                            track = item,
                            cornerRadius = 8.dp,
                            showOverlayTypography = false,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isCurrent) AmberWaveform else AlabasterWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.artist} • ${formatTimestamp(item.durationMs, false)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmSandMuted,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { onTogglePinCanvas(item) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isPinnedToCanvas) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.cd_pin_track),
                                tint = if (item.isPinnedToCanvas) CoralPinAccent else WarmSandMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { onPinShortcutHome(item) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = stringResource(R.string.pin_to_home_screen),
                                tint = if (item.isPinnedToHome) AmberWaveform else WarmSandMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTimestamp(ms: Long, prefixMinus: Boolean): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val prefix = if (prefixMinus) "-" else ""
    return "$prefix$minutes:%02d".format(java.util.Locale.US, seconds)
}
