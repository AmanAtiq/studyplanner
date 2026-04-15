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

// ── DTOs ──────────────────────────────────────────────
data class AIMessage(
    val role: String,
    val content: String
)

data class AIRequest(
    val model: String = "deepseek-chat",
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    val messages: List<AIMessage>,
    val temperature: Double = 0.7
)

