package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    // Return the ID of the newly inserted note
    suspend operator fun invoke(note: NoteEntity): Long {
        if (note.title.isBlank() && note.content.isBlank()) {
            throw IllegalArgumentException("Note must have a title or content")
        }
        return repository.upsertNote(note)
    }
}