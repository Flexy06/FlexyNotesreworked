package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: NoteEntity) {
        if (note.title.isBlank()) {
            throw IllegalArgumentException("title can not be empty.")
        }
        // repository.insertNote(note)
    }
}
