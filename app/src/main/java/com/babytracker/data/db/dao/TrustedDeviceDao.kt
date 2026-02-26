package com.babytracker.data.db.dao

import androidx.room.*
import com.babytracker.data.db.entity.TrustedDevice

@Dao
interface TrustedDeviceDao {

    @Query("SELECT COUNT(*) FROM trusted_devices WHERE deviceId = :id")
    suspend fun isTrusted(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: TrustedDevice)

    @Query("DELETE FROM trusted_devices WHERE deviceId = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM trusted_devices ORDER BY addedAt DESC")
    suspend fun getAll(): List<TrustedDevice>
}
