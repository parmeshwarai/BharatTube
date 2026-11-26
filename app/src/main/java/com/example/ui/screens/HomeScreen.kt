package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.BharatTubeRepository
import com.example.data.model.AdCampaign
import com.example.data.model.Channel
import com.example.data.model.GlobalCatalog
import com.example.data.model.LiveStream
import com.example.data.model.PlatformConfig
import com.example.data.model.SubscriptionItem
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.WatchHistoryItem
import com.example.domain.MonetizationEngine
import com.example.ui.components.BharatTubeAvatar
import com.example.ui.components.BharatTubeLogoMark
import com.example.ui.components.SectionHeader
import com.example.ui.components.SponsoredAdCard
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.channelAccentColor
import com.example.ui.components.formatCompactCount
import com.example.ui.components.resolveThumbnailDrawable
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@Composable
fun HomeScreen(
    user: UserProfile,
    config: PlatformConfig,
    recommendedVideos: List<Video>,
    allVideos: List<Video>,
    channels: List<Channel>,
    myChannels: List<Channel>,
    activeChannel: Channel?,
    liveStreams: List<LiveStream>,
    subscriptions: List<SubscriptionItem>,
    watchHistory: List<WatchHistoryItem>,
    activeAds: List<AdCampaign>,
    unreadNotifCount: Int,
    aiInsight: String?,
    aiInsightLoading: Boolean,
    isAdmin: Boolean,
    onRefreshAiInsight: () -> Unit,
    onCompleteOnboarding: (String, String, String, String) -> Unit,
    onSwitchActiveChannel: (String) -> Unit,
    onOpenCreateChannel: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenShorts: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLiveStream: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onSponsoredAdClick: (AdCampaign, Video) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var regionalFilterOnly by remember { mutableStateOf(false) }

    val subIds = remember(subscriptions) {
        subscriptions.filter { it.subscribed }.map { it.channelId }.toSet()
    }

    val longVideos = remember(recommendedVideos, selectedCategory, regionalFilterOnly, user.countryCode, user.languageCode) {
        recommendedVideos.filter { v ->
            v.format == "LONG" &&
                (selectedCategory == "All" || v.category.equals(selectedCategory, ignoreCase = true)) &&
                (!regionalFilterOnly || v.countryCode.equals(user.countryCode, true) || v.languageCode.equals(user.languageCode, true))
        }
    }
    val shortsPreview = remember(allVideos) { allVideos.filter { it.format == "SHORT" && it.visibility == "PUBLIC" } }
    val subscribedVideos = remember(allVideos, subIds) {
        allVideos.filter { it.creatorId in subIds && it.visibility == "PUBLIC" }
    }
    val trendingVideos = remember(allVideos) {
        allVideos.filter { it.visibility == "PUBLIC" }.sortedByDescending { it.viewCount }
    }
    val homeSponsoredAd = remember(activeAds, config, user.countryCode) {
        if (MonetizationEngine.isAdEligibleForViewer(config, user.countryCode, "HOME_SPONSORED")) {
            activeAds.firstOrNull { it.placement == "HOME_SPONSORED" }
                ?: activeAds.firstOrNull()
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Brand Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BharatTubeLogoMark()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        IconButton(
                            onClick = onOpenAdmin,
                            modifier = Modifier.testTag("home_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Super Admin Dashboard",
                                tint = GoldHighlight
                            )
                        }
                    }
                    if (config.searchEnabled) {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search BharatTube")
                        }
                    }
                    if (config.notificationsEnabled) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("home_notifications_button")
                        ) {
                            Box {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                if (unreadNotifCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(SaffronPrimary)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Announcement Banner
        if (config.announcementText.isNotBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Platform announcement",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = config.announcementText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Google Account Onboarding Card (shown after first Google sign-in until completed)
        if (!user.onboardingCompleted && user.userId.isNotBlank()) {
            item {
                var onboardingName by remember(user.displayName) { mutableStateOf(user.displayName) }
                var onboardingPhoto by remember(user.photoUrl) { mutableStateOf(user.photoUrl) }
                var onboardingCountry by remember(user.countryCode) { mutableStateOf(user.countryCode) }
                var onboardingLang by remember(user.languageCode) { mutableStateOf(user.languageCode) }
                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) onboardingPhoto = uri.toString()
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.testTag("home_onboarding_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BharatTubeAvatar(
                                name = onboardingName.ifBlank { "Creator" },
                                photoUrl = onboardingPhoto,
                                size = 46.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Welcome! Finish Your BharatTube Setup", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "ID: ${user.bharatTubeId.ifBlank { user.userId.take(10) }} • ${user.email.ifBlank { "Google Account" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PeacockTeal
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Profile photo", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Photo")
                            }
                        }
                        OutlinedTextField(
                            value = onboardingName,
                            onValueChange = { onboardingName = it },
                            label = { Text("BharatTube Profile Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Country & Preferred Language", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(GlobalCatalog.countries.take(6)) { c ->
                                FilterChip(
                                    selected = onboardingCountry == c.code,
                                    onClick = { onboardingCountry = c.code },
                                    label = { Text("${c.name} (${c.currencySymbol})") }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(GlobalCatalog.starterLanguages.take(10)) { lang ->
                                FilterChip(
                                    selected = onboardingLang == lang.code,
                                    onClick = { onboardingLang = lang.code },
                                    label = { Text(lang.nativeName) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    onCompleteOnboarding(onboardingName, onboardingPhoto, onboardingCountry, onboardingLang)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Profile")
                            }
                            Button(
                                onClick = {
                                    onCompleteOnboarding(onboardingName, onboardingPhoto, onboardingCountry, onboardingLang)
                                    onOpenCreateChannel()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create Channel", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Create Your Channel & Multi-Channel Quick Switcher Bar
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BharatTubeAvatar(
                                name = activeChannel?.name ?: user.displayName,
                                photoUrl = activeChannel?.avatarUrl?.ifBlank { user.photoUrl } ?: user.photoUrl,
                                fallbackKey = activeChannel?.avatarKey ?: "",
                                accentColor = channelAccentColor(activeChannel?.appearanceTheme ?: "SAFFRON"),
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activeChannel?.name ?: "Launch Your Creator Channel",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activeChannel != null) {
                                        "${activeChannel.handle} • ${myChannels.size}/${BharatTubeRepository.MAX_CHANNELS_PER_ACCOUNT} channels"
                                    } else {
                                        "Pick a unique @handle, banner & category"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PeacockTeal
                                )
                            }
                        }
                        Button(
                            onClick = onOpenCreateChannel,
                            enabled = myChannels.size < BharatTubeRepository.MAX_CHANNELS_PER_ACCOUNT,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.testTag("home_create_channel_button")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Your Channel", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (myChannels.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(myChannels, key = { it.id }) { ch ->
                                val isSelected = ch.id == activeChannel?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSwitchActiveChannel(ch.id) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Active", modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    label = { Text(ch.handle) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Featured Hero Card + Regional Toggle
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable {
                        longVideos.firstOrNull()?.let { onOpenVideo(it.id) }
                    }
                    .testTag("home_hero_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_bharattube),
                    contentDescription = "Featured BharatTube spotlight",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.25f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${GlobalCatalog.countryByCode(user.countryCode).name.uppercase()} • ${GlobalCatalog.languageByCode(user.languageCode).nativeName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PeacockTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = longVideos.firstOrNull()?.title ?: "Discover Global Creators on BharatTube",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = SaffronPrimary, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Watch Now",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onOpenShorts() }
                        ) {
                            Text(
                                text = "Open Shorts",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Intelligent Gemini Recommendation Curator
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Personalization",
                                tint = PeacockTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "For You • Smart Feed Curator",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        TextButton(
                            onClick = onRefreshAiInsight,
                            enabled = !aiInsightLoading,
                            modifier = Modifier.testTag("refresh_ai_curator_button")
                        ) {
                            if (aiInsightLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Refresh")
                            }
                        }
                    }
                    Text(
                        text = aiInsight
                            ?: "Prioritizing ${user.interests.joinToString(", ")} in ${GlobalCatalog.languageByCode(user.languageCode).englishName} (${user.countryCode}). Tap Refresh for Gemini personalization insight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category & Country/Language Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = regionalFilterOnly,
                        onClick = { regionalFilterOnly = !regionalFilterOnly },
                        label = {
                            Text("${user.countryCode} / ${GlobalCatalog.languageByCode(user.languageCode).nativeName}")
                        },
                        modifier = Modifier.testTag("regional_filter_chip")
                    )
                }
                items(GlobalCatalog.categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("category_chip_$cat")
                    )
                }
            }
        }

        // Sponsored Home Ad Placement
        if (homeSponsoredAd != null && allVideos.isNotEmpty()) {
            item {
                SponsoredAdCard(
                    campaign = homeSponsoredAd,
                    onAdClick = {
                        onSponsoredAdClick(homeSponsoredAd, allVideos.first())
                    }
                )
            }
        }

        // Continue Watching
        if (watchHistory.isNotEmpty()) {
            item {
                SectionHeader(title = "Continue Watching", subtitle = "Resume right where you paused")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(watchHistory.take(6)) { item ->
                        val progress = (item.positionMs.toFloat() / item.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onOpenVideo(item.videoId) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = resolveThumbnailDrawable(item.thumbnailKey)),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SaffronPrimary
                                )
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.creatorName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Streams Strip
        if (config.liveStreamingEnabled && liveStreams.isNotEmpty()) {
            item {
                SectionHeader(title = "Live Now", subtitle = "Real-time broadcasts with live chat")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(liveStreams) { stream ->
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .clickable { onOpenLiveStream(stream.id) }
                                .testTag("live_stream_card_${stream.id}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(6.dp)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.LiveTv,
                                                contentDescription = "Live",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${stream.viewerCount} watching",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PeacockTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stream.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stream.creatorName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shorts & Reels Preview Strip
        if (config.shortsEnabled && shortsPreview.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Shorts & Reels",
                    subtitle = "Swipe vertical short stories",
                    trailing = {
                        TextButton(onClick = onOpenShorts) { Text("Watch All") }
                    }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shortsPreview) { shortItem ->
                        Box(
                            modifier = Modifier
                                .width(136.dp)
                                .height(210.dp)
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onOpenShorts() }
                        ) {
                            Image(
                                painter = painterResource(id = resolveThumbnailDrawable(shortItem.thumbnailKey)),
                                contentDescription = shortItem.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play short",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(36.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = shortItem.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatCompactCount(shortItem.viewCount)} views",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PeacockTeal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popular Creators
        if (channels.isNotEmpty()) {
            item {
                SectionHeader(title = "Popular Creators", subtitle = "Follow channels worldwide")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(channels) { channel ->
                        Card(
                            modifier = Modifier
                                .width(156.dp)
                                .clickable { onOpenChannel(channel.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SaffronPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channel.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = SaffronPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (channel.verified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = PeacockTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatCompactCount(channel.subscriberCount)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subscribed Creators' Latest
        if (subscribedVideos.isNotEmpty()) {
            item {
                SectionHeader(title = "From Subscriptions", subtitle = "Latest uploads from channels you follow")
            }
            items(subscribedVideos.take(3)) { vid ->
                VideoFeedCard(video = vid, onClick = { onOpenVideo(vid.id) })
            }
        }

        // Recommended & Trending Long Videos
        item {
            SectionHeader(
                title = if (selectedCategory == "All") "Recommended & Trending" else "$selectedCategory Videos",
                subtitle = "${longVideos.size} playable videos • Pre-roll & Mid-roll ad supported"
            )
        }
        if (!config.longVideosEnabled) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Text(
                        text = "Long videos are temporarily disabled by the platform administrator.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (longVideos.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("No videos match this filter yet.", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try clearing filters or upload a new 4K/HD video in the Upload tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(longVideos, key = { it.id }) { video ->
                VideoFeedCard(video = video, onClick = { onOpenVideo(video.id) })
            }
        }

        // Trending Snapshot (if filtered)
        if (trendingVideos.isNotEmpty() && selectedCategory == "All") {
            item {
                SectionHeader(title = "Global Trending", subtitle = "Highest engagement across regions")
            }
            items(trendingVideos.take(2), key = { "tr_${it.id}" }) { vid ->
                VideoFeedCard(video = vid, onClick = { onOpenVideo(vid.id) })
            }
        }
    }
}
