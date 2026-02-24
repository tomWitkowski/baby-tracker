package com.babytracker.data.repository

import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.dao.SyncTombstoneDao
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.db.entity.SyncTombstone
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class DayStats(
    val date: Long,
    val totalFeedings: Int,
    val bottleFeedings: Int,
    val breastFeedings: Int,   // wszystkie BREAST_* + legacy NATURAL
    val pumpFeedings: Int,     // wszystkie PUMP_*
    val totalMl: Int,          // ml z butelek
    val totalPumpMl: Int,      // ml z laktatora (wszystkie PUMP_*)
    val totalDiapers: Int,
    val peeDiapers: Int,
    val poopDiapers: Int,
    val mixedDiapers: Int,
    val spitUpCount: Int,
    val events: List<BabyEvent>
)

@Singleton
class EventRepository @Inject constructor(
    private val dao: BabyEventDao,
    private val tombstoneDao: SyncTombstoneDao
) {
    fun getAllEvents(): Flow<List<BabyEvent>> = dao.getAllEvents()

    fun getRecentEvents(): Flow<List<BabyEvent>> = dao.getRecentEvents()

    fun getEventsForDay(dayTimestamp: Long): Flow<List<BabyEvent>> {
        val (start, end) = getDayBounds(dayTimestamp)
        return dao.getEventsForDay(start, end)
    }

    suspend fun logFeeding(subType: FeedingSubType, milliliters: Int? = null) {
        dao.insertEvent(
            BabyEvent(
                eventType = EventType.FEEDING.name,
                subType = subType.name,
                milliliters = milliliters
            )
        )
    }

    suspend fun logDiaper(subType: DiaperSubType) {
        dao.insertEvent(
            BabyEvent(
                eventType = EventType.DIAPER.name,
                subType = subType.name
            )
        )
    }

    suspend fun logSpitUp() {
        dao.insertEvent(
            BabyEvent(
                eventType = EventType.SPIT_UP.name,
                subType = EventType.SPIT_UP.name
            )
        )
    }

    suspend fun deleteEvent(event: BabyEvent) {
        tombstoneDao.insertTombstone(SyncTombstone(syncId = event.syncId))
        dao.deleteEvent(event)
    }

    suspend fun updateEvent(event: BabyEvent) {
        dao.updateEvent(event.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteById(id: Long) {
        val event = dao.getEventById(id)
        if (event != null) {
            tombstoneDao.insertTombstone(SyncTombstone(syncId = event.syncId))
        }
        dao.deleteById(id)
    }

    suspend fun getDayStats(dayTimestamp: Long): DayStats {
        val (start, end) = getDayBounds(dayTimestamp)
        val totalFeedings = dao.countEventsOfType(EventType.FEEDING.name, start, end)
        val bottleFeedings = dao.countEventsOfSubType(EventType.FEEDING.name, FeedingSubType.BOTTLE.name, start, end)
        val breastFeedings = dao.countBreastFeedings(EventType.FEEDING.name, start, end)
        val pumpFeedings = dao.countPumpFeedings(start, end)
        val totalMl = dao.totalMlForDay(start, end) ?: 0
        val totalPumpMl = dao.totalPumpMlForDay(start, end) ?: 0
        val totalDiapers = dao.countEventsOfType(EventType.DIAPER.name, start, end)
        val peeDiapers = dao.countEventsOfSubType(EventType.DIAPER.name, DiaperSubType.PEE.name, start, end)
        val poopDiapers = dao.countEventsOfSubType(EventType.DIAPER.name, DiaperSubType.POOP.name, start, end)
        val mixedDiapers = dao.countEventsOfSubType(EventType.DIAPER.name, DiaperSubType.MIXED.name, start, end)
        val spitUpCount = dao.countSpitUpEvents(start, end)

        return DayStats(
            date = dayTimestamp,
            totalFeedings = totalFeedings,
            bottleFeedings = bottleFeedings,
            breastFeedings = breastFeedings,
            pumpFeedings = pumpFeedings,
            totalMl = totalMl,
            totalPumpMl = totalPumpMl,
            totalDiapers = totalDiapers,
            peeDiapers = peeDiapers,
            poopDiapers = poopDiapers,
            mixedDiapers = mixedDiapers,
            spitUpCount = spitUpCount,
            events = emptyList()
        )
    }

    suspend fun getWeekStats(weekStartTimestamp: Long): List<DayStats> {
        return (0 until 7).map { dayOffset ->
            getDayStats(weekStartTimestamp + dayOffset * 86_400_000L)
        }
    }

    suspend fun getAllEventsForExport(): List<BabyEvent> = dao.getAllEventsSync()

    private fun getDayBounds(dayTimestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = dayTimestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
