package com.babytracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    FEEDING, DIAPER
}

enum class FeedingSubType {
    BOTTLE, NATURAL
}

enum class DiaperSubType {
    PEE, POOP, MIXED
}

@Entity(tableName = "baby_events")
data class BabyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,      // EventType name
    val subType: String,        // FeedingSubType or DiaperSubType name
    val timestamp: Long = System.currentTimeMillis(),
    val milliliters: Int? = null,  // Only for bottle feeding
    val note: String? = null
) {
    fun toEventType(): EventType = EventType.valueOf(eventType)
    fun toFeedingSubType(): FeedingSubType? = if (eventType == EventType.FEEDING.name)
        FeedingSubType.valueOf(subType) else null
    fun toDiaperSubType(): DiaperSubType? = if (eventType == EventType.DIAPER.name)
        DiaperSubType.valueOf(subType) else null
}
