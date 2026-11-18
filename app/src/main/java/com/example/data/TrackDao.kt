package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY id ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY id ASC")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE audioUriString = :uriString LIMIT 1")
    suspend fun getTrackByUri(uriString: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isPinnedToCanvas = :pinned, canvasPinXFraction = :xFraction, canvasPinYFraction = :yFraction WHERE id = :trackId")
    suspend fun updateCanvasPin(trackId: Long, pinned: Boolean, xFraction: Float, yFraction: Float)

    @Query("UPDATE tracks SET isPinnedToHome = :pinned WHERE id = :trackId")
    suspend fun updateHomePin(trackId: Long, pinned: Boolean)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)
}
