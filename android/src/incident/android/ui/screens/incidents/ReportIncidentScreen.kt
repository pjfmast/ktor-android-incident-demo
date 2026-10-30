package incident.android.ui.screens.incidents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import incident.android.R
import incident.android.ui.components.IncidentMap
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.components.TopNavBar
import incident.android.ui.icons.AddIcon
import incident.android.ui.icons.CheckCircleFilledIcon
import incident.android.ui.icons.CloseIcon
import incident.android.ui.icons.LocationOnFilledIcon
import incident.android.ui.icons.WarningFilledIcon
import incident.android.util.PhotoPermissionHandler
import incident.android.util.PhotoUtils
import incident.android.util.rememberPhotoPermissionLauncher
import incident.android.viewmodel.ReportIncidentUiState
import incident.android.viewmodel.ReportIncidentViewModel
import incident.shared.incidents.Category
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportIncidentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToIncidentList: () -> Unit,
    viewModel: ReportIncidentViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPermissionHandler = remember {
        PhotoPermissionHandler(
            context = context,
            onPermissionsResult = { granted ->
                viewModel.onPhotoPermissionResult(granted)
            }
        )
    }

    val photoPermissionLauncher = rememberPhotoPermissionLauncher { granted ->
        viewModel.onPhotoPermissionResult(granted)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addPhoto(it.toString()) }
    }

    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentCameraUri?.let { viewModel.addPhoto(it.toString()) }
        }
        currentCameraUri = null
    }

    ReportIncidentDialogs(
        uiState = uiState,
        onDismissImageSource = { viewModel.dismissImageSourceDialog() },
        onCameraClick = {
            PhotoUtils.createImageUri(context)?.let { uri ->
                currentCameraUri = uri
                cameraLauncher.launch(uri)
            }
        },
        onGalleryClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onDismissPermissionWarning = { viewModel.dismissPermissionWarning() },
        onContinueAfterSuccess = {
            viewModel.dismissSuccessDialog()
            viewModel.resetForm()
            if (uiState.createdIncident?.reportedBy != null) {
                onNavigateToIncidentList()
            } else {
                onNavigateToLogin()
            }
        }
    )

    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopNavBar(
                    title = stringResource(R.string.report_incident),
                    showBackButton = true,
                    onBackClick = onNavigateBack,
                )
            }
        ) { paddingValues ->
            ReportIncidentContent(
                paddingValues = paddingValues,
                uiState = uiState,
                isLoading = isLoading,
                onCategorySelected = { viewModel.updateCategory(it) },
                onDescriptionChange = { viewModel.updateDescription(it) },
                onAddPhotoClick = {
                    if (photoPermissionHandler.hasPermissions()) {
                        viewModel.showImageSourceDialog()
                    } else {
                        photoPermissionHandler.requestPermissions(photoPermissionLauncher)
                    }
                },
                onRemovePhoto = { viewModel.removePhoto(it.toString()) },
                onUseCurrentLocation = { viewModel.requestUseCurrentLocation() },
                onLocationSelected = { lat, lon -> viewModel.updateLocation(lat, lon) },
                onMapTouch = { isTouching -> /* handled inside content */ },
                onLocationPermissionHandled = { viewModel.onLocationPermissionHandled() },
                onCurrentLocationUsed = { viewModel.onCurrentLocationUsed() },
                onLocationError = { error -> viewModel.showLocationError(error) },
                onSubmit = {
                    viewModel.submitReport(context)
                }
            )
        }
        LoadingOverlay(isLoading = isLoading)
    }
}

@Composable
private fun ReportIncidentContent(
    paddingValues: PaddingValues,
    uiState: ReportIncidentUiState,
    isLoading: Boolean,
    onCategorySelected: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onMapTouch: (Boolean) -> Unit,
    onLocationPermissionHandled: () -> Unit,
    onCurrentLocationUsed: () -> Unit,
    onLocationError: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var parentScrollEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState(), enabled = parentScrollEnabled)
    ) {
        WarningBanner()

        CategorySelectionCard(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = onCategorySelected
        )

        DescriptionInputCard(
            description = uiState.description,
            onDescriptionChange = onDescriptionChange
        )

        PhotoUploadCard(
            photos = uiState.photos.map { it.toUri() },
            onAddPhoto = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto
        )

        MapLocationCard(
            shouldRequestLocationPermission = uiState.shouldRequestLocationPermission,
            shouldUseCurrentLocation = uiState.shouldUseCurrentLocation,
            onUseCurrentLocation = onUseCurrentLocation,
            onLocationSelected = onLocationSelected,
            onMapTouch = { isTouchingMap ->
                parentScrollEnabled = !isTouchingMap
                onMapTouch(isTouchingMap)
            },
            onLocationPermissionHandled = onLocationPermissionHandled,
            onCurrentLocationUsed = onCurrentLocationUsed,
            onLocationError = onLocationError
        )

        ErrorMessage(errorMessage = uiState.errorMessage)

        SubmitButton(
            isLoading = isLoading,
            onClick = onSubmit
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ReportIncidentDialogs(
    uiState: ReportIncidentUiState,
    onDismissImageSource: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismissPermissionWarning: () -> Unit,
    onContinueAfterSuccess: () -> Unit,
) {
    if (uiState.showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = onDismissImageSource,
            onCameraClick = {
                onDismissImageSource()
                onCameraClick()
            },
            onGalleryClick = {
                onDismissImageSource()
                onGalleryClick()
            }
        )
    }

    if (uiState.showPermissionDeniedWarning) {
        PermissionDeniedDialog(
            onDismiss = onDismissPermissionWarning
        )
    }

    if (uiState.showSuccessDialog) {
        ReportSuccessDialog(
            onDismiss = { },
            onContinue = onContinueAfterSuccess
        )
    }
}

// Component Composable

@Composable
fun WarningBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 16.dp, 16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8DC)
        ),
        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp, 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.suspicious_activity_or_emergency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E)
                )
                Text(
                    text = stringResource(R.string.for_immediate_danger_call_emergency_services),
                    fontSize = 13.sp,
                    color = Color(0xFFA16207)
                )
            }
        }
    }
}

@Composable
fun CategorySelectionCard(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.what_type_of_incident_is_this),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(category) },
                        label = {
                            Text(
                                text = category.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Text(
                text = when (selectedCategory) {
                    Category.CRIME -> stringResource(R.string.illegal_activities_and_safety_threats)
                    Category.ENVIRONMENT -> stringResource(R.string.nature_pollution_and_conservation_issues)
                    Category.COMMUNAL -> stringResource(R.string.shared_spaces_and_neighborhood_quality_of_life)
                    Category.TRAFFIC -> stringResource(R.string.roads_vehicles_and_transportation_safety)
                    Category.OTHER -> stringResource(R.string.any_issue_that_doesn_t_fit_the_other_categories)
                },
                fontSize = 13.sp,
                color = Color(0xFF656D76)
            )
        }
    }
}

@Composable
fun DescriptionInputCard(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.provide_a_short_but_detailed_description),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text(stringResource(R.string.what_exactly_did_you_observe)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF6F8FA),
                    focusedContainerColor = Color(0xFFF6F8FA),
                    unfocusedBorderColor = Color(0xFFD0D7DE),
                    focusedBorderColor = Color(0xFF0969DA)
                ),
                minLines = 4
            )
        }
    }
}

@Composable
fun PhotoUploadCard(
    photos: List<Uri>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Uri) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.can_you_please_add_some_photos),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.visual_evidence_helps_us_respond_more_effectively),
                    fontSize = 13.sp,
                    color = Color(0xFF656D76)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(
                            width = 2.dp,
                            color = Color(0xFFD0D7DE),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Color(0xFFF6F8FA), RoundedCornerShape(12.dp))
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF0969DA), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AddIcon,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.add_photo),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF656D76)
                        )
                    }
                }

                photos.forEach { photoUri ->
                    Box(modifier = Modifier.size(120.dp)) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Photo",
                            modifier = Modifier
                                .size(120.dp)
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .offset((-8).dp, (-8).dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .background(Color(0xFFDC2626), RoundedCornerShape(12.dp))
                                .clickable { onRemovePhoto(photoUri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CloseIcon,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLocationCard(
    shouldRequestLocationPermission: Boolean,
    shouldUseCurrentLocation: Boolean,
    onUseCurrentLocation: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onMapTouch: (Boolean) -> Unit,
    onLocationPermissionHandled: () -> Unit,
    onCurrentLocationUsed: () -> Unit,
    onLocationError: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp, 16.dp, 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFD0D7DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.where_did_you_observe_this),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.tap_on_the_map_to_mark_the_exact_location),
                    fontSize = 13.sp,
                    color = Color(0xFF656D76)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                IncidentMap(
                    modifier = Modifier.fillMaxSize(),
                    incidents = emptyList(),
                    isLocationSelectionEnabled = true,
                    allowDetailNavigation = false,
                    onIncidentClick = { },
                    onLocationSelected = onLocationSelected,
                    onMapTouch = onMapTouch,
                    shouldRequestLocationPermission = shouldRequestLocationPermission,
                    shouldUseCurrentLocation = shouldUseCurrentLocation,
                    onLocationPermissionHandled = onLocationPermissionHandled,
                    onCurrentLocationUsed = onCurrentLocationUsed,
                    onLocationError = onLocationError
                )
            }

            Button(
                onClick = onUseCurrentLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDDF4FF),
                    contentColor = Color(0xFF0969DA)
                ),
                border = BorderStroke(1.dp, Color(0xFF54AEFF)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = LocationOnFilledIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.use_current_location),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(errorMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Text(
                text = errorMessage,
                color = Color(0xFFDC2626),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubmitButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626),
            contentColor = Color.White
        ),
        enabled = !isLoading
    ) {
        Text(
            stringResource(R.string.submit_report),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_photo),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0969DA),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        stringResource(R.string.take_photo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0969DA),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        stringResource(R.string.choose_from_gallery),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = WarningFilledIcon,
                    contentDescription = "Warning",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFF59E0B)
                )

                Text(
                    text = stringResource(R.string.permission_required),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.to_add_photos_please_grant_camera_and_storage_permissions_in_your_device_settings),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0969DA),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        stringResource(R.string.ok),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ReportSuccessDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = CheckCircleFilledIcon,
                    contentDescription = stringResource(R.string.success),
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF10B981)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.thank_you),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.your_incident_report_has_been_successfully_submitted_our_team_will_review_it_shortly),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.continue_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}