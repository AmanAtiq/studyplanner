package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import javax.inject.Inject

class DetectWeakAreasUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend operator fun invoke(userId: String): Result<List<WeakArea>> {
        val quizHistory = mutableListOf<Quiz>()
        firebaseRepository.getQuizHistory(userId).collect { quizHistory.addAll(it) }
        return aiRepository.detectWeakAreas(quizHistory)
    }
}
