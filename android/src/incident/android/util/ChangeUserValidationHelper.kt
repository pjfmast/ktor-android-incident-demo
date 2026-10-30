package incident.android.util

import android.content.Context
import android.widget.Toast
import java.util.regex.Pattern

object ChangeUserValidationHelper {

    fun validateUserProfile(
        context: Context,
        username: String,
        email: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        // Username validation
        if (username.isBlank()) {
            showToast(context, "Username cannot be empty")
            return false
        }
        if (username.length < 3) {
            showToast(context, "Username must be at least 3 characters")
            return false
        }

        // Email validation
        if (email.isBlank()) {
            showToast(context, "Email cannot be empty")
            return false
        }
        if (!isValidEmail(email)) {
            showToast(context, "Please enter a valid email address")
            return false
        }

        val isChangingPassword = currentPassword.isNotBlank() ||
                newPassword.isNotBlank() ||
                confirmPassword.isNotBlank()

        if (isChangingPassword) {
            if (currentPassword.isBlank()) {
                showToast(context, "Current password is required")
                return false
            }

            if (newPassword.isBlank()) {
                showToast(context, "New password cannot be empty")
                return false
            }
            if (newPassword.length < 6) {
                showToast(context, "Password must be at least 6 characters")
                return false
            }

            if (confirmPassword.isBlank()) {
                showToast(context, "Please confirm your password")
                return false
            }
            if (newPassword != confirmPassword) {
                showToast(context, "Passwords do not match")
                return false
            }
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
        )
        return emailRegex.matcher(email).matches()
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}