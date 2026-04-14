package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.NoteEntity
import com.studyassistant.data.local.entity.QuizEntity
import com.studyassistant.data.local.entity.StudyPlanEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans LIMIT 1")
    suspend fun getStudyPlan(): StudyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans")
    suspend fun clearStudyPlans()
}