package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.TranslatedVideoText
import com.example.data.firebase.UiState
import com.example.data.model.AdCampaign
import com.example.data.model.Channel
import com.example.data.model.GlobalCatalog
import com.example.data.model.PlatformConfig
import com.example.data.model.Playlist
import com.example.data.model.SubscriptionItem
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.VideoComment
import com.example.data.model.WatchHistoryItem
import com.example.domain.MonetizationEngine
import com.example.ui.components.BharatTubeVideoPlayer
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Video?,
    user: UserProfile,
    config: PlatformConfig,
    channels: List<Channel>,
    subscriptions: List<SubscriptionItem>,
    playlists: List<Playlist>,
    watchHistory: List<WatchHistoryItem>,
    activeAds: List<AdCampaign>,
    comments: List<VideoComment>,
    translatedVideoState: UiState<TranslatedVideoText>?,
    translatedComments: Map<String, String>,
    onBack: () -> Unit,
    onRecordView: (Video) -> Unit,
    onLike: (Video) -> Unit,
    onDislike: (Video) -> Unit,
    onSavePosition: (Video, Long, Long) -> Unit,
    onToggleSubscribe: (Channel, Boolean, Boolean) -> Unit,
    onAddToWatchLater: (String) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onAddComment: (String, String, String) -> Unit,
    onTranslateVideo: (Video, String) -> Unit,
    onClearTranslation: () -> Unit,
    onTranslateComment: (String, String, String) -> Unit,
    onAdCompleted: (AdCampaign, Video, Boolean, Boolean) -> Unit,
    onReportVideo: (String, String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onModerateComment: (String, String, String) -> Unit
) {
    BackHandler { onBack() }

    if (video == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Video not found.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to Home") }
            }
        }
        return
    }

    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var replyingToId by remember { mutableStateOf("") }
    var replyingToName by remember { mutableStateOf("") }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        onRecordView(video)
    }

    val startPosMs = remember(video.id, watchHistory) {
        watchHistory.firstOrNull { it.videoId == video.id }?.positionMs ?: 0L
    }
    val preRollAd = remember(activeAds, config, user.countryCode, video.monetized) {
        if (video.monetized && MonetizationEngine.isAdEligibleForViewer(config, user.countryCode, "PRE_ROLL")) {
            activeAds.firstOrNull { it.placement == "PRE_ROLL" } ?: activeAds.firstOrNull()
        } else null
    }
    val midRollAd = remember(activeAds, config, user.countryCode, video.monetized) {
        if (video.monetized && MonetizationEngine.isAdEligibleForViewer(config, user.countryCode, "MID_ROLL")) {
            activeAds.firstOrNull { it.placement == "MID_ROLL" } ?: activeAds.lastOrNull()
        } else null
    }

    val creatorChannel = remember(channels, video) {
        channels.firstOrNull { it.ownerId == video.creatorId }
            ?: Channel(
                id = video.creatorId,
                ownerId = video.creatorId,
                name = video.creatorName,
                handle = video.creatorHandle
            )
    }
    val subItem = remember(subscriptions, creatorChannel.id) {
        subscriptions.firstOrNull { it.channelId == creatorChannel.id }
    }
    val isSubscribed = subItem?.subscribed == true
    val bellOn = subItem?.bellEnabled != false

    val translated = (translatedVideoState as? UiState.Success)?.data
    val displayTitle = translated?.title ?: video.title
    val displayDesc = translated?.description ?: video.description
    val displaySubtitles = translated?.subtitles ?: video.subtitlesText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isFullscreen) {
            TopAppBar(
                title = { Text("BharatTube Watch", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("player_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }

        BharatTubeVideoPlayer(
            video = video,
            startPositionMs = startPosMs,
            preRollAd = preRollAd,
            midRollAd = midRollAd,
            captionsText = displaySubtitles,
            isFullscreen = isFullscreen,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onSavePosition = { pos, dur -> onSavePosition(video, pos, dur) },
            onAdCompleted = { camp, valid, click -> onAdCompleted(camp, video, valid, click) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = displayTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatCompactCount(video.viewCount)} views • ${video.category} • ${video.maxResolution} • ${GlobalCatalog.languageByCode(video.languageCode).nativeName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (video.copyrightStatus == "WARNING") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = GoldHighlight.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Fingerprint similarity notice attached — creator appeal & review supported.",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldHighlight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // AI Translate metadata & captions row
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Translate, contentDescription = "AI Translate", tint = PeacockTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (translated != null) {
                                    "Translated to ${translated.targetLanguageName}"
                                } else {
                                    "Translate to ${GlobalCatalog.languageByCode(user.languageCode).nativeName} via Gemini"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        if (translatedVideoState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (translated != null) {
                            TextButton(onClick = onClearTranslation) { Text("Original") }
                        } else {
                            Button(
                                onClick = { onTranslateVideo(video, user.languageCode) },
                                modifier = Modifier.testTag("translate_video_button")
                            ) {
                                Text("Translate")
                            }
                        }
                    }
                }
            }

            // Action chips row: Like, Dislike, Watch Later, Playlist, Share, Report
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(
                            onClick = { onLike(video) },
                            leadingIcon = { Icon(Icons.Default.ThumbUp, contentDescription = "Like") },
                            label = { Text(formatCompactCount(video.likeCount)) },
                            modifier = Modifier.testTag("watch_like_button")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onDislike(video) },
                            leadingIcon = { Icon(Icons.Default.ThumbDown, contentDescription = "Dislike") },
                            label = { Text(formatCompactCount(video.dislikeCount)) },
                            modifier = Modifier.testTag("watch_dislike_button")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onAddToWatchLater(video.id) },
                            leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = "Watch Later") },
                            label = { Text("Watch Later") },
                            modifier = Modifier.testTag("watch_later_button")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showPlaylistDialog = true },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Save to Playlist") },
                            label = { Text("Playlist") }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on BharatTube!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                            label = { Text("Share") }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showReportDialog = true },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = "Report") },
                            label = { Text("Report") },
                            modifier = Modifier.testTag("watch_report_button")
                        )
                    }
                }
            }

            // Creator Channel Bar + Subscribe + Bell
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary.copy(alpha = 0.2f))
                                .clickable { onOpenChannel(creatorChannel.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = creatorChannel.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = SaffronPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenChannel(creatorChannel.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(creatorChannel.name, style = MaterialTheme.typography.titleSmall)
                                if (creatorChannel.verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = PeacockTeal, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "${formatCompactCount(creatorChannel.subscriberCount)} subscribers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSubscribed) {
                            IconButton(
                                onClick = { onToggleSubscribe(creatorChannel, true, !bellOn) },
                                modifier = Modifier.testTag("watch_bell_button")
                            ) {
                                Icon(
                                    imageVector = if (bellOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = "Notification preference",
                                    tint = if (bellOn) GoldHighlight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { onToggleSubscribe(creatorChannel, isSubscribed, bellOn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainerHighest else SaffronPrimary
                            ),
                            modifier = Modifier.testTag("watch_subscribe_button")
                        ) {
                            Text(if (isSubscribed) "Subscribed" else "Subscribe")
                        }
                    }
                }
            }

            // Description & Hashtags
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(displayDesc, style = MaterialTheme.typography.bodyMedium)
                        if (video.hashtags.isNotEmpty()) {
                            Text(
                                text = video.hashtags.joinToString(" "),
                                style = MaterialTheme.typography.labelLarge,
                                color = PeacockTeal
                            )
                        }
                    }
                }
            }

            // Comments Header & Input
            item {
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
                if (replyingToId.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Replying to $replyingToName", style = MaterialTheme.typography.labelMedium, color = PeacockTeal)
                        TextButton(onClick = { replyingToId = ""; replyingToName = "" }) { Text("Cancel") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text(if (config.commentsEnabled) "Add a public comment..." else "Comments disabled by Admin") },
                        enabled = config.commentsEnabled && !user.commentRestricted,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("watch_comment_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentDraft.isNotBlank()) {
                                onAddComment(video.id, commentDraft, replyingToId)
                                commentDraft = ""
                                replyingToId = ""
                                replyingToName = ""
                            }
                        },
                        enabled = config.commentsEnabled && !user.commentRestricted,
                        modifier = Modifier.testTag("watch_comment_send")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment")
                    }
                }
            }

            items(comments, key = { it.id }) { comment ->
                val translatedComment = translatedComments[comment.id]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (comment.parentCommentId.isNotBlank()) "${comment.authorName} (Reply)" else comment.authorName,
                                style = MaterialTheme.typography.labelLarge,
                                color = PeacockTeal
                            )
                            Row {
                                TextButton(onClick = {
                                    replyingToId = comment.id
                                    replyingToName = comment.authorName
                                }) {
                                    Text("Reply", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = {
                                    onTranslateComment(comment.id, comment.text, user.languageCode)
                                }) {
                                    Text("Translate", style = MaterialTheme.typography.labelSmall)
                                }
                                if (video.creatorId == user.userId) {
                                    TextButton(onClick = {
                                        onModerateComment(video.id, comment.id, "HIDDEN")
                                    }) {
                                        Text("Hide", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        Text(translatedComment ?: comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Save to Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    playlists.forEach { pl ->
                        OutlinedButton(
                            onClick = {
                                onAddToPlaylist(pl.id, video.id)
                                showPlaylistDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${pl.title} (${pl.videoIds.size})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Close") }
            }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Video") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Issue details (copyright, spam, abuse)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onReportVideo(video.id, reason.ifBlank { "Inappropriate content report" })
                    showReportDialog = false
                }) {
                    Text("Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}
