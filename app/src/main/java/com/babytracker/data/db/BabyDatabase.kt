package com.babytracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.entity.BabyEvent

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE baby_events ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE baby_events SET syncId = lower(hex(randomblob(16)))")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_baby_events_syncId ON baby_events(syncId)"
        )
    }
}

@Database(
    entities = [BabyEvent::class],
    version = 2,
    exportSchema = false
)
abstract class BabyDatabase : RoomDatabase() {
    abstract fun babyEventDao(): BabyEventDao
}
