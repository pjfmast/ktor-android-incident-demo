package incident.android.data.repository

import incident.android.data.api.AuthApi
import incident.android.data.model.ApiResult
import incident.android.data.store.TokenPreferences

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenPreferences: TokenPreferences
) {
    suspend fun login(username: String, password: String): ApiResult<Unit> {
        return when (val result = authApi.login(username, password)) {
            is ApiResult.Success -> {
                tokenPreferences.saveToken(result.data)
                ApiResult.Success(Unit)
            }
            is ApiResult.HttpError -> ApiResult.HttpError(result.code, result.message)
            is ApiResult.NetworkError -> ApiResult.NetworkError(result.exception)
            is ApiResult.Timeout -> ApiResult.Timeout(result.exception)
            is ApiResult.Unknown -> ApiResult.Unknown(result.exception)
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getSavedToken(): String? = tokenPreferences.getToken()

    suspend fun logout() {
        tokenPreferences.clearToken()
    }
}
