package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.GradeEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntryEntity)

    @Query("SELECT * FROM grade_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGradesForUser(userId: String): Flow<List<GradeEntryEntity>>

    @Query("SELECT * FROM grade_entries WHERE id = :id")
    suspend fun getGradeById(id: String): GradeEntryEntity?

    @Query("DELETE FROM grade_entries WHERE id = :id")
    suspend fun deleteGrade(id: String)
}
