package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioOutputRoute
import com.example.audio.OfflineAudioController
import com.example.audio.PlaybackStatus
import com.example.data.AuraDatabase
import com.example.data.TrackEntity
import com.example.data.TrackRepository
import com.example.pinning.HomePinningManager
import com.example.widget.AuraCapsuleWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CapsuleMode {
    EDGE_DOCK_PILL,
    MINI_PILL,
    REFERENCE_CARD,
    QUEUE_DRAWER
}

enum class CapsulePopover {
    NONE,
    PIN_MENU,
    AUDIO_OUTPUT_MENU
}

data class PlayerUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val currentTrackIndex: Int = 0,
    val playbackStatus: PlaybackStatus = PlaybackStatus.PAUSED,
    val currentPositionMs: Long = 6_000L, // Starts at 0:06 (-1:54 remaining) matching reference UI
    val waveformPhase: Float = 0f,
    val capsuleMode: CapsuleMode = CapsuleMode.REFERENCE_CARD,
    val capsuleScale: Float = 1.0f,
    val capsuleOffsetX: Float = 0f,
    val capsuleOffsetY: Float = 0f,
    val savedPreDockOffsetX: Float = 0f,
    val isDockedLeft: Boolean = false,
    val isInPipMode: Boolean = false,
    val isWaveformStripExpanded: Boolean = true,
    val availableRoutes: List<AudioOutputRoute> = emptyList(),
    val selectedRoute: AudioOutputRoute = AudioOutputRoute(-2, "Studio Spatial AirPods / BT", true),
    val activePopover: CapsulePopover = CapsulePopover.NONE,
    val isDoNotDisturbActive: Boolean = true,
    val searchQuery: String = "",
    val feedbackMessage: String? = null
) {
    val currentTrack: TrackEntity?
        get() = tracks.getOrNull(currentTrackIndex) ?: tracks.firstOrNull()

    val pinnedCanvasTracks: List<TrackEntity>
        get() = tracks.filter { it.isPinnedToCanvas }

    val filteredTracks: List<TrackEntity>
        get() = if (searchQuery.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
            }
        }
}

class PlayerViewModel(
    application: Application,
    private val repository: TrackRepository,
    private val audioController: OfflineAudioController
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressTickerJob: Job? = null
    private var toastClearJob: Job? = null
    private var pendingPinnedTrackId: Long? = null

    init {
        audioController.onTrackCompleted = {
            viewModelScope.launch {
                skipToNext(autoPlay = true)
            }
        }

        val routes = audioController.getAvailableOutputRoutes()
        _uiState.update { state ->
            state.copy(
                availableRoutes = routes,
                selectedRoute = routes.firstOrNull() ?: state.selectedRoute
            )
        }

        viewModelScope.launch {
            repository.ensureSeededTracks()
            repository.scanAccessibleMediaStoreAudio()
        }

        viewModelScope.launch {
            repository.allTracks.collect { loadedTracks ->
                _uiState.update { current ->
                    val safeIndex = current.currentTrackIndex.coerceIn(
                        0,
                        (loadedTracks.size - 1).coerceAtLeast(0)
                    )
                    current.copy(
                        tracks = loadedTracks,
                        currentTrackIndex = safeIndex
                    )
                }
                pendingPinnedTrackId?.let { targetId ->
                    val idx = loadedTracks.indexOfFirst { it.id == targetId }
                    if (idx >= 0) {
                        pendingPinnedTrackId = null
                        selectTrackIndex(idx, playImmediately = true)
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value
        val track = current.currentTrack ?: return
        if (current.playbackStatus == PlaybackStatus.PLAYING) {
            audioController.pause()
            stopProgressTicker()
            _uiState.update { it.copy(playbackStatus = PlaybackStatus.PAUSED) }
            syncHomeWidget(track, isPlaying = false)
        } else {
            val startPos = if (current.currentPositionMs >= track.durationMs) 0L else current.currentPositionMs
            audioController.play(track.audioUriString, startPos)
            _uiState.update {
                it.copy(
                    playbackStatus = PlaybackStatus.PLAYING,
                    currentPositionMs = startPos
                )
            }
            startProgressTicker()
            syncHomeWidget(track, isPlaying = true)
        }
    }

    /**
     * Explicit Stop action: halts audio playback, resets scrubber to 0:00, and stops
     * background waveform/progress coroutine tickers for optimal battery life.
     */
    fun stopPlayback() {
        val track = _uiState.value.currentTrack
        audioController.stop()
        stopProgressTicker()
        _uiState.update {
            it.copy(
                playbackStatus = PlaybackStatus.STOPPED,
                currentPositionMs = 0L,
                waveformPhase = 0f
            )
        }
        if (track != null) {
            syncHomeWidget(track, isPlaying = false)
        }
        showFeedback("Playback stopped • Audio decoder idle")
    }

    fun skipToNext(autoPlay: Boolean = _uiState.value.playbackStatus == PlaybackStatus.PLAYING) {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        val nextIndex = (_uiState.value.currentTrackIndex + 1) % tracks.size
        selectTrackIndex(nextIndex, playImmediately = autoPlay)
    }

    fun skipToPrevious() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        val current = _uiState.value
        val prevIndex = if (current.currentTrackIndex - 1 < 0) {
            tracks.lastIndex
        } else {
            current.currentTrackIndex - 1
        }
        selectTrackIndex(prevIndex, playImmediately = current.playbackStatus == PlaybackStatus.PLAYING)
    }

    fun selectTrackIndex(index: Int, playImmediately: Boolean = true) {
        val tracks = _uiState.value.tracks
        val target = tracks.getOrNull(index) ?: return
        stopProgressTicker()

        val initialPos = 0L
        if (playImmediately) {
            audioController.prepareAndSeek(target.audioUriString, initialPos, playImmediately = true)
            _uiState.update {
                it.copy(
                    currentTrackIndex = index,
                    currentPositionMs = initialPos,
                    playbackStatus = PlaybackStatus.PLAYING
                )
            }
            startProgressTicker()
            syncHomeWidget(target, isPlaying = true)
        } else {
            audioController.prepareAndSeek(target.audioUriString, initialPos, playImmediately = false)
            _uiState.update {
                it.copy(
                    currentTrackIndex = index,
                    currentPositionMs = initialPos,
                    playbackStatus = PlaybackStatus.PAUSED
                )
            }
            syncHomeWidget(target, isPlaying = false)
        }
    }

    fun playTrackById(trackId: Long) {
        val tracks = _uiState.value.tracks
        val idx = tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            selectTrackIndex(idx, playImmediately = true)
        } else {
            pendingPinnedTrackId = trackId
        }
    }

    fun seekToFraction(fraction: Float) {
        val track = _uiState.value.currentTrack ?: return
        val targetMs = (track.durationMs * fraction.coerceIn(0f, 1f)).toLong()
        audioController.seekTo(targetMs)
        _uiState.update { it.copy(currentPositionMs = targetMs) }
    }

    fun setCapsuleMode(mode: CapsuleMode) {
        _uiState.update { state ->
            val restoredX = if (state.capsuleMode == CapsuleMode.EDGE_DOCK_PILL && mode != CapsuleMode.EDGE_DOCK_PILL) {
                state.savedPreDockOffsetX
            } else {
                state.capsuleOffsetX
            }
            state.copy(
                capsuleMode = mode,
                capsuleOffsetX = restoredX,
                activePopover = CapsulePopover.NONE
            )
        }
    }

    /**
     * Collapses the floating panel into a sleek draggable Edge Dock Pill snapped to the nearest
     * left or right screen edge so the Android Home Screen behind it is completely unobstructed.
     */
    fun collapseToEdgePill(maxAbsX: Float) {
        _uiState.update { state ->
            if (state.capsuleMode == CapsuleMode.EDGE_DOCK_PILL) {
                return@update state
            }
            val dockLeft = state.capsuleOffsetX < 0f
            val targetEdgeX = if (dockLeft) -maxAbsX * 0.92f else maxAbsX * 0.92f
            state.copy(
                capsuleMode = CapsuleMode.EDGE_DOCK_PILL,
                savedPreDockOffsetX = state.capsuleOffsetX,
                capsuleOffsetX = targetEdgeX,
                isDockedLeft = dockLeft,
                activePopover = CapsulePopover.NONE
            )
        }
    }

    fun expandFromEdgePill() {
        _uiState.update { state ->
            state.copy(
                capsuleMode = CapsuleMode.REFERENCE_CARD,
                capsuleOffsetX = state.savedPreDockOffsetX,
                activePopover = CapsulePopover.NONE
            )
        }
    }

    fun setPipMode(inPip: Boolean) {
        _uiState.update { state ->
            state.copy(
                isInPipMode = inPip,
                activePopover = CapsulePopover.NONE
            )
        }
    }

    fun updateCapsuleOffset(deltaX: Float, deltaY: Float, maxAbsX: Float, maxAbsY: Float) {
        _uiState.update { state ->
            val newX = (state.capsuleOffsetX + deltaX).coerceIn(-maxAbsX, maxAbsX)
            val newY = (state.capsuleOffsetY + deltaY).coerceIn(-maxAbsY, maxAbsY)
            state.copy(
                capsuleOffsetX = newX,
                capsuleOffsetY = newY,
                isDockedLeft = newX < 0f
            )
        }
    }

    fun resetCapsulePosition() {
        _uiState.update {
            it.copy(
                capsuleOffsetX = 0f,
                capsuleOffsetY = 0f,
                savedPreDockOffsetX = 0f,
                capsuleScale = 1.0f
            )
        }
    }

    fun updateCapsuleScale(scaleDelta: Float) {
        _uiState.update { state ->
            val nextScale = (state.capsuleScale + scaleDelta).coerceIn(0.82f, 1.18f)
            state.copy(capsuleScale = nextScale)
        }
    }

    fun setCapsuleScalePreset(scale: Float) {
        _uiState.update { it.copy(capsuleScale = scale.coerceIn(0.82f, 1.18f)) }
    }

    fun toggleWaveformStrip() {
        _uiState.update { it.copy(isWaveformStripExpanded = !it.isWaveformStripExpanded) }
    }

    fun togglePopover(popover: CapsulePopover) {
        _uiState.update { state ->
            state.copy(
                activePopover = if (state.activePopover == popover) CapsulePopover.NONE else popover
            )
        }
    }

    fun dismissPopover() {
        _uiState.update { it.copy(activePopover = CapsulePopover.NONE) }
    }

    fun selectAudioRoute(route: AudioOutputRoute) {
        _uiState.update {
            it.copy(
                selectedRoute = route,
                activePopover = CapsulePopover.NONE
            )
        }
        showFeedback("Output switched to ${route.label}")
    }

    fun toggleDoNotDisturbEco() {
        _uiState.update { state ->
            val next = !state.isDoNotDisturbActive
            state.copy(isDoNotDisturbActive = next)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePinTrackToCanvas(track: TrackEntity) {
        viewModelScope.launch {
            val nextPinned = !track.isPinnedToCanvas
            val pinCount = _uiState.value.pinnedCanvasTracks.size
            val defaultX = if (pinCount % 2 == 0) 0.06f else 0.70f
            val defaultY = (0.14f + (pinCount / 2) * 0.12f).coerceIn(0.10f, 0.78f)
            repository.setCanvasPinned(
                trackId = track.id,
                pinned = nextPinned,
                xFraction = if (nextPinned) defaultX else track.canvasPinXFraction,
                yFraction = if (nextPinned) defaultY else track.canvasPinYFraction
            )
            _uiState.update { it.copy(activePopover = CapsulePopover.NONE) }
            showFeedback(
                if (nextPinned) "Pinned '${track.title}' floating card to screen"
                else "Removed '${track.title}' floating card"
            )
        }
    }

    fun updateCanvasPinPosition(track: TrackEntity, newXFraction: Float, newYFraction: Float) {
        viewModelScope.launch {
            repository.setCanvasPinned(
                trackId = track.id,
                pinned = true,
                xFraction = newXFraction.coerceIn(0.02f, 0.78f),
                yFraction = newYFraction.coerceIn(0.08f, 0.84f)
            )
        }
    }

    fun pinTrackToAndroidHomeScreen(track: TrackEntity) {
        viewModelScope.launch {
            val success = HomePinningManager.pinTrackShortcutToHomeScreen(getApplication(), track)
            repository.setHomePinned(track.id, true)
            _uiState.update { it.copy(activePopover = CapsulePopover.NONE) }
            showFeedback(
                if (success) "Shortcut '${track.title}' sent to Android Home Screen"
                else "Shortcut added to launcher quick-actions"
            )
        }
    }

    fun pinCapsuleWidgetToHomeScreen() {
        val current = _uiState.value
        val requested = HomePinningManager.requestPinCapsuleWidget(
            context = getApplication(),
            currentTrack = current.currentTrack,
            isPlaying = current.playbackStatus == PlaybackStatus.PLAYING
        )
        _uiState.update { it.copy(activePopover = CapsulePopover.NONE) }
        showFeedback(
            if (requested) "Resizable Aura Capsule Widget sent to Home Screen"
            else "Widget synced • Long-press Home Screen -> Widgets to place"
        )
    }

    fun onAudioFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val count = repository.importAudioFiles(uris)
            showFeedback(
                if (count > 0) "Imported $count offline track(s) to capsule library"
                else "Selected track(s) already in offline library"
            )
        }
    }

    fun onMusicFolderPicked(treeUri: Uri?) {
        viewModelScope.launch {
            val count = repository.scanFolderTreeAndMediaStore(treeUri)
            showFeedback(
                if (count > 0) "Scanned & added $count track(s) from storage"
                else "Library up to date (${_uiState.value.tracks.size} offline tracks)"
            )
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressTickerJob = viewModelScope.launch {
            while (isActive && _uiState.value.playbackStatus == PlaybackStatus.PLAYING) {
                val track = _uiState.value.currentTrack
                if (track != null) {
                    val hardwarePos = audioController.currentPositionMs()
                    val nextPos = hardwarePos ?: (_uiState.value.currentPositionMs + 100L)
                    if (nextPos >= track.durationMs) {
                        skipToNext(autoPlay = true)
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                currentPositionMs = nextPos,
                                waveformPhase = (state.waveformPhase + 0.22f) % 6.283f
                            )
                        }
                    }
                }
                val intervalMs = if (_uiState.value.isDoNotDisturbActive) 100L else 66L
                delay(intervalMs)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun syncHomeWidget(track: TrackEntity, isPlaying: Boolean) {
        AuraCapsuleWidgetProvider.updateAllWidgets(
            context = getApplication(),
            title = track.title,
            artist = track.artist,
            isPlaying = isPlaying
        )
    }

    fun showFeedback(message: String) {
        toastClearJob?.cancel()
        _uiState.update { it.copy(feedbackMessage = message) }
        toastClearJob = viewModelScope.launch {
            delay(2800L)
            _uiState.update { it.copy(feedbackMessage = null) }
        }
    }

    override fun onCleared() {
        stopProgressTicker()
        audioController.releasePlayer()
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AuraDatabase.getInstance(application)
            val repo = TrackRepository(application, db.trackDao())
            val audio = OfflineAudioController(application)
            return PlayerViewModel(application, repo, audio) as T
        }
    }
}
