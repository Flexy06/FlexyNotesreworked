package com.flexynotes.domain.usecase

import com.flexynotes.data.UserPreferences
import com.flexynotes.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    // Returns the flow of user preferences
    operator fun invoke(): Flow<UserPreferences> {
        return repository.userPreferencesFlow
    }
}