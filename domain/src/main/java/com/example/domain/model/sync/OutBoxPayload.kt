package com.example.domain.model.sync

data class OutBoxPayload(
    val jsonData: String
) {
    companion object {
        fun create(jsonData: String): OutBoxPayload {
            require(jsonData.isNotBlank()) { "JSON data cannot be blank" }
            return OutBoxPayload(jsonData)
        }
        
        fun empty(): OutBoxPayload = OutBoxPayload("{}")
    }
    
    fun isEmpty(): Boolean = jsonData.isEmpty() || jsonData == "{}"
    fun isValidJson(): Boolean {
        return try {
            jsonData.isNotBlank() && (jsonData.startsWith("{") || jsonData.startsWith("["))
        } catch (e: Exception) {
            false
        }
    }
}