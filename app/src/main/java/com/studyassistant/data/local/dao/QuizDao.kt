package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.NoteEntity
import com.studyassistant.data.local.entity.QuizEntity
import com.studyassistant.data.local.entity.StudyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Query("DELETE FROM quizzes WHERE id = :id")
    suspend fun deleteById(id: String)
}
