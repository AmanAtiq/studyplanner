package com.studyassistant.data.remote

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart> = emptyList()
)

data class GeminiPart(
    val text: String = ""
)

data class GeminiGenerationConfig(
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
    @SerializedName("responseMimeType") val responseMimeType: String? = null
)

