package com.example.core_navigation.core

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val TypeSafeRouteSaver = Saver<TypeSafeRoute, String>(
    save = { route -> Json.encodeToString(route) },
    restore = { str -> Json.decodeFromString<TypeSafeRoute>(str) }
) 