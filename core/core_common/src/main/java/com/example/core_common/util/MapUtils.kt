package com.example.core_common.util

object MapUtils {

    /**
     * Compares two maps and returns a new map containing only the fields that have changed.
     *
     * @param originalState The original map representing the old state.
     * @param newState The new map representing the current state.
     * @return A map containing only the key-value pairs that are different in newState compared to originalState.
     */
    fun getChangedFields(
        originalState: Map<String, Any?>,
        newState: Map<String, Any?>
    ): Map<String, Any?> {
        val changedFields = mutableMapOf<String, Any?>()

        newState.forEach { (key, newValue) ->
            val oldValue = originalState[key]
            if (newValue != oldValue) {
                changedFields[key] = newValue
            }
        }
        return changedFields
    }
}
