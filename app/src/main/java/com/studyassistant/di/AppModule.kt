package com.studyassistant.di

import android.content.Context
import androidx.room.Room
import com.studyassistant.data.local.dao.StudyGroupDao
import com.studyassistant.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "study_assistant_database"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideStudyGroupDao(database: AppDatabase): StudyGroupDao =
        database.studyGroupDao()
}
