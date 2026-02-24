package com.babytracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_tombstones")
data class SyncTombstone(
    @PrimaryKey val syncId: String,
    val deletedAt: Long = System.currentTimeMillis()
)
