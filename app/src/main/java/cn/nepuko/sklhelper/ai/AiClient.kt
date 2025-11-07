package cn.nepuko.sklhelper.ai

import android.util.Log
import cn.nepuko.sklhelper.util.AiSettings
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    val max_tokens: Int = 5
)

@Serializable
data class ChatChoice(
    val message: ChatMessage
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>
)

object AiClient {
    private const val TAG = "AiClient"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Call AI service to choose an answer among A/B/C/D.
     * Returns index 0-3, or -1 on failure.
     */
    suspend fun chooseAnswer(
        question: String,
        options: List<String>,
        settings: AiSettings
    ): Int {
        if (settings.apiKey.isBlank()) {
            return -1
        }

        val baseUrl = settings.apiUrl.trimEnd('/')
        val endpoint = "$baseUrl/chat/completions"

        val timeout = settings.timeout.toLongOrNull() ?: 15000L
        val client = HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = timeout
                connectTimeoutMillis = timeout
                socketTimeoutMillis = timeout
            }
        }

        val userContent = """
            请根据题目选择最合适的选项，只输出A/B/C/D其中一个字母。
            题目：$question
            选项：
            A. ${options.getOrNull(0) ?: ""}
            B. ${options.getOrNull(1) ?: ""}
            C. ${options.getOrNull(2) ?: ""}
            D. ${options.getOrNull(3) ?: ""}
            注意：只输出A、B、C或D，不要输出其他任何内容。
        """.trimIndent()

        val chatRequest = ChatRequest(
            model = settings.modelName,
            messages = listOf(
                ChatMessage("system", "你是英语单词选择题助手。根据题干与四个选项选择正确答案。"),
                ChatMessage("user", userContent)
            ),
            temperature = settings.temperature.toDoubleOrNull() ?: 0.2
        )

        val retries = settings.retries.toIntOrNull() ?: 3
        val totalAttempts = 1 + retries.coerceAtLeast(0)

        for (attempt in 1..totalAttempts) {
            try {
                val response: HttpResponse = client.post(endpoint) {
                    header("Content-Type", "application/json")
                    if (settings.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${settings.apiKey}")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(ChatRequest.serializer(), chatRequest))
                }

                if (response.status != HttpStatusCode.OK) {
                    Log.w(TAG, "[AI 第${attempt}/${totalAttempts}次] HTTP ${response.status.value}")
                    if (attempt < totalAttempts) {
                        delay(500)
                    }
                    continue
                }

                val body = response.bodyAsText()
                val chatResponse = json.decodeFromString<ChatResponse>(body)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
                val text = content.trim().uppercase()

                // Try to extract A/B/C/D
                var idx = -1
                val letterRegex = Regex("[ABCD]")
                val match = letterRegex.find(text)
                if (match != null) {
                    val letter = match.value
                    idx = when (letter) {
                        "A" -> 0
                        "B" -> 1
                        "C" -> 2
                        "D" -> 3
                        else -> -1
                    }
                }

                // Try to extract 1-4
                if (idx == -1) {
                    val numberRegex = Regex("\\b([1-4])\\b")
                    val numberMatch = numberRegex.find(text)
                    if (numberMatch != null) {
                        idx = numberMatch.groupValues[1].toInt() - 1
                    }
                }

                // Try to find option in text
                if (idx == -1) {
                    for (i in options.indices) {
                        if (options[i].isNotBlank() && text.contains(options[i].trim())) {
                            idx = i
                            break
                        }
                    }
                }

                if (idx in 0..3) {
                    Log.i(TAG, "AI判定答案: ${('A' + idx)}（第${attempt}次）")
                    return idx
                } else {
                    Log.w(TAG, "[AI 第${attempt}/${totalAttempts}次] 返回无法解析: $text")
                    if (attempt < totalAttempts) {
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[AI 第${attempt}/${totalAttempts}次] 请求异常：${e.message}")
                if (attempt < totalAttempts) {
                    delay(500)
                }
            }
        }

        return -1
    }
}
