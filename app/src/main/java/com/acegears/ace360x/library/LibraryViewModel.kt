package com.acegears.ace360x.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryUiState(
    val folders: List<Uri> = emptyList(),
    val scanning: Boolean = false,
    val report: ScanReport? = null,
    val error: String? = null,
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LibraryRepository(app)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            repo.folders.distinctUntilChanged().collect { folders ->
                _state.update { it.copy(folders = folders) }
                rescan()
            }
        }
    }

    fun addFolder(uri: Uri) = viewModelScope.launch {
        runCatching { repo.addFolder(uri) }
            .onFailure { e -> _state.update { it.copy(error = "Couldn't keep access to that folder: ${e.message}") } }
    }

    fun removeFolder(uri: Uri) = viewModelScope.launch { repo.removeFolder(uri) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun rescan() {
        scanJob?.cancel()
        val folders = _state.value.folders
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true) }
            val result = runCatching { withContext(Dispatchers.IO) { repo.scan(folders) } }
            _state.update {
                it.copy(
                    scanning = false,
                    report = result.getOrNull() ?: it.report,
                    error = result.exceptionOrNull()?.let { e -> "Scan failed: ${e.message}" },
                )
            }
        }
    }
}
