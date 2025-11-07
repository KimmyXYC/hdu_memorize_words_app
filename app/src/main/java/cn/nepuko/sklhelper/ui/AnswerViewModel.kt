package cn.nepuko.sklhelper.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.nepuko.sklhelper.api.AnswerSubmission
import cn.nepuko.sklhelper.api.HduApiClient
import cn.nepuko.sklhelper.processor.QuestionProcessor
import cn.nepuko.sklhelper.util.AiSettings
import cn.nepuko.sklhelper.util.HduAuthService
import cn.nepuko.sklhelper.util.NotificationHelper
import cn.nepuko.sklhelper.util.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class AnswerState {
    object Idle : AnswerState()
    data class Running(val log: String) : AnswerState()
    data class Success(val log: String) : AnswerState()
    data class Error(val message: String) : AnswerState()
}

class AnswerViewModel(
    private val context: Context
) : ViewModel() {

    private val _answerState = MutableStateFlow<AnswerState>(AnswerState.Idle)
    val answerState: StateFlow<AnswerState> = _answerState.asStateFlow()

    private val logBuilder = StringBuilder()
    private val notificationHelper = NotificationHelper(context)

    private fun appendLog(message: String) {
        logBuilder.append(message).append("\n")
        _answerState.value = AnswerState.Running(logBuilder.toString())
    }

    fun startAnswering(
        username: String,
        password: String,
        targetScore: Int,
        targetTime: Int,
        examType: String,
        aiAssistEnabled: Boolean,
        aiSettings: AiSettings?
    ) {
        viewModelScope.launch {
            try {
                logBuilder.clear()
                _answerState.value = AnswerState.Running("")

                // Show ongoing notification
                notificationHelper.showOngoingNotification()

                val startTime = System.currentTimeMillis()

                // 1. Login and get token
                appendLog("正在登录...")
                val loginResult = HduAuthService.loginAndGetToken(username, password)
                val xAuthToken = when (loginResult) {
                    is cn.nepuko.sklhelper.util.LoginResult.Success -> loginResult.token
                    is cn.nepuko.sklhelper.util.LoginResult.Failure -> {
                        _answerState.value = AnswerState.Error("登录失败: ${loginResult.message}")
                        return@launch
                    }
                }
                appendLog("登录成功")

                val apiClient = HduApiClient(xAuthToken)

                // 2. Get current week
                appendLog("获取当前周次...")
                val week = apiClient.fetchCurrentWeek()
                if (week == null) {
                    _answerState.value = AnswerState.Error("无法获取当前周次")
                    return@launch
                }
                appendLog("当前周次: $week")

                // 3. Get new paper
                appendLog("获取试卷...")
                val paperData = apiClient.getNewPaper(week, examType)
                if (paperData == null) {
                    _answerState.value = AnswerState.Error("无法获取试卷（可能短时间内重试过多）")
                    return@launch
                }

                val paperId = paperData.paperId
                val questions = paperData.list
                appendLog("获取到试卷 $paperId，共 ${questions.size} 题")

                // 4. Answer questions
                appendLog("开始处理题目...")
                val finalAnswers = mutableListOf<AnswerSubmission>()

                // Calculate number of questions to answer incorrectly
                val wrongCount = (100 - targetScore).coerceIn(0, questions.size)
                val wrongIndices = if (wrongCount > 0) {
                    questions.indices.shuffled().take(wrongCount).toSet()
                } else {
                    emptySet()
                }

                if (wrongCount > 0) {
                    appendLog("期望分数: $targetScore 分，将随机做错 $wrongCount 题")
                }

                // Initialize question processor
                val processor = if (aiAssistEnabled && aiSettings != null) {
                    QuestionProcessor(context, aiSettings)
                } else {
                    QuestionProcessor(context, null)
                }
                processor.reloadQuestionBank()

                for ((idx, qData) in questions.withIndex()) {
                    val paperDetailId = qData.paperDetailId
                    val title = qData.title.trim().trimEnd('.')
                    val options = listOf(
                        qData.answerA.trim().trimEnd('.'),
                        qData.answerB.trim().trimEnd('.'),
                        qData.answerC.trim().trimEnd('.'),
                        qData.answerD.trim().trimEnd('.')
                    )

                    appendLog("题目 ${idx + 1}: $title")

                    // Get answer index
                    val correctAnswerIdx = processor.getAnswerIndex(title, options)
                    val correctAnswerChar = if (correctAnswerIdx != -1) {
                        ('A' + correctAnswerIdx)
                    } else {
                        appendLog("  未找到答案，默认选择 A")
                        'A'
                    }

                    if (correctAnswerIdx != -1) {
                        appendLog("  找到答案: $correctAnswerChar")
                    }

                    // Determine final answer (intentionally wrong if needed)
                    val finalAnswerChar = if (idx in wrongIndices) {
                        val wrongOptions = listOf('A', 'B', 'C', 'D').filter { it != correctAnswerChar }
                        val wrongChoice = wrongOptions.random()
                        appendLog("  故意做错，选择 $wrongChoice 而不是 $correctAnswerChar")
                        wrongChoice
                    } else {
                        correctAnswerChar
                    }

                    finalAnswers.add(
                        AnswerSubmission(
                            paperDetailId = paperDetailId,
                            input = finalAnswerChar.toString()
                        )
                    )
                }

                // 5. Wait for the specified answer time
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
                val remainingTime = targetTime - elapsedTime
                if (remainingTime > 0) {
                    appendLog("等待 ${remainingTime}秒 以满足答题时间要求...")
                    delay(remainingTime * 1000)
                }

                // 6. Submit paper
                appendLog("提交答案...")
                val submitSuccess = apiClient.submitPaper(paperId, finalAnswers)

                // Cancel ongoing notification
                notificationHelper.cancelOngoingNotification()

                if (submitSuccess) {
                    appendLog("答题完成！")
                    // Show completion notification
                    notificationHelper.showCompletionNotification(success = true)
                    _answerState.value = AnswerState.Success(logBuilder.toString())
                } else {
                    notificationHelper.showCompletionNotification(success = false)
                    _answerState.value = AnswerState.Error("提交答案失败")
                }

            } catch (e: Exception) {
                // Cancel ongoing notification on error
                notificationHelper.cancelOngoingNotification()
                // Show error notification
                notificationHelper.showCompletionNotification(success = false)
                _answerState.value = AnswerState.Error("发生错误: ${e.message}")
            }
        }
    }

    private fun extractToken(): String? {
        // This method is no longer needed as we use HduAuthService.loginAndGetToken
        return null
    }

    fun reset() {
        _answerState.value = AnswerState.Idle
        logBuilder.clear()
    }
}

class AnswerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnswerViewModel::class.java)) {
            return AnswerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
