package com.example

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlaybackStatus
import com.example.pinning.HomePinningManager
import com.example.ui.components.SpatialBackdropCanvas
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CapsuleMode
import com.example.viewmodel.CapsulePopover
import com.example.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels {
        PlayerViewModel.Factory(application)
    }

    private val pipTransportReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> playerViewModel.togglePlayPause()
                ACTION_PIP_STOP -> playerViewModel.stopPlayback()
                ACTION_PIP_NEXT -> playerViewModel.skipToNext()
                ACTION_PIP_PREV -> playerViewModel.skipToPrevious()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure the Activity window is 100% transparent over the Android Home Screen wallpaper
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        registerPipReceiver()
        handleIncomingShortcutIntent(intent)

        setContent {
            MyApplicationTheme {
                val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

                // Keep Picture-in-Picture params and transport actions synchronized with playback state
                LaunchedEffect(uiState.playbackStatus) {
                    updatePipParams(uiState.playbackStatus == PlaybackStatus.PLAYING)
                }

                // Zero-permission multi-file audio picker
                val pickAudioFilesLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        playerViewModel.onAudioFilesPicked(uris)
                    }
                }

                // Zero-permission directory tree scanner for local music folders
                val scanMusicFolderLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { treeUri ->
                    playerViewModel.onMusicFolderPicked(treeUri)
                }

                // BackHandler collapses Queue Drawer or Popover, or collapses Card to Edge Dock Pill
                BackHandler(
                    enabled = uiState.activePopover != CapsulePopover.NONE ||
                        uiState.capsuleMode != CapsuleMode.EDGE_DOCK_PILL
                ) {
                    when {
                        uiState.activePopover != CapsulePopover.NONE -> {
                            playerViewModel.dismissPopover()
                        }
                        uiState.capsuleMode == CapsuleMode.QUEUE_DRAWER -> {
                            playerViewModel.setCapsuleMode(CapsuleMode.REFERENCE_CARD)
                        }
                        else -> {
                            playerViewModel.collapseToEdgePill(maxAbsX = 280f)
                        }
                    }
                }

                SpatialBackdropCanvas(
                    uiState = uiState,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onStopPlayback = playerViewModel::stopPlayback,
                    onSkipNext = { playerViewModel.skipToNext() },
                    onSkipPrevious = playerViewModel::skipToPrevious,
                    onSeekFraction = playerViewModel::seekToFraction,
                    onSetCapsuleMode = playerViewModel::setCapsuleMode,
                    onCollapseToEdgePill = playerViewModel::collapseToEdgePill,
                    onExpandFromEdgePill = playerViewModel::expandFromEdgePill,
                    onEnterPipMode = ::enterFloatingPipPanel,
                    onUpdateCapsuleOffset = playerViewModel::updateCapsuleOffset,
                    onResizeCapsuleScale = playerViewModel::updateCapsuleScale,
                    onSetScalePreset = playerViewModel::setCapsuleScalePreset,
                    onResetCapsulePosition = playerViewModel::resetCapsulePosition,
                    onToggleWaveformStrip = playerViewModel::toggleWaveformStrip,
                    onTogglePopover = playerViewModel::togglePopover,
                    onDismissPopover = playerViewModel::dismissPopover,
                    onSelectAudioRoute = playerViewModel::selectAudioRoute,
                    onTogglePinCanvas = playerViewModel::togglePinTrackToCanvas,
                    onUpdateCanvasPinPosition = playerViewModel::updateCanvasPinPosition,
                    onPinShortcutHome = playerViewModel::pinTrackToAndroidHomeScreen,
                    onPinWidgetHome = playerViewModel::pinCapsuleWidgetToHomeScreen,
                    onSelectTrackIndex = { idx ->
                        playerViewModel.selectTrackIndex(idx, playImmediately = true)
                    },
                    onPlayTrackById = playerViewModel::playTrackById,
                    onSearchQueryChange = playerViewModel::updateSearchQuery,
                    onScanMusicFolder = {
                        scanMusicFolderLauncher.launch(null)
                    },
                    onPickAudioFiles = {
                        pickAudioFilesLauncher.launch(arrayOf("audio/*"))
                    },
                    onToggleDoNotDisturb = playerViewModel::toggleDoNotDisturbEco
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically transition into the floating Picture-in-Picture panel when pressing Home
        enterFloatingPipPanel()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerViewModel.setPipMode(isInPictureInPictureMode)
    }

    private fun enterFloatingPipPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val isPlaying = playerViewModel.uiState.value.playbackStatus == PlaybackStatus.PLAYING
                val params = buildPipParams(isPlaying)
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
                playerViewModel.collapseToEdgePill(maxAbsX = 280f)
            }
        } else {
            playerViewModel.collapseToEdgePill(maxAbsX = 280f)
        }
    }

    private fun updatePipParams(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                setPictureInPictureParams(buildPipParams(isPlaying))
            } catch (_: Exception) {
            }
        }
    }

    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw IllegalStateException("PiP requires API 26+")
        }
        val actions = listOf(
            createRemoteAction(
                requestCode = 101,
                action = ACTION_PIP_PREV,
                iconRes = android.R.drawable.ic_media_previous,
                title = getString(R.string.cd_previous_track)
            ),
            createRemoteAction(
                requestCode = 102,
                action = ACTION_PIP_PLAY_PAUSE,
                iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                title = getString(R.string.cd_play_pause)
            ),
            createRemoteAction(
                requestCode = 103,
                action = ACTION_PIP_STOP,
                iconRes = android.R.drawable.ic_delete,
                title = getString(R.string.cd_stop_playback)
            ),
            createRemoteAction(
                requestCode = 104,
                action = ACTION_PIP_NEXT,
                iconRes = android.R.drawable.ic_media_next,
                title = getString(R.string.cd_next_track)
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun createRemoteAction(
        requestCode: Int,
        action: String,
        iconRes: Int,
        title: String
    ): RemoteAction {
        val intent = Intent(action).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(this, iconRes)
        return RemoteAction(icon, title, title, pendingIntent)
    }

    private fun registerPipReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_STOP)
            addAction(ACTION_PIP_NEXT)
            addAction(ACTION_PIP_PREV)
        }
        ContextCompat.registerReceiver(
            this,
            pipTransportReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pipTransportReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShortcutIntent(intent)
    }

    private fun handleIncomingShortcutIntent(intent: Intent?) {
        if (intent?.action == HomePinningManager.ACTION_PLAY_PINNED_TRACK) {
            val trackId = intent.getLongExtra(HomePinningManager.EXTRA_TRACK_ID, -1L)
            if (trackId != -1L) {
                playerViewModel.playTrackById(trackId)
            }
        }
    }

    companion object {
        private const val ACTION_PIP_PLAY_PAUSE = "com.example.action.PIP_PLAY_PAUSE"
        private const val ACTION_PIP_STOP = "com.example.action.PIP_STOP"
        private const val ACTION_PIP_NEXT = "com.example.action.PIP_NEXT"
        private const val ACTION_PIP_PREV = "com.example.action.PIP_PREV"
    }
}
