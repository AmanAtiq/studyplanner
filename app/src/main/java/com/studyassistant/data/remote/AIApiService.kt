package com.studyassistant.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

// ── Retrofit Service ──────────────────────────────────
interface AIApiService {
    @POST("chat/completions")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Body request: AIRequest
    ): AIResponse
}
