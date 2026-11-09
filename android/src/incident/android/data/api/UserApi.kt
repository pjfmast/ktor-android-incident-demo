package incident.android.data.api

import incident.android.data.model.ApiResult
import incident.android.data.store.TokenPreferences
import incident.android.di.NetworkConfig
import incident.android.util.performRequest
import incident.shared.incidents.IncidentResponse
import incident.shared.users.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(
    private val client: HttpClient,
    private val tokenPreferences: TokenPreferences,
    private val baseUrl: String = "${NetworkConfig.BASE_URL}/api/users"
) {

    // Public endpoint
    suspend fun register(
        username: String,
        password: String,
        email: String,
        avatar: String?
    ): ApiResult<Unit> =
        performRequest(tokenPreferences, requiresAuth = false) { _ ->
            client.post("$baseUrl/register") {
                contentType(ContentType.Application.Json)
                setBody(CreateUserRequest(username, password, email, avatar))
            }
        }

    suspend fun getCurrentUser(): ApiResult<UserResponse> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/me") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun updateCurrentUser(updateRequest: UpdateUserRequest): ApiResult<UserResponse> =
        performRequest(tokenPreferences) { token ->
            client.put("$baseUrl/me") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }
        }

    suspend fun getAllUsers(): ApiResult<List<UserResponse>> =
        performRequest(tokenPreferences) { token ->
            client.get(baseUrl) {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun getUserById(id: Long): ApiResult<UserResponse> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/$id") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun updateUserRole(id: Long, role: Role): ApiResult<UserResponse> {
        return performRequest(tokenPreferences) { token ->
            client.put("$baseUrl/$id/role") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateRoleRequest(role))
            }
        }
    }

    suspend fun deleteUser(id: Long): ApiResult<Unit> =
        performRequest(tokenPreferences) { token ->
            client.delete("$baseUrl/$id") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun getUserIncidents(id: Long): ApiResult<List<IncidentResponse>> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/$id/incidents") {
                header("Authorization", "Bearer $token")
            }
        }
}
