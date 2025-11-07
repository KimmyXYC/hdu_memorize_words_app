package cn.nepuko.sklhelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.nepuko.sklhelper.util.AiSettings
import cn.nepuko.sklhelper.util.AiSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AiSettingsViewModel(private val repository: AiSettingsRepository) : ViewModel() {

    val aiSettings: StateFlow<AiSettings> = repository.aiSettingsFlow
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AiSettings(
                apiUrl = "",
                apiKey = "",
                modelName = "",
                temperature = "",
                timeout = "",
                retries = ""
            )
        )

    fun saveAiSettings(settings: AiSettings) {
        viewModelScope.launch {
            repository.saveAiSettings(settings)
        }
    }
}

class AiSettingsViewModelFactory(private val repository: AiSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
