package cn.nepuko.sklhelper.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.nepuko.sklhelper.data.QuestionBankRepository
import cn.nepuko.sklhelper.util.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionBankSettingsUiState(
    val questionCount: Int = 0,
    val message: String? = null,
    val autoUpdateEnabled: Boolean = true,
    val isUpdating: Boolean = false
)

class QuestionBankSettingsViewModel(
    private val context: Context,
    private val repository: QuestionBankRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankSettingsUiState())
    val uiState: StateFlow<QuestionBankSettingsUiState> = _uiState.asStateFlow()

    init {
        loadQuestionCount()
        loadAutoUpdatePreference()
    }

    private fun loadAutoUpdatePreference() {
        val enabled = UserPreferences.getAutoUpdateQuestionBank(context)
        _uiState.update { it.copy(autoUpdateEnabled = enabled) }
    }

    private fun loadQuestionCount() {
        viewModelScope.launch {
            val questions = repository.getQuestions()
            _uiState.update { it.copy(questionCount = questions.size) }
        }
    }

    fun importQuestionBank(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                    val jsonString = reader?.readText()
                    if (jsonString != null) {
                        if (repository.saveQuestions(jsonString)) {
                            loadQuestionCount()
                            _uiState.update { it.copy(message = "导入成功") }
                        } else {
                            repository.resetToDefault()
                            loadQuestionCount()
                            _uiState.update { it.copy(message = "题库格式错误，已重置为默认题库") }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                repository.resetToDefault()
                loadQuestionCount()
                _uiState.update { it.copy(message = "导入失败，已重置为默认题库") }
            }
        }
    }

    fun exportQuestionBank(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                    val jsonString = repository.getQuestionsJsonString()
                    writer?.write(jsonString)
                }
                _uiState.update { it.copy(message = "导出成功") }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(message = "导出失败") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun updateQuestionBankOnline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val result = repository.updateQuestionBankFromOnline()
                if (result.isSuccess) {
                    loadQuestionCount()
                    _uiState.update { it.copy(message = result.getOrNull() ?: "更新成功", isUpdating = false) }
                } else {
                    _uiState.update { it.copy(message = "更新失败：${result.exceptionOrNull()?.message}", isUpdating = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "更新失败：${e.message}", isUpdating = false) }
            }
        }
    }

    fun toggleAutoUpdate(enabled: Boolean) {
        UserPreferences.saveAutoUpdateQuestionBank(context, enabled)
        _uiState.update { it.copy(autoUpdateEnabled = enabled) }
    }

    class QuestionBankSettingsViewModelFactory(private val context: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuestionBankSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuestionBankSettingsViewModel(
                    context.applicationContext,
                    QuestionBankRepository(context.applicationContext)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
