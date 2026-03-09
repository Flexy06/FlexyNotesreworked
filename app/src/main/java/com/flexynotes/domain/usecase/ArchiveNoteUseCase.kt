package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class ArchiveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: NoteEntity) = repository.archiveNote(note)
}