package com.studyassistant.data.remote

import com.google.gson.annotations.SerializedName

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    @SerializedName("usageMetadata") val usageMetadata: GeminiUsageMetadata = GeminiUsageMetadata()
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerializedName("finishReason") val finishReason: String? = null
)

data class GeminiUsageMetadata(
    @SerializedName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int = 0,
    @SerializedName("totalTokenCount") val totalTokenCount: Int = 0
)

fun GeminiResponse.firstTextOrEmpty(): String {
    val parts = candidates.firstOrNull()?.content?.parts.orEmpty()
    return parts.joinToString("\n") { it.text }.trim()
}
