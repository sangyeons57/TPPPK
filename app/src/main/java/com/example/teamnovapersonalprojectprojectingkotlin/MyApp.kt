package com.example.teamnovapersonalprojectprojectingkotlin

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
    }
}