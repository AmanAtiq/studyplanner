package com.studyassistant.domain.usecase

import android.util.Log
import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class GetStudyPlanUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) {
    suspend operator fun invoke(userId: String, targetExam: String): Result<StudyPlan> {
        return try {
            Log.d("GetStudyPlanUC", "invoke: starting for user=$userId exam=$targetExam")

            // Step 1: Fetch quiz history with timeout
            Log.d("GetStudyPlanUC", "invoke: fetching quiz history")
            val quizHistory = mutableListOf<Quiz>()
            val fetchResult = withTimeoutOrNull(5_000) { // Quick 5s timeout
                firebaseRepository.getQuizHistory(userId).collect { quizzes ->
                    Log.d("GetStudyPlanUC", "invoke: received ${quizzes.size} quizzes")
                    quizHistory.clear()
                    quizHistory.addAll(quizzes)
                }
            }

            if (fetchResult == null) {
                Log.w("GetStudyPlanUC", "invoke: quiz history fetch timed out, using empty list")
            }

            Log.d("GetStudyPlanUC", "invoke: quiz history ready, count=${quizHistory.size}")

            // Step 2: Detect weak areas
            Log.d("GetStudyPlanUC", "invoke: detecting weak areas from ${quizHistory.size} quizzes")
            val weakAreasResult = aiRepository.detectWeakAreas(quizHistory)
            val weakAreas = weakAreasResult.getOrDefault(emptyList())
            Log.d("GetStudyPlanUC", "invoke: detected ${weakAreas.size} weak areas: ${weakAreas.map { it.topic }}")

            // Step 3: Generate study plan (uses local fallback if no API key or AI fails)
            Log.d("GetStudyPlanUC", "invoke: generating study plan for exam=$targetExam")
            val planResult = aiRepository.generateStudyPlan(weakAreas, targetExam)

            planResult.fold(
                onSuccess = { plan ->
                    Log.d("GetStudyPlanUC", "invoke: plan generated with ${plan.tasks.size} tasks")
                    val withUser = plan.copy(userId = userId)
                    try {
                        firebaseRepository.saveStudyPlan(withUser)
                        localRepository.cacheStudyPlan(withUser)
                        Log.d("GetStudyPlanUC", "invoke: plan saved to Firebase and local cache")
                    } catch (e: Exception) {
                        Log.w("GetStudyPlanUC", "invoke: failed to save plan, but returning local copy", e)
                    }
                    Result.success(withUser)
                },
                onFailure = { e ->
                    Log.e("GetStudyPlanUC", "invoke: plan generation failed: ${e.message}", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e("GetStudyPlanUC", "invoke: unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
