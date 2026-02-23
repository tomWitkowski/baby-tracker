package com.babytracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babytracker.data.db.entity.SyncTombstone

@Dao
interface SyncTombstoneDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(tombstone: SyncTombstone)

    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAllTombstones(): List<SyncTombstone>

    @Query("SELECT COUNT(*) FROM sync_tombstones WHERE syncId = :syncId")
    suspend fun countBySyncId(syncId: String): Int
}
