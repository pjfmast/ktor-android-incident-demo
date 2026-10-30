package incident.android.data.api

import incident.android.data.model.ApiResult
import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthApi(private val client: HttpClient) {
    private val baseUrl = "http://10.0.2.2:8080/api/auth"

    suspend fun login(username: String, password: String): ApiResult<String> {
        return try {
            val response = client.post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                ApiResult.Success(tokenResponse.token)
            } else {
                ApiResult.HttpError(response.status.value, response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
