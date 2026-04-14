package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import javax.inject.Inject

class SummarizeNoteUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) {
    suspend operator fun invoke(note: Note, language: AppLanguage): Result<Note> {
        val summaryResult = aiRepository.summarizeNote(note.originalContent, language)
        return summaryResult.map { summary ->
            val updated = note.copy(summary = summary, language = language)
            firebaseRepository.saveNote(updated)
            localRepository.cacheNote(updated)
            updated
        }
    }
}
