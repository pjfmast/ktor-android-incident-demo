package incident.android.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.IncidentRepository
import incident.android.util.PhotoUtils
import incident.shared.incidents.Category
import incident.shared.incidents.CreateIncidentRequest
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReportIncidentUiState(
    val selectedCategory: Category = Category.COMMUNAL,
    val description: String = "",
    val photos: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val errorMessage: String? = null,
    val showSuccessDialog: Boolean = false,
    val createdIncident: IncidentResponse? = null,
    val showPermissionDeniedWarning: Boolean = false,
    val showImageSourceDialog: Boolean = false,
    val shouldRequestLocationPermission: Boolean = false,
    val shouldUseCurrentLocation: Boolean = false
)

class ReportIncidentViewModel(
    private val repository: IncidentRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private companion object {
        const val UI_STATE_KEY = "report_incident_ui_state"
    }

    private val _uiState = MutableStateFlow(
        savedStateHandle.get<String>(UI_STATE_KEY)?.let { jsonString ->
            runCatching { Json.decodeFromString<ReportIncidentUiState>(jsonString) }.getOrNull()
        } ?: ReportIncidentUiState()
    )
    val uiState = _uiState.asStateFlow()

    private fun updateState(block: (ReportIncidentUiState) -> ReportIncidentUiState) {
        _uiState.update { currentState ->
            val newState = block(currentState)
            runCatching {
                savedStateHandle[UI_STATE_KEY] = Json.encodeToString(newState)
            }
            newState
        }
    }

    fun updateCategory(category: Category) =
        updateState { it.copy(selectedCategory = category) }

    fun updateDescription(description: String) =
        updateState { it.copy(description = description) }

    fun addPhoto(uri: String) =
        updateState { it.copy(photos = it.photos + uri) }

    fun removePhoto(uri: String) =
        updateState { it.copy(photos = it.photos - uri) }

    fun updateLocation(latitude: Double, longitude: Double) =
        updateState {
            it.copy(latitude = latitude, longitude = longitude, errorMessage = null)
        }

    fun clearLocation() =
        updateState { it.copy(latitude = null, longitude = null) }

    fun showLocationError(message: String) =
        updateState { it.copy(errorMessage = message) }

    fun requestUseCurrentLocation() =
        updateState {
            it.copy(shouldRequestLocationPermission = true, shouldUseCurrentLocation = true)
        }

    fun onLocationPermissionHandled() =
        updateState { it.copy(shouldRequestLocationPermission = false) }

    fun onCurrentLocationUsed() =
        updateState { it.copy(shouldUseCurrentLocation = false) }

    fun showImageSourceDialog() =
        updateState { it.copy(showImageSourceDialog = true) }

    fun dismissImageSourceDialog() =
        updateState { it.copy(showImageSourceDialog = false) }

    fun dismissPermissionWarning() =
        updateState { it.copy(showPermissionDeniedWarning = false) }

    fun onPhotoPermissionResult(granted: Boolean) {
        if (granted) {
            showImageSourceDialog()
        } else {
            updateState { it.copy(showPermissionDeniedWarning = true) }
        }
    }

    fun submitReport(context: Context) {
        val state = _uiState.value

        // Local 'val' for latitude and longitude to avoid null checks later
        val latitude = state.latitude
        val longitude = state.longitude

        // Validate description first
        if (state.description.isBlank()) {
            updateState { it.copy(errorMessage = "Please enter a description") }
            return
        }

        // Validate a location is selected:
        if (latitude == null || longitude == null) {
            updateState { it.copy(errorMessage = "Please select a location") }
            return
        }


        viewModelScope.launch {
            withLoading {
                try {
                    val result = repository.createIncident(
                        CreateIncidentRequest(
                            category = state.selectedCategory,
                            description = state.description,
                            latitude = latitude,
                            longitude = longitude,
                            priority = Priority.LOW
                        )
                    )

                    when (result) {
                        is ApiResult.Success -> handleIncidentCreated(context, state.photos, result.data)
                        is ApiResult.HttpError -> setError("Failed to report incident, please try again later")
                        is ApiResult.NetworkError -> setError("Network error occurred while reporting incident")
                        is ApiResult.Timeout -> setError("Request timed out. Please try again.")
                        is ApiResult.Unknown -> setError("Unexpected error occurred while reporting incident.")
                        is ApiResult.Unauthorized -> Unit
                    }
                } catch (e: Exception) {
                    setError("Unexpected error: ${e.message ?: "Please try again"}")
                }
            }
        }
    }

    private suspend fun handleIncidentCreated(context: Context, photos: List<String>, incident: IncidentResponse) {
        photos.forEach { uriString ->
            val file = PhotoUtils.getFileFromUri(context, uriString.toUri())
            file?.let {
                when (repository.uploadImageToIncident(
                    incidentId = incident.id,
                    imageFile = it,
                    description = ""
                )) {
                    is ApiResult.HttpError -> setError("Failed to upload image: ${file.name}")
                    is ApiResult.NetworkError -> setError("Network error uploading image: ${file.name}")
                    is ApiResult.Timeout -> setError("Image upload timed out: ${file.name}")
                    is ApiResult.Unknown -> setError("Unknown error uploading image: ${file.name}")
                    else -> Unit
                }
            }
        }

        updateState {
            it.copy(showSuccessDialog = true, createdIncident = incident, errorMessage = null)
        }
    }

    private fun setError(message: String) =
        updateState { it.copy(errorMessage = message) }

    fun dismissSuccessDialog() =
        updateState { it.copy(showSuccessDialog = false) }

    fun resetForm() =
        updateState { ReportIncidentUiState(selectedCategory = Category.COMMUNAL) }
}