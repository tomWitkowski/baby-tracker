package com.babytracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.dao.SyncTombstoneDao
import com.babytracker.data.db.dao.TrustedDeviceDao
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.SyncTombstone
import com.babytracker.data.db.entity.TrustedDevice

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE baby_events ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE baby_events SET syncId = lower(hex(randomblob(16)))")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_baby_events_syncId ON baby_events(syncId)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE baby_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE baby_events SET updatedAt = timestamp")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS sync_tombstones (
                syncId TEXT NOT NULL PRIMARY KEY,
                deletedAt INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS trusted_devices (
                deviceId TEXT NOT NULL PRIMARY KEY,
                deviceName TEXT NOT NULL DEFAULT '',
                addedAt INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}

@Database(
    entities = [BabyEvent::class, SyncTombstone::class, TrustedDevice::class],
    version = 4,
    exportSchema = false
)
abstract class BabyDatabase : RoomDatabase() {
    abstract fun babyEventDao(): BabyEventDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao
    abstract fun trustedDeviceDao(): TrustedDeviceDao
}
