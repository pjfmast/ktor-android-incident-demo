package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.IncidentRepository
import incident.android.data.store.IncidentDataStore
import incident.shared.incidents.Category
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.UpdateIncidentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyIncidentDetailViewModel(
    private val incidentRepository: IncidentRepository,
    private val incidentDataStore: IncidentDataStore
) : BaseViewModel() {

    data class MyIncidentDetailUiState(
        val updateResult: ApiResult<IncidentResponse>? = null,
        val deleteResult: ApiResult<Unit>? = null,
        val toastMessage: String? = null
    )

    private val _uiState = MutableStateFlow(MyIncidentDetailUiState())
    val uiState: StateFlow<MyIncidentDetailUiState> = _uiState.asStateFlow()

    val selectedIncident = incidentDataStore.selectedIncident

    fun updateIncident(
        incidentId: Long,
        category: Category? = null,
        description: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            withLoading {
                try {
                    val updateRequest = UpdateIncidentRequest(
                        category = category,
                        description = description,
                        latitude = latitude,
                        longitude = longitude
                    )

                    val result = incidentRepository.updateIncident(incidentId, updateRequest)
                    _uiState.update { it.copy(updateResult = result) }

                    when (result) {
                        is ApiResult.Success -> incidentDataStore.saveSelectedIncident(result.data)
                        is ApiResult.Timeout -> _uiState.update { it.copy(toastMessage = "Update request timed out.") }
                        is ApiResult.Unknown -> _uiState.update { it.copy(toastMessage = "Unexpected error while updating.") }
                        is ApiResult.HttpError -> _uiState.update { it.copy(toastMessage = "Failed to update incident") }
                        is ApiResult.NetworkError -> _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        is ApiResult.Unauthorized -> _uiState.update { it.copy(toastMessage = "Unauthorized action.") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(updateResult = ApiResult.NetworkError(e), toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun deleteIncident(incidentId: Long) {
        viewModelScope.launch {
            withLoading {
                try {
                    val result = incidentRepository.deleteIncident(incidentId)
                    _uiState.update { it.copy(deleteResult = result) }

                    when (result) {
                        is ApiResult.Success -> _uiState.update { it.copy(toastMessage = "Incident deleted successfully.") }
                        is ApiResult.Timeout -> _uiState.update { it.copy(toastMessage = "Delete request timed out.") }
                        is ApiResult.Unknown -> _uiState.update { it.copy(toastMessage = "Unexpected error while deleting.") }
                        is ApiResult.HttpError -> _uiState.update { it.copy(toastMessage = "Failed to delete incident") }
                        is ApiResult.NetworkError -> _uiState.update { it.copy(toastMessage = "Network error: ${result.exception.message ?: "Please try again"}") }
                        is ApiResult.Unauthorized -> _uiState.update { it.copy(toastMessage = "Unauthorized action.") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(deleteResult = ApiResult.NetworkError(e), toastMessage = "Unexpected error: ${e.message ?: "Please try again"}") }
                }
            }
        }
    }

    fun resetUpdateResult() {
        _uiState.update { it.copy(updateResult = null) }
    }

    fun resetDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }

    fun clearSelectedIncident() {
        viewModelScope.launch {
            incidentDataStore.clearSelectedIncident()
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
