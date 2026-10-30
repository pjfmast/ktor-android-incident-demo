package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.UserRepository
import incident.android.data.store.TokenPreferences
import incident.shared.users.Role
import incident.shared.users.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserManagementViewModel(
    private val repository: UserRepository,
    private val tokenPreferences: TokenPreferences
) : BaseViewModel() {

    data class UserManagementUiState(
        val userRole: Role? = null,
        val users: List<UserResponse> = emptyList(),
        val unauthorizedState: Boolean = false,
        val showLoadMore: Boolean = false,
        val toastMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 10

    init {
        loadUserRole()
        loadUsers()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            _uiState.update { it.copy(userRole = tokenPreferences.getUserRole()) }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            withLoading {
                try {
                    when (val result = repository.getAllUsers()) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    users = result.data,
                                    showLoadMore = result.data.size >= pageSize,
                                    toastMessage = null
                                )
                            }
                        }
                        is ApiResult.HttpError -> {
                            _uiState.update { it.copy(toastMessage = "Failed to load users: ${result.message}") }
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        }
                        is ApiResult.Timeout -> {
                            _uiState.update { it.copy(toastMessage = "Request timed out. Please try again.") }
                        }
                        is ApiResult.Unknown -> {
                            _uiState.update { it.copy(toastMessage = "Unexpected error occurred.") }
                        }
                        is ApiResult.Unauthorized -> {
                            _uiState.update { it.copy(unauthorizedState = true) }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun loadMoreUsers() {
        viewModelScope.launch {
            withLoading {
                currentPage++
                try {
                    when (val result = repository.getAllUsers()) {
                        is ApiResult.Success -> {
                            val combined = uiState.value.users + result.data
                            _uiState.update { it.copy(users = combined, showLoadMore = result.data.size >= pageSize) }
                        }
                        is ApiResult.HttpError -> {
                            currentPage--
                            _uiState.update { it.copy(toastMessage = "Failed to load more users: ${result.message}") }
                        }
                        is ApiResult.NetworkError -> {
                            currentPage--
                            _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        }
                        is ApiResult.Timeout -> {
                            currentPage--
                            _uiState.update { it.copy(toastMessage = "Request timed out. Please try again.") }
                        }
                        is ApiResult.Unknown -> {
                            currentPage--
                            _uiState.update { it.copy(toastMessage = "Unexpected error occurred.") }
                        }
                        is ApiResult.Unauthorized -> {
                            currentPage--
                            _uiState.update { it.copy(unauthorizedState = true) }
                        }
                    }
                } catch (e: Exception) {
                    currentPage--
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun changeUserRole(userId: Long, newRole: Role) {
        viewModelScope.launch {
            withLoading {
                try {
                    when (val result = repository.updateUserRole(userId, newRole)) {
                        is ApiResult.Success -> {
                            val updated = uiState.value.users.map { user ->
                                if (user.id == userId.toString()) user.copy(role = newRole) else user
                            }
                            _uiState.update { it.copy(users = updated, toastMessage = "Role updated successfully") }
                        }
                        is ApiResult.HttpError -> {
                            _uiState.update { it.copy(toastMessage = "Failed to update role: ${result.message}") }
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        }
                        is ApiResult.Timeout -> {
                            _uiState.update { it.copy(toastMessage = "Request timed out. Please try again.") }
                        }
                        is ApiResult.Unknown -> {
                            _uiState.update { it.copy(toastMessage = "Unexpected error occurred.") }
                        }
                        is ApiResult.Unauthorized -> {
                            _uiState.update { it.copy(unauthorizedState = true) }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            withLoading {
                try {
                    when (val result = repository.deleteUser(userId)) {
                        is ApiResult.Success -> {
                            val updated = uiState.value.users.filter { it.id != userId.toString() }
                            _uiState.update { it.copy(users = updated, toastMessage = "User deleted successfully") }
                        }
                        is ApiResult.HttpError -> {
                            _uiState.update { it.copy(toastMessage = "Failed to delete user: ${result.message}") }
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        }
                        is ApiResult.Timeout -> {
                            _uiState.update { it.copy(toastMessage = "Request timed out. Please try again.") }
                        }
                        is ApiResult.Unknown -> {
                            _uiState.update { it.copy(toastMessage = "Unexpected error occurred.") }
                        }
                        is ApiResult.Unauthorized -> {
                            _uiState.update { it.copy(unauthorizedState = true) }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
