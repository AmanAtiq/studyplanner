package com.studyassistant.di

import com.studyassistant.data.remote.AIApiClient
import com.studyassistant.data.remote.AIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideAIApiService(): AIApiService = AIApiClient.create()
}
