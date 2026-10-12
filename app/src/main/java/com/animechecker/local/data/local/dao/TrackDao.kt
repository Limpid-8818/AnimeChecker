package com.animechecker.local.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.animechecker.local.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert
    suspend fun insert(track: TrackEntity): Long

    @Upsert
    suspend fun upsert(track: TrackEntity)

    @Query("SELECT * FROM tracks")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE subjectId = :subjectId LIMIT 1")
    fun observeBySubject(subjectId: Long): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE localId = :localId")
    suspend fun byId(localId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE subjectId = :subjectId LIMIT 1")
    suspend fun bySubject(subjectId: Long): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun all(): List<TrackEntity>

    @Query("DELETE FROM tracks WHERE localId = :localId")
    suspend fun delete(localId: Long)
}
