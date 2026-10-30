package incident.android.data.repository

import incident.android.data.api.IncidentApi
import incident.android.data.model.ApiResult
import incident.android.data.model.ImageUploadResponse
import incident.shared.core.PaginatedItemResponse
import incident.shared.incidents.*
import java.io.File

class IncidentRepository(private val incidentApi: IncidentApi) {

    suspend fun getMyIncidents(): ApiResult<List<IncidentResponse>> =
        incidentApi.getMyIncidents()

    suspend fun getAllIncidents(): ApiResult<List<IncidentResponse>> =
        incidentApi.getAllIncidents()

    suspend fun getPaginatedIncidents(page: Int = 1, pageSize: Int = 10): ApiResult<PaginatedItemResponse<IncidentResponse>> =
        incidentApi.getPaginatedIncidents(page, pageSize)

    suspend fun getIncidentById(incidentId: Long): ApiResult<IncidentResponse> =
        incidentApi.getIncidentById(incidentId)

    suspend fun createIncident(createIncidentRequest: CreateIncidentRequest): ApiResult<IncidentResponse> =
        incidentApi.createIncident(createIncidentRequest) // can be unauthorized

    suspend fun updateIncident(incidentId: Long, updateRequest: UpdateIncidentRequest): ApiResult<IncidentResponse> =
        incidentApi.updateIncident(incidentId, updateRequest)

    suspend fun deleteIncident(incidentId: Long): ApiResult<Unit> =
        incidentApi.deleteIncident(incidentId)

    suspend fun changeIncidentPriority(incidentId: Long, priority: Priority): ApiResult<IncidentResponse> =
        incidentApi.changeIncidentPriority(incidentId, priority)

    suspend fun changeIncidentStatus(incidentId: Long, status: Status): ApiResult<IncidentResponse> =
        incidentApi.changeIncidentStatus(incidentId, status)

    suspend fun uploadImageToIncident(
        incidentId: Long,
        imageFile: File,
        description: String = ""
    ): ApiResult<ImageUploadResponse> =
        incidentApi.uploadImageToIncident(incidentId, imageFile, description) // can be unauthorized

    suspend fun uploadMultipleImagesToIncident(
        incidentId: Long,
        imageFiles: List<File>,
        description: String = ""
    ): ApiResult<List<ImageUploadResponse>> =
        incidentApi.uploadMultipleImagesToIncident(incidentId, imageFiles, description) // can be unauthorized
}
