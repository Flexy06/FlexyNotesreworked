package com.flexynotes.domain.usecase

import com.flexynotes.data.UserPreferences
import com.flexynotes.data.UserPreferencesRepository
import javax.inject.Inject

class UpdatePreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    // Updates preferences using a lambda function
    suspend operator fun invoke(update: (UserPreferences) -> UserPreferences) {
        repository.updatePreferences(update)
    }
}