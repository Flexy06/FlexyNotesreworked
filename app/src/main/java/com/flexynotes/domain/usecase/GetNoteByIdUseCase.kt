package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: String): NoteEntity? {
        return repository.getNoteById(id)
    }
}