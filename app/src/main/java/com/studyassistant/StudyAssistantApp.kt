package com.studyassistant

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("StudyAssistantApp", "Local JSON persistence initialized.")
    }
}