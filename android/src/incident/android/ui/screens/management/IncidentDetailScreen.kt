package incident.android.ui.screens.management

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import incident.android.R
import incident.android.ui.components.IncidentMap
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.components.TopNavBar
import incident.android.ui.icons.ArrowDropDownIcon
import incident.android.ui.icons.CheckCircleIcon
import incident.android.ui.icons.CloseIcon
import incident.android.ui.icons.DateRangeIcon
import incident.android.ui.icons.DeleteIcon
import incident.android.ui.icons.LocationOnIcon
import incident.android.ui.icons.PersonIcon
import incident.android.util.ImageUrlHelper
import incident.android.util.IncidentDisplayHelper.formatCategoryText
import incident.android.util.IncidentDisplayHelper.formatDateForDisplay
import incident.android.util.IncidentDisplayHelper.getStatusColor
import incident.android.viewmodel.IncidentDetailViewModel
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import incident.shared.users.UserResponse
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun IncidentDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMyIncidentList: () -> Unit,
    incidentId: Long?,
    viewModel: IncidentDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isLoading.collectAsStateWithLifecycle()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val context = LocalContext.current

    // Load incident when screen opens or incidentId changes
    LaunchedEffect(incidentId) {
        if (incidentId != null) {
            viewModel.getIncidentById(incidentId)
        }
    }

    LaunchedEffect(uiState.unauthorizedState) {
        if (uiState.unauthorizedState) {
            onNavigateToMyIncidentList()
        }
    }

    LaunchedEffect(uiState.currentIncident) {
        uiState.currentIncident?.let {
            selectedLocation = it.latitude to it.longitude
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    // Clear data when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentIncident()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_incident),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.are_you_sure_you_want_to_delete_this_incident_this_action_cannot_be_undone),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        uiState.currentIncident?.let { inc ->
                            viewModel.deleteIncident(inc.id)
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    selectedImageUrl?.let { imageUrl ->
        FullscreenImageDialog(
            imageUrl = imageUrl,
            onDismiss = { selectedImageUrl = null }
        )
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = stringResource(R.string.incident_details),
                showBackButton = true,
                onBackClick = { onNavigateBack() },
                backgroundColor = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (isBusy && uiState.currentIncident == null) {
                LoadingOverlay(isLoading = true)
            } else if (uiState.currentIncident == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.incident_details_not_available),
                        color = Color(0xFF6B7280),
                        fontSize = 16.sp
                    )
                }
            } else {
                uiState.currentIncident?.let { incident ->
                    IncidentManagementContent(
                        incident = incident,
                        reportedUser = uiState.reportedUser,
                        selectedLocation = selectedLocation,
                        userFetchTimeout = uiState.userFetchTimeout,
                        onPriorityChange = { priority ->
                            viewModel.updatePriority(incident.id, priority)
                        },
                        onStatusChange = { status ->
                            viewModel.updateStatus(incident.id, status)
                        },
                        onDelete = {
                            showDeleteConfirmDialog = true
                        },
                        onImageClick = { imageUrl ->
                            selectedImageUrl = imageUrl
                        },
                        onLocationSelected = { lat, lon -> selectedLocation = lat to lon },
                        onSaveLocation = {
                            selectedLocation?.let { (lat, lon) ->
                                viewModel.updateLocation(incident.id, lat, lon)
                            }
                        }
                    )
                }
            }

            LoadingOverlay(isLoading = isBusy)
        }
    }
}

@Composable
private fun FullscreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Fullscreen image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun IncidentManagementContent(
    incident: IncidentResponse,
    reportedUser: UserResponse?,
    userFetchTimeout: Boolean,
    selectedLocation: Pair<Double, Double>?,
    onPriorityChange: (Priority) -> Unit,
    onStatusChange: (Status) -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onSaveLocation: () -> Unit
) {
    var parentScrollEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = parentScrollEnabled)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IncidentManagementHeaderCard(
            incident = incident,
            onPriorityChange = onPriorityChange,
            onStatusChange = onStatusChange
        )

        ReporterInfoCard(
            incident = incident,
            reportedUser = reportedUser,
            userFetchTimeout = userFetchTimeout
        )

        IncidentDescriptionCard(incident.description)

        IncidentImagesCard(
            incident = incident,
            onImageClick = onImageClick
        )

        IncidentLocationCard(
            incident = incident,
            parentScrollEnabled = parentScrollEnabled,
            onParentScrollEnabledChange = { parentScrollEnabled = it },
            onLocationSelected = onLocationSelected,
            onSaveLocation = onSaveLocation
        )

        DeleteButton(onDelete = onDelete)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    OutlinedButton(
        onClick = onDelete,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFD32F2F)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFD32F2F))
    ) {
        Icon(
            imageVector = DeleteIcon,
            contentDescription = stringResource(R.string.delete_incident),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.delete_incident_button),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun IncidentManagementHeaderCard(
    incident: IncidentResponse,
    onPriorityChange: (Priority) -> Unit,
    onStatusChange: (Status) -> Unit
) {
    var priorityExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // CATEGORY
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.category),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Text(
                        text = formatCategoryText(incident.category),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // PRIORITY DROPDOWN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.priority),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { priorityExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = getPriorityColor(incident.priority).copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, getPriorityColor(incident.priority))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = incident.priority.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = getPriorityColor(incident.priority)
                            )
                            Icon(
                                imageVector = ArrowDropDownIcon,
                                contentDescription = "Expand",
                                tint = getPriorityColor(incident.priority)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Priority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = priority.name,
                                        color = getPriorityColor(priority)
                                    )
                                },
                                onClick = {
                                    onPriorityChange(priority)
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // STATUS DROPDOWN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.status),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = getStatusColor(incident.status).copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, getStatusColor(incident.status))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            getStatusColor(incident.status),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = incident.status.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getStatusColor(incident.status)
                                )
                            }
                            Icon(
                                imageVector = ArrowDropDownIcon,
                                contentDescription = "Expand",
                                tint = getStatusColor(incident.status)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Status.entries.forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = status.name,
                                        color = getStatusColor(status)
                                    )
                                },
                                onClick = {
                                    onStatusChange(status)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))

            // DATE/TIME INFORMATION
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DateInfoRow(
                    label = stringResource(R.string.created),
                    date = formatDateForDisplay(incident.createdAt)
                )

                DateInfoRow(
                    label = stringResource(R.string.updated),
                    date = formatDateForDisplay(incident.updatedAt)
                )

                DateInfoRow(
                    label = stringResource(R.string.due),
                    date = formatDateForDisplay(incident.dueAt)
                )

                if (incident.completedAt != null) {
                    DateInfoRow(
                        label = stringResource(R.string.completed),
                        date = formatDateForDisplay(incident.completedAt),
                        icon = CheckCircleIcon
                    )
                }
            }
        }
    }
}

@Composable
private fun DateInfoRow(
    label: String,
    date: String,
    icon: ImageVector = DateRangeIcon
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = date,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun ReporterInfoCard(
    incident: IncidentResponse,
    reportedUser: UserResponse?,
    userFetchTimeout: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PersonIcon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.reported_by),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )
            }

            when {
                // Anonymous or no reporter ID
                incident.isAnonymous || incident.reportedBy == null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PersonIcon,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.anonymous_report),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
                // Timeout occurred
                userFetchTimeout -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = DeleteIcon,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.failed_to_retrieve_user_information),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = stringResource(R.string.request_timed_out_please_try_again),
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
                // User data loaded
                reportedUser != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.username),
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = reportedUser.username,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E7EB))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.email),
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = reportedUser.email,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                // Loading state
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentDescriptionCard(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.description),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                letterSpacing = 0.8.sp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Text(
                    text = description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF374151),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun IncidentImagesCard(
    incident: IncidentResponse,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.photos),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = incident.images.size.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (incident.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(incident.images.size) { index ->
                        val imageUrl = ImageUrlHelper.getFullImageUrl(incident.images[index])

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(140.dp)
                                .clickable {
                                    imageUrl?.let { onImageClick(it) }
                                },
                            color = Color(0xFFF3F4F6)
                        ) {
                            if (!imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = stringResource(R.string.incident_image) + "${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_image),
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Color(0xFFF9FAFB),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_photos_available),
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun IncidentLocationCard(
    incident: IncidentResponse,
    parentScrollEnabled: Boolean,
    onParentScrollEnabledChange: (Boolean) -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onSaveLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LocationOnIcon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.location),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )
            }

            // Always show the instruction text
            Text(
                text = stringResource(R.string.tap_on_the_map_to_select_a_new_location),
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9FAFB),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                IncidentMap(
                    modifier = Modifier.fillMaxSize(),
                    incidents = listOf(incident),
                    isLocationSelectionEnabled = true, // Always enabled
                    allowDetailNavigation = false,
                    onMapTouch = { isTouchingMap ->
                        onParentScrollEnabledChange(!isTouchingMap)
                    },
                    onLocationSelected = onLocationSelected
                )
            }

            // Always show the save button
            Button(
                onClick = onSaveLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1)
                )
            ) {
                Text(
                    text = "Save Location Changes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.LOW -> Color(0xFF10B981)
        Priority.NORMAL -> Color(0xFFF59E0B)
        Priority.HIGH -> Color(0xFFFF6B35)
        Priority.CRITICAL -> (Color(0xFFDC2626))
    }
}