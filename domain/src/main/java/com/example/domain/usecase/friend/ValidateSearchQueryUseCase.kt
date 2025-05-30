package com.example.domain.usecase.friend

import javax.inject.Inject

/**
 * Use case to validate a search query string.
 * Ensures the query is not blank and meets a minimum length requirement.
 */
class ValidateSearchQueryUseCase @Inject constructor() {

    /**
     * Invokes the use case to validate the search query.
     *
     * @param query The search query string to validate.
     * @return True if the query is valid, false otherwise.
     */
    operator fun invoke(query: String): Boolean {
        if (query.isBlank()) {
            return false
        }
        // Example: Minimum query length of 2 characters
        if (query.length < 2) {
            return false
        }
        return true
    }
}
