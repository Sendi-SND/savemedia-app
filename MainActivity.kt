package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.DownloadRepository
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DownloaderViewModel
import com.example.viewmodel.DownloaderViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize local SQLite Room persistence
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DownloadRepository(database.downloadDao())
        
        // 2. Instantiate viewmodel using custom Factory Injection pattern
        val factory = DownloaderViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[DownloaderViewModel::class.java]
        
        // 3. Enable edge to edge and load modern Material 3 Jetpack Compose layout
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout(viewModel = viewModel)
            }
        }
    }
}
