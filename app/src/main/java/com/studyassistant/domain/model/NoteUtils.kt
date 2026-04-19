package com.studyassistant.domain.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Returns a user-friendly title for a note. Priority:
 * 1. note.title if non-blank
 * 2. first non-empty line of summary
 * 3. first non-empty line of originalContent
 * 4. filename from fileUrl
 * 5. "Lecture <MMM dd, yyyy>"
 */
fun friendlyNoteTitle(note: Note, maxLength: Int = 60): String {
    note.title.takeIf { it.isNotBlank() }?.let { return it }

    // helper to pick first meaningful line
    fun firstLine(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        return lines.firstOrNull()
    }

    firstLine(note.summary)?.let { line ->
        return if (line.length > maxLength) line.take(maxLength).trimEnd() + "…" else line
    }

    firstLine(note.originalContent)?.let { line ->
        return if (line.length > maxLength) line.take(maxLength).trimEnd() + "…" else line
    }

    if (note.fileUrl.isNotBlank()) {
        try {
            val parts = note.fileUrl.split('/', '\\')
            val fileName = parts.lastOrNull()?.takeIf { it.isNotBlank() }
            if (!fileName.isNullOrBlank()) return fileName
        } catch (_: Exception) {
        }
    }

    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return "Lecture ${df.format(note.createdAt)}"
}

