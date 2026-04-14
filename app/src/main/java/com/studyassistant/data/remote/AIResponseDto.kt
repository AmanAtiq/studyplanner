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



data class AIResponse(
    val id: String = "",
    val content: List<ContentBlock> = emptyList(),
    val model: String = "",
    val usage: Usage = Usage()
)

data class ContentBlock(
    val type: String = "text",
    val text: String = ""
)

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int = 0,
    @SerializedName("output_tokens") val outputTokens: Int = 0
)
