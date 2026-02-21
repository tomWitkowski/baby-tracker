package com.babytracker.data.db.dao

import androidx.room.*
import com.babytracker.data.db.entity.BabyEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BabyEvent): Long

    @Delete
    suspend fun deleteEvent(event: BabyEvent)

    @Update
    suspend fun updateEvent(event: BabyEvent)

    @Query("SELECT * FROM baby_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events ORDER BY timestamp DESC LIMIT 5")
    fun getRecentEvents(): Flow<List<BabyEvent>>

    @Query("SELECT COUNT(*) FROM baby_events WHERE eventType = :eventType AND timestamp BETWEEN :startOfDay AND :endOfDay")
    suspend fun countEventsOfType(eventType: String, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM baby_events WHERE eventType = :eventType AND subType = :subType AND timestamp BETWEEN :startOfDay AND :endOfDay")
    suspend fun countEventsOfSubType(eventType: String, subType: String, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT SUM(milliliters) FROM baby_events WHERE eventType = 'FEEDING' AND subType = 'BOTTLE' AND milliliters IS NOT NULL AND timestamp BETWEEN :startOfDay AND :endOfDay")
    suspend fun totalMlForDay(startOfDay: Long, endOfDay: Long): Int?

    @Query("SELECT * FROM baby_events ORDER BY timestamp DESC")
    suspend fun getAllEventsSync(): List<BabyEvent>

    @Query("DELETE FROM baby_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
