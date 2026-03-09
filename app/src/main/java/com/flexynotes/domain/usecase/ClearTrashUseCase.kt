package com.flexynotes.domain.usecase

import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class ClearTrashUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke() = repository.clearTrash()
}