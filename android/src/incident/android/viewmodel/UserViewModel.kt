package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.UserRepository
import incident.shared.users.UpdateUserRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository
) : BaseViewModel() {

    data class UserUiState(
        val updateSuccess: Boolean = false,
        val errorMessage: String? = null,
        val unauthorizedState: Boolean = false
    )

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun updateProfile(
        username: String,
        email: String,
        newPassword: String?
    ) {
        viewModelScope.launch {
            withLoading {
                _uiState.update { it.copy(errorMessage = null, updateSuccess = false, unauthorizedState = false) }

                val updateRequest = UpdateUserRequest(
                    username = username,
                    email = email,
                    password = newPassword,
                    avatar = null
                )

                try {
                    val newState = when (userRepository.updateCurrentUser(updateRequest)) {
                        is ApiResult.Success -> UserUiState(updateSuccess = true, errorMessage = null, unauthorizedState = false)
                        is ApiResult.HttpError -> UserUiState(updateSuccess = false, errorMessage = "Failed to update profile", unauthorizedState = false)
                        is ApiResult.NetworkError -> UserUiState(updateSuccess = false, errorMessage = "Network error, please try again later", unauthorizedState = false)
                        is ApiResult.Timeout -> UserUiState(updateSuccess = false, errorMessage = "Request timed out. Please try again.", unauthorizedState = false)
                        is ApiResult.Unknown -> UserUiState(updateSuccess = false, errorMessage = "Unexpected error occurred.", unauthorizedState = false)
                        is ApiResult.Unauthorized -> UserUiState(updateSuccess = false, errorMessage = null, unauthorizedState = true)
                    }
                    _uiState.update { newState }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun resetUpdateState() {
        _uiState.update { UserUiState() }
    }
}
