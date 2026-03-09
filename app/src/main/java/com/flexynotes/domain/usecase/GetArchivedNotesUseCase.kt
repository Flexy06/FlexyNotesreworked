package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArchivedNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteEntity>> = repository.archivedNotes
}