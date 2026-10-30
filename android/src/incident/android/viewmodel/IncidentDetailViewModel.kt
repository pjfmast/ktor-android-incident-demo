package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.IncidentRepository
import incident.android.data.repository.UserRepository
import incident.android.data.store.TokenPreferences
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import incident.shared.incidents.UpdateIncidentRequest
import incident.shared.users.Role
import incident.shared.users.UserResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentDetailUiState(
    val userRole: Role? = null,
    val currentIncident: IncidentResponse? = null,
    val reportedUser: UserResponse? = null,
    val userFetchTimeout: Boolean = false,
    val unauthorizedState: Boolean = false,
    val toastMessage: String? = null
)

class IncidentDetailViewModel(
    private val incidentRepository: IncidentRepository,
    private val tokenPreferences: TokenPreferences,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    private var userFetchJob: Job? = null

    init {
        loadUserRole()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                userRole = tokenPreferences.getUserRole()
            )
        }
    }

    fun getIncidentById(incidentId: Long) {
        viewModelScope.launch {
            withLoading {
                when (val result = incidentRepository.getIncidentById(incidentId)) {
                    is ApiResult.Success -> {
                        val incident = result.data
                        _uiState.value = _uiState.value.copy(currentIncident = incident)
                        val reporterId = incident.reportedBy
                        if (!incident.isAnonymous && reporterId != null) {
                            fetchReportedUser(reporterId)
                        }
                    }
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to load incident: ${result.message}"
                    )
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while loading incident."
                    )
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred while loading incident."
                    )
                }
            }
        }
    }

    fun clearCurrentIncident() {
        _uiState.value = _uiState.value.copy(currentIncident = null)
        clearReportedUser()
    }

    fun fetchReportedUser(userId: Long) {
        userFetchJob?.cancel()
        _uiState.value = _uiState.value.copy(userFetchTimeout = false)

        userFetchJob = viewModelScope.launch {
            withLoading {
                when (val result = userRepository.getUserById(userId)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(reportedUser = result.data)
                    }
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(userFetchTimeout = true)
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(userFetchTimeout = true)
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(userFetchTimeout = true)
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(userFetchTimeout = true)
                }
            }
        }
    }

    fun clearReportedUser() {
        userFetchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            reportedUser = null,
            userFetchTimeout = false
        )
    }

    fun updatePriority(incidentId: Long, priority: Priority) {
        viewModelScope.launch {
            withLoading {
                when (val result = incidentRepository.changeIncidentPriority(incidentId, priority)) {
                    is ApiResult.Success -> {
                        getIncidentById(incidentId)
                        _uiState.value = _uiState.value.copy(toastMessage = "Priority updated successfully")
                    }
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to update priority: ${result.message}"
                    )
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while updating priority."
                    )
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred while updating priority."
                    )
                }
            }
        }
    }

    fun updateStatus(incidentId: Long, status: Status) {
        viewModelScope.launch {
            withLoading {
                when (val result = incidentRepository.changeIncidentStatus(incidentId, status)) {
                    is ApiResult.Success -> {
                        getIncidentById(incidentId)
                        _uiState.value = _uiState.value.copy(toastMessage = "Status updated successfully")
                    }
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to update status: ${result.message}"
                    )
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while updating status."
                    )
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred while updating status."
                    )
                }
            }
        }
    }

    fun deleteIncident(incidentId: Long) {
        viewModelScope.launch {
            withLoading {
                when (val result = incidentRepository.deleteIncident(incidentId)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Incident deleted successfully"
                    )
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to delete incident: ${result.message}"
                    )
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while deleting incident."
                    )
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred while deleting incident."
                    )
                }
            }
        }
    }

    fun updateLocation(incidentId: Long, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            withLoading {
                val updateRequest = UpdateIncidentRequest(
                    latitude = latitude,
                    longitude = longitude
                )
                when (val result = incidentRepository.updateIncident(incidentId, updateRequest)) {
                    is ApiResult.Success -> {
                        getIncidentById(incidentId)
                        _uiState.value = _uiState.value.copy(toastMessage = "Location updated successfully")
                    }
                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)
                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to update location: ${result.message}"
                    )
                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while updating location."
                    )
                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred while updating location."
                    )
                }
            }
        }
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        userFetchJob?.cancel()
    }
}
