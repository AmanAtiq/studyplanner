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



@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideAIRepository(apiService: AIApiService): AIRepository =
        AIRepositoryImpl(apiService)

    @Provides @Singleton
    fun provideFirebaseRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): FirebaseRepository = FirebaseRepositoryImpl(auth, firestore, storage)

    @Provides @Singleton
    fun provideLocalRepository(
        noteDao: NoteDao,
        quizDao: QuizDao,
        studyPlanDao: StudyPlanDao,
        dataStore: DataStore<Preferences>
    ): LocalRepository = LocalRepositoryImpl(noteDao, quizDao, studyPlanDao, dataStore)
}