package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

data class RegisterUiState(
    val state: RegisterState = RegisterState.Idle
)

class RegisterViewModel(
    private val userRepository: UserRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(username: String, password: String, email: String, confirmPassword: String) {
        if (username.isBlank() || password.isBlank() || email.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(state = RegisterState.Error("Please fill in all fields")) }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(state = RegisterState.Error("Passwords do not match")) }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(state = RegisterState.Error("Password must be at least 6 characters")) }
            return
        }

        if (!isValidEmail(email)) {
            _uiState.update { it.copy(state = RegisterState.Error("Please enter a valid email address")) }
            return
        }

        viewModelScope.launch {
            withLoading {
                try {
                    val result = userRepository.register(username, password, email, null)
                    val newState = when (result) {
                        is ApiResult.Success -> RegisterState.Success
                        is ApiResult.HttpError -> RegisterState.Error("Registration failed, please try again later.)")
                        is ApiResult.NetworkError -> RegisterState.Error("Network error, please check your internet connection and try again")
                        is ApiResult.Timeout -> RegisterState.Error("Registration request timed out. Please try again.")
                        is ApiResult.Unauthorized -> RegisterState.Error("You are not authorized to perform this action.")
                        is ApiResult.Unknown -> RegisterState.Error("Unexpected error occurred during registration.")
                    }
                    _uiState.update { it.copy(state = newState) }
                } catch (e: Exception) {
                    val message = when (e) {
                        is ConnectException, is UnknownHostException ->
                            "Network error: Unable to connect to server."
                        else -> "Unexpected error: ${e.message ?: "Please try again later"}"
                    }
                    _uiState.update { it.copy(state = RegisterState.Error(message)) }
                }
            }
        }
    }

    fun clearRegisterState() {
        _uiState.update { it.copy(state = RegisterState.Idle) }
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}
