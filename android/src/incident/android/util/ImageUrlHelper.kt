package incident.android.util

object ImageUrlHelper {
    private const val BASE_URL = "http://10.0.2.2:8080"

    fun getFullImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null

        return if (imagePath.startsWith("http")) {
            imagePath
        } else {
            "$BASE_URL/uploads/incidentsimages/$imagePath"
        }
    }
}