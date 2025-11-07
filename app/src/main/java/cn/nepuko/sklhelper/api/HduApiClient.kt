package cn.nepuko.sklhelper.api

import android.util.Log
import cn.nepuko.sklhelper.util.SklTicketGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class WeekResponse(
    val week: Int = 0
)

@Serializable
data class QuestionData(
    val paperDetailId: String,
    val title: String,
    val answerA: String = "",
    val answerB: String = "",
    val answerC: String = "",
    val answerD: String = ""
)

@Serializable
data class PaperResponse(
    val paperId: String,
    val list: List<QuestionData> = emptyList()
)

@Serializable
data class AnswerSubmission(
    val paperDetailId: String,
    val input: String
)

@Serializable
data class SubmitPayload(
    val paperId: String,
    val type: String,
    val list: List<AnswerSubmission>
)

class HduApiClient(private val xAuthToken: String, private val timeout: Long = 30000) {

    companion object {
        private const val TAG = "HduApiClient"
        private const val BASE_URL = "https://skl.hdu.edu.cn/api"
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun getCommonHeaders(sklTicket: String): Map<String, String> {
        return mapOf(
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
            "Connection" to "keep-alive",
            "Referer" to "https://skl.hdu.edu.cn/",
            "User-Agent" to "Mozilla/5.0 (Linux; U; Android 12; zh-CN; M2102J2SC Build/SKQ1.211006.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 UWS/3.22.1.210 Mobile Safari/537.36 AliApp(DingTalk/6.5.20) com.alibaba.android.rimet/24646881 Channel/700159 language/zh-CN abi/64 UT4Aplus/0.2.25 colorScheme/light",
            "X-Auth-Token" to xAuthToken,
            "skl-ticket" to sklTicket
        )
    }

    suspend fun fetchCurrentWeek(): Int? {
        return try {
            val sklTicket = SklTicketGenerator.generate()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val url = "$BASE_URL/course?startTime=$today"

            val response: HttpResponse = client.get(url) {
                getCommonHeaders(sklTicket).forEach { (key, value) ->
                    header(key, value)
                }
                header("Cache-Control", "no-cache")
                header("Pragma", "no-cache")
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val weekResponse = json.decodeFromString<WeekResponse>(body)
                val week = weekResponse.week
                if (week > 0) {
                    Log.i(TAG, "Current week: $week")
                    return week
                }
            }

            Log.e(TAG, "Failed to fetch current week: ${response.status}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch current week", e)
            null
        }
    }

    suspend fun getNewPaper(week: Int, examType: String = "0"): PaperResponse? {
        return try {
            val sklTicket = SklTicketGenerator.generate()
            val startTime = System.currentTimeMillis()
            val url = "$BASE_URL/paper/new?type=$examType&week=$week&startTime=$startTime"

            val response: HttpResponse = client.get(url) {
                getCommonHeaders(sklTicket).forEach { (key, value) ->
                    header(key, value)
                }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    val paperResponse = json.decodeFromString<PaperResponse>(body)
                    Log.i(TAG, "Got new paper: ${paperResponse.paperId} with ${paperResponse.list.size} questions")
                    paperResponse
                }
                HttpStatusCode.BadRequest -> {
                    val body = response.bodyAsText()
                    Log.w(TAG, "Rate limited or error: $body")
                    null
                }
                else -> {
                    Log.e(TAG, "Failed to get new paper: ${response.status}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get new paper", e)
            null
        }
    }

    suspend fun submitPaper(paperId: String, answers: List<AnswerSubmission>): Boolean {
        return try {
            val sklTicket = SklTicketGenerator.generate()
            val url = "$BASE_URL/paper/save"

            val payload = SubmitPayload(
                paperId = paperId,
                type = "0",
                list = answers
            )

            val response: HttpResponse = client.post(url) {
                getCommonHeaders(sklTicket).forEach { (key, value) ->
                    header(key, value)
                }
                header("Content-Type", "application/json")
                header("Origin", "https://skl.hdu.edu.cn")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SubmitPayload.serializer(), payload))
            }

            if (response.status == HttpStatusCode.OK) {
                Log.i(TAG, "Paper submitted successfully")
                true
            } else {
                Log.e(TAG, "Failed to submit paper: ${response.status}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit paper", e)
            false
        }
    }
}

