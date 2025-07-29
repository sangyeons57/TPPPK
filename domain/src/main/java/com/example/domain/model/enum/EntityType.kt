package com.example.domain.model.enum

import com.example.domain.model.base.Message

enum class EntityType(val collectionName: String) {
    MESSAGE(Message.COLLECTION_NAME);

    companion object {
        fun fromCollectionName(collectionName: String): EntityType? {
            return values().find { it.collectionName == collectionName }
        }
    }
}