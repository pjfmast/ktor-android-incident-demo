package incident.android.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import incident.android.ui.components.IncidentsTextField
import incident.android.ui.components.LoadingOverlay
import incident.android.ui.components.TopNavBar
import incident.android.util.ChangeUserValidationHelper
import incident.android.viewmodel.UserViewModel
import incident.shared.users.UserResponse
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.net.URLDecoder
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import incident.android.R

@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    userJson: String?,
    viewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val initialUser = remember(userJson) {
        userJson?.let { json ->
            try {
                val decodedJson = URLDecoder.decode(json, "UTF-8")
                Json.decodeFromString<UserResponse>(decodedJson)
            } catch (_: Exception) {
                null
            }
        }
    }

    if (initialUser == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    val isBusy by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf(initialUser.username) }
    var email by remember { mutableStateOf(initialUser.email) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val updateProfileSuccessMessage = stringResource(R.string.profile_updated_successfully)
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            Toast.makeText(context,
                updateProfileSuccessMessage, Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.resetUpdateState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopNavBar(
                    title = stringResource(R.string.edit_profile),
                    showBackButton = true,
                    onBackClick = { onNavigateBack() },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    textColor = MaterialTheme.colorScheme.onSurface
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.personal_information),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IncidentsTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = stringResource(R.string.username)
                            )

                            IncidentsTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = stringResource(R.string.email)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.security),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.leave_blank_to_keep_current_password),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            IncidentsTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                placeholder = stringResource(R.string.current_password),
                                isPassword = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            IncidentsTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                placeholder = stringResource(R.string.new_password),
                                isPassword = true
                            )

                            IncidentsTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = stringResource(R.string.confirm_new_password),
                                isPassword = true
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(
                        onClick = {
                            if (ChangeUserValidationHelper.validateUserProfile(
                                    context = context,
                                    username = username,
                                    email = email,
                                    currentPassword = currentPassword,
                                    newPassword = newPassword,
                                    confirmPassword = confirmPassword
                                )) {
                                viewModel.updateProfile(
                                    username = username,
                                    email = email,
                                    newPassword = newPassword.ifBlank { null }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(25.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save_changes),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                TextButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    enabled = !isBusy
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        LoadingOverlay(isLoading = isBusy)
    }
}