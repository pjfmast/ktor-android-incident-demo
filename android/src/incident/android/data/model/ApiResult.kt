package incident.android.data.model

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()
    data class Timeout(val exception: Throwable) : ApiResult<Nothing>()
    data class Unknown(val exception: Throwable) : ApiResult<Nothing>()
    object Unauthorized : ApiResult<Nothing>()
}
