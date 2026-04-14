package com.studyassistant.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.studyassistant.data.local.dao.NoteDao
import com.studyassistant.data.local.dao.QuizDao
import com.studyassistant.data.local.dao.StudyPlanDao
import com.studyassistant.data.local.entity.NoteEntity
import com.studyassistant.data.local.entity.QuizEntity
import com.studyassistant.data.local.entity.StudyPlanEntity

@Database(
    entities = [NoteEntity::class, QuizEntity::class, StudyPlanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun quizDao(): QuizDao
    abstract fun studyPlanDao(): StudyPlanDao
}