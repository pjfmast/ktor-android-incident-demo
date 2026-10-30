package incident.android.ui.screens.incidents

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import incident.android.R
import incident.android.navigation.IncidentListKey
import incident.android.navigation.IncidentMapKey
import incident.android.navigation.MyIncidentListKey
import incident.android.navigation.UserManagementKey
import incident.android.ui.components.BottomNavBar
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.icons.AddIcon
import incident.android.ui.icons.LanguageIcon
import incident.android.ui.icons.LogoutIcon
import incident.android.ui.icons.PersonIcon
import incident.android.ui.icons.SettingsIcon
import incident.android.ui.icons.UserAttributesIcon
import incident.android.util.IncidentDisplayHelper.formatCategoryText
import incident.android.util.IncidentDisplayHelper.formatDateForDisplay
import incident.android.util.IncidentDisplayHelper.getStatusColor
import incident.android.viewmodel.MyIncidentListViewModel
import incident.shared.incidents.IncidentResponse
import incident.shared.incidents.Status
import incident.shared.users.Role
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyIncidentListScreen(
    onNavigateToDetail: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToIncidentList: () -> Unit,
    onNavigateToIncidentMap: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MyIncidentListViewModel = koinViewModel(),
    backStack: NavBackStack<NavKey>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isLoading.collectAsStateWithLifecycle()
    val userRole = uiState.userRole
    val context = LocalContext.current

    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collect { currentKey ->
                if (currentKey == MyIncidentListKey) {
                    viewModel.refreshIncidents()
                }
            }
    }

    LaunchedEffect(uiState.logoutEvent) {
        if (uiState.logoutEvent) {
            onLogout()
            viewModel.resetLogoutEvent()
        }
    }

    val fullName = uiState.user?.username ?: stringResource(R.string.loading)
    val totalIncidents = uiState.incidents.size
    val activeIncidents =
        uiState.incidents.count { it.status == Status.ASSIGNED  }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (userRole == Role.OFFICIAL || userRole == Role.ADMIN) {
                BottomNavBar(
                    modifier = Modifier.navigationBarsPadding(),
                    currentKey = MyIncidentListKey,
                    userRole = userRole,
                    onNavigateTo = { route ->
                        when (route) {
                            IncidentListKey -> onNavigateToIncidentList()
                            IncidentMapKey -> onNavigateToIncidentMap()
                            UserManagementKey -> onNavigateToUserManagement()
                            else -> {}
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        MyIncidentListContent(
            paddingValues = paddingValues,
            uiState = uiState,
            isBusy = isBusy,
            onIncidentClick = { incident ->
                viewModel.saveSelectedIncident(incident)
                onNavigateToDetail()
            },
            onNavigateToReport = onNavigateToReport,
            onNavigateToUserProfile = onNavigateToUserProfile,
            onOpenLanguageSettings = {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                context.startActivity(intent)
            },
            onLogout = { viewModel.logout() },
            onRefresh = { viewModel.refreshIncidents() }
        )
    }
}

@Composable
private fun MyIncidentListContent(
    paddingValues: PaddingValues,
    uiState: incident.android.viewmodel.MyIncidentListUiState,
    isBusy: Boolean,
    onIncidentClick: (IncidentResponse) -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
) {
    val fullName = uiState.user?.username ?: stringResource(R.string.loading)
    val totalIncidents = uiState.incidents.size
    val activeIncidents =
        uiState.incidents.count { it.status == Status.ASSIGNED }

    var isDropdownVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF1976D2)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PersonIcon,
                            contentDescription = "User Avatar",
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 15.dp)
                    ) {
                        Text(
                            text = fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.account_dashboard),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { isDropdownVisible = !isDropdownVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SettingsIcon,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.total_incidents),
                    value = totalIncidents.toString(),
                    valueColor = Color(0xFF0D47A1)
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.active),
                    value = activeIncidents.toString(),
                    valueColor = Color(0xFFFF6B35)
                )
            }

            Text(
                text = stringResource(R.string.my_incidents),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)
            )

            PullToRefreshBox(
                isRefreshing = isBusy,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.incidents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_incidents_found),
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        contentPadding = PaddingValues(vertical = 15.dp)
                    ) {
                        items(uiState.incidents) { incident ->
                            IncidentCard(
                                incident = incident,
                                onClick = { onIncidentClick(incident) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNavigateToReport() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp),
            containerColor = Color(0xFF0D47A1),
            shape = CircleShape
        ) {
            Icon(
                imageVector = AddIcon,
                contentDescription = stringResource(R.string.create_incident),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        if (isDropdownVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isDropdownVisible = false }
            )
        }

        AnimatedVisibility(
            visible = isDropdownVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(200.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Column {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = UserAttributesIcon,
                                    contentDescription = "User profile",
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.profile),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            isDropdownVisible = false
                            uiState.user?.let { userData ->
                                val userJson = Json.encodeToString(userData)
                                onNavigateToUserProfile(userJson)
                            }
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = LanguageIcon,
                                    contentDescription = "Language settings",
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.language),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            isDropdownVisible = false
                            onOpenLanguageSettings()
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = LogoutIcon,
                                    contentDescription = "Logout",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.logout),
                                    color = Color(0xFFD32F2F),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            isDropdownVisible = false
                            onLogout()
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        LoadingOverlay(isLoading = isBusy)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun IncidentCard(
    incident: IncidentResponse,
    onClick: () -> Unit
) {
    val shortDescription = if (incident.description.length > 60) {
        incident.description.take(60) + "..."
    } else {
        incident.description
    }

    val formattedDate = formatDateForDisplay(incident.createdAt)
    val formattedCategory = formatCategoryText(incident.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp, 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formattedCategory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = shortDescription,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = getStatusColor(incident.status).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = incident.status.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getStatusColor(incident.status),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "›",
                fontSize = 24.sp,
                color = Color(0xFFCCCCCC)
            )
        }
    }
}