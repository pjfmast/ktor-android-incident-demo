package incident.android.util

import android.util.Base64
import org.json.JSONObject

class JwtDecoder {
    companion object {
        fun getRoleFromToken(token: String): String? {
            return try {
                val parts = token.split(".")
                if (parts.size != 3) return null

                val payload = String(
                    Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING),
                    Charsets.UTF_8
                )

                val jsonObject = JSONObject(payload)
                jsonObject.getString("role")
            } catch (e: Exception) {
                null
            }
        }
    }
}