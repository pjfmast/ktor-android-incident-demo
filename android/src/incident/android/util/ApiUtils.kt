package incident.android.util

import android.util.Log
import incident.android.data.model.ApiResult
import incident.android.data.store.TokenPreferences
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException

suspend inline fun <reified T> performRequest(
    tokenPreferences: TokenPreferences,
    requiresAuth: Boolean = true,
    optionalAuth: Boolean = false,
    crossinline block: suspend (token: String?) -> HttpResponse
): ApiResult<T> = withContext(Dispatchers.IO) {
    val token = if (requiresAuth || optionalAuth) tokenPreferences.getToken() else null

    // Only fail if auth is required AND token is missing
    if (requiresAuth && !optionalAuth && token == null) {
        return@withContext ApiResult.Unauthorized
    }

    try {
        val response = block(token)
        when {
            response.status == HttpStatusCode.Unauthorized -> {
                if (requiresAuth) tokenPreferences.clearToken()
                ApiResult.Unauthorized
            }
            response.status.isSuccess() -> ApiResult.Success(response.body())
            else -> ApiResult.HttpError(response.status.value, response.status.description)
        }
    } catch (e: SocketTimeoutException) {
        Log.e("ApiUtils", "Request timeout", e)
        ApiResult.Timeout(e)
    } catch (e: IOException) {
        Log.e("ApiUtils", "Network error", e)
        ApiResult.NetworkError(e)
    } catch (e: Exception) {
        Log.e("ApiUtils", "Unexpected error", e)
        ApiResult.Unknown(e)
    }
}
