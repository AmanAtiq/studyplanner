package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import javax.inject.Inject


class GenerateQuizUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend operator fun invoke(
        note: Note,
        numQuestions: Int = 10,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Result<Quiz> {
        val content = note.summary.ifEmpty { note.originalContent }
        val questionsResult = aiRepository.generateQuiz(content, numQuestions, language)
        return questionsResult.map { questions ->
            // Try to generate a short, user-friendly title for this quiz
            val title = try {
                aiRepository.generateQuizTitle(content, language).getOrElse { "" }
            } catch (e: Exception) {
                ""
            }

            val quiz = Quiz(
                noteId = note.id,
                userId = note.userId,
                title = title.ifBlank { note.title.ifBlank { "Quiz for ${note.id.take(6)}" } },
                questions = questions
            )
            firebaseRepository.saveQuiz(quiz).getOrDefault(quiz)
        }
    }
}
