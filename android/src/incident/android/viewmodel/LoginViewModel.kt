package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loginState: LoginState = LoginState.Idle,
    val autoLoginState: AutoLoginState = AutoLoginState.Checking
)

class LoginViewModel(
    private val repository: AuthRepository
) : BaseViewModel() {

    // Consolidated uiState
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(loginState = LoginState.Error("Please enter both username and password")) }
            return
        }

        _uiState.update { it.copy(loginState = LoginState.Loading) }

        viewModelScope.launch {
            try {
                val result = withLoading {
                    repository.login(username, password)
                }

                val newState = when (result) {
                    is ApiResult.Success -> LoginState.Success
                    is ApiResult.HttpError -> LoginState.Error("Login failed")
                    is ApiResult.NetworkError -> LoginState.Error("Network error, Please check your internet connection or try again later")
                    is ApiResult.Timeout -> LoginState.Error("Request timed out. Please try again.")
                    is ApiResult.Unknown -> LoginState.Error("Unexpected error occurred. Please try again later.")
                    is ApiResult.Unauthorized -> LoginState.Error("Invalid username or password.")
                }
                _uiState.update { it.copy(loginState = newState) }
            } catch (e: Exception) {
                // purely a safety net for unexpected cases
                _uiState.update { it.copy(loginState = LoginState.Error("Unexpected error: ${e.message ?: "Something went wrong"}")) }
            }
        }
    }



    fun checkAutoLogin() {
        viewModelScope.launch {
            try {
                val token = withLoading { repository.getSavedToken() }
                val state = if (!token.isNullOrEmpty()) AutoLoginState.TokenFound else AutoLoginState.NoToken
                _uiState.update { it.copy(autoLoginState = state) }
            } catch (_: Exception) {
                _uiState.update { it.copy(autoLoginState = AutoLoginState.Error("Error checking saved login")) }
            }
        }
    }

    fun clearLoginState() {
        _uiState.update { it.copy(loginState = LoginState.Idle) }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class AutoLoginState {
    data object Checking : AutoLoginState()
    data object TokenFound : AutoLoginState()
    data object NoToken : AutoLoginState()
    data class Error(val message: String) : AutoLoginState()
}
