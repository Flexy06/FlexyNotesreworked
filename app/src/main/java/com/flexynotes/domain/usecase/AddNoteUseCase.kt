package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: NoteEntity) {
        if (note.title.isBlank() && note.content.isBlank()) {
            throw IllegalArgumentException("Note must have a title or content")
        }
        repository.upsertNote(note)
    }
}