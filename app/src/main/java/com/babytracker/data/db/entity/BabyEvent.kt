package com.babytracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class EventType {
    FEEDING, DIAPER
}

enum class FeedingSubType {
    BOTTLE,
    BREAST_LEFT,       // lewa pierś
    BREAST_RIGHT,      // prawa pierś
    BREAST_BOTH_LR,    // lewa+prawa
    BREAST_BOTH_RL,    // prawa+lewa
    PUMP,              // laktator
    NATURAL            // legacy — zachowane dla kompatybilności ze starymi wpisami
}

enum class DiaperSubType {
    PEE, POOP, MIXED
}

@Entity(
    tableName = "baby_events",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class BabyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val eventType: String,      // EventType name
    val subType: String,        // FeedingSubType or DiaperSubType name
    val timestamp: Long = System.currentTimeMillis(),
    val milliliters: Int? = null,  // Only for bottle feeding
    val note: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEventType(): EventType = EventType.valueOf(eventType)
    fun toFeedingSubType(): FeedingSubType? = if (eventType == EventType.FEEDING.name)
        FeedingSubType.valueOf(subType) else null
    fun toDiaperSubType(): DiaperSubType? = if (eventType == EventType.DIAPER.name)
        DiaperSubType.valueOf(subType) else null
}
