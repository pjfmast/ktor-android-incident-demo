package incident.android.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import incident.android.R
import incident.android.ui.components.IncidentsTextField
import incident.android.ui.components.LoadingOverlay
import incident.android.viewmodel.AutoLoginState
import incident.android.viewmodel.LoginUiState
import incident.android.viewmodel.LoginState
import incident.android.viewmodel.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToIncidentList: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isBusy by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkAutoLogin()
    }

    LaunchedEffect(uiState.autoLoginState) {
        if (uiState.autoLoginState is AutoLoginState.TokenFound) {
            onNavigateToIncidentList()
        }
    }

    LaunchedEffect(uiState.loginState) {
        if (uiState.loginState is LoginState.Success) {
            onNavigateToIncidentList()
        }
    }

    LoginContent(
        uiState = uiState,
        isBusy = isBusy,
        onLogin = { u, p -> viewModel.login(u, p) },
        onClearError = { viewModel.clearLoginState() },
        onNavigateToReport = onNavigateToReport,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    isBusy: Boolean,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoggingIn = remember {
        derivedStateOf {
            uiState.loginState is LoginState.Loading || (isBusy && uiState.loginState !is LoginState.Error)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF6EE7FF)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(200.dp)
                    .padding(vertical = 16.dp)
            )

            Surface(
                modifier = Modifier.width(360.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(30.dp)
                ) {
                    IncidentsTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            if (uiState.loginState is LoginState.Error) {
                                onClearError()
                            }
                        },
                        placeholder = stringResource(R.string.username),
                        isError = uiState.loginState is LoginState.Error
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    IncidentsTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState.loginState is LoginState.Error) {
                                onClearError()
                            }
                        },
                        placeholder = stringResource(R.string.password),
                        isPassword = true,
                        isError = uiState.loginState is LoginState.Error
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.loginState is LoginState.Error) {
                        Text(
                            text = uiState.loginState.message,
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Button(
                        onClick = { onLogin(username, password) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoggingIn.value && username.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(stringResource(R.string.login))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onNavigateToReport() },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoggingIn.value
                    ) {
                        Text(stringResource(R.string.report_anonymously))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.don_t_have_an_account_register_here),
                        fontSize = 14.sp,
                        color = Color(0xFF0D47A1),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isLoggingIn.value) {
                                    onNavigateToRegister()
                                }
                            }
                    )
                }
            }
        }

        LoadingOverlay(
            isLoading = uiState.autoLoginState is AutoLoginState.Checking || isLoggingIn.value
        )
    }
}