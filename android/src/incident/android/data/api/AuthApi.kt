package incident.android.data.api

import android.util.Log
import incident.android.data.model.ApiResult
import incident.android.di.NetworkConfig
import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.IOException
import java.net.SocketTimeoutException

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = "${NetworkConfig.BASE_URL}/api/auth"
) {

    suspend fun login(username: String, password: String): ApiResult<String> {
        return try {
            val response = client.post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                ApiResult.Success(tokenResponse.token)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                ApiResult.Unauthorized
            } else {
                ApiResult.HttpError(response.status.value, response.status.description)
            }
        } catch (e: SocketTimeoutException) {
            Log.e("AuthApi", "Login timeout at $baseUrl/login", e)
            ApiResult.Timeout(e)
        } catch (e: IOException) {
            Log.e("AuthApi", "Login network error at $baseUrl/login", e)
            ApiResult.NetworkError(e)
        } catch (e: Exception) {
            Log.e("AuthApi", "Login unexpected error at $baseUrl/login", e)
            ApiResult.NetworkError(e)
        }
    }
}
