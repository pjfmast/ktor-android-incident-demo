package incident.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import incident.android.R

import incident.android.ui.icons.AccountCircleIcon
import incident.android.ui.icons.AccountCircleFilledIcon
import incident.android.ui.icons.PeopleIcon
import incident.android.ui.icons.PeopleFilledIcon

import incident.android.navigation.IncidentMapKey
import incident.android.navigation.MyIncidentListKey
import incident.android.navigation.UserManagementKey
import incident.android.navigation.IncidentListKey
import androidx.navigation3.runtime.NavKey
import incident.shared.users.Role
import incident.android.ui.icons.ListFilledIcon
import incident.android.ui.icons.ListIcon
import incident.android.ui.icons.MapFilledIcon
import incident.android.ui.icons.MapIcon

sealed class BottomNavItem(
    val key: NavKey,
    val titleResId: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val requiredRoles: Set<Role>
) {
    data object List : BottomNavItem(
        key = IncidentListKey,
        titleResId = R.string.list,
        unselectedIcon = ListIcon,
        selectedIcon = ListFilledIcon,
        requiredRoles = setOf(Role.OFFICIAL, Role.ADMIN)
    )

    data object Map : BottomNavItem(
        key = IncidentMapKey,
        titleResId = R.string.map,
        unselectedIcon = MapIcon,
        selectedIcon = MapFilledIcon,
        requiredRoles = setOf(Role.OFFICIAL, Role.ADMIN)
    )

    data object Users : BottomNavItem(
        key = UserManagementKey,
        titleResId = R.string.users,
        unselectedIcon = PeopleIcon,
        selectedIcon = PeopleFilledIcon,
        requiredRoles = setOf(Role.ADMIN)
    )

    data object Profile : BottomNavItem(
        key = MyIncidentListKey,
        titleResId = R.string.profile,
        unselectedIcon = AccountCircleIcon,
        selectedIcon = AccountCircleFilledIcon,
        requiredRoles = setOf(Role.OFFICIAL, Role.ADMIN)
    )
}


@Composable
fun BottomNavBar(
    currentKey: NavKey,
    userRole: Role?,
    onNavigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    if (userRole == Role.USER || userRole == null) return

    val navItems = listOf(
        BottomNavItem.List,
        BottomNavItem.Map,
        BottomNavItem.Users,
        BottomNavItem.Profile
    ).filter { userRole in it.requiredRoles }

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        navItems.forEach { item ->
            val isSelected = currentKey::class == item.key::class

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(item.key) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.titleResId),
                        modifier = Modifier.size(32.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.titleResId),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color(0xFF6B7280),
                    unselectedTextColor = Color(0xFF6B7280),
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
