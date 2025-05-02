package com.example.data.util

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseUtil {
    fun initializeApp(config: Context) {
        FirebaseApp.initializeApp(config);
    }
}