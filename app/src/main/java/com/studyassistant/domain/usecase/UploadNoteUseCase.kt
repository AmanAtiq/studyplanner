package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import javax.inject.Inject

class UploadNoteUseCase @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        content: String,
        fileBytes: ByteArray? = null,
        fileName: String? = null,
        fileType: FileType = FileType.TEXT
    ): Result<Note> {
        var fileUrl = ""
        if (fileBytes != null && fileName != null) {
            val uploadResult = firebaseRepository.uploadFile(userId, fileBytes, fileName)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("File upload failed"))
            }
            fileUrl = uploadResult.getOrDefault("")
        }

        val note = Note(
            userId = userId,
            title = title,
            originalContent = content,
            fileUrl = fileUrl,
            fileType = fileType
        )

        return firebaseRepository.saveNote(note).also { result ->
            result.onSuccess { saved -> localRepository.cacheNote(saved) }
        }
    }
}