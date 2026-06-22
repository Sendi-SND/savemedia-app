package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun insert(item: DownloadItem): Long {
        return downloadDao.insertDownload(item)
    }

    suspend fun update(item: DownloadItem) {
        downloadDao.updateDownload(item)
    }

    suspend fun delete(id: Int) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearAll() {
        downloadDao.clearAll()
    }

    suspend fun getStreamUrl(
        apiEndpoint: String,
        targetUrl: String,
        mediaType: String,
        quality: String
    ): CobaltResponse {
        val apiService = RetrofitHelper.getApiService()
        
        // Setup request body. Let's make sure both properties (isAudioOnly & downloadMode) are set properly
        val isAudioOnly = mediaType == "MP3"
        val mode = if (isAudioOnly) "audio" else "video"
        
        val request = CobaltRequest(
            url = targetUrl,
            videoQuality = quality,
            audioFormat = "mp3",
            isAudioOnly = isAudioOnly,
            downloadMode = mode
        )
        
        // We trim trailing slash if any on endpoint to construct proper call URL
        val formattedEndpoint = if (apiEndpoint.endsWith("/")) apiEndpoint else "$apiEndpoint/"
        return apiService.getStreamUrl(formattedEndpoint, request)
    }
}
