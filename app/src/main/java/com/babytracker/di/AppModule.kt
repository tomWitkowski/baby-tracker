package com.babytracker.di

import android.content.Context
import androidx.room.Room
import com.babytracker.data.db.BabyDatabase
import com.babytracker.data.db.dao.BabyEventDao
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideBabyEventDao(database: BabyDatabase): BabyEventDao {
        return database.babyEventDao()
    }
}
