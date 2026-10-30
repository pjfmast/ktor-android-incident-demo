package incident.android.ui.screens.incidents

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import incident.android.R
import incident.android.data.model.ApiResult
import incident.shared.incidents.Category
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Status
import incident.android.ui.components.IncidentMap
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.components.TopNavBar
import incident.android.ui.icons.ArrowDropDownIcon
import incident.android.ui.icons.CheckCircleFilledIcon
import incident.android.ui.icons.CheckCircleIcon
import incident.android.ui.icons.DateRangeIcon
import incident.android.ui.icons.DeleteIcon
import incident.android.ui.icons.DescriptionIcon
import incident.android.ui.icons.ImageIcon
import incident.android.ui.icons.LocationOnIcon
import incident.android.util.ImageUrlHelper
import incident.android.util.IncidentDisplayHelper.formatCategoryText
import incident.android.util.IncidentDisplayHelper.formatDateForDisplay
import incident.android.util.IncidentDisplayHelper.getStatusColor
import incident.android.viewmodel.MyIncidentDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyIncidentDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyIncidentDetailViewModel = koinViewModel()
) {
    var incident by remember { mutableStateOf<IncidentResponse?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var editableDescription by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var showResolvedDialog by remember { mutableStateOf(false) }
    var showCannotDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val selectedIncidentFlow = viewModel.selectedIncident
    val isBusy by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect incident from the flow
    LaunchedEffect(Unit) {
        selectedIncidentFlow.collect { selectedIncident ->
            incident = selectedIncident
            selectedIncident?.let {
                selectedCategory = it.category
                editableDescription = it.description
                // Initialize selected location with current incident location
                selectedLocation = it.latitude to it.longitude
            }
        }
    }

    val successUpdateMessage = stringResource(R.string.incident_updated_successfully)
    val failureUpdateMessage = stringResource(R.string.failed_to_update_incident)

    LaunchedEffect(uiState.updateResult) {
        uiState.updateResult?.let { result ->
            if (result is ApiResult.Success) {
                Toast.makeText(context, successUpdateMessage, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    "$failureUpdateMessage ${
                        when (result) {
                            is ApiResult.HttpError -> result.message
                            is ApiResult.NetworkError -> result.exception.message
                            is ApiResult.Unauthorized -> {
                                onNavigateBack()
                                ""
                            }

                            else -> "Unknown error"
                        }
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
            viewModel.resetUpdateResult()
        }
    }

    val successDeleteMessage = stringResource(R.string.incident_deleted_successfully)
    val failureDeleteMessage = stringResource(R.string.failed_to_delete_incident)

    LaunchedEffect(uiState.deleteResult) {
        uiState.deleteResult?.let { result ->
            if (result is ApiResult.Success) {
                Toast.makeText(context, successDeleteMessage, Toast.LENGTH_LONG).show()
                onNavigateBack()
            } else {
                Toast.makeText(
                    context,
                    "$failureDeleteMessage ${
                        when (result) {
                            is ApiResult.HttpError -> result.message
                            is ApiResult.NetworkError -> result.exception.message
                            is ApiResult.Unauthorized -> {
                                onNavigateBack()
                                ""
                            }

                            else -> "Unknown error"
                        }
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
            viewModel.resetDeleteResult()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedIncident()
        }
    }

    // Resolved Incident Dialog
    if (showResolvedDialog) {
        AlertDialog(
            onDismissRequest = { showResolvedDialog = false },
            icon = {
                Icon(
                    imageVector = CheckCircleFilledIcon,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.incident_already_resolved),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.this_incident_has_been_marked_as_resolved_and_can_no_longer_be_modified),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showResolvedDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Cannot Delete Dialog
    if (showCannotDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.cannot_delete_incident),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.incidents_that_are_assigned_or_resolved_cannot_be_deleted),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCannotDeleteDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
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
                        incident?.let { inc ->
                            viewModel.deleteIncident(inc.id)
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
            // local 'val' so smart cast works
            val currentIncident = incident
            if (currentIncident == null) {
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
                IncidentDetailContent(
                    incident = currentIncident,
                    selectedCategory = selectedCategory,
                    editableDescription = editableDescription,
                    selectedLocation = selectedLocation, // Pass the location
                    onCategoryChange = { selectedCategory = it },
                    onDescriptionChange = { editableDescription = it },
                    onLocationSelected = { lat, lon ->
                        selectedLocation = lat to lon
                    }, // Handle location selection
                    onSave = {
                        if (currentIncident.status == Status.RESOLVED) {
                            showResolvedDialog = true
                        } else {
                            viewModel.updateIncident(
                                incidentId = currentIncident.id,
                                category = selectedCategory,
                                description = editableDescription.takeIf { it.isNotBlank() },
                                latitude = selectedLocation?.first, // Pass latitude
                                longitude = selectedLocation?.second // Pass longitude
                            )
                        }
                    },
                    onDelete = {
                        val status = currentIncident.status
                        if (status == Status.ASSIGNED || status == Status.RESOLVED) {
                            showCannotDeleteDialog = true
                        } else {
                            showDeleteConfirmDialog = true
                        }
                    }
                )
            }

            LoadingOverlay(isLoading = isBusy)
        }
    }
}

@Composable
private fun IncidentDetailContent(
    incident: IncidentResponse,
    selectedCategory: Category?,
    editableDescription: String,
    selectedLocation: Pair<Double, Double>?, // Add location parameter
    onCategoryChange: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationSelected: (Double, Double) -> Unit, // Add location callback
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var parentScrollEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = parentScrollEnabled)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IncidentHeaderCard(
            incident = incident,
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange
        )
        IncidentDescriptionCard(
            description = editableDescription,
            onDescriptionChange = onDescriptionChange
        )
        IncidentImagesCard(incident)
        IncidentLocationCard(
            incident = incident,
            onParentScrollEnabledChange = { parentScrollEnabled = it },
            onLocationSelected = onLocationSelected
        )

        ActionButtons(
            onSave = onSave,
            onDelete = onDelete
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ActionButtons(
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D47A1)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = stringResource(R.string.save_changes),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD32F2F)
            ),
            border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = DeleteIcon,
                contentDescription = stringResource(R.string.delete_incident),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun IncidentHeaderCard(
    incident: IncidentResponse,
    selectedCategory: Category?,
    onCategoryChange: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            // CATEGORY DROPDOWN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.category),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCategory?.let { formatCategoryText(it) }
                                    ?: stringResource(R.string.select_category),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Icon(
                                imageVector = ArrowDropDownIcon,
                                contentDescription = "Expand",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Category.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(formatCategoryText(category)) },
                                onClick = {
                                    onCategoryChange(category)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // STATUS
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = getStatusColor(incident.status).copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(incident.status),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = DateRangeIcon,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.created),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = formatDateForDisplay(incident.createdAt),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CheckCircleIcon,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (incident.completedAt != null) stringResource(R.string.completed) else stringResource(
                                R.string.pending
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = incident.completedAt?.let { formatDateForDisplay(it) }
                            ?: stringResource(
                                R.string.not_completed
                            ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                }
            }
        }
    }
}

@Composable
private fun IncidentDescriptionCard(
    description: String,
    onDescriptionChange: (String) -> Unit
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
                    imageVector = DescriptionIcon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.description),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.8.sp
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .padding(16.dp),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF374151),
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF0D47A1))
                )
            }
        }
    }
}

@Composable
private fun IncidentImagesCard(incident: IncidentResponse) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageIcon,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.photos),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.8.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = if (incident.images.isNotEmpty()) "${incident.images.size}" else "0",
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
                    items(incident.images.count()) { index ->
                        val imageUrl = ImageUrlHelper.getFullImageUrl(incident.images[index])
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(140.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            if (!imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = stringResource(R.string.incident_image) + " ${index + 1}",
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
    onParentScrollEnabledChange: (Boolean) -> Unit,
    onLocationSelected: (Double, Double) -> Unit // Add this parameter
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Text(
                text = stringResource(R.string.tap_on_the_map_to_select_a_new_location),
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 4.dp)
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
                    isLocationSelectionEnabled = true,
                    allowDetailNavigation = false,
                    onMapTouch = { isTouchingMap -> onParentScrollEnabledChange(!isTouchingMap) },
                    onLocationSelected = onLocationSelected
                )
            }
        }
    }
}