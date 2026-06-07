package com.studyassistant.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.studyassistant.data.local.dao.*

import com.studyassistant.data.local.entity.*

@Database(
    entities = [
        NoteEntity::class,
        NoteBadgeCrossRef::class,
        QuizEntity::class,
        StudyPlanEntity::class,
        StudyGroupEntity::class,
        StudyGroupMemberEntity::class,
        GroupMessageEntity::class,
        ChatMessageEntity::class,
        GradeEntryEntity::class,
        FlashcardEntity::class,
        BadgeEntity::class

    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun quizDao(): QuizDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyGroupDao(): StudyGroupDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun gradeEntryDao(): GradeEntryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun badgeDao(): BadgeDao

}
