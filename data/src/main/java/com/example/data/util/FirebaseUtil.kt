package com.example.core_common

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseUtil {
    fun initializeApp(config: Context) {
        FirebaseApp.initializeApp(config);
    }
}