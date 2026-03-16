package com.flexynotes.domain.usecase

import com.flexynotes.data.TombstoneEntity
import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class InsertTombstoneUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tombstone: TombstoneEntity) {
        // Saves the incoming cloud tombstone to the local database
        repository.insertTombstone(tombstone)
    }
}