package com.flexynotes.domain.usecase

import com.flexynotes.data.TombstoneEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class InsertTombstoneUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tombstone: TombstoneEntity) {
        //  might need to add a simple insertTombstone method to your NoteRepository for this
        // repository.insertTombstone(tombstone)
    }
}