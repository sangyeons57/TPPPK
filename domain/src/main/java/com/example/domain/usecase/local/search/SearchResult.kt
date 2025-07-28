package com.example.domain.usecase.local.search

data class SearchResult(
    val users: List<Any> = emptyList(),
    val projects: List<Any> = emptyList(),
    val messages: List<Any> = emptyList()
) 