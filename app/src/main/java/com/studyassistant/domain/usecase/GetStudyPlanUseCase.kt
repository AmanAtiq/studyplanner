package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import javax.inject.Inject

class GetStudyPlanUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) {
    suspend operator fun invoke(userId: String, targetExam: String): Result<StudyPlan> {
        val quizHistory = mutableListOf<Quiz>()
        firebaseRepository.getQuizHistory(userId).collect { quizHistory.addAll(it) }

        val weakAreasResult = aiRepository.detectWeakAreas(quizHistory)
        val weakAreas = weakAreasResult.getOrDefault(emptyList())

        val planResult = aiRepository.generateStudyPlan(weakAreas, targetExam)
        return planResult.map { plan ->
            val withUser = plan.copy(userId = userId)
            firebaseRepository.saveStudyPlan(withUser)
            localRepository.cacheStudyPlan(withUser)
            withUser
        }
    }
}
