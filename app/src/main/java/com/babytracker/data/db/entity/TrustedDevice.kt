package com.babytracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A device that the user has permanently trusted for WiFi sync.
 * Session-only trust is kept in-memory in SyncManager.sessionTrustedIds.
 */
@Entity(tableName = "trusted_devices")
data class TrustedDevice(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val addedAt: Long
)
