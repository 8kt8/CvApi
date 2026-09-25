package com.katlewski.cv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katlewski.cv.data.Cv
import com.katlewski.cv.data.CvRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CvUiState(
    val cv: Cv? = null,
    val isRefreshing: Boolean = false,
    /** One-off message for a snackbar; clear it with [CvViewModel.onMessageShown]. */
    val message: String? = null,
)

/**
 * Offline-first: shows the bundled CV immediately, then refreshes from [remote].
 * A failed refresh keeps the current content and only reports a message.
 */
class CvViewModel(
    private val bundled: CvRepository,
    private val remote: CvRepository,
    vararg closeables: AutoCloseable,
) : ViewModel(*closeables) {

    private val _state = MutableStateFlow(CvUiState())
    val state: StateFlow<CvUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(cv = bundled.getCv()) }
            refresh(userInitiated = false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch { refresh(userInitiated = true) }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun refresh(userInitiated: Boolean) {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = userInitiated) }
        try {
            val cv = remote.getCv()
            _state.update { it.copy(cv = cv, isRefreshing = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isRefreshing = false,
                    message = if (userInitiated) "Couldn't refresh - showing the saved CV" else null,
                )
            }
        }
    }
}
