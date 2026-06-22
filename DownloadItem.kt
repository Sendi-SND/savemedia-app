package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalUrl: String,
    val title: String,
    val mediaType: String, // "MP3" or "MP4"
    val status: String,    // "PENDING", "PROCESSING", "DOWNLOADING", "SUCCESS", "FAILED"
    val downloadProgress: Float = 0.0f,
    val downloadSize: String = "Calculating",
    val filePath: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
