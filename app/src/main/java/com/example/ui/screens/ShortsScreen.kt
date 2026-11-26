package com.example.ui.screens

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.AdCampaign
import com.example.data.model.Channel
import com.example.data.model.GlobalCatalog
import com.example.data.model.PlatformConfig
import com.example.data.model.SubscriptionItem
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.VideoComment
import com.example.domain.MonetizationEngine
import com.example.ui.components.formatCompactCount
import com.example.ui.components.resolveThumbnailDrawable
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary
import kotlinx.coroutines.delay

private sealed interface ShortsFeedItem {
    data class ShortVideo(val video: Video) : ShortsFeedItem
    data class ShortsAd(val campaign: AdCampaign, val anchorVideo: Video) : ShortsFeedItem
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun ShortsScreen(
    user: UserProfile,
    config: PlatformConfig,
    videos: List<Video>,
    channels: List<Channel>,
    subscriptions: List<SubscriptionItem>,
    activeAds: List<AdCampaign>,
    comments: List<VideoComment>,
    onSelectVideoComments: (String?) -> Unit,
    onRecordView: (Video) -> Unit,
    onLikeShort: (Video) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleFollow: (Channel, Boolean) -> Unit,
    onReportShort: (String, String) -> Unit,
    onAdImpression: (AdCampaign, Video, Boolean, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit
) {
    if (!config.shortsEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Shorts & Reels are currently paused by the BharatTube Administrator.",
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    val shorts = remember(videos) {
        videos.filter { it.format == "SHORT" && it.visibility == "PUBLIC" }
    }
    if (shorts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Shorts published yet. Upload a vertical Short in the Upload tab!",
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    val adsEligible = MonetizationEngine.isAdEligibleForViewer(config, user.countryCode, "SHORTS_FEED")
    val shortsAd = remember(activeAds, adsEligible) {
        if (adsEligible) {
            activeAds.firstOrNull { it.placement == "SHORTS_FEED" } ?: activeAds.firstOrNull()
        } else null
    }
    val feedItems = remember(shorts, shortsAd, config.adFrequency) {
        val list = mutableListOf<ShortsFeedItem>()
        val freq = config.adFrequency.coerceAtLeast(1L).toInt()
        shorts.forEachIndexed { idx, video ->
            list.add(ShortsFeedItem.ShortVideo(video))
            if (shortsAd != null && (idx + 1) % freq == 0) {
                list.add(ShortsFeedItem.ShortsAd(shortsAd, video))
            }
        }
        list
    }

    val pagerState = rememberPagerState(pageCount = { feedItems.size })
    val context = LocalContext.current
    var showCaptions by remember { mutableStateOf(true) }
    var commentSheetVideo by remember { mutableStateOf<Video?>(null) }
    var reportTarget by remember { mutableStateOf<Video?>(null) }

    val subMap = remember(subscriptions) {
        subscriptions.associateBy { it.channelId }
    }
    val channelMap = remember(channels) {
        channels.associateBy { it.ownerId }
    }

    // Track views / impressions on page change
    LaunchedEffect(pagerState.currentPage, feedItems) {
        when (val item = feedItems.getOrNull(pagerState.currentPage)) {
            is ShortsFeedItem.ShortVideo -> onRecordView(item.video)
            else -> Unit
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_vertical_pager")
    ) { page ->
        val isActivePage = pagerState.currentPage == page
        when (val item = feedItems[page]) {
            is ShortsFeedItem.ShortVideo -> {
                val shortVideo = item.video
                val creatorChannel = channelMap[shortVideo.creatorId]
                    ?: Channel(
                        id = shortVideo.creatorId,
                        ownerId = shortVideo.creatorId,
                        name = shortVideo.creatorName,
                        handle = shortVideo.creatorHandle
                    )
                val isFollowing = subMap[creatorChannel.id]?.following == true ||
                    subMap[creatorChannel.id]?.subscribed == true

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isActivePage) {
                        ShortExoPlayerSurface(
                            streamUrl = shortVideo.videoUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = resolveThumbnailDrawable(shortVideo.thumbnailKey)),
                            contentDescription = shortVideo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom gradient legibility overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )

                    // Top bar with Captions & Language
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Shorts • ${GlobalCatalog.languageByCode(shortVideo.languageCode).nativeName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(
                            onClick = { showCaptions = !showCaptions },
                            modifier = Modifier.testTag("shorts_captions_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Toggle captions",
                                tint = if (showCaptions) PeacockTeal else Color.White
                            )
                        }
                    }

                    // Right action rail
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { onLikeShort(shortVideo) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("shorts_like_${shortVideo.id}")
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Like Short", tint = Color.White)
                            }
                            Text(
                                text = formatCompactCount(shortVideo.likeCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    onSelectVideoComments(shortVideo.id)
                                    commentSheetVideo = shortVideo
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("shorts_comment_${shortVideo.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Open comments", tint = Color.White)
                            }
                            Text(
                                text = formatCompactCount(shortVideo.commentCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "Watch \"${shortVideo.title}\" by ${shortVideo.creatorName} on BharatTube!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("shorts_share_${shortVideo.id}")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Short", tint = Color.White)
                        }
                        IconButton(
                            onClick = { reportTarget = shortVideo },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("shorts_report_${shortVideo.id}")
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Report Short", tint = Color.White)
                        }
                    }

                    // Bottom metadata + Captions + Music License
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(0.8f)
                            .padding(start = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showCaptions && shortVideo.subtitlesText.isNotBlank()) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.68f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = shortVideo.subtitlesText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shortVideo.creatorHandle.ifBlank { shortVideo.creatorName },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onOpenChannel(creatorChannel.id) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { onToggleFollow(creatorChannel, isFollowing) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.White.copy(alpha = 0.2f) else SaffronPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("shorts_follow_${shortVideo.id}")
                            ) {
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = shortVideo.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (shortVideo.description.isNotBlank()) {
                            Text(
                                text = shortVideo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (shortVideo.hashtags.isNotEmpty()) {
                            Text(
                                text = shortVideo.hashtags.joinToString(" "),
                                style = MaterialTheme.typography.labelMedium,
                                color = PeacockTeal
                            )
                        }
                        if (shortVideo.musicTitle.isNotBlank()) {
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Licensed music",
                                        tint = GoldHighlight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${shortVideo.musicTitle} (${shortVideo.musicLicenseId.ifBlank { "Licensed" }})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        }
                    }
                }
            }

            is ShortsFeedItem.ShortsAd -> {
                ShortsSponsoredAdCard(
                    campaign = item.campaign,
                    isActive = isActivePage,
                    onValidAdView = { clicked ->
                        onAdImpression(item.campaign, item.anchorVideo, true, clicked)
                    }
                )
            }
        }
    }

    // Comments Bottom Sheet
    commentSheetVideo?.let { target ->
        var commentText by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = {
                commentSheetVideo = null
                onSelectVideoComments(null)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Comments • ${target.title}", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shorts_comment_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(target.id, commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier.testTag("shorts_send_comment")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelLarge, color = PeacockTeal)
                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { target ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report Short") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Report \"${target.title}\" for policy review:")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (spam, copyright, safety)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReportShort(target.id, reason.ifBlank { "Community policy concern" })
                    reportTarget = null
                }) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortExoPlayerSurface(streamUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view -> if (view.player != player) view.player = player },
        modifier = modifier
    )
}

@Composable
private fun ShortsSponsoredAdCard(
    campaign: AdCampaign,
    isActive: Boolean,
    onValidAdView: (Boolean) -> Unit
) {
    var secondsViewed by remember(campaign.id, isActive) { mutableIntStateOf(0) }
    var reported by remember(campaign.id, isActive) { mutableStateOf(false) }

    LaunchedEffect(isActive, campaign.id) {
        if (isActive && !reported) {
            while (secondsViewed < 3) {
                delay(1000L)
                secondsViewed++
            }
            reported = true
            onValidAdView(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF141B2D), Color(0xFF0A0D14))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Sponsored Short Ad",
                tint = GoldHighlight,
                modifier = Modifier.size(56.dp)
            )
            Surface(color = GoldHighlight, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "SHORTS SPONSORED AD • 50:50 CREATOR REVENUE SHARE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = campaign.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                text = "Advertiser: ${campaign.advertiserName}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
            Text(
                text = if (secondsViewed >= 3) "Valid ad view credited to Creator & BharatTube!" else "Counting valid view (${3 - secondsViewed}s)...",
                style = MaterialTheme.typography.labelMedium,
                color = PeacockTeal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onValidAdView(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.testTag("shorts_ad_cta")
                ) {
                    Text(campaign.ctaText, color = Color.White)
                }
                Button(
                    onClick = { onValidAdView(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Swipe up to continue")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Swipe Up")
                }
            }
        }
    }
}
