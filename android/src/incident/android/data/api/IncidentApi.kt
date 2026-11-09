package incident.android.data.api

import incident.android.data.model.ApiResult
import incident.android.data.model.ImageUploadResponse
import incident.android.data.store.TokenPreferences
import incident.android.di.NetworkConfig
import incident.android.util.performRequest
import incident.shared.core.PaginatedItemResponse
import incident.shared.incidents.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.io.File

class IncidentApi(
    private val client: HttpClient,
    private val tokenPreferences: TokenPreferences,
    private val baseUrl: String = "${NetworkConfig.BASE_URL}/api"
) {
    suspend fun getMyIncidents(): ApiResult<List<IncidentResponse>> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/incidents/my-incidents") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun getAllIncidents(): ApiResult<List<IncidentResponse>> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/incidents") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun getPaginatedIncidents(
        page: Int = 1,
        pageSize: Int = 10
    ): ApiResult<PaginatedItemResponse<IncidentResponse>> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/incidents/paginated") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
        }

    suspend fun getIncidentById(incidentId: Long): ApiResult<IncidentResponse> =
        performRequest(tokenPreferences) { token ->
            client.get("$baseUrl/incidents/$incidentId") {
                header("Authorization", "Bearer $token")
            }
        }

    // Public endpoint: create without authorization
    suspend fun createIncident(request: CreateIncidentRequest): ApiResult<IncidentResponse> =
        performRequest(tokenPreferences, requiresAuth = false, optionalAuth = true) { token ->
            client.post("$baseUrl/incidents") {
                contentType(ContentType.Application.Json)
                token?.let { bearerAuth(it) }
                setBody(request)
            }
        }

    suspend fun updateIncident(
        incidentId: Long,
        request: UpdateIncidentRequest
    ): ApiResult<IncidentResponse> =
        performRequest(tokenPreferences) { token ->
            client.put("$baseUrl/incidents/$incidentId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun deleteIncident(incidentId: Long): ApiResult<Unit> =
        performRequest(tokenPreferences) { token ->
            client.delete("$baseUrl/incidents/$incidentId") {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun changeIncidentStatus(
        incidentId: Long,
        status: Status
    ): ApiResult<IncidentResponse> =
        performRequest(tokenPreferences) { token ->
            client.patch("$baseUrl/incidents/$incidentId/status") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ChangeStatusRequest(status))
            }
        }

    suspend fun changeIncidentPriority(
        incidentId: Long,
        priority: Priority
    ): ApiResult<IncidentResponse> =
        performRequest(tokenPreferences) { token ->
            client.patch("$baseUrl/incidents/$incidentId/priority") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ChangePriorityRequest(priority))
            }
        }

    // Public: Upload image without authorization
// Public: Upload image without authorization
    suspend fun uploadImageToIncident(
        incidentId: Long,
        imageFile: File,
        description: String = ""
    ): ApiResult<ImageUploadResponse> =
        performRequest(tokenPreferences, requiresAuth = false, optionalAuth = true) { token ->
            require(imageFile.exists()) { "Image file does not exist" }

            client.post("$baseUrl/incidents/$incidentId/images") {
                token?.let { bearerAuth(it) }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            if (description.isNotEmpty()) append("description", description)
                            append(
                                "image",
                                imageFile.readBytes(),
                                headers = Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"image\"; filename=\"${imageFile.name}\""
                                    )
                                    append(HttpHeaders.ContentType, getContentTypeForFile(imageFile))
                                }
                            )
                        }
                    )
                )
            }
        }

    suspend fun uploadMultipleImagesToIncident(
        incidentId: Long,
        imageFiles: List<File>,
        description: String = ""
    ): ApiResult<List<ImageUploadResponse>> =
        performRequest(tokenPreferences, requiresAuth = false, optionalAuth = true) { token ->
            val validFiles = imageFiles.filter { it.exists() }
            require(validFiles.isNotEmpty()) { "No valid image files provided" }

            client.post("$baseUrl/incidents/$incidentId/images") {
                header("Authorization", "Bearer $token")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            if (description.isNotEmpty()) append("description", description)
                            validFiles.forEachIndexed { index, file ->
                                append(
                                    "image$index",
                                    file.readBytes(),
                                    headers = Headers.build {
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "form-data; name=\"image$index\"; filename=\"${file.name}\""
                                        )
                                        append(HttpHeaders.ContentType, getContentTypeForFile(file))
                                    }
                                )
                            }
                        }
                    )
                )
            }
        }

    private fun getContentTypeForFile(file: File): String = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}
