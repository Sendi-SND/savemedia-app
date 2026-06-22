package com.example.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ApiState {
    object Idle : ApiState()
    object Processing : ApiState()
    data class Success(val downloadUrl: String) : ApiState()
    data class PickerRequired(val items: List<com.example.data.CobaltPickerItem>) : ApiState()
    data class Error(val message: String) : ApiState()
}

class DownloaderViewModel(private val repository: DownloadRepository) : ViewModel() {

    // Main download records list stream
    val downloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form inputs and configuration
    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _selectedMediaType = MutableStateFlow("MP4") // "MP4" (Video) or "MP3" (Audio)
    val selectedMediaType: StateFlow<String> = _selectedMediaType.asStateFlow()

    private val _selectedQuality = MutableStateFlow("1080") // "1080", "720", "480", "360"
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _apiEndpoint = MutableStateFlow("https://api.cobalt.tools")
    val apiEndpoint: StateFlow<String> = _apiEndpoint.asStateFlow()

    // Current query state
    private val _apiState = MutableStateFlow<ApiState>(ApiState.Idle)
    val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    fun updateUrl(url: String) {
        _inputUrl.value = url
    }

    fun updateMediaType(mediaType: String) {
        _selectedMediaType.value = mediaType
    }

    fun updateQuality(quality: String) {
        _selectedQuality.value = quality
    }

    fun loadCustomEndpoint(context: Context) {
        val prefs = context.getSharedPreferences("savemedia_prefs", Context.MODE_PRIVATE)
        _apiEndpoint.value = prefs.getString("api_endpoint", "https://api.cobalt.tools") ?: "https://api.cobalt.tools"
    }

    fun saveCustomEndpoint(context: Context, endpoint: String) {
        val prefs = context.getSharedPreferences("savemedia_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("api_endpoint", endpoint).apply()
        _apiEndpoint.value = endpoint
    }

    fun resetApiState() {
        _apiState.value = ApiState.Idle
    }

    /**
     * Start process of submitting URL to Cobalt to resolve direct stream link.
     */
    fun startProcessingUrl(context: Context) {
        val url = _inputUrl.value.trim()
        if (url.isEmpty()) {
            _apiState.value = ApiState.Error("Masukkan tautan video atau audio terlebih dahulu.")
            return
        }

        _apiState.value = ApiState.Processing

        viewModelScope.launch {
            try {
                val platformName = extractPlatformName(url)
                val type = _selectedMediaType.value
                val quality = _selectedQuality.value
                val endpoint = _apiEndpoint.value

                // Insert pending item to Room database
                val title = "$platformName - ${System.currentTimeMillis() % 10000}"
                val initialItem = DownloadItem(
                    originalUrl = url,
                    title = title,
                    mediaType = type,
                    status = "PROCESSING",
                    downloadProgress = 0.1f
                )
                val recordId = repository.insert(initialItem).toInt()

                // Request stream link from Cobalt repository
                val response = withContext(Dispatchers.IO) {
                    repository.getStreamUrl(
                        apiEndpoint = endpoint,
                        targetUrl = url,
                        mediaType = type,
                        quality = quality
                    )
                }

                when (response.status) {
                    "success", "stream", "redirect" -> {
                        val directUrl = response.url
                        if (directUrl != null) {
                            _apiState.value = ApiState.Success(directUrl)
                            
                            // Update status in Room
                            val updatedItem = initialItem.copy(
                                id = recordId,
                                title = "$platformName Download",
                                status = "DOWNLOADING",
                                downloadProgress = 0.2f
                            )
                            repository.update(updatedItem)

                            // Enqueue network download using native android DownloadManager
                            enqueueDownload(context, directUrl, "$platformName - Video/Audio", type, recordId)
                        } else {
                            handleFailure(recordId, initialItem, "Tautan download tidak ditemukan dalam respon API.")
                        }
                    }
                    "picker" -> {
                        val pickerItems = response.picker
                        if (pickerItems != null && pickerItems.isNotEmpty()) {
                            _apiState.value = ApiState.PickerRequired(pickerItems)
                            val updatedItem = initialItem.copy(
                                id = recordId,
                                status = "SUCCESS",
                                downloadProgress = 1.0f,
                                downloadSize = "Multiple Files",
                                filePath = "PICKER"
                            )
                            repository.update(updatedItem)
                        } else {
                            handleFailure(recordId, initialItem, "Respon slider/picker kosong.")
                        }
                    }
                    "error" -> {
                        val errMsg = response.text ?: "Gagal mendapatkan tautan dari server."
                        handleFailure(recordId, initialItem, errMsg)
                    }
                    else -> {
                        handleFailure(recordId, initialItem, "Respon server tidak diketahui.")
                    }
                }
            } catch (e: Exception) {
                _apiState.value = ApiState.Error(e.message ?: "Terjadi kesalahan jaringan.")
            }
        }
    }

    /**
     * Download an individual item selected via picker
     */
    fun downloadPickerItem(context: Context, directUrl: String, itemType: String) {
        viewModelScope.launch {
            val title = "Picker ${itemType.uppercase()} - ${System.currentTimeMillis() % 10000}"
            val platformType = if (itemType.contains("video", ignoreCase = true)) "MP4" else "MP3"
            
            val item = DownloadItem(
                originalUrl = _inputUrl.value,
                title = title,
                mediaType = platformType,
                status = "DOWNLOADING",
                downloadProgress = 0.2f
            )
            
            val recordId = repository.insert(item).toInt()
            enqueueDownload(context, directUrl, title, platformType, recordId)
        }
    }

    private suspend fun handleFailure(recordId: Int, initialItem: DownloadItem, errorMsg: String) {
        _apiState.value = ApiState.Error(errorMsg)
        val failedItem = initialItem.copy(
            id = recordId,
            status = "FAILED",
            downloadProgress = 0.0f,
            errorMessage = errorMsg
        )
        repository.update(failedItem)
    }

    /**
     * Dispatch standard Android DownloadManager to asynchronously download files and update Room stats dynamically
     */
    private fun enqueueDownload(
        context: Context,
        directUrl: String,
        title: String,
        mediaType: String,
        recordId: Int
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(directUrl))
            request.setTitle(title)
            request.setDescription("Mengunduh dari SaveMedia")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val extension = if (mediaType == "MP3") ".mp3" else ".mp4"
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "SaveMedia_${sanitizedTitle}_${System.currentTimeMillis()}$extension"
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // Start coroutine query loop to track loading progress
            monitorProgress(context, downloadId, recordId)
        } catch (e: Exception) {
            viewModelScope.launch {
                val dbItem = DownloadItem(
                    id = recordId,
                    originalUrl = _inputUrl.value,
                    title = title,
                    mediaType = mediaType,
                    status = "FAILED",
                    errorMessage = e.message ?: "Gagal mendaftarkan download di sistem Android."
                )
                repository.update(dbItem)
            }
        }
    }

    private fun monitorProgress(context: Context, downloadId: Long, recordId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var isDownloading = true
            var lastProgress = 0.0f

            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    if (bytesDownloadedIdx != -1 && totalBytesIdx != -1 && statusIdx != -1) {
                        val bytesDownloaded = cursor.getInt(bytesDownloadedIdx)
                        val totalBytes = cursor.getInt(totalBytesIdx)
                        val status = cursor.getInt(statusIdx)
                        
                        var progress = 0.0f
                        if (totalBytes > 0) {
                            progress = bytesDownloaded.toFloat() / totalBytes.toFloat()
                        } else if (bytesDownloaded > 0) {
                            // If total size is unknown, show visual rotation/indefinite
                            progress = lastProgress + 0.05f
                            if (progress > 0.95f) progress = 0.95f
                        }
                        lastProgress = progress
                        
                        val formattedSize = if (totalBytes > 0) {
                            "%.2f MB".format(totalBytes.toFloat() / (1024 * 1024))
                        } else if (bytesDownloaded > 0) {
                            "%.2f MB".format(bytesDownloaded.toFloat() / (1024 * 1024))
                        } else {
                            "Calculating"
                        }
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isDownloading = false
                                val fileUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                val fileUri = if (fileUriIdx != -1) cursor.getString(fileUriIdx) else null
                                val itemFlow = downloads.value.find { it.id == recordId }
                                if (itemFlow != null) {
                                    repository.update(itemFlow.copy(
                                        status = "SUCCESS",
                                        downloadProgress = 1.0f,
                                        downloadSize = formattedSize,
                                        filePath = fileUri
                                    ))
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isDownloading = false
                                val itemFlow = downloads.value.find { it.id == recordId }
                                if (itemFlow != null) {
                                    repository.update(itemFlow.copy(
                                        status = "FAILED",
                                        errorMessage = "Unduhan dibatalkan atau jaringan tidak stabil."
                                    ))
                                }
                            }
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                                val itemFlow = downloads.value.find { it.id == recordId }
                                if (itemFlow != null) {
                                    repository.update(itemFlow.copy(
                                        status = "DOWNLOADING",
                                        downloadProgress = progress,
                                        downloadSize = formattedSize
                                    ))
                                }
                            }
                        }
                    }
                } else {
                    // Download cancelled or removed from system
                    isDownloading = false
                }
                cursor?.close()
                delay(1000)
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun extractPlatformName(url: String): String {
        return when {
            url.contains("tiktok", ignoreCase = true) -> "TikTok"
            url.contains("instagram", ignoreCase = true) -> "Instagram"
            url.contains("youtube", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
            url.contains("twitter", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "Twitter"
            url.contains("facebook", ignoreCase = true) || url.contains("fb.", ignoreCase = true) -> "Facebook"
            url.contains("pinterest", ignoreCase = true) -> "Pinterest"
            url.contains("soundcloud", ignoreCase = true) -> "SoundCloud"
            else -> "Media"
        }
    }
}

class DownloaderViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DownloaderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
