package incident.android.util

import incident.shared.incidents.Category
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status

object IncidentFilterHelper {

    fun filterIncidents(
        incidents: List<IncidentResponse>,
        searchQuery: String = "",
        priorityFilter: Set<Priority> = emptySet(),
        statusFilter: Set<Status> = emptySet(),
        categoryFilter: Set<Category> = emptySet()
    ): List<IncidentResponse> {

        return incidents.filter { incident ->
            val matchesSearch = searchQuery.isBlank() ||
                    incident.category.name.contains(searchQuery, ignoreCase = true) ||
                    incident.description.contains(searchQuery, ignoreCase = true) ||
                    incident.priority.name.contains(searchQuery, ignoreCase = true) ||
                    incident.status.name.contains(searchQuery, ignoreCase = true)

            val matchesPriority = priorityFilter.isEmpty() ||
                    priorityFilter.contains(incident.priority)

            val matchesStatus = statusFilter.isEmpty() ||
                    statusFilter.contains(incident.status)

            val matchesCategory = categoryFilter.isEmpty() ||
                    categoryFilter.contains(incident.category)

            matchesSearch && matchesPriority && matchesStatus && matchesCategory
        }
    }

    fun hasActiveFilters(
        searchQuery: String = "",
        priorityFilter: Set<Priority> = emptySet(),
        statusFilter: Set<Status> = emptySet(),
        categoryFilter: Set<Category> = emptySet()
    ): Boolean {
        return searchQuery.isNotEmpty() ||
                priorityFilter.isNotEmpty() ||
                statusFilter.isNotEmpty() ||
                categoryFilter.isNotEmpty()
    }
}
