package com.studyassistant.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.studyassistant.data.local.dao.NoteDao
import com.studyassistant.data.local.dao.QuizDao
import com.studyassistant.data.local.dao.StudyPlanDao
import com.studyassistant.data.local.db.AppDatabase
import com.studyassistant.data.remote.AIApiClient
import com.studyassistant.data.remote.AIApiService
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import com.studyassistant.repository.ai.AIRepositoryImpl
import com.studyassistant.repository.firebase.FirebaseRepositoryImpl
import com.studyassistant.repository.local.LocalRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "study_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
