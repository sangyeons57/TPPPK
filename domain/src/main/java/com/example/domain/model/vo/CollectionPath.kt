package com.example.domain.model.vo

@JvmInline
value class CollectionPath(val value : String){
    companion object{
        val USERS = CollectionPath("users")
    }
}
