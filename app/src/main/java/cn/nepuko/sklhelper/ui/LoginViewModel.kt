package cn.nepuko.sklhelper.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.nepuko.sklhelper.util.HduAuthService
import cn.nepuko.sklhelper.util.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false
)

class LoginViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // 加载已保存的用户名，但不加载密码（出于安全考虑）
        val savedUsername = UserPreferences.getUsername(context)
        if (savedUsername != null) {
            _uiState.update { it.copy(username = savedUsername) }
        }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun login(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = HduAuthService.login(_uiState.value.username, _uiState.value.password)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is cn.nepuko.sklhelper.util.LoginResult.Success -> {
                    UserPreferences.saveCredentials(context, _uiState.value.username, _uiState.value.password)
                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                }
                is cn.nepuko.sklhelper.util.LoginResult.Failure -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
