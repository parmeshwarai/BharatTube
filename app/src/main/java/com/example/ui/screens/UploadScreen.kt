package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GlobalCatalog
import com.example.data.model.PlatformConfig
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.upload.UploadProgressState
import com.example.data.upload.UploadStatus
import com.example.domain.CopyrightFingerprintEngine
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@Composable
fun UploadScreen(
    user: UserProfile,
    config: PlatformConfig,
    existingVideos: List<Video>,
    uploadState: UploadProgressState,
    onStartUpload: (
        fileName: String,
        totalMb: Double,
        simulateDisconnect: Boolean,
        title: String,
        description: String,
        videoUrl: String,
        thumbnailKey: String,
        format: String,
        visibility: String,
        category: String,
        languageCode: String,
        countryCode: String,
        hashtagsRaw: String,
        durationSec: Long,
        maxResolution: String,
        musicTitle: String,
        musicLicenseId: String,
        chaptersSummary: String,
        subtitlesText: String,
        monetized: Boolean,
        scheduledPublishAtMs: Long
    ) -> Unit,
    onPauseUpload: () -> Unit,
    onResumeUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onResetUpload: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("LONG") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var category by remember { mutableStateOf("Technology") }
    var languageCode by remember { mutableStateOf(user.languageCode) }
    var countryCode by remember { mutableStateOf(user.countryCode) }
    var resolution by remember { mutableStateOf("4K") }
    var hashtagsRaw by remember { mutableStateOf("#BharatTube #CreatorIndia") }
    var chaptersSummary by remember { mutableStateOf("0:00 Intro | 0:30 Main Story | 1:15 Summary") }
    var subtitlesText by remember { mutableStateOf("Welcome to BharatTube global creator network.") }
    var selectedSoundIdx by remember { mutableStateOf(0) }
    var monetized by remember { mutableStateOf(!user.minorMode) }
    var simulateDisconnect by remember { mutableStateOf(false) }
    var pickedVideoName by remember { mutableStateOf("creator_master_4k.mp4") }
    var pickedVideoUri by remember { mutableStateOf(GlobalCatalog.STREAM_FUN) }
    var selectedThumbnailKey by remember { mutableStateOf("img_thumb_tech") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri.toString()
            pickedVideoName = uri.lastPathSegment?.take(28)?.ifBlank { "picked_video.mp4" } ?: "picked_video.mp4"
        }
    }

    val thumbPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedThumbnailKey = "img_hero_bharattube"
        }
    }

    val selectedSound = GlobalCatalog.licensedSounds[selectedSoundIdx]
    val fingerprintPreview = remember(title, selectedSound.title, pickedVideoUri, existingVideos) {
        CopyrightFingerprintEngine.scanUpload(
            title = title,
            musicTitle = selectedSound.title,
            mediaUri = pickedVideoUri,
            durationSec = if (format == "SHORT") 45L else 240L,
            existingHashes = existingVideos.map { it.copyrightHash }.toSet()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Creator Upload Studio", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Chunked resumable uploads, auto-retry, HLS/MP4 transcoding & 50:50 monetization.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!config.uploadsEnabled || user.uploadRestricted) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f))) {
                    Text(
                        text = "Uploads are currently disabled by Super Admin or restricted on your account.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            return@LazyColumn
        }

        // Live Chunked Upload Progress Card
        if (uploadState.status != UploadStatus.IDLE) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.testTag("upload_progress_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(uploadState.fileName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${uploadState.percentage}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = SaffronPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { uploadState.percentage / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (uploadState.status == UploadStatus.COMPLETED) PeacockTeal else SaffronPrimary
                        )
                        Text(uploadState.statusMessage, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${String.format("%.1f", uploadState.uploadedMb)} / ${String.format("%.1f", uploadState.totalMb)} MB • " +
                                "${String.format("%.1f", uploadState.speedMbps)} MB/s • ETA ${uploadState.remainingSeconds}s • Retries: ${uploadState.retryCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (uploadState.status == UploadStatus.UPLOADING) {
                                OutlinedButton(
                                    onClick = onPauseUpload,
                                    modifier = Modifier.testTag("pause_upload_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            } else if (uploadState.status == UploadStatus.PAUSED || uploadState.status == UploadStatus.FAILED) {
                                Button(
                                    onClick = onResumeUpload,
                                    modifier = Modifier.testTag("resume_upload_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }
                            if (uploadState.status != UploadStatus.COMPLETED && uploadState.status != UploadStatus.CANCELLED) {
                                OutlinedButton(
                                    onClick = onCancelUpload,
                                    modifier = Modifier.testTag("cancel_upload_button")
                                ) {
                                    Text("Cancel")
                                }
                            } else {
                                Button(onClick = onResetUpload) { Text("Upload Another") }
                            }
                        }
                    }
                }
            }
        }

        // Photo Picker Media + Thumbnail Selection (Zero Broad Storage Permission)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_video_button")
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = "Pick video")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Video", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        thumbPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_thumbnail_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Pick thumbnail")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thumbnail", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Selected source: $pickedVideoName",
                style = MaterialTheme.typography.labelSmall,
                color = PeacockTeal
            )
        }

        // Format & Resolution & Visibility
        item {
            Text("Format & Quality", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("LONG" to "Long Video", "SHORT" to "Short / Reel")) { (code, label) ->
                    FilterChip(
                        selected = format == code,
                        onClick = { format = code },
                        label = { Text(label) },
                        modifier = Modifier.testTag("upload_format_$code")
                    )
                }
                items(listOf("4K", "1080p", "720p")) { res ->
                    FilterChip(
                        selected = resolution == res,
                        onClick = { resolution = res },
                        label = { Text(res) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Visibility", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("PUBLIC", "UNLISTED", "PRIVATE", "DRAFT", "SCHEDULED")) { vis ->
                    FilterChip(
                        selected = visibility == vis,
                        onClick = { visibility = vis },
                        label = { Text(vis) },
                        modifier = Modifier.testTag("upload_visibility_$vis")
                    )
                }
            }
        }

        // Metadata Fields
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_description_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = hashtagsRaw,
                onValueChange = { hashtagsRaw = it },
                label = { Text("Hashtags (space or comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (format == "LONG") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = chaptersSummary,
                    onValueChange = { chaptersSummary = it },
                    label = { Text("Chapters (e.g. 0:00 Intro | 0:45 Demo)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = subtitlesText,
                onValueChange = { subtitlesText = it },
                label = { Text("Captions / Subtitles Line") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Category, Language & Licensed Sound
        item {
            Text("Category", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.categories.drop(1)) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Licensed Sound Library", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.licensedSounds.indices.toList()) { idx ->
                    val track = GlobalCatalog.licensedSounds[idx]
                    FilterChip(
                        selected = selectedSoundIdx == idx,
                        onClick = { selectedSoundIdx = idx },
                        label = { Text("${track.title} (${track.licenseId})") }
                    )
                }
            }
        }

        // Copyright Fingerprint Pre-Scan Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (fingerprintPreview.similarityDetected) GoldHighlight.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Copyright fingerprint check",
                        tint = if (fingerprintPreview.similarityDetected) GoldHighlight else PeacockTeal
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Fingerprint Hash: ${fingerprintPreview.contentHash}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = fingerprintPreview.advisoryMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Monetization & Network Retry Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable 50:50 Ad Monetization", style = MaterialTheme.typography.titleSmall)
                    Text("50% Creator / 50% BharatTube net ad split", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = monetized && !user.minorMode,
                    onCheckedChange = { monetized = it },
                    enabled = !user.minorMode
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Disconnect Auto-Retry", style = MaterialTheme.typography.titleSmall)
                    Text("Simulates network drop at 50% and resumes automatically", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = simulateDisconnect,
                    onCheckedChange = { simulateDisconnect = it },
                    modifier = Modifier.testTag("simulate_disconnect_switch")
                )
            }
        }

        // Publish Button
        item {
            Button(
                onClick = {
                    val safeTitle = title.trim().ifBlank { "BharatTube ${category} Showcase" }
                    val playbackStream = if (pickedVideoUri.startsWith("http")) pickedVideoUri else GlobalCatalog.STREAM_FUN
                    onStartUpload(
                        pickedVideoName,
                        if (format == "SHORT") 36.0 else 128.0,
                        simulateDisconnect,
                        safeTitle,
                        description.ifBlank { "Uploaded on BharatTube in $resolution ($languageCode)." },
                        playbackStream,
                        selectedThumbnailKey,
                        format,
                        visibility,
                        category,
                        languageCode,
                        countryCode,
                        hashtagsRaw,
                        if (format == "SHORT") 45L else 240L,
                        resolution,
                        selectedSound.title,
                        selectedSound.licenseId,
                        chaptersSummary,
                        subtitlesText,
                        monetized,
                        if (visibility == "SCHEDULED") System.currentTimeMillis() + 86_400_000L else 0L
                    )
                },
                enabled = uploadState.status != UploadStatus.UPLOADING && uploadState.status != UploadStatus.TRANSCODING,
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_upload_button")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Resumable Upload & Publish", color = Color.White)
            }
        }
    }
}
