package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.AuthRepository
import incident.android.data.repository.IncidentRepository
import incident.android.data.repository.UserRepository
import incident.android.data.store.IncidentDataStore
import incident.android.data.store.TokenPreferences
import incident.shared.incidents.IncidentResponse
import incident.shared.users.Role
import incident.shared.users.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyIncidentListUiState(
    val user: UserResponse? = null,
    val incidents: List<IncidentResponse> = emptyList(),
    val logoutEvent: Boolean = false,
    val userRole: Role? = null,
    val unauthorizedState: Boolean = false,
    val toastMessage: String? = null
)

class MyIncidentListViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val incidentRepository: IncidentRepository,
    private val tokenPreferences: TokenPreferences,
    private val incidentDataStore: IncidentDataStore
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(MyIncidentListUiState())
    val uiState: StateFlow<MyIncidentListUiState> = _uiState.asStateFlow()

    init {
        loadUserRole()
        loadUserData()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            val role = tokenPreferences.getUserRole()
            _uiState.update { it.copy(userRole = role) }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            withLoading {
                try {
                    when (val userResult = userRepository.getCurrentUser()) {
                        is ApiResult.Success -> {
                            _uiState.update { it.copy(user = userResult.data, toastMessage = null) }
                            loadUserIncidents()
                        }

                        is ApiResult.HttpError -> handleUserError(userResult)
                        is ApiResult.NetworkError -> handleUserError(userResult)
                        is ApiResult.Timeout -> {
                            _uiState.update { it.copy(user = null, toastMessage = "Fetching user data timed out.") }
                        }
                        is ApiResult.Unknown -> {
                            _uiState.update { it.copy(user = null, toastMessage = "Unexpected error fetching user data.") }
                        }
                        is ApiResult.Unauthorized -> logout()
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                    logout()
                }
            }
        }
    }

    private fun loadUserIncidents() {
        viewModelScope.launch {
            withLoading {
                try {
                    when (val incidentsResult = incidentRepository.getMyIncidents()) {
                        is ApiResult.Success -> {
                            _uiState.update { it.copy(incidents = incidentsResult.data, toastMessage = null) }
                        }

                        is ApiResult.HttpError -> handleIncidentError(incidentsResult)
                        is ApiResult.NetworkError -> handleIncidentError(incidentsResult)
                        is ApiResult.Timeout -> {
                            _uiState.update { it.copy(toastMessage = "Fetching incidents timed out.") }
                        }
                        is ApiResult.Unknown -> {
                            _uiState.update { it.copy(toastMessage = "Unexpected error fetching incidents.") }
                        }
                        is ApiResult.Unauthorized -> logout()
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    private fun handleUserError(result: ApiResult<*>): Boolean {
        if (result is ApiResult.HttpError && result.code == 401) {
            _uiState.update { it.copy(unauthorizedState = true) }
        }
        logout()
        return false
    }

    private fun handleIncidentError(result: ApiResult<*>): Boolean {
        if (result is ApiResult.HttpError && result.code == 401) {
            _uiState.update { it.copy(unauthorizedState = true) }
        }
        // Leave incidents list unchanged
        return false
    }

    fun logout() {
        viewModelScope.launch {
            withLoading {
                try {
                    authRepository.logout()
                } finally {
                    _uiState.update { it.copy(user = null, incidents = emptyList(), logoutEvent = true) }
                }
            }
        }
    }

    fun resetLogoutEvent() {
        _uiState.update { it.copy(logoutEvent = false) }
    }

    fun saveSelectedIncident(incident: IncidentResponse) {
        viewModelScope.launch {
            incidentDataStore.saveSelectedIncident(incident)
        }
    }

    fun refreshIncidents() {
        loadUserIncidents()
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
