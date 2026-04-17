package com.studyassistant.repository.ai

import android.util.Log
import com.google.gson.Gson
import com.studyassistant.data.remote.AIApiService
import com.studyassistant.data.remote.GeminiContent
import com.studyassistant.data.remote.GeminiGenerationConfig
import com.studyassistant.data.remote.GeminiPart
import com.studyassistant.data.remote.GeminiRequest
import com.studyassistant.data.remote.firstTextOrEmpty
import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.util.Constants
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val apiService: AIApiService
) : AIRepository {

    private val gson = Gson()

    override suspend fun summarizeNote(content: String, language: AppLanguage): Result<String> {
        return try {
            if (Constants.GEMINI_API_KEY.isBlank()) {
                Log.w("AIRepo", "summarizeNote: API key is blank, using local fallback")
                return Result.success(localSummarize(content))
            }

            val result = withTimeoutOrNull(30_000) {
                val langInstruction = if (language == AppLanguage.URDU)
                    "Respond entirely in Urdu (اردو). Use simple Urdu suitable for students."
                else "Respond in clear English."

                val response = apiService.generateContent(
                    model = Constants.GEMINI_MODEL,
                    apiKey = Constants.GEMINI_API_KEY,
                    request = GeminiRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(
                                GeminiPart(
                                    text = """You are an expert study assistant for Pakistani students (O/A Levels, MDCAT, ECAT).
                                    $langInstruction
                                    Create concise, well-structured summaries with key points, important terms, and exam tips.""".trimIndent()
                                )
                            )
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(
                                    GeminiPart(text = "Summarize the following study notes:\n\n$content")
                                )
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(
                            maxOutputTokens = 1200,
                            temperature = 0.5
                        )
                    )
                )

                val rawText = response.firstTextOrEmpty()

                // CLEANING LOGIC: Removes markdown wrappers that make the UI look "weird"
                rawText.trim()
                    .removePrefix("```markdown")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
            }

            if (result.isNullOrBlank()) {
                Log.w("AIRepo", "summarizeNote: AI empty or timeout, using local fallback")
                return Result.success(localSummarize(content))
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e("AIRepo", "summarizeNote error: ${e.message}")
            Result.success(localSummarize(content))
        }
    }
    override suspend fun generateQuiz(
        content: String,
        numQuestions: Int,
        language: AppLanguage
    ): Result<List<QuizQuestion>> {
        return try {
            if (Constants.GEMINI_API_KEY.isBlank()) {
                Log.w("AIRepo", "generateQuiz: API key missing, using local fallback")
                return localGenerateQuiz(content, numQuestions)
            }
            Log.d("AIRepo", "generateQuiz: starting with timeout")

            // Try AI with 30-second timeout
            val result = withTimeoutOrNull(30_000) {
                Log.d("AIRepo", "generateQuiz: calling AI API")
                val langInstruction = if (language == AppLanguage.URDU)
                    "Write all questions and answers in Urdu (اردو)."
                else "Write in English."

                val response = apiService.generateContent(
                    model = Constants.GEMINI_MODEL,
                    apiKey = Constants.GEMINI_API_KEY,
                    request = GeminiRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(
                                GeminiPart(
                                    text = """You are an exam question generator for Pakistani students.
                                    $langInstruction
                                    IMPORTANT: Respond ONLY with valid JSON array. No markdown, no explanation.""".trimIndent()
                                )
                            )
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(
                                    GeminiPart(
                                        text = """Generate $numQuestions MCQ questions from this content.
                                            Return JSON array with this exact structure:
                                            [
                                              {
                                                "question": "question text",
                                                "options": ["A", "B", "C", "D"],
                                                "correctAnswerIndex": 0,
                                                "explanation": "why this is correct"
                                              }
                                            ]

                                            Content: $content""".trimIndent()
                                    )
                                )
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(
                            maxOutputTokens = 2048,
                            temperature = 0.4,
                            responseMimeType = "application/json"
                        )
                    )
                )

                val rawJson = response.firstTextOrEmpty()
                    ?.trim()
                    ?.removePrefix("```json")
                    ?.removeSuffix("```")
                    ?.trim() ?: "[]"

                // Parse JSON
                val parsed: Array<QuizQuestionDto> = try {
                    gson.fromJson(rawJson, Array<QuizQuestionDto>::class.java)
                } catch (parseEx: Exception) {
                    Log.w("AIRepo", "generateQuiz: parse failed, trying regex extraction")
                    val arrayRegex = Regex("""\[.*\]""", RegexOption.DOT_MATCHES_ALL)
                    val match = arrayRegex.find(rawJson)
                    if (match != null) {
                        try {
                            gson.fromJson(match.value, Array<QuizQuestionDto>::class.java)
                        } catch (ex2: Exception) {
                            throw parseEx
                        }
                    } else {
                        throw parseEx
                    }
                }
                parsed
            }

            if (result == null) {
                Log.w("AIRepo", "generateQuiz: AI timeout after 30s, using local fallback")
                return localGenerateQuiz(content, numQuestions)
            }

            val questions = result.map { dto ->
                QuizQuestion(
                    id = UUID.randomUUID().toString(),
                    question = dto.question,
                    options = dto.options,
                    correctAnswerIndex = dto.correctAnswerIndex,
                    explanation = dto.explanation,
                    type = QuestionType.MCQ
                )
            }
            Log.d("AIRepo", "generateQuiz: AI returned ${questions.size} questions")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e("AIRepo", "generateQuiz: error (falling back to local): ${e.message}", e)
            return localGenerateQuiz(content, numQuestions)
        }
    }

    override suspend fun generateStudyPlan(
        weakAreas: List<WeakArea>,
        targetExam: String
    ): Result<StudyPlan> {
        return try {
            if (Constants.GEMINI_API_KEY.isBlank()) {
                Log.w("AIRepo", "generateStudyPlan: API key missing, using local fallback")
                return localGenerateStudyPlan(weakAreas, targetExam)
            }

            Log.d("AIRepo", "generateStudyPlan: starting with timeout")

            // Try AI with 30-second timeout
            val result = withTimeoutOrNull(30_000) {
                Log.d("AIRepo", "generateStudyPlan: calling AI API")
                val weakTopics = weakAreas.joinToString(", ") { it.topic }
                    .ifEmpty { "Mathematics, Physics, Chemistry, Biology, English" }

                val response = apiService.generateContent(
                    model = Constants.GEMINI_MODEL,
                    apiKey = Constants.GEMINI_API_KEY,
                    request = GeminiRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(
                                GeminiPart(
                                    text = "You are a study planner for Pakistani students preparing for $targetExam. Respond ONLY in valid JSON."
                                )
                            )
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(
                                    GeminiPart(
                                        text = """Create a 7-day study plan focusing on these topics: $weakTopics.
                                            Return ONLY this JSON structure (no markdown, no backticks):
                                            {
                                              "title": "7-Day Study Plan",
                                              "tasks": [
                                                {
                                                  "title": "Study Task Title",
                                                  "description": "What to study",
                                                  "dayOffset": 0,
                                                  "priority": "HIGH"
                                                }
                                              ]
                                            }""".trimIndent()
                                    )
                                )
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(
                            maxOutputTokens = 2048,
                            temperature = 0.3,
                            responseMimeType = "application/json"
                        )
                    )
                )

                val rawJson = response.firstTextOrEmpty()
                    ?.trim()
                    ?.removePrefix("```json")
                    ?.removePrefix("```")
                    ?.removeSuffix("```")
                    ?.trim() ?: "{}"

                gson.fromJson(rawJson, StudyPlanDto::class.java)
            }

            if (result == null) {
                Log.w("AIRepo", "generateStudyPlan: AI timeout after 30s, using local fallback")
                return localGenerateStudyPlan(weakAreas, targetExam)
            }

            if (result.tasks.isEmpty()) {
                Log.w("AIRepo", "generateStudyPlan: AI returned empty tasks, using local fallback")
                return localGenerateStudyPlan(weakAreas, targetExam)
            }

            val now = java.util.Date()
            val plan = StudyPlan(
                id = java.util.UUID.randomUUID().toString(),
                title = result.title.ifEmpty { "Study Plan for $targetExam" },
                tasks = result.tasks.map { task ->
                    val due = java.util.Calendar.getInstance().apply {
                        time = now
                        add(java.util.Calendar.DAY_OF_YEAR, task.dayOffset)
                    }.time
                    StudyTask(
                        id = java.util.UUID.randomUUID().toString(),
                        title = task.title.ifEmpty { "Study Task" },
                        description = task.description.ifEmpty { "Study and practice" },
                        dueDate = due,
                        priority = try {
                            Priority.valueOf(task.priority.uppercase())
                        } catch (e: Exception) {
                            Priority.MEDIUM
                        }
                    )
                },
                startDate = now,
                endDate = java.util.Calendar.getInstance().apply {
                    time = now
                    add(java.util.Calendar.DAY_OF_YEAR, 7)
                }.time
            )
            Log.d("AIRepo", "generateStudyPlan: AI returned plan with ${plan.tasks.size} tasks")
            Result.success(plan)
        } catch (e: Exception) {
            Log.e("AIRepo", "generateStudyPlan: error (falling back to local): ${e.message}", e)
            return localGenerateStudyPlan(weakAreas, targetExam)
        }
    }

    override suspend fun detectWeakAreas(quizHistory: List<Quiz>): Result<List<WeakArea>> {
        val weakAreas = mutableListOf<WeakArea>()
        quizHistory.forEach { quiz ->
            val wrong = quiz.questions.filter {
                it.selectedAnswerIndex != it.correctAnswerIndex && it.selectedAnswerIndex != -1
            }
            if (wrong.isNotEmpty()) {
                weakAreas.add(
                    WeakArea(
                        id = UUID.randomUUID().toString(),
                        topic = "Quiz ${quiz.id.take(6)}",
                        accuracy = (quiz.score.toFloat() / quiz.questions.size.coerceAtLeast(1)),
                        totalAttempts = quiz.questions.size,
                        suggestions = wrong.map { it.explanation }.take(3)
                    )
                )
            }
        }
        return Result.success(weakAreas)
    }

    override suspend fun translateContent(content: String, targetLanguage: AppLanguage): Result<String> {
        return try {
            if (Constants.GEMINI_API_KEY.isBlank()) return Result.success(content)

            val lang = if (targetLanguage == AppLanguage.URDU) "Urdu (اردو)" else "English"
            val response = apiService.generateContent(
                model = Constants.GEMINI_MODEL,
                apiKey = Constants.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(
                                GeminiPart(
                                    text = "Translate the following study content to $lang. Keep formatting intact:\n\n$content"
                                )
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        maxOutputTokens = 1800,
                        temperature = 0.2
                    )
                )
            )
            Result.success(response.firstTextOrEmpty().ifEmpty { content })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun localSummarize(content: String): String {
        val paragraphs = content.split(Regex("\\n\\s*\\n"))
        val candidate = paragraphs.firstOrNull { it.isNotBlank() } ?: content
        return candidate.trim().take(800)
    }

    private fun localGenerateQuiz(content: String, numQuestions: Int): Result<List<QuizQuestion>> {
        val sentences = content.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length > 20 }.distinct()
        if (sentences.isEmpty()) return Result.failure(Exception("Insufficient content for quiz generation."))

        val questions = (1..numQuestions).map { idx ->
            val questionContent = sentences.getOrNull(idx % sentences.size) ?: "?"
            val options = List(4) { optionIdx -> "Option ${optionIdx + 1} for '$questionContent'" }
            val correctAnswerIndex = 0 // First option as correct for fallback
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                question = "What is $questionContent?",
                options = options,
                correctAnswerIndex = correctAnswerIndex,
                explanation = "This is a placeholder explanation for the quiz question.",
                type = QuestionType.MCQ
            )
        }
        return Result.success(questions)
    }

    private fun localGenerateStudyPlan(
        weakAreas: List<WeakArea>,
        targetExam: String
    ): Result<StudyPlan> {
        return try {
            val now = java.util.Date()
            val topics = if (weakAreas.isNotEmpty()) {
                weakAreas.take(5).map { it.topic }
            } else {
                listOf("Mathematics", "Physics", "Chemistry", "Biology", "English")
            }

            val tasks = mutableListOf<StudyTask>()
            topics.forEachIndexed { idx, topic ->
                val dayOffset = idx + 1
                val due = java.util.Calendar.getInstance().apply {
                    time = now
                    add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
                }.time

                tasks.add(
                    StudyTask(
                        id = UUID.randomUUID().toString(),
                        title = "Review: $topic",
                        description = "Study and revise $topic. Focus on weak areas identified from your quizzes.",
                        dueDate = due,
                        priority = if (idx < 2) Priority.HIGH else Priority.MEDIUM
                    )
                )
            }

            val plan = StudyPlan(
                id = UUID.randomUUID().toString(),
                title = "Study Plan for $targetExam",
                tasks = tasks,
                startDate = now,
                endDate = java.util.Calendar.getInstance().apply {
                    time = now
                    add(java.util.Calendar.DAY_OF_YEAR, 7)
                }.time
            )

            Log.d("AIRepo", "localGenerateStudyPlan: created fallback plan with ${plan.tasks.size} tasks")
            Result.success(plan)
        } catch (e: Exception) {
            Log.e("AIRepo", "localGenerateStudyPlan: failed", e)
            Result.failure(e)
        }
    }
}

// Internal DTOs for JSON parsing
private data class QuizQuestionDto(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val explanation: String = ""
)

private data class StudyPlanDto(
    val title: String = "",
    val tasks: List<StudyTaskDto> = emptyList()
)

private data class StudyTaskDto(
    val title: String = "",
    val description: String = "",
    val dayOffset: Int = 0,
    val priority: String = "MEDIUM"
)