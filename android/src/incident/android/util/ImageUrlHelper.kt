package incident.android.util

import incident.android.di.NetworkConfig

object ImageUrlHelper {
    fun getFullImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null

        return if (imagePath.startsWith("http")) {
            imagePath
        } else {
            "${NetworkConfig.BASE_URL}/uploads/incidentsimages/$imagePath"
        }
    }
}