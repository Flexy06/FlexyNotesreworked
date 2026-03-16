package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: NoteEntity) {
        // Return early instead of throwing an exception to prevent app crashes
        // when a user navigates away from a completely empty note.
        if (note.title.isBlank() && note.content.isBlank()) {
            return
        }
        repository.upsertNote(note)
    }
}