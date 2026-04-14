package com.studyassistant.repository.ai

import com.google.gson.Gson
import com.studyassistant.data.remote.AIApiService
import com.studyassistant.data.remote.AIMessage
import com.studyassistant.data.remote.AIRequest
import com.studyassistant.domain.model.*
import com.studyassistant.repository.AIRepository
import com.studyassistant.util.Constants
import java.util.UUID
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val apiService: AIApiService
) : AIRepository {

    private val gson = Gson()

    override suspend fun summarizeNote(content: String, language: AppLanguage): Result<String> {
        return try {
            val langInstruction = if (language == AppLanguage.URDU)
                "Respond entirely in Urdu (اردو). Use simple Urdu suitable for students."
            else "Respond in clear English."

            val request = AIRequest(
                system = """You are an expert study assistant for Pakistani students (O/A Levels, MDCAT, ECAT).
                    $langInstruction
                    Create concise, well-structured summaries with key points, important terms, and exam tips.""",
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = """Summarize the following study notes. 
                            Include: 
                            1. Main concepts (bullet points)
                            2. Key definitions
                            3. Important formulas or dates (if any)
                            4. 2-3 exam tips
                            
                            Notes:
                            $content"""
                    )
                )
            )

            val response = apiService.sendMessage(
                apiKey = Constants.ANTHROPIC_API_KEY,
                request = request
            )
            Result.success(response.content.firstOrNull()?.text ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateQuiz(
        content: String,
        numQuestions: Int,
        language: AppLanguage
    ): Result<List<QuizQuestion>> {
        return try {
            val langInstruction = if (language == AppLanguage.URDU)
                "Write all questions and answers in Urdu (اردو)."
            else "Write in English."

            val request = AIRequest(
                system = """You are an exam question generator for Pakistani students.
                    $langInstruction
                    IMPORTANT: Respond ONLY with valid JSON array. No markdown, no explanation.""",
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = """Generate $numQuestions MCQ questions from this content.
                            Return JSON array with this exact structure:
                            [
                              {
                                "question": "question text",
                                "options": ["A", "B", "C", "D"],
                                "correctAnswerIndex": 0,
                                "explanation": "why this is correct"
                              }
                            ]
                            
                            Content: $content"""
                    )
                )
            )

            val response = apiService.sendMessage(
                apiKey = Constants.ANTHROPIC_API_KEY,
                request = request
            )

            val rawJson = response.content.firstOrNull()?.text
                ?.trim()
                ?.removePrefix("```json")
                ?.removeSuffix("```")
                ?.trim() ?: "[]"

            val parsed = gson.fromJson(rawJson, Array<QuizQuestionDto>::class.java)
            val questions = parsed.map { dto ->
                QuizQuestion(
                    id = UUID.randomUUID().toString(),
                    question = dto.question,
                    options = dto.options,
                    correctAnswerIndex = dto.correctAnswerIndex,
                    explanation = dto.explanation,
                    type = QuestionType.MCQ
                )
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateStudyPlan(
        weakAreas: List<WeakArea>,
        targetExam: String
    ): Result<StudyPlan> {
        return try {
            val weakTopics = weakAreas.joinToString(", ") { it.topic }
            val request = AIRequest(
                system = "You are a study planner for Pakistani students preparing for $targetExam. Respond ONLY in JSON.",
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = """Create a 7-day study plan focusing on these weak areas: $weakTopics.
                            Return JSON:
                            {
                              "title": "plan title",
                              "tasks": [
                                {
                                  "title": "task title",
                                  "description": "what to study",
                                  "dayOffset": 0,
                                  "priority": "HIGH"
                                }
                              ]
                            }"""
                    )
                )
            )

            val response = apiService.sendMessage(
                apiKey = Constants.ANTHROPIC_API_KEY,
                request = request
            )

            val rawJson = response.content.firstOrNull()?.text
                ?.trim()
                ?.removePrefix("```json")
                ?.removeSuffix("```")
                ?.trim() ?: "{}"

            val dto = gson.fromJson(rawJson, StudyPlanDto::class.java)
            val now = java.util.Date()
            val plan = StudyPlan(
                id = UUID.randomUUID().toString(),
                title = dto.title,
                tasks = dto.tasks.map { task ->
                    val due = java.util.Calendar.getInstance().apply {
                        time = now
                        add(java.util.Calendar.DAY_OF_YEAR, task.dayOffset)
                    }.time
                    StudyTask(
                        id = UUID.randomUUID().toString(),
                        title = task.title,
                        description = task.description,
                        dueDate = due,
                        priority = Priority.valueOf(task.priority.uppercase())
                    )
                },
                startDate = now
            )
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
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
            val lang = if (targetLanguage == AppLanguage.URDU) "Urdu (اردو)" else "English"
            val request = AIRequest(
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = "Translate the following study content to $lang. Keep formatting intact:\n\n$content"
                    )
                )
            )
            val response = apiService.sendMessage(
                apiKey = Constants.ANTHROPIC_API_KEY,
                request = request
            )
            Result.success(response.content.firstOrNull()?.text ?: content)
        } catch (e: Exception) {
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