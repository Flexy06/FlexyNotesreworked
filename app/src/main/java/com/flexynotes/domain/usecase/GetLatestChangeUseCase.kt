package com.flexynotes.domain.usecase

import com.flexynotes.repository.NoteRepository
import javax.inject.Inject

class GetLatestChangeUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(): Long {
        return repository.getLatestLocalModificationTime()
    }
}