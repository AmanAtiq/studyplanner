package com.studyassistant

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure Firebase is initialized early
        val app = FirebaseApp.initializeApp(this)
        val expectedProjectId = "aistudenthelper-1ca6b"
        val expectedProjectNumber = "22334162419"

        if (app == null) {
            Log.e("StudyAssistantApp", "FirebaseApp.initializeApp returned null — google-services.json may be missing or malformed.")
        } else {
            val projectId = app.options.projectId ?: "<none>"
            val applicationId = app.options.applicationId ?: "<none>"
            Log.i("StudyAssistantApp", "Firebase initialized: projectId=$projectId applicationId=$applicationId")

            if (projectId != expectedProjectId) {
                Log.w("StudyAssistantApp", "Firebase projectId mismatch: expected '$expectedProjectId' but initialized with '$projectId'. Replace app/google-services.json with the one from the Firebase project aistudenthelper-1ca6b.")
            }

            // applicationId contains the google app id string (like 1:NNNN:android:xxxx)
            if (!applicationId.contains(expectedProjectNumber) && !applicationId.contains("${expectedProjectNumber}")) {
                Log.w("StudyAssistantApp", "Firebase applicationId (google_app_id) does not contain project number '$expectedProjectNumber'. This indicates the google-services.json belongs to a different Firebase project.")
            }
        }

        // Extra check: see if generated google_app_id resource exists
        try {
            val resId = resources.getIdentifier("google_app_id", "string", packageName)
            if (resId == 0) {
                Log.e("StudyAssistantApp", "Resource 'google_app_id' not found. Google services plugin may not have processed google-services.json.")
            } else {
                val valStr = getString(resId)
                Log.i("StudyAssistantApp", "Found google_app_id resource: $valStr")
            }
        } catch (e: Exception) {
            Log.e("StudyAssistantApp", "Error checking google_app_id resource: ${e.message}")
        }
    }
}