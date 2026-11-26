package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.R
import com.example.data.firebase.BharatTubeRepository
import com.example.data.model.Channel
import com.example.data.model.SubscriptionItem
import com.example.data.model.Video
import com.example.ui.components.BharatTubeAvatar
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.channelAccentColor
import com.example.ui.components.formatCompactCount
import com.example.ui.components.resolveThumbnailDrawable
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.CreateChannelScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.BharatTubeViewModel

const val ROUTE_HOME = "home"
const val ROUTE_SHORTS = "shorts"
const val ROUTE_UPLOAD = "upload"
const val ROUTE_STUDIO = "studio"
const val ROUTE_HUB = "hub"
const val ROUTE_WATCH = "watch"
const val ROUTE_SEARCH = "search"
const val ROUTE_CHANNEL = "channel"
const val ROUTE_CREATE_CHANNEL = "create_channel"
const val ROUTE_ADMIN = "admin"

private data class TopLevelNavDest(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelNavDest(ROUTE_HOME, "Home", Icons.Default.Home),
    TopLevelNavDest(ROUTE_SHORTS, "Shorts", Icons.Default.PlayCircle),
    TopLevelNavDest(ROUTE_UPLOAD, "Upload", Icons.Default.AddCircle),
    TopLevelNavDest(ROUTE_STUDIO, "Studio", Icons.Default.Analytics),
    TopLevelNavDest(ROUTE_HUB, "Hub", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun BharatTubeApp(
    viewModel: BharatTubeViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val widthSizeClass = if (activity != null) {
        calculateWindowSizeClass(activity).widthSizeClass
    } else {
        WindowWidthSizeClass.Compact
    }
    val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val config by viewModel.platformConfig.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val publicVideos by viewModel.publicVideos.collectAsStateWithLifecycle()
    val myVideos by viewModel.myVideos.collectAsStateWithLifecycle()
    val recommendedVideos by viewModel.recommendedVideos.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val myChannels by viewModel.myChannels.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val handleCheckState by viewModel.handleCheckState.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val activeAds by viewModel.activeAdCampaigns.collectAsStateWithLifecycle()
    val myAds by viewModel.myAdCampaigns.collectAsStateWithLifecycle()
    val revenueLedger by viewModel.revenueLedger.collectAsStateWithLifecycle()
    val payouts by viewModel.payouts.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val chatMessages by viewModel.selectedChatMessages.collectAsStateWithLifecycle()
    val liveMessages by viewModel.selectedLiveMessages.collectAsStateWithLifecycle()
    val comments by viewModel.selectedVideoComments.collectAsStateWithLifecycle()
    val copyrightClaims by viewModel.copyrightClaims.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val aiInsight by viewModel.aiInsight.collectAsStateWithLifecycle()
    val aiInsightLoading by viewModel.aiInsightLoading.collectAsStateWithLifecycle()
    val translatedVideo by viewModel.translatedVideo.collectAsStateWithLifecycle()
    val translatedComments by viewModel.translatedComments.collectAsStateWithLifecycle()

    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()
    val adminClaims by viewModel.adminCopyrightClaims.collectAsStateWithLifecycle()
    val adminTickets by viewModel.adminSupportTickets.collectAsStateWithLifecycle()
    val adminLedger by viewModel.adminRevenueLedger.collectAsStateWithLifecycle()
    val adminPayouts by viewModel.adminPayouts.collectAsStateWithLifecycle()

    val unreadNotifCount = remember(notifications) { notifications.count { !it.read } }

    // Account Ban / Suspension Gate
    if (user.accountStatus in listOf("SUSPENDED", "BANNED_TEMP", "BANNED_PERM")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Account ${user.accountStatus}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                Text(
                    text = "Your account has been restricted by BharatTube moderation. Contact support or sign out.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onSignOut) { Text("Sign Out") }
            }
        }
        return
    }

    // Platform Maintenance Gate (Non-Admins)
    if (config.maintenanceMode && !viewModel.isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BharatTube Maintenance", style = MaterialTheme.typography.headlineMedium, color = SaffronPrimary)
                Text(
                    text = "BharatTube is undergoing scheduled global maintenance. Please check back shortly.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onSignOut) { Text("Sign Out") }
            }
        }
        return
    }

    val isTopLevelRoute = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!useNavRail && isTopLevelRoute) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) },
                            modifier = Modifier.testTag("nav_bar_${dest.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail && isTopLevelRoute) {
                NavigationRail {
                    topLevelDestinations.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        user = user,
                        config = config,
                        recommendedVideos = recommendedVideos,
                        allVideos = publicVideos,
                        channels = channels,
                        myChannels = myChannels,
                        activeChannel = activeChannel,
                        liveStreams = liveStreams,
                        subscriptions = subscriptions,
                        watchHistory = watchHistory,
                        activeAds = activeAds,
                        unreadNotifCount = unreadNotifCount,
                        aiInsight = aiInsight,
                        aiInsightLoading = aiInsightLoading,
                        isAdmin = viewModel.isAdmin,
                        onRefreshAiInsight = viewModel::refreshAiRecommendationInsight,
                        onCompleteOnboarding = viewModel::completeAccountOnboarding,
                        onSwitchActiveChannel = viewModel::switchActiveChannel,
                        onOpenCreateChannel = { navController.navigate(ROUTE_CREATE_CHANNEL) },
                        onOpenVideo = { vid ->
                            viewModel.selectVideoForWatch(vid)
                            navController.navigate("$ROUTE_WATCH/$vid")
                        },
                        onOpenShorts = { navController.navigate(ROUTE_SHORTS) },
                        onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                        onOpenNotifications = { navController.navigate(ROUTE_HUB) },
                        onOpenLiveStream = { sid ->
                            viewModel.selectLiveStream(sid)
                            navController.navigate(ROUTE_HUB)
                        },
                        onOpenChannel = { cid -> navController.navigate("$ROUTE_CHANNEL/$cid") },
                        onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                        onSponsoredAdClick = { camp, vid ->
                            viewModel.recordAdView(camp, vid, isValidView = true, isClick = true)
                        }
                    )
                }

                composable(ROUTE_SHORTS) {
                    ShortsScreen(
                        user = user,
                        config = config,
                        videos = publicVideos,
                        channels = channels,
                        subscriptions = subscriptions,
                        activeAds = activeAds,
                        comments = comments,
                        onSelectVideoComments = viewModel::selectVideoForWatch,
                        onRecordView = viewModel::recordView,
                        onLikeShort = viewModel::likeVideo,
                        onAddComment = { vid, txt -> viewModel.addComment(vid, txt) },
                        onToggleFollow = { ch, following ->
                            viewModel.toggleSubscription(ch, following, true)
                        },
                        onReportShort = { vid, reason ->
                            viewModel.submitSupportTicket("CONTENT_REPORT", "Reported Short $vid", reason, vid)
                        },
                        onAdImpression = { camp, vid, valid, click ->
                            viewModel.recordAdView(camp, vid, valid, click)
                        },
                        onOpenChannel = { cid -> navController.navigate("$ROUTE_CHANNEL/$cid") }
                    )
                }

                composable(ROUTE_UPLOAD) {
                    UploadScreen(
                        user = user,
                        config = config,
                        existingVideos = publicVideos + myVideos,
                        uploadState = uploadState,
                        onStartUpload = viewModel::startResumableUpload,
                        onPauseUpload = viewModel::pauseUpload,
                        onResumeUpload = viewModel::resumeUpload,
                        onCancelUpload = viewModel::cancelUpload,
                        onResetUpload = viewModel::resetUpload
                    )
                }

                composable(ROUTE_STUDIO) {
                    StudioScreen(
                        user = user,
                        config = config,
                        myVideos = myVideos,
                        myChannels = myChannels,
                        activeChannel = activeChannel,
                        revenueLedger = revenueLedger,
                        payouts = payouts,
                        myCampaigns = myAds,
                        copyrightClaims = copyrightClaims,
                        onSwitchActiveChannel = viewModel::switchActiveChannel,
                        onOpenCreateChannel = { navController.navigate(ROUTE_CREATE_CHANNEL) },
                        onSettleSampleAdRevenue = viewModel::settleSampleAdRevenueAndCheckPayout,
                        onTriggerPayoutCheck = viewModel::triggerManualAutoPayoutCheck,
                        onRetryPayout = viewModel::retryFailedPayout,
                        onUpdatePaymentSettings = { map, msg -> viewModel.updateUserSettings(map, msg) },
                        onCreateAdCampaign = viewModel::createAdCampaign,
                        onToggleCampaignStatus = viewModel::toggleCampaignStatus,
                        onAppealCopyright = viewModel::appealCopyright,
                        onUpdateVideoMetadata = viewModel::updateVideoMetadata
                    )
                }

                composable(ROUTE_HUB) {
                    CommunityHubScreen(
                        user = user,
                        config = config,
                        isAdmin = viewModel.isAdmin,
                        myChannels = myChannels,
                        activeChannel = activeChannel,
                        myVideos = myVideos,
                        subscriptions = subscriptions,
                        watchHistory = watchHistory,
                        playlists = playlists,
                        liveStreams = liveStreams,
                        liveMessages = liveMessages,
                        chats = chats,
                        chatMessages = chatMessages,
                        notifications = notifications,
                        sessions = sessions,
                        supportTickets = supportTickets,
                        onOpenVideo = { vid ->
                            viewModel.selectVideoForWatch(vid)
                            navController.navigate("$ROUTE_WATCH/$vid")
                        },
                        onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                        onSwitchChannel = viewModel::switchActiveChannel,
                        onOpenCreateChannel = { navController.navigate(ROUTE_CREATE_CHANNEL) },
                        onOpenCustomizeChannel = { ch ->
                            navController.navigate("$ROUTE_CREATE_CHANNEL?channelId=${ch.id}")
                        },
                        onVerifyOwnership = viewModel::verifyChannelOwnership,
                        onCreatePlaylist = viewModel::createPlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onClearHistory = viewModel::clearWatchHistory,
                        onUpdateUserSettings = viewModel::updateUserSettings,
                        onStartLive = viewModel::startLiveStream,
                        onSelectLive = viewModel::selectLiveStream,
                        onSendLiveMessage = viewModel::sendLiveMessage,
                        onBlockLiveUser = viewModel::blockLiveUser,
                        onEndLive = viewModel::endLiveStream,
                        onCreateChat = viewModel::createChat,
                        onSelectChat = viewModel::selectChat,
                        onSendChatMessage = viewModel::sendChatMessage,
                        onMarkNotificationRead = viewModel::markNotificationRead,
                        onSubmitTicket = { cat, sub, desc -> viewModel.submitSupportTicket(cat, sub, desc) },
                        onSignOut = onSignOut
                    )
                }

                composable(
                    route = "$ROUTE_WATCH/{videoId}",
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                ) { entry ->
                    val videoId = entry.arguments?.getString("videoId").orEmpty()
                    val video = remember(videoId, publicVideos, myVideos) {
                        (publicVideos + myVideos).firstOrNull { it.id == videoId }
                    }
                    LaunchedEffect(videoId) {
                        viewModel.selectVideoForWatch(videoId)
                    }
                    VideoPlayerScreen(
                        video = video,
                        user = user,
                        config = config,
                        channels = channels,
                        subscriptions = subscriptions,
                        playlists = playlists,
                        watchHistory = watchHistory,
                        activeAds = activeAds,
                        comments = comments,
                        translatedVideoState = translatedVideo,
                        translatedComments = translatedComments,
                        onBack = { navController.popBackStack() },
                        onRecordView = viewModel::recordView,
                        onLike = viewModel::likeVideo,
                        onDislike = viewModel::dislikeVideo,
                        onSavePosition = viewModel::saveWatchPosition,
                        onToggleSubscribe = viewModel::toggleSubscription,
                        onAddToWatchLater = viewModel::addToWatchLater,
                        onAddToPlaylist = viewModel::addVideoToPlaylist,
                        onAddComment = viewModel::addComment,
                        onTranslateVideo = viewModel::translateVideo,
                        onClearTranslation = viewModel::clearTranslation,
                        onTranslateComment = viewModel::translateComment,
                        onAdCompleted = { camp, v, valid, click ->
                            viewModel.recordAdView(camp, v, valid, click)
                        },
                        onReportVideo = { vid, reason ->
                            viewModel.submitSupportTicket("CONTENT_REPORT", "Reported Video $vid", reason, vid)
                        },
                        onOpenChannel = { cid -> navController.navigate("$ROUTE_CHANNEL/$cid") },
                        onModerateComment = viewModel::moderateComment
                    )
                }

                composable(ROUTE_SEARCH) {
                    SearchScreen(
                        config = config,
                        videos = publicVideos,
                        channels = channels,
                        liveStreams = liveStreams,
                        searchHistory = searchHistory,
                        onBack = { navController.popBackStack() },
                        onSaveSearch = viewModel::saveSearchQuery,
                        onClearHistory = viewModel::clearSearchHistory,
                        onOpenVideo = { vid ->
                            viewModel.selectVideoForWatch(vid)
                            navController.navigate("$ROUTE_WATCH/$vid")
                        },
                        onOpenShorts = { navController.navigate(ROUTE_SHORTS) },
                        onOpenChannel = { cid -> navController.navigate("$ROUTE_CHANNEL/$cid") },
                        onOpenLive = { sid ->
                            viewModel.selectLiveStream(sid)
                            navController.navigate(ROUTE_HUB)
                        },
                        onNotify = viewModel::emitMessage
                    )
                }

                composable(
                    route = "$ROUTE_CHANNEL/{channelId}",
                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                ) { entry ->
                    val cid = entry.arguments?.getString("channelId").orEmpty()
                    val channel = channels.firstOrNull { it.id == cid || it.ownerId == cid }
                    val channelVideos = (publicVideos + myVideos).filter {
                        if (channel != null) {
                            it.channelId == channel.id || (it.channelId.isBlank() && it.creatorId == channel.ownerId)
                        } else {
                            it.creatorId == cid
                        }
                    }
                    ChannelProfileDetailScreen(
                        channel = channel,
                        videos = channelVideos,
                        subscriptions = subscriptions,
                        onBack = { navController.popBackStack() },
                        onToggleSubscribe = viewModel::toggleSubscription,
                        onOpenVideo = { vid ->
                            viewModel.selectVideoForWatch(vid)
                            navController.navigate("$ROUTE_WATCH/$vid")
                        }
                    )
                }

                composable(
                    route = "$ROUTE_CREATE_CHANNEL?channelId={channelId}",
                    arguments = listOf(
                        navArgument("channelId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val editChannelId = entry.arguments?.getString("channelId")
                    val editingChannel = remember(editChannelId, myChannels) {
                        if (editChannelId.isNullOrBlank()) null
                        else myChannels.firstOrNull { it.id == editChannelId }
                    }
                    CreateChannelScreen(
                        user = user,
                        ownedChannelCount = myChannels.size,
                        maxChannels = BharatTubeRepository.MAX_CHANNELS_PER_ACCOUNT,
                        editingChannel = editingChannel,
                        handleCheckState = handleCheckState,
                        onCheckHandle = viewModel::checkHandleAvailability,
                        onResetHandleCheck = viewModel::resetHandleCheck,
                        onCreateChannel = { name, handle, desc, cat, avatarUrl, avatarKey, bannerUrl, bannerKey, links, theme, country, lang ->
                            viewModel.createChannel(
                                name = name,
                                rawHandle = handle,
                                description = desc,
                                category = cat,
                                avatarUrl = avatarUrl,
                                avatarKey = avatarKey,
                                bannerUrl = bannerUrl,
                                bannerKey = bannerKey,
                                socialLinks = links,
                                appearanceTheme = theme,
                                countryCode = country,
                                languageCode = lang,
                                onCreated = { navController.popBackStack() }
                            )
                        },
                        onUpdateChannel = { cid, name, newHandle, oldHandle, desc, cat, avatarUrl, bannerUrl, links, theme, country, lang ->
                            viewModel.updateChannelFull(
                                channelId = cid,
                                name = name,
                                rawHandle = newHandle,
                                oldHandle = oldHandle,
                                description = desc,
                                category = cat,
                                avatarUrl = avatarUrl,
                                bannerUrl = bannerUrl,
                                socialLinks = links,
                                appearanceTheme = theme,
                                countryCode = country,
                                languageCode = lang,
                                onUpdated = { navController.popBackStack() }
                            )
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ROUTE_ADMIN) {
                    AdminDashboardScreen(
                        isAdmin = viewModel.isAdmin,
                        adminEmail = viewModel.currentUserEmail,
                        config = config,
                        allUsers = adminUsers,
                        allClaims = adminClaims,
                        allTickets = adminTickets,
                        allLedger = adminLedger,
                        allPayouts = adminPayouts,
                        auditLogs = adminAuditLogs,
                        onBack = { navController.popBackStack() },
                        onSaveConfig = viewModel::adminSaveConfig,
                        onModerateUser = viewModel::adminModerateUser,
                        onResolveCopyright = viewModel::adminResolveCopyright,
                        onResolveTicket = viewModel::adminResolveTicket
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelProfileDetailScreen(
    channel: Channel?,
    videos: List<Video>,
    subscriptions: List<SubscriptionItem>,
    onBack: () -> Unit,
    onToggleSubscribe: (Channel, Boolean, Boolean) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(channel?.name ?: "Creator Channel") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (channel != null) {
                val sub = subscriptions.firstOrNull { it.channelId == channel.id }
                val isSub = sub?.subscribed == true
                val accent = channelAccentColor(channel.appearanceTheme)
                item {
                    if (channel.bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.bannerUrl,
                            contentDescription = "Channel banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = resolveThumbnailDrawable(channel.bannerKey)),
                            contentDescription = "Channel banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BharatTubeAvatar(
                                name = channel.name,
                                photoUrl = channel.avatarUrl,
                                fallbackKey = channel.avatarKey,
                                accentColor = accent,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(channel.name, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = "${channel.handle} • ${channel.category} • ${formatCompactCount(channel.subscriberCount)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onToggleSubscribe(channel, isSub, sub?.bellEnabled != false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSub) MaterialTheme.colorScheme.surfaceContainerHighest else SaffronPrimary
                            )
                        ) {
                            Text(if (isSub) "Subscribed" else "Subscribe", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (channel.ownershipVerified) {
                        Text(
                            text = "✓ ${channel.ownershipProofLabel.ifBlank { "Verified Channel Ownership" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PeacockTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(channel.description, style = MaterialTheme.typography.bodyMedium)
                    if (channel.socialLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Links: ${channel.socialLinks.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldHighlight
                        )
                    }
                }
            }
            items(videos, key = { it.id }) { vid ->
                VideoFeedCard(video = vid, onClick = { onOpenVideo(vid.id) })
            }
        }
    }
}
