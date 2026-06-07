package com.studyassistant.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyassistant.data.local.dao.*
import com.studyassistant.data.local.dao.FlashcardDao

import com.studyassistant.data.local.db.AppDatabase
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

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

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao =
        database.chatMessageDao()

    @Provides
    @Singleton
    fun provideGradeEntryDao(database: AppDatabase): GradeEntryDao =
        database.gradeEntryDao()

    @Provides
    @Singleton
    fun provideFlashcardDao(database: AppDatabase): FlashcardDao =
        database.flashcardDao()

    @Provides
    @Singleton
    fun provideBadgeDao(database: AppDatabase): BadgeDao =
        database.badgeDao()
}
