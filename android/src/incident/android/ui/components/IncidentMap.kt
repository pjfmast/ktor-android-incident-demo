package incident.android.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import incident.android.R
import incident.shared.incidents.Category
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import incident.android.ui.icons.CloseIcon
import incident.android.ui.screens.management.PriorityChip
import incident.android.util.IncidentDisplayHelper.formatDateForDisplay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.rememberDefaultHeadingProvider
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun IncidentMap(
    modifier: Modifier = Modifier,
    incidents: List<IncidentResponse>,
    isLocationSelectionEnabled: Boolean = false,
    allowDetailNavigation: Boolean = false,
    onIncidentClick: (IncidentResponse) -> Unit = {},
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    onMapTouch: (Boolean) -> Unit = {},
    shouldRequestLocationPermission: Boolean = false,
    shouldUseCurrentLocation: Boolean = false,
    onLocationPermissionHandled: () -> Unit = {},
    onCurrentLocationUsed: () -> Unit = {},
    onLocationError: (String) -> Unit = {}
) {
    val locationProvider = rememberDefaultLocationProvider()
    val headingProvider = rememberDefaultHeadingProvider()
    val locationState = rememberLocationState(
        provider = locationProvider,
        headingProvider = headingProvider
    )

    val hasLocationPermission = locationState.permission is LocationPermission.Granted
    val userLocation = locationState.lastLocation?.position?.let { it.latitude to it.longitude }
    var selectedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) } // Keep this here

    // Handle location permission request trigger
    LaunchedEffect(shouldRequestLocationPermission) {
        if (shouldRequestLocationPermission) {
            if (!hasLocationPermission) {
                locationState.requestPermission()
            }
            onLocationPermissionHandled()
        }
    }

    // Handle location permission status change
    LaunchedEffect(locationState.permission) {
        if (locationState.permission is LocationPermission.NotGranted) {
            onLocationError("Location permission denied. Please enable location services.")
        }
    }

    // Handle "use current location" trigger
    LaunchedEffect(shouldUseCurrentLocation, userLocation) {
        if (shouldUseCurrentLocation) {
            if (!hasLocationPermission) {
                locationState.requestPermission()
            }
            userLocation?.let { (lat, long) ->
                selectedLocation = userLocation
                onLocationSelected(lat, long)
                onCurrentLocationUsed()
            }
        }
    }

    var selectedIncident by remember { mutableStateOf<IncidentResponse?>(null) }

    // Incidents' source - recompute when incidents change
    val incidentsGeoJson = remember(
        incidents,
        incidents.map { it.id to it.latitude to it.longitude }
    ) {
        createIncidentsGeoJson(incidents).takeIf { it.features.isNotEmpty() }
            ?: FeatureCollection(features = listOf(
                Feature(
                    geometry = Point(Position(0.0, 0.0)),
                    properties = buildJsonObject { }
                )
            ))
    }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        initialCameraPosition = calculateInitialCamera(incidents, userLocation)
    ) {
        val incidentsSource = rememberGeoJsonSource(
            GeoJsonData.Features(incidentsGeoJson)
        )

        // Selected location source
        val selectedLocationSource = selectedLocation?.let { location ->
            val feature = Feature(
                geometry = Point(Position(longitude = location.second, latitude = location.first)),
                properties = buildJsonObject {}
            )
            val featureCollection = FeatureCollection(features = listOf(feature))
            rememberGeoJsonSource(GeoJsonData.Features(featureCollection))
        }

        // Incidents layers
        CircleLayer(
            id = "incidents-outer",
            source = incidentsSource,
            radius = const(10.dp),
            color = const(Color.Red),
            onClick = { features ->
                val feature = features.firstOrNull()
                val idString = feature?.getStringProperty("id")
                val id = idString?.toLongOrNull()

                if (id != null) {
                    incidents.find { it.id == id }?.let { incident ->
                        selectedIncident = incident
                    }
                }
                ClickResult.Consume
            }
        )

        CircleLayer(
            id = "incidents-inner",
            source = incidentsSource,
            radius = const(4.dp),
            color = const(Color.White)
        )

        // Selected location layers
        selectedLocationSource?.let { source ->
            CircleLayer(
                id = "selected-location-outer",
                source = source,
                radius = const(12.dp),
                color = const(Color(0xFF2196F3))
            )
            CircleLayer(
                id = "selected-location-inner",
                source = source,
                radius = const(6.dp),
                color = const(Color.White)
            )
        }

        // User location layer
        LocationIndicatorLayer(
            id = "user-location-indicator",
            locationState = locationState,
            onClick = {
                locationState.lastLocation?.position?.let { pos ->
                    if (isLocationSelectionEnabled) {
                        selectedLocation = pos.latitude to pos.longitude
                        onLocationSelected(pos.latitude, pos.longitude)
                    }
                }
                ClickResult.Consume
            }
        )
    }

    // Update camera when user location changes or when using current location
    LaunchedEffect(userLocation, incidents) {
        val newCamera = calculateInitialCamera(incidents, userLocation)
        mapState.animateCamera(
            CameraUpdate(
                center = newCamera.center,
                zoom = newCamera.zoom,
                bearing = newCamera.bearing,
                pitch = newCamera.pitch
            ),
            CameraAnimation.Ease { duration = 0.5.seconds }
        )
    }

    // Animate to user location when "use current location" is triggered
    LaunchedEffect(shouldUseCurrentLocation) {
        if (shouldUseCurrentLocation) {
            userLocation?.let { (lat, long) ->
                mapState.animateCamera(
                    CameraUpdate(
                        center = Position(
                            longitude = long,
                            latitude = lat
                        ),
                        zoom = 15.0
                    ),
                    CameraAnimation.Ease { duration = 0.8.seconds }
                )
            }
        }
    }

    val mapInteractions = remember(isLocationSelectionEnabled) {
        MapInteractions {
            callbacks {
                click {
                    onUnhandled { clickEvent ->
                        val position = clickEvent.position
                        if (position != null && isLocationSelectionEnabled) {
                            selectedLocation = position.latitude to position.longitude
                            onLocationSelected(position.latitude, position.longitude)
                            ClickResult.Consume
                        } else {
                            selectedIncident = null
                            ClickResult.Pass
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier) {
        if (hasLocationPermission) {
            MaplibreMap(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when {
                                    event.changes.any { it.pressed } -> onMapTouch(true)
                                    event.changes.all { !it.pressed } -> onMapTouch(false)
                                }
                            }
                        }
                    },
                state = mapState,
                interactions = mapInteractions
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.location_permission_is_required_to_show_the_map),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        locationState.requestPermission()
                    },
                    modifier = Modifier.height(52.dp),
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
                        text = stringResource(R.string.grant_permission),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        selectedIncident?.let { incident ->
            IncidentInfoCard(
                incident = incident,
                allowDetailNavigation = allowDetailNavigation,
                onClose = {
                    selectedIncident = null
                },
                onNavigateToDetails = {
                    onIncidentClick(incident)
                    selectedIncident = null
                }
            )
        }
    }
}

// ---------------------- INCIDENT CARD & CHIP HELPERS ----------------------

@Composable
fun IncidentInfoCard(
    incident: IncidentResponse,
    allowDetailNavigation: Boolean,
    onClose: () -> Unit,
    onNavigateToDetails: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatCategory(incident.category),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(status = incident.status)
                            PriorityChip(priority = incident.priority)
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = CloseIcon,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                incident.description.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description.take(120) + if (description.length > 120) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Due date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateForDisplay(incident.dueAt),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (allowDetailNavigation) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.go_to_details),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { onNavigateToDetails() }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: Status) {
    val (backgroundColor, textColor) = when (status) {
        Status.REPORTED -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        Status.ASSIGNED -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        Status.RESOLVED -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
    }

    Surface(shape = RoundedCornerShape(16.dp), color = backgroundColor) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (backgroundColor, textColor) = when (priority) {
        Priority.LOW -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
        Priority.NORMAL -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        Priority.HIGH -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        Priority.CRITICAL -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Surface(shape = RoundedCornerShape(16.dp), color = backgroundColor) {
        Text(
            text = priority.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// ---------------------- UTILITY FUNCTIONS ----------------------

private fun createIncidentsGeoJson(incidents: List<IncidentResponse>): FeatureCollection<Point, JsonObject?> {
    val features = incidents.mapNotNull { incident ->
        try {
            Feature(
                geometry = Point(Position(incident.longitude, incident.latitude)),
                properties = buildJsonObject {
                    put("id", incident.id.toString())
                    put("category", incident.category.name)
                    put("priority", incident.priority.name)
                    put("status", incident.status.name)
                    put("dueAt", incident.dueAt.toString())
                },
                id = JsonPrimitive(incident.id.toString())
            )
        } catch (_: Exception) {
            null
        }
    }
    return FeatureCollection(features = features)
}

private fun formatCategory(category: Category): String {
    return category.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun calculateInitialCamera(
    incidents: List<IncidentResponse>,
    userLocation: Pair<Double, Double>?
): CameraPosition {
    if (incidents.isEmpty()) {
        return if (userLocation != null) {
            CameraPosition(
                center = Position(longitude = userLocation.second, latitude = userLocation.first),
                zoom = 15.0
            )
        } else {
            CameraPosition(
                center = Position(longitude = 5.0, latitude = 52.0),
                zoom = 7.0
            )
        }
    }

    val latitudes = incidents.map { it.latitude }
    val longitudes = incidents.map { it.longitude }

    var minLat = latitudes.minOrNull() ?: 52.0
    var maxLat = latitudes.maxOrNull() ?: 52.0
    var minLon = longitudes.minOrNull() ?: 5.0
    var maxLon = longitudes.maxOrNull() ?: 5.0

    val latSpan = maxLat - minLat
    val lonSpan = maxLon - minLon
    val padding = 0.1

    minLat -= latSpan * padding
    maxLat += latSpan * padding
    minLon -= lonSpan * padding
    maxLon += lonSpan * padding

    val centerLat = (minLat + maxLat) / 2
    val centerLon = (minLon + maxLon) / 2

    val zoom = calculateZoomLevel(minLat, maxLat, minLon, maxLon)

    return CameraPosition(
        center = Position(longitude = centerLon, latitude = centerLat),
        zoom = zoom
    )
}

private fun calculateZoomLevel(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Double {
    val latDiff = maxLat - minLat
    val lonDiff = maxLon - minLon
    val maxDiff = max(latDiff, lonDiff)

    return when {
        maxDiff > 180.0 -> 1.0
        maxDiff > 90.0 -> 2.0
        maxDiff > 45.0 -> 3.0
        maxDiff > 22.0 -> 4.0
        maxDiff > 11.0 -> 5.0
        maxDiff > 5.0 -> 6.0
        maxDiff > 2.5 -> 7.0
        maxDiff > 1.2 -> 8.0
        maxDiff > 0.6 -> 9.0
        maxDiff > 0.3 -> 10.0
        maxDiff > 0.15 -> 11.0
        maxDiff > 0.07 -> 12.0
        maxDiff > 0.035 -> 13.0
        maxDiff > 0.017 -> 14.0
        else -> 15.0
    }
}