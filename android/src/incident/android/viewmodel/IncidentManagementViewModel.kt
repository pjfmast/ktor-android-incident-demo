package incident.android.viewmodel

import androidx.lifecycle.viewModelScope
import incident.android.data.model.ApiResult
import incident.android.data.repository.IncidentRepository
import incident.android.data.store.TokenPreferences
import incident.android.util.IncidentFilterHelper
import incident.shared.incidents.Category
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import incident.shared.users.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentManagementUiState(
    val userRole: Role? = null,
    val allIncidents: List<IncidentResponse> = emptyList(),
    val displayedIncidents: List<IncidentResponse> = emptyList(),
    val unauthorizedState: Boolean = false,
    val showLoadMore: Boolean = false,
    val toastMessage: String? = null,
    val searchQuery: String = "",
    val selectedPriorityFilter: Set<Priority> = emptySet(),
    val selectedStatusFilter: Set<Status> = emptySet(),
    val selectedCategoryFilter: Set<Category> = emptySet(),
    val filteredIncidents: List<IncidentResponse> = emptyList()
)

class IncidentManagementViewModel(
    private val incidentRepository: IncidentRepository,
    private val tokenPreferences: TokenPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(IncidentManagementUiState())
    val uiState: StateFlow<IncidentManagementUiState> = _uiState.asStateFlow()

    private var currentDisplayCount = 0
    private val pageSize = 20

    init {
        loadUserRole()
        loadAllIncidents()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                userRole = tokenPreferences.getUserRole()
            )
        }
    }

    fun loadAllIncidents(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.allIncidents.isNotEmpty())
            return

        viewModelScope.launch {
            withLoading {
                when (val result = incidentRepository.getAllIncidents()) {
                    is ApiResult.Success -> {
                        val incidents = result.data
                        currentDisplayCount = minOf(pageSize, incidents.size)
                        _uiState.value = _uiState.value.copy(
                            allIncidents = incidents,
                            displayedIncidents = incidents.take(currentDisplayCount),
                            showLoadMore = incidents.size > currentDisplayCount,
                            toastMessage = null
                        )
                        applyFilters()
                    }

                    is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)

                    is ApiResult.HttpError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to load incidents: ${result.message}"
                    )

                    is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )

                    is ApiResult.Timeout -> _uiState.value = _uiState.value.copy(
                        toastMessage = "Request timed out while loading incidents."
                    )

                    is ApiResult.Unknown -> _uiState.value = _uiState.value.copy(
                        toastMessage = "An unexpected error occurred while loading incidents."
                    )
                }
            }
        }
    }

    fun deleteIncident(incidentId: Long) {
        viewModelScope.launch {
            when (val result = incidentRepository.deleteIncident(incidentId)) {
                is ApiResult.Success -> {
                    val updatedList = _uiState.value.allIncidents.filterNot { it.id == incidentId }

                    // Update display after deletion
                    currentDisplayCount = minOf(currentDisplayCount, updatedList.size)
                    _uiState.value = _uiState.value.copy(
                        allIncidents = updatedList,
                        displayedIncidents = updatedList.take(currentDisplayCount),
                        toastMessage = "Incident deleted successfully"
                    )

                    applyFilters()
                }

                is ApiResult.Unauthorized -> _uiState.value = _uiState.value.copy(unauthorizedState = true)

                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Failed to delete incident: ${result.message}"
                    )
                }

                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Network error: ${result.exception.message}"
                    )
                }

                is ApiResult.Timeout -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Delete request timed out"
                    )
                }

                is ApiResult.Unknown -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Unexpected error occurred"
                    )
                }
            }
        }
    }

    fun loadMoreIncidents() {
        val allIncidents = _uiState.value.allIncidents
        val newDisplayCount = minOf(currentDisplayCount + pageSize, allIncidents.size)

        if (newDisplayCount > currentDisplayCount) {
            _uiState.value = _uiState.value.copy(
                displayedIncidents = allIncidents.take(newDisplayCount),
                showLoadMore = allIncidents.size > newDisplayCount
            )
            currentDisplayCount = newDisplayCount
            applyFilters()
        } else {
            _uiState.value = _uiState.value.copy(showLoadMore = false)
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = IncidentFilterHelper.filterIncidents(
            incidents = state.displayedIncidents,
            searchQuery = state.searchQuery,
            priorityFilter = state.selectedPriorityFilter,
            statusFilter = state.selectedStatusFilter,
            categoryFilter = state.selectedCategoryFilter
        )
        _uiState.value = state.copy(filteredIncidents = filtered)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun updatePriorityFilter(priorities: Set<Priority>) {
        _uiState.value = _uiState.value.copy(selectedPriorityFilter = priorities)
        applyFilters()
    }

    fun updateStatusFilter(statuses: Set<Status>) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = statuses)
        applyFilters()
    }

    fun updateCategoryFilter(categories: Set<Category>) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = categories)
        applyFilters()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedPriorityFilter = emptySet(),
            selectedStatusFilter = emptySet(),
            selectedCategoryFilter = emptySet()
        )
        applyFilters()
    }

    val hasActiveFilters: Boolean
        get() = IncidentFilterHelper.hasActiveFilters(
            searchQuery = _uiState.value.searchQuery,
            priorityFilter = _uiState.value.selectedPriorityFilter,
            statusFilter = _uiState.value.selectedStatusFilter,
            categoryFilter = _uiState.value.selectedCategoryFilter
        )

    fun refreshIncidents() {
        currentDisplayCount = 0
        loadAllIncidents()
    }
}
