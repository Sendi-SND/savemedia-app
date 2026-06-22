package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadItem
import com.example.data.RetrofitHelper
import com.example.viewmodel.ApiState
import com.example.viewmodel.DownloaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(viewModel: DownloaderViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Shared state
    val inputUrl by viewModel.inputUrl.collectAsState()
    val mediaType by viewModel.selectedMediaType.collectAsState()
    val quality by viewModel.selectedQuality.collectAsState()
    val apiEndpoint by viewModel.apiEndpoint.collectAsState()
    val apiState by viewModel.apiState.collectAsState()
    val downloadsList by viewModel.downloads.collectAsState()

    // Nav active tab: "UNDUH" (0), "RIWAYAT" (1), "PENGATURAN" (2)
    var activeTab by remember { mutableStateOf(0) }

    // First initial load for preference endpoint
    LaunchedEffect(Unit) {
        viewModel.loadCustomEndpoint(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Unduh") },
                    label = { Text("Unduh", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_download")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Riwayat") },
                    label = { Text("Riwayat", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant top branding banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small circular launcher-style logo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "SaveMedia Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "SaveMedia",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Unduh Video & Audio Instan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Connection Status indicator light
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676)) // Live Green status
                            )
                            Text(
                                text = "Cobalt Online",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Body with cross-transitions
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() with
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> DownloadHubTab(viewModel, inputUrl, mediaType, quality, apiState, context)
                        1 -> HistoryTab(viewModel, downloadsList, context)
                        2 -> SettingsTab(viewModel, apiEndpoint, downloadsList, context)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadHubTab(
    viewModel: DownloaderViewModel,
    inputUrl: String,
    mediaType: String,
    quality: String,
    apiState: ApiState,
    context: Context
) {
    val clipboard = LocalClipboardManager.current
    var inputExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and hint card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bagaimana cara mengunduh?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Salin tautan (URL) video/audio dari TikTok, Instagram, YouTube, X, atau Facebook.\n" +
                               "2. Tempel tautan di bidang input di bawah.\n" +
                               "3. Pilih format MP4 atau MP3 lalu klik tombol Unduh.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // URL Input and Quick Pastes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { viewModel.updateUrl(it) },
                    placeholder = { Text("https://tiktok.com/@user/video/...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    label = { Text("Tautan Media Sosial", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateUrl("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus")
                                }
                            }
                            TextButton(
                                onClick = {
                                    clipboard.getText()?.let {
                                        viewModel.updateUrl(it.text)
                                        Toast.makeText(context, "Berhasil menempel!", Toast.LENGTH_SHORT).show()
                                    } ?: Toast.makeText(context, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.padding(end = 4.dp).testTag("paste_button")
                            ) {
                                Text("Tempel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.startProcessingUrl(context) }
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("url_input_field")
                )

                // Service speed dial suggesters
                Text(
                    text = "Platform Terintegrasi :",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    val quickPill = listOf(
                        "TikTok" to "https://www.tiktok.com/",
                        "Instagram" to "https://www.instagram.com/",
                        "YouTube" to "https://youtube.com/",
                        "X / Twitter" to "https://x.com/",
                        "Facebook" to "https://facebook.com/"
                    )
                    items(quickPill) { (label, placeholder) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable {
                                    Toast.makeText(context, "Silakan salin tautan dari aplikasi $label Anda", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Format selector (Video / MP4 vs Audio / MP3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih Jenis Format :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("MP4" to "Video", "MP3" to "Audio Only").forEach { (type, description) ->
                        val isSelected = mediaType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateMediaType(type) }
                                .testTag("format_${type.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (type == "MP4") Icons.Default.PlayArrow else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = type,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quality selection (only visible for MP4)
        if (mediaType == "MP4") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Resolusi Video (Jika tersedia) :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1080", "720", "480", "360").forEach { q ->
                            val isSelected = quality == q
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateQuality(q) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${q}p",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trigger Submit Action Button
        item {
            Button(
                onClick = { viewModel.startProcessingUrl(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_download_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = apiState !is ApiState.Processing
            ) {
                if (apiState is ApiState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Menghubungkan Server...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Muat",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mulai Unduh Sekarang", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Live status engine feedback blocks
        item {
            AnimatedVisibility(
                visible = apiState !is ApiState.Idle,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    
                    when (apiState) {
                        is ApiState.Idle -> {}
                        is ApiState.Processing -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Mengurai Tautan Tautan...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Menghubungkan ke gateway Cobalt untuk mendapatkan aliran stream file.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        is ApiState.Success -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Sukses Dialirkan!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF00E676))
                                        Text(
                                            "Unduhan didelegasikan ke Android DownloadManager. Silakan pantau perkembangannya di tab Riwayat.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        is ApiState.PickerRequired -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Saluran Multi-File Ditemukan!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "Tautan ini memiliki beberapa media. Klik tombol di bawah untuk mengunduh media satu per satu.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(apiState.items) { pickerItem ->
                                            Column(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = if (pickerItem.type == "video") Icons.Default.PlayArrow else Icons.Default.Favorite,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = pickerItem.type?.uppercase() ?: "MEDIA",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.downloadPickerItem(context, pickerItem.url, pickerItem.type ?: "photo")
                                                        Toast.makeText(context, "Mulai mengunduh berkas!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.fillMaxWidth().height(28.dp)
                                                ) {
                                                    Text("Unduh", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is ApiState.Error -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFFF5252))
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Gagal Mengambil File!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFFF5252))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = apiState.message,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.resetApiState() },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Tutup Alert", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    viewModel: DownloaderViewModel,
    downloads: List<DownloadItem>,
    context: Context
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Riwayat Unduhan Kosong",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tautan yang Anda salin dan unduh akan muncul di sini bersama kontrol pemutaran instan.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Anda (${downloads.size} berkas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { viewModel.clearAllHistory() },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Hapus Semua", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(downloads, key = { it.id }) { item ->
                HistoryItemCard(item, onDelete = { viewModel.deleteHistoryItem(item.id) }, context)
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: DownloadItem,
    onDelete: () -> Unit,
    context: Context
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when (item.status) {
                "SUCCESS" -> Color(0xFF00E676).copy(alpha = 0.3f)
                "FAILED" -> Color(0xFFFF5252).copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular format logo icon (Video player or music note)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.mediaType == "MP3") Color(0xFF00E5FF).copy(alpha = 0.15f)
                            else Color(0xFF1DE9B6).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.mediaType == "MP3") Icons.Default.Favorite else Icons.Default.PlayArrow,
                        contentDescription = item.mediaType,
                        tint = if (item.mediaType == "MP3") Color(0xFF00E5FF) else Color(0xFF1DE9B6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.mediaType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.mediaType == "MP3") Color(0xFF00E5FF) else Color(0xFF1DE9B6)
                        )
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            text = item.downloadSize,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Close/Delete history button
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Riwayat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Live progress / status state row
            Spacer(modifier = Modifier.height(10.dp))
            
            when (item.status) {
                "PROCESSING", "DOWNLOADING" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (item.status == "PROCESSING") "Memindai URL..." else "Mengunduh...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "%.0f%%".format(item.downloadProgress * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = item.downloadProgress,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
                "SUCCESS" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Unduhan Berhasil!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.filePath != "PICKER" && item.filePath != null) {
                                Button(
                                    onClick = { openFileIntent(context, item.filePath, item.mediaType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Buka File", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                                OutlinedButton(
                                    onClick = { shareFileIntent(context, item.filePath) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (item.filePath == "PICKER") {
                                Text(
                                    text = "Menggunakan Carousel",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                "FAILED" -> {
                    Column {
                        Text(
                            text = "Download Gagal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                        if (item.errorMessage != null) {
                            Text(
                                text = item.errorMessage,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: DownloaderViewModel,
    endpoint: String,
    downloads: List<DownloadItem>,
    context: Context
) {
    var textInput by remember { mutableStateOf(endpoint) }
    var pingingState by remember { mutableStateOf(0) } // 0: Idle, 1: Loading, 2: Done, 3: Failed
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // API Settings section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Konfigurasi Server API",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Aplikasi ini memproses link melalui Cobalt API secara server-side. Secara default, aplikasi menggunakan server publik resmi. Jika server utama lambat atau penuh, Anda dapat menggantinya dengan server/instance buatan sendiri atau publik lainnya.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("URL Cobalt API Endpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveCustomEndpoint(context, textInput)
                                Toast.makeText(context, "Sistem server diperbarui!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Server", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                pingingState = 1
                                coroutineScope.launch {
                                    val isOk = testServicePing(textInput)
                                    pingingState = if (isOk) 2 else 3
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (pingingState == 1) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                                }
                                Text(
                                    text = when (pingingState) {
                                        1 -> "Checking..."
                                        2 -> "Online!"
                                        3 -> "Offline!"
                                        else -> "Uji Koneksi"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            textInput = "https://api.cobalt.tools"
                            viewModel.saveCustomEndpoint(context, "https://api.cobalt.tools")
                            Toast.makeText(context, "Reset ke default berhasil", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset ke Server Resmi Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Statistik SaveMedia",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Download:", fontSize = 12.sp)
                        Text("${downloads.size} berkas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format Video (MP4):", fontSize = 12.sp)
                        Text("${downloads.count { it.mediaType == "MP4" }} berkas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format Audio (MP3):", fontSize = 12.sp)
                        Text("${downloads.count { it.mediaType == "MP3" }} berkas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Developer info
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SaveMedia v1.0.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dipersembahkan oleh Cobalt Engine. Desain Modern & Bebas Iklan.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Quick ping verification to test if user's API server is reachable
 */
suspend fun testServicePing(endpoint: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(if (endpoint.endsWith("/")) endpoint else "$endpoint/")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val code = connection.responseCode
            code in 200..399 || code == 405 // 405 Method Not Allowed is OK since we did GET on POST endpoint
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Fire an action intent to allow system file viewer / Google photo player to open and share our downloaded file safely
 */
fun openFileIntent(context: Context, localUriStr: String?, mediaType: String) {
    if (localUriStr == null) return
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        val uri = android.net.Uri.parse(localUriStr)
        val mime = if (mediaType == "MP3") "audio/*" else "video/*"
        intent.setDataAndType(uri, mime)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal memutar berkas. Silakan pasang pemutar audio/video.", Toast.LENGTH_LONG).show()
    }
}

fun shareFileIntent(context: Context, localUriStr: String?) {
    if (localUriStr == null) return
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        val uri = android.net.Uri.parse(localUriStr)
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
        intent.type = "*/*"
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Berkas Media"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagi berkas.", Toast.LENGTH_SHORT).show()
    }
}
