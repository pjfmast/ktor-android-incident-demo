package incident.android.ui.screens.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import incident.android.R
import incident.android.navigation.IncidentListKey
import incident.android.navigation.IncidentMapKey
import incident.android.navigation.MyIncidentListKey
import incident.android.navigation.UserManagementKey
import incident.android.ui.components.BottomNavBar
import incident.android.ui.components.FilterDialog
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.components.SearchAndFilterBar
import incident.android.ui.icons.DeleteIcon
import incident.android.util.IncidentDisplayHelper
import incident.android.viewmodel.IncidentManagementViewModel
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllIncidentListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToIncidentMap: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToMyIncidentList: () -> Unit,
    viewModel: IncidentManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unauthorizedState = uiState.unauthorizedState
    val userRole = uiState.userRole
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val filteredIncidents = uiState.filteredIncidents
    val showLoadMore = uiState.showLoadMore

    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(unauthorizedState) {
        if (unauthorizedState) {
            onNavigateToMyIncidentList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshIncidents()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                currentKey = IncidentListKey,
                userRole = userRole,
                onNavigateTo = { route ->
                    when (route) {
                        IncidentMapKey -> onNavigateToIncidentMap()
                        UserManagementKey -> onNavigateToUserManagement()
                        MyIncidentListKey -> onNavigateToMyIncidentList()
                        else -> {}
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SearchAndFilterBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    hasActiveFilters = viewModel.hasActiveFilters,
                    onFilterClick = { showFilterMenu = true }
                )

                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refreshIncidents() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filteredIncidents.isEmpty() && !isLoading) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredIncidents, key = { it.id }) { incident ->
                                IncidentCard(
                                    incident = incident,
                                    onClick = {
                                        onNavigateToDetail(incident.id)
                                    },
                                    onDelete = { incidentId ->
                                        viewModel.deleteIncident(incidentId)
                                    }
                                )
                            }

                            if (showLoadMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMoreIncidents() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(stringResource(R.string.load_more))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LoadingOverlay(isLoading = isLoading)
        }
    }

    if (showFilterMenu) {
        FilterDialog(
            selectedPriority = uiState.selectedPriorityFilter,
            selectedStatus = uiState.selectedStatusFilter,
            selectedCategory = uiState.selectedCategoryFilter,
            onUpdatePriority = { viewModel.updatePriorityFilter(it) },
            onUpdateStatus = { viewModel.updateStatusFilter(it) },
            onUpdateCategory = { viewModel.updateCategoryFilter(it) },
            onClearAll = { viewModel.clearAllFilters() },
            onDismiss = { showFilterMenu = false }
        )
    }
}

@Composable
fun IncidentCard(
    incident: IncidentResponse,
    onClick: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete(incident.id)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF6B6B), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = "Delete incident",
                    tint = Color.White
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = IncidentDisplayHelper.formatCategoryText(incident.category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        StatusBadge(status = incident.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = incident.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PriorityChip(priority = incident.priority)

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.due) + ": " + IncidentDisplayHelper.formatDateForDisplay(incident.dueAt),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StatusBadge(status: Status) {
    val statusColor = IncidentDisplayHelper.getStatusColor(status)

    Surface(
        color = statusColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (backgroundColor, textColor) = when (priority) {
        Priority.CRITICAL -> Color(0xFFD32F2F) to Color.White
        Priority.HIGH -> Color(0xFFF57C00) to Color.White
        Priority.NORMAL -> Color(0xFFFDD835) to Color.Black
        Priority.LOW -> Color(0xFF66BB6A) to Color.White
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.no_incidents_found),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.try_adjusting_your_search_or_filters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}