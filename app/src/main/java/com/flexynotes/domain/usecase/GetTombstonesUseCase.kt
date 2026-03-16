package com.flexynotes.domain.usecase

import com.flexynotes.data.TombstoneEntity
import com.flexynotes.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTombstonesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<TombstoneEntity>> = repository.tombstones
}