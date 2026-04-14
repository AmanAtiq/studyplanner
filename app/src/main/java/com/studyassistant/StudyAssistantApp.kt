package com.studyassistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}