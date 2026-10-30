package incident.android.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

//General permission handler that can be used for any permission type
class PermissionHandler(
    private val context: Context,
    private val permission: String,
    private val onPermissionResult: (Boolean) -> Unit
) {
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        launcher.launch(permission)
    }
}

// photo and camera permission handler
class PhotoPermissionHandler(
    private val context: Context,
    private val onPermissionsResult: (Boolean) -> Unit
) {
    fun requestPermissions(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>) {
        launcher.launch(PhotoUtils.getRequiredPermissions())
    }

    fun hasPermissions(): Boolean {
        return PhotoUtils.hasAllPermissions(context)
    }
}

@Composable
fun rememberPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }
}

@Composable
fun rememberPhotoPermissionLauncher(
    onPermissionsResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allRequiredGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasCamera = permissions[android.Manifest.permission.CAMERA] == true
            val hasFullImageAccess = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] == true
            val hasSelectedAccess = permissions[android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
            hasCamera && (hasFullImageAccess || hasSelectedAccess)
        } else {
            permissions.values.all { it }
        }
        onPermissionsResult(allRequiredGranted)
    }
}