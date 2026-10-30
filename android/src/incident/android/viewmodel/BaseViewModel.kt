package incident.android.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun setBusy(busy: Boolean) {
        _isLoading.value = busy
    }

    protected suspend fun <T> withLoading(block: suspend () -> T): T {
        setBusy(true)
        return try {
            block()
        } finally {
            setBusy(false)
        }
    }
}