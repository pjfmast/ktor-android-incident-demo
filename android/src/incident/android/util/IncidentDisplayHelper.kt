package incident.android.util

import androidx.compose.ui.graphics.Color
import incident.shared.incidents.Category
import incident.shared.incidents.Status
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

object IncidentDisplayHelper {
    fun getStatusColor(status: Status): Color {
        return when (status) {
            Status.REPORTED -> Color(0xFFFFC107)
            Status.ASSIGNED -> Color(0xFFFF6B35)
            Status.RESOLVED -> Color(0xFF4CAF50)
        }
    }

    fun formatDateForDisplay(date: LocalDateTime?): String {
        if (date == null) return ""
        val day = date.day.toString().padStart(2, '0')
        val month = date.month.number.toString().padStart(2, '0')
        val year = date.year
        return "$day-$month-$year"
    }

    fun formatCategoryText(category: Category): String {
        val name = category.name
        return if (name.isNotEmpty()) {
            name.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            name
        }
    }
}