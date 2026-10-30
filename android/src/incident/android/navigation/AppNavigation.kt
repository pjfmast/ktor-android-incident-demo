package incident.android.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import incident.android.ui.screens.auth.LoginScreen
import incident.android.ui.screens.auth.RegisterScreen
import incident.android.ui.screens.auth.UserProfileScreen
import incident.android.ui.screens.incidents.MyIncidentDetailScreen
import incident.android.ui.screens.incidents.MyIncidentListScreen
import incident.android.ui.screens.management.IncidentDetailScreen
import incident.android.ui.screens.management.AllIncidentListScreen
import incident.android.ui.screens.management.IncidentMapScreen
import incident.android.ui.screens.management.UserManagementScreen
import incident.android.ui.screens.incidents.ReportIncidentScreen

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(LoginKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<LoginKey> {
                LoginScreen(
                    onNavigateToIncidentList = {
                        backStack.removeAll { true }
                        backStack.add(MyIncidentListKey)
                    },
                    onNavigateToReport = { backStack.add(ReportIncidentKey) },
                    onNavigateToRegister = { backStack.add(RegisterKey) }
                )
            }

            entry<RegisterKey> {
                RegisterScreen(
                    onNavigateToLogin = {
                        backStack.removeLastOrNull() }
                )
            }

            entry<MyIncidentListKey> {
                MyIncidentListScreen(
                    backStack = backStack,
                    onNavigateToDetail = { backStack.add(MyIncidentDetailKey) },
                    onNavigateToUserProfile = { userJson ->
                        backStack.add(UserProfileKey(userJson))
                    },
                    onNavigateToReport = { backStack.add(ReportIncidentKey) },
                    onLogout = {
                        backStack.removeAll { true }
                        backStack.add(LoginKey)
                    },
                    onNavigateToIncidentList = {
                        backStack.removeAll { it !is IncidentListKey }
                        if (backStack.none { it is IncidentListKey }) {
                            backStack.add(IncidentListKey)
                        }
                    },
                    onNavigateToIncidentMap = {
                        backStack.removeAll { it !is IncidentMapKey }
                        if (backStack.none { it is IncidentMapKey }) {
                            backStack.add(IncidentMapKey)
                        }
                    },
                    onNavigateToUserManagement = {
                        backStack.removeAll { it !is UserManagementKey }
                        if (backStack.none { it is UserManagementKey }) {
                            backStack.add(UserManagementKey)
                        }
                    }
                )
            }

            entry<MyIncidentDetailKey> {
                MyIncidentDetailScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<ReportIncidentKey> {
                ReportIncidentScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToLogin = {
                        backStack.removeAll { true }
                        backStack.add(LoginKey)
                    },
                    onNavigateToIncidentList = {
                        backStack.removeAll { true }
                        backStack.add(MyIncidentListKey)
                    }
                )
            }

            entry<IncidentListKey> {
                AllIncidentListScreen(
                    onNavigateToDetail = { incidentId: Long ->
                        backStack.add(IncidentDetailKey(incidentId))
                    },
                    onNavigateToIncidentMap = {
                        backStack.removeAll { it !is IncidentMapKey }
                        if (backStack.none { it is IncidentMapKey }) {
                            backStack.add(IncidentMapKey)
                        }
                    },
                    onNavigateToUserManagement = {
                        backStack.removeAll { it !is UserManagementKey }
                        if (backStack.none { it is UserManagementKey }) {
                            backStack.add(UserManagementKey)
                        }
                    },
                    onNavigateToMyIncidentList = {
                        backStack.removeAll { it !is MyIncidentListKey }
                        if (backStack.none { it is MyIncidentListKey }) {
                            backStack.add(MyIncidentListKey)
                        }
                    }
                )
            }

            entry<IncidentMapKey> {
                IncidentMapScreen(
                    onNavigateToDetail = { incidentId : Long ->
                        backStack.add(IncidentDetailKey(incidentId))
                    },
                    onNavigateToMyIncidentList = {
                        backStack.removeAll { it !is MyIncidentListKey }
                        if (backStack.none { it is MyIncidentListKey }) {
                            backStack.add(MyIncidentListKey)
                        }
                    },
                    onNavigateToIncidentList = {
                        backStack.removeAll { it !is IncidentListKey }
                        if (backStack.none { it is IncidentListKey }) {
                            backStack.add(IncidentListKey)
                        }
                    },
                    onNavigateToUserManagement = {
                        backStack.removeAll { it !is UserManagementKey }
                        if (backStack.none { it is UserManagementKey }) {
                            backStack.add(UserManagementKey)
                        }
                    }
                )
            }

            entry<UserManagementKey> {
                UserManagementScreen(
                    onNavigateToMyIncidentList = {
                        backStack.removeAll { it !is MyIncidentListKey }
                        if (backStack.none { it is MyIncidentListKey }) {
                            backStack.add(MyIncidentListKey)
                        }
                    },
                    onNavigateToIncidentList = {
                        backStack.removeAll { it !is IncidentListKey }
                        if (backStack.none { it is IncidentListKey }) {
                            backStack.add(IncidentListKey)
                        }
                    },
                    onNavigateToIncidentMap = {
                        backStack.removeAll { it !is IncidentMapKey }
                        if (backStack.none { it is IncidentMapKey }) {
                            backStack.add(IncidentMapKey)
                        }
                    },
                )
            }

            entry<UserProfileKey> {
                UserProfileScreen(
                    userJson = it.userJson,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<IncidentDetailKey> {
                IncidentDetailScreen(
                    incidentId = it.incidentId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToMyIncidentList = {
                        backStack.removeAll { true }
                        backStack.add(MyIncidentListKey)
                    }
                )
            }
        }
    )
}

