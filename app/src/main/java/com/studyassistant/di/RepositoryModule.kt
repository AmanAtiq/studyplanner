package com.studyassistant.di

import com.studyassistant.data.local.dao.StudyGroupDao
import com.studyassistant.data.remote.AIApiService
import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import com.studyassistant.repository.StudyGroupRepository
import com.studyassistant.repository.ai.AIRepositoryImpl
import com.studyassistant.repository.firebase.FirebaseRepositoryImpl
import com.studyassistant.repository.local.LocalRepositoryImpl
import com.studyassistant.repository.studygroup.StudyGroupRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideAIRepository(apiService: AIApiService): AIRepository =
        AIRepositoryImpl(apiService)

    @Provides @Singleton
    fun provideFirebaseRepository(store: JsonPersistenceStore): FirebaseRepository =
        FirebaseRepositoryImpl(store)

    @Provides @Singleton
    fun provideLocalRepository(store: JsonPersistenceStore): LocalRepository =
        LocalRepositoryImpl(store)

    @Provides @Singleton
    fun provideStudyGroupRepository(dao: StudyGroupDao): StudyGroupRepository =
        StudyGroupRepositoryImpl(dao)
}
