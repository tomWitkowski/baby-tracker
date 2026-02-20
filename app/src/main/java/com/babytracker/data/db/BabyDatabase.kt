package com.babytracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.entity.BabyEvent

@Database(
    entities = [BabyEvent::class],
    version = 1,
    exportSchema = false
)
abstract class BabyDatabase : RoomDatabase() {
    abstract fun babyEventDao(): BabyEventDao
}
