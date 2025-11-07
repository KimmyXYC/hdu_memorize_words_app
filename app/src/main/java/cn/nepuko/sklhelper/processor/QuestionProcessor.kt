package cn.nepuko.sklhelper.processor

import android.content.Context
import android.util.Log
import cn.nepuko.sklhelper.ai.AiClient
import cn.nepuko.sklhelper.data.QuestionBankRepository
import cn.nepuko.sklhelper.util.AiSettings
import kotlinx.serialization.json.Json

/**
 * Handles the logic of finding answers to questions, coordinating between
 * the local question bank and the AI client.
 */
class QuestionProcessor(
    private val context: Context,
    private val aiSettings: AiSettings? = null
) {
    companion object {
        private const val TAG = "QuestionProcessor"
    }

    private val repository = QuestionBankRepository(context)
    private val json = Json { ignoreUnknownKeys = true }

    private var questionBank: Map<String, String> = loadQuestionBank()

    private fun loadQuestionBank(): Map<String, String> {
        return try {
            val jsonString = repository.getQuestionsJsonString()
            val rawMap = json.decodeFromString<Map<String, String>>(jsonString)
            // Trim all question keys to remove leading/trailing spaces
            rawMap.mapKeys { (key, _) -> key.trim() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load question bank", e)
            emptyMap()
        }
    }

    fun reloadQuestionBank() {
        questionBank = loadQuestionBank()
        Log.i(TAG, "Question bank reloaded with ${questionBank.size} entries")
    }

    private fun normalizeText(text: String): String {
        return text.replace(Regex("\\s+"), "").trim()
    }

    /**
     * Finds the answer for a given question and its options.
     * Returns the index (0-3) of the correct answer, or -1 if not found.
     */
    suspend fun getAnswerIndex(question: String, options: List<String>): Int {
        // Trim trailing and leading spaces from the question
        val trimmedQuestion = question.trim()

        // 1. Search in the local question bank
        val expectedAnswers = questionBank[trimmedQuestion]
        if (expectedAnswers != null) {
            val orderedMeanings = mutableListOf<String>()

            // Split by | or ｜
            val parts = expectedAnswers.split(Regex("\\s*[|｜]\\s*"))
            for (part in parts) {
                if (part.isNotBlank()) {
                    orderedMeanings.add(part.trim())
                }
            }

            // Remove duplicates while preserving order
            val seen = mutableSetOf<String>()
            val uniqueOrderedMeanings = mutableListOf<String>()
            for (meaning in orderedMeanings) {
                val normMeaning = normalizeText(meaning)
                if (normMeaning.isNotEmpty() && normMeaning !in seen) {
                    seen.add(normMeaning)
                    uniqueOrderedMeanings.add(normMeaning)
                }
            }

            // Match with options
            for (normMeaning in uniqueOrderedMeanings) {
                for (i in options.indices) {
                    if (normalizeText(options[i]) == normMeaning) {
                        return i
                    }
                }
            }
        }

        // 2. If not found in bank and AI is enabled, try AI
        if (aiSettings != null && aiSettings.apiKey.isNotBlank()) {
            val aiIdx = AiClient.chooseAnswer(trimmedQuestion, options, aiSettings)
            if (aiIdx != -1) {
                // Note: AI answer persistence is not implemented per requirements
                return aiIdx
            }
        }

        // 3. If still not found, return -1 (default to A in caller)
        Log.w(TAG, "Question not found in bank and AI failed: $trimmedQuestion")
        return -1
    }
}
