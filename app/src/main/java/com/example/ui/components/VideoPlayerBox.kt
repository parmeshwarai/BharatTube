package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.AdCampaign
import com.example.data.model.Video
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun BharatTubeVideoPlayer(
    video: Video,
    startPositionMs: Long,
    preRollAd: AdCampaign?,
    midRollAd: AdCampaign?,
    captionsText: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onSavePosition: (positionMs: Long, durationMs: Long) -> Unit,
    onAdCompleted: (campaign: AdCampaign, isValidView: Boolean, isClick: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(video.id, video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            prepare()
            if (startPositionMs > 0L) seekTo(startPositionMs)
            playWhenReady = preRollAd == null
        }
    }

    var showingPreRoll by remember(video.id) { mutableStateOf(preRollAd != null) }
    var preRollCountdown by remember(video.id) { mutableIntStateOf(5) }
    var showingMidRoll by remember(video.id) { mutableStateOf(false) }
    var midRollTriggered by remember(video.id) { mutableStateOf(false) }
    var midRollCountdown by remember(video.id) { mutableIntStateOf(5) }

    var showCaptions by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf(video.maxResolution) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var qualityMenuExpanded by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(1L)
            onSavePosition(pos, dur)
            exoPlayer.release()
        }
    }

    // Pre-roll countdown & valid-view tracking
    LaunchedEffect(showingPreRoll, video.id) {
        if (showingPreRoll && preRollAd != null) {
            exoPlayer.pause()
            preRollCountdown = 5
            while (preRollCountdown > 0 && showingPreRoll) {
                delay(1000L)
                preRollCountdown--
            }
            if (showingPreRoll) {
                onAdCompleted(preRollAd, true, false)
            }
        } else {
            if (!showingMidRoll) exoPlayer.play()
        }
    }

    // Mid-roll trigger & periodic resume position save
    LaunchedEffect(exoPlayer, video.id) {
        while (true) {
            delay(2000L)
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0L) {
                    onSavePosition(pos, dur)
                    if (midRollAd != null && !midRollTriggered && video.format == "LONG" && pos >= dur / 2) {
                        midRollTriggered = true
                        showingMidRoll = true
                        midRollCountdown = 5
                        exoPlayer.pause()
                    }
                }
            }
        }
    }

    LaunchedEffect(showingMidRoll) {
        if (showingMidRoll && midRollAd != null) {
            while (midRollCountdown > 0 && showingMidRoll) {
                delay(1000L)
                midRollCountdown--
            }
            if (showingMidRoll) {
                onAdCompleted(midRollAd, true, false)
            }
        }
    }

    val chapters = remember(video.chaptersSummary) {
        video.chaptersSummary.split("|")
            .mapNotNull { segment ->
                val trimmed = segment.trim()
                val spaceIdx = trimmed.indexOf(' ')
                if (spaceIdx > 0) {
                    val timePart = trimmed.substring(0, spaceIdx)
                    val labelPart = trimmed.substring(spaceIdx + 1)
                    val parts = timePart.split(":")
                    val sec = if (parts.size == 2) {
                        (parts[0].toLongOrNull() ?: 0L) * 60L + (parts[1].toLongOrNull() ?: 0L)
                    } else 0L
                    (sec * 1000L) to "$timePart $labelPart"
                } else null
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 320.dp else 228.dp)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )

            // Live Captions / Subtitles overlay
            if (showCaptions && captionsText.isNotBlank() && !showingPreRoll && !showingMidRoll) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 48.dp)
                ) {
                    Text(
                        text = captionsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Pre-Roll or Mid-Roll Ad Overlay
            val activeAd = when {
                showingPreRoll -> preRollAd
                showingMidRoll -> midRollAd
                else -> null
            }
            val activeCountdown = if (showingPreRoll) preRollCountdown else midRollCountdown
            if (activeAd != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE0B0E14))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = GoldHighlight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (showingPreRoll) "PRE-ROLL AD" else "MID-ROLL AD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeAd.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sponsored by ${activeAd.advertiserName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onAdCompleted(activeAd, true, true)
                                    showingPreRoll = false
                                    showingMidRoll = false
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PeacockTeal),
                                modifier = Modifier.testTag("player_ad_cta")
                            ) {
                                Text(activeAd.ctaText, color = Color.Black)
                            }
                            Button(
                                onClick = {
                                    val validView = activeCountdown == 0
                                    onAdCompleted(activeAd, validView, false)
                                    showingPreRoll = false
                                    showingMidRoll = false
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeCountdown == 0) SaffronPrimary else Color.DarkGray
                                ),
                                modifier = Modifier.testTag("skip_ad_button")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip ad")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (activeCountdown > 0) "Skip in ${activeCountdown}s" else "Skip Ad"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Player Controls Bar: Quality, Speed, CC, Fullscreen
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(
                            onClick = { speedMenuExpanded = true },
                            modifier = Modifier.testTag("speed_selector_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Playback speed", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${playbackSpeed}x", style = MaterialTheme.typography.labelLarge)
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        playbackSpeed = spd
                                        exoPlayer.setPlaybackSpeed(spd)
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        TextButton(
                            onClick = { qualityMenuExpanded = true },
                            modifier = Modifier.testTag("quality_selector_button")
                        ) {
                            Icon(Icons.Default.HighQuality, contentDescription = "Video quality", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedQuality, style = MaterialTheme.typography.labelLarge)
                        }
                        DropdownMenu(
                            expanded = qualityMenuExpanded,
                            onDismissRequest = { qualityMenuExpanded = false }
                        ) {
                            listOf("4K", "1080p", "720p", "480p", "Auto").forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q) },
                                    onClick = {
                                        selectedQuality = q
                                        qualityMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showCaptions = !showCaptions },
                        modifier = Modifier.testTag("captions_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Toggle captions",
                            tint = if (showCaptions) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen"
                    )
                }
            }
        }

        // Clickable Video Chapters
        if (chapters.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters) { (seekMs, label) ->
                    AssistChip(
                        onClick = { exoPlayer.seekTo(seekMs) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
    }
}
