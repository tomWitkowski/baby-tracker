package com.babytracker.di

import android.content.Context
import androidx.room.Room
import com.babytracker.data.db.BabyDatabase
import com.babytracker.data.db.MIGRATION_1_2
import com.babytracker.data.db.MIGRATION_2_3
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.dao.SyncTombstoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BabyDatabase {
        return Room.databaseBuilder(
            context,
            BabyDatabase::class.java,
            "baby_tracker_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    @Singleton
    fun provideBabyEventDao(database: BabyDatabase): BabyEventDao {
        return database.babyEventDao()
    }

    @Provides
    @Singleton
    fun provideSyncTombstoneDao(database: BabyDatabase): SyncTombstoneDao {
        return database.syncTombstoneDao()
    }
}
