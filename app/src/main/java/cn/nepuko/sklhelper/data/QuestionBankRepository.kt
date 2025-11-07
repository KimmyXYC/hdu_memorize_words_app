package cn.nepuko.sklhelper.data

import android.content.Context
import android.util.Log
import cn.nepuko.sklhelper.model.Question
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

class QuestionBankRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val internalFile = File(context.filesDir, "questions.json")

    companion object {
        private const val TAG = "QuestionBankRepository"
        private const val QUESTION_BANK_URL = "https://gh-proxy.com/raw.githubusercontent.com/KimmyXYC/hdu_memorize_words/refs/heads/main/questions.json"
    }

    fun getQuestions(): List<Question> {
        return try {
            val jsonString = if (internalFile.exists()) {
                internalFile.readText()
            } else {
                context.assets.open("questions.json").bufferedReader().use { it.readText() }
            }
            val questionMap = json.decodeFromString<Map<String, String>>(jsonString)
            questionMap.map { (question, answer) -> Question(question, answer) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getQuestionsJsonString(): String {
        return if (internalFile.exists()) {
            internalFile.readText()
        } else {
            context.assets.open("questions.json").bufferedReader().use { it.readText() }
        }
    }

    fun saveQuestions(jsonString: String): Boolean {
        return try {
            // Validate json
            json.decodeFromString<Map<String, String>>(jsonString)
            internalFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetToDefault() {
        if (internalFile.exists()) {
            internalFile.delete()
        }
    }

    suspend fun updateQuestionBankFromOnline(): Result<String> {
        return try {
            Log.d(TAG, "开始从在线更新题库")
            val jsonString = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                URL(QUESTION_BANK_URL).readText()
            }

            // Validate json format
            json.decodeFromString<Map<String, String>>(jsonString)

            // Save to internal storage
            internalFile.writeText(jsonString)
            Log.d(TAG, "题库更新成功")
            Result.success("题库更新成功")
        } catch (e: Exception) {
            Log.e(TAG, "题库更新失败", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
