package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.BharatTubeRepository
import com.example.data.firebase.GeminiAiService
import com.example.data.firebase.TranslatedVideoText
import com.example.data.firebase.UiState
import com.example.data.firebase.asUiState
import com.example.data.model.AdCampaign
import com.example.data.model.AdminAuditLog
import com.example.data.model.Channel
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.CopyrightClaim
import com.example.data.model.DeviceSession
import com.example.data.model.LiveChatMessage
import com.example.data.model.LiveStream
import com.example.data.model.NotificationItem
import com.example.data.model.PayoutRecord
import com.example.data.model.PlatformConfig
import com.example.data.model.Playlist
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.SearchHistoryEntry
import com.example.data.model.SubscriptionItem
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.VideoComment
import com.example.data.model.WatchHistoryItem
import com.example.data.upload.ResumableUploadManager
import com.example.data.upload.UploadProgressState
import com.example.domain.CopyrightFingerprintEngine
import com.example.domain.MonetizationEngine
import com.example.domain.RecommendationEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BharatTubeViewModel(
    private val repository: BharatTubeRepository,
    private val geminiAiService: GeminiAiService,
    val currentUserId: String
) : ViewModel() {

    val isAdmin: Boolean = repository.isAdminUser()
    val currentUserEmail: String = repository.currentUserEmail()

    private val uploadManager = ResumableUploadManager(viewModelScope)
    val uploadState: StateFlow<UploadProgressState> = uploadManager.state

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight.asStateFlow()

    private val _aiInsightLoading = MutableStateFlow(false)
    val aiInsightLoading: StateFlow<Boolean> = _aiInsightLoading.asStateFlow()

    private val _translatedVideo = MutableStateFlow<UiState<TranslatedVideoText>?>(null)
    val translatedVideo: StateFlow<UiState<TranslatedVideoText>?> = _translatedVideo.asStateFlow()

    private val _translatedComments = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedComments: StateFlow<Map<String, String>> = _translatedComments.asStateFlow()

    private val _selectedVideoId = MutableStateFlow<String?>(null)
    private val _selectedStreamId = MutableStateFlow<String?>(null)
    private val _selectedChatId = MutableStateFlow<String?>(null)
    private val _handleCheckState = MutableStateFlow<Pair<String, Boolean?>>("" to null)
    val handleCheckState: StateFlow<Pair<String, Boolean?>> = _handleCheckState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                repository.ensureUserAndCatalogInitialized(user)
            }
        }
    }

    private fun <T> Flow<T>.safeState(initial: T, label: String): StateFlow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Stream error ($label)", firestoreError ?: error)
            _events.tryEmit(firestoreError?.localizedMessage ?: error.localizedMessage ?: "Failed to sync $label")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val platformConfig: StateFlow<PlatformConfig> =
        repository.observePlatformConfig().safeState(PlatformConfig(), "platform_config")

    val userProfile: StateFlow<UserProfile> =
        repository.observeUserProfile(currentUserId)
            .map { it ?: UserProfile(id = currentUserId, userId = currentUserId, displayName = "Creator") }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                Log.w(TAG, "Profile error", fe ?: error)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                UserProfile(id = currentUserId, userId = currentUserId, displayName = "Creator")
            )

    val publicVideosState: StateFlow<UiState<List<Video>>> =
        repository.observePublicVideos()
            .asUiState(TAG, "Failed to load videos")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val publicVideos: StateFlow<List<Video>> =
        repository.observePublicVideos().safeState(emptyList(), "videos")

    val myVideos: StateFlow<List<Video>> =
        repository.observeCreatorVideos(currentUserId).safeState(emptyList(), "my_videos")

    val channels: StateFlow<List<Channel>> =
        repository.observeChannels().safeState(emptyList(), "channels")

    val myChannels: StateFlow<List<Channel>> =
        channels.map { list -> list.filter { it.ownerId == currentUserId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeChannel: StateFlow<Channel?> =
        combine(myChannels, userProfile) { owned, profile ->
            owned.firstOrNull { it.id == profile.activeChannelId } ?: owned.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val liveStreams: StateFlow<List<LiveStream>> =
        repository.observeLiveStreams().safeState(emptyList(), "live_streams")

    val activeAdCampaigns: StateFlow<List<AdCampaign>> =
        repository.observeActiveAdCampaigns().safeState(emptyList(), "active_ads")

    val myAdCampaigns: StateFlow<List<AdCampaign>> =
        repository.observeMyAdCampaigns(currentUserId).safeState(emptyList(), "my_ads")

    val revenueLedger: StateFlow<List<RevenueLedgerEntry>> =
        repository.observeRevenueLedger(currentUserId).safeState(emptyList(), "revenue_ledger")

    val payouts: StateFlow<List<PayoutRecord>> =
        repository.observePayouts(currentUserId).safeState(emptyList(), "payouts")

    val watchHistory: StateFlow<List<WatchHistoryItem>> =
        repository.observeWatchHistory(currentUserId).safeState(emptyList(), "watch_history")

    val playlists: StateFlow<List<Playlist>> =
        repository.observePlaylists(currentUserId).safeState(emptyList(), "playlists")

    val subscriptions: StateFlow<List<SubscriptionItem>> =
        repository.observeSubscriptions(currentUserId).safeState(emptyList(), "subscriptions")

    val notifications: StateFlow<List<NotificationItem>> =
        repository.observeNotifications(currentUserId).safeState(emptyList(), "notifications")

    val searchHistory: StateFlow<List<SearchHistoryEntry>> =
        repository.observeSearchHistory(currentUserId).safeState(emptyList(), "search_history")

    val sessions: StateFlow<List<DeviceSession>> =
        repository.observeSessions(currentUserId).safeState(emptyList(), "sessions")

    val chats: StateFlow<List<ChatThread>> =
        repository.observeChats(currentUserId).safeState(emptyList(), "chats")

    val copyrightClaims: StateFlow<List<CopyrightClaim>> =
        repository.observeCopyrightClaimsForCreator(currentUserId).safeState(emptyList(), "copyright_claims")

    val supportTickets: StateFlow<List<SupportTicket>> =
        repository.observeSupportTickets(currentUserId).safeState(emptyList(), "support_tickets")

    val recommendedVideos: StateFlow<List<Video>> =
        combine(publicVideos, userProfile, watchHistory, subscriptions) { vids, user, history, subs ->
            val subIds = subs.filter { it.subscribed }.map { it.channelId }.toSet()
            RecommendationEngine.rankPersonalizedFeed(vids, user, history, subIds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedVideoComments: StateFlow<List<VideoComment>> =
        _selectedVideoId.flatMapLatest { vid ->
            if (vid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeComments(vid)
        }.safeState(emptyList(), "comments")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedLiveMessages: StateFlow<List<LiveChatMessage>> =
        _selectedStreamId.flatMapLatest { sid ->
            if (sid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeLiveMessages(sid)
        }.safeState(emptyList(), "live_messages")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedChatMessages: StateFlow<List<ChatMessage>> =
        _selectedChatId.flatMapLatest { cid ->
            if (cid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeChatMessages(cid)
        }.safeState(emptyList(), "chat_messages")

    // Admin streams (active only for verified Admin)
    val adminUsers: StateFlow<List<UserProfile>> =
        (if (isAdmin) repository.observeAllUsersAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_users")

    val adminAuditLogs: StateFlow<List<AdminAuditLog>> =
        (if (isAdmin) repository.observeAllAuditLogsAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_audit_logs")

    val adminCopyrightClaims: StateFlow<List<CopyrightClaim>> =
        (if (isAdmin) repository.observeAllCopyrightClaimsAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_claims")

    val adminSupportTickets: StateFlow<List<SupportTicket>> =
        (if (isAdmin) repository.observeAllSupportTicketsAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_tickets")

    val adminRevenueLedger: StateFlow<List<RevenueLedgerEntry>> =
        (if (isAdmin) repository.observeAllRevenueLedgerAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_revenue")

    val adminPayouts: StateFlow<List<PayoutRecord>> =
        (if (isAdmin) repository.observeAllPayoutsAdmin() else flowOf(emptyList()))
            .safeState(emptyList(), "admin_payouts")

    // --- SELECTION ---

    fun selectVideoForWatch(videoId: String?) {
        _selectedVideoId.value = videoId
        _translatedVideo.value = null
    }

    fun selectLiveStream(streamId: String?) {
        _selectedStreamId.value = streamId
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
    }

    // --- UPLOAD ---

    fun startResumableUpload(
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
    ) {
        val cfg = platformConfig.value
        val user = userProfile.value
        if (!cfg.uploadsEnabled || user.uploadRestricted) {
            _events.tryEmit("Uploads are currently restricted or disabled by Admin.")
            return
        }
        val existingHashes = publicVideos.value.map { it.copyrightHash }.toSet()
        val fpResult = CopyrightFingerprintEngine.scanUpload(
            title = title,
            musicTitle = musicTitle,
            mediaUri = videoUrl,
            durationSec = durationSec,
            existingHashes = existingHashes
        )
        val hashtags = hashtagsRaw.split(Regex("[,\\s]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#")) it else "#$it" }
            .take(10)

        val ch = activeChannel.value
        uploadManager.startUpload(
            fileName = fileName,
            totalMb = totalMb,
            simulateDisconnect = simulateDisconnect
        ) {
            val res = repository.publishVideo(
                title = title,
                description = description,
                videoUrl = videoUrl,
                thumbnailKey = thumbnailKey,
                thumbnailUrl = "",
                format = format,
                visibility = visibility,
                category = category,
                languageCode = languageCode,
                countryCode = countryCode,
                hashtags = hashtags,
                durationSec = durationSec,
                maxResolution = maxResolution,
                musicTitle = musicTitle,
                musicLicenseId = musicLicenseId,
                chaptersSummary = chaptersSummary,
                subtitlesText = subtitlesText,
                copyrightHash = fpResult.contentHash,
                copyrightStatus = fpResult.status,
                similarityPercent = fpResult.similarityPercent,
                matchedReference = fpResult.matchedReferenceTitle,
                monetized = monetized && !user.minorMode,
                scheduledPublishAtMs = scheduledPublishAtMs,
                creatorName = ch?.name ?: user.displayName,
                creatorHandle = ch?.handle ?: user.handle,
                channelId = ch?.id ?: user.activeChannelId.ifBlank { user.userId }
            )
            if (res.isSuccess) {
                _events.tryEmit(
                    if (fpResult.similarityDetected) fpResult.advisoryMessage
                    else "Video \"$title\" uploaded & published ($visibility)!"
                )
            }
            res.map { Unit }
        }
    }

    fun pauseUpload() = uploadManager.pauseUpload()
    fun resumeUpload() = uploadManager.resumeUpload()
    fun cancelUpload() = uploadManager.cancelUpload()
    fun resetUpload() = uploadManager.reset()

    // --- PLAYBACK, ENGAGEMENT & ADS ---

    fun recordView(video: Video) {
        viewModelScope.launch {
            repository.recordVideoEngagement(video.id, viewDelta = 1L)
        }
    }

    fun likeVideo(video: Video) {
        if (!platformConfig.value.likesEnabled) {
            _events.tryEmit("Likes are disabled by platform administrator.")
            return
        }
        viewModelScope.launch {
            repository.recordVideoEngagement(video.id, likeDelta = 1L)
                .onSuccess { _events.tryEmit("Liked \"${video.title}\"") }
        }
    }

    fun dislikeVideo(video: Video) {
        if (!platformConfig.value.likesEnabled) return
        viewModelScope.launch {
            repository.recordVideoEngagement(video.id, dislikeDelta = 1L)
        }
    }

    fun saveWatchPosition(video: Video, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.saveWatchHistory(
                video = video,
                positionMs = positionMs,
                durationMs = durationMs,
                watchHistoryPaused = userProfile.value.watchHistoryPaused
            )
        }
    }

    fun addComment(videoId: String, text: String, parentCommentId: String = "") {
        val cfg = platformConfig.value
        val user = userProfile.value
        if (!cfg.commentsEnabled || user.commentRestricted) {
            _events.tryEmit("Comments are currently disabled or restricted.")
            return
        }
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(videoId, text, user.displayName, parentCommentId)
                .onSuccess { _events.tryEmit("Comment posted") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to post comment") }
        }
    }

    fun recordAdView(campaign: AdCampaign, video: Video, isValidView: Boolean, isClick: Boolean = false) {
        viewModelScope.launch {
            repository.recordAdImpressionAndSplitRevenue(
                campaign = campaign,
                video = video,
                config = platformConfig.value,
                viewerCountryCode = userProfile.value.countryCode,
                isValidView = isValidView,
                isClick = isClick
            ).onSuccess { entry ->
                if (entry != null) {
                    _events.tryEmit(
                        "Valid Ad View recorded • Net ₹${entry.netRevenueInr}: Creator ₹${entry.creatorEarnedInr} (${entry.creatorSharePercent.toInt()}%) | BharatTube ₹${entry.platformEarnedInr} (${entry.platformSharePercent.toInt()}%)"
                    )
                }
            }
        }
    }

    fun settleSampleAdRevenueAndCheckPayout(grossInr: Double, video: Video?) {
        val targetVideo = video ?: myVideos.value.firstOrNull() ?: publicVideos.value.firstOrNull()
        if (targetVideo == null) {
            _events.tryEmit("Publish a video first to record ad revenue.")
            return
        }
        viewModelScope.launch {
            repository.settleEligibleAdBatchAndEvaluateAutoPayout(
                grossRevenueInr = grossInr,
                video = targetVideo,
                user = userProfile.value,
                config = platformConfig.value
            ).onSuccess { msg ->
                _events.tryEmit(msg)
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Revenue settlement failed")
            }
        }
    }

    fun triggerManualAutoPayoutCheck() {
        val user = userProfile.value
        val cfg = platformConfig.value
        val eval = MonetizationEngine.evaluateAutomaticPayout(
            availableBalanceInr = user.availableBalanceInr,
            countryCode = user.countryCode,
            kycVerified = user.kycVerified,
            hasPaymentDetails = user.upiId.isNotBlank() || user.bankAccountMasked.isNotBlank(),
            minorMode = user.minorMode,
            creatorMonetizationEnabled = user.monetizationEnabled,
            config = cfg
        )
        if (!eval.eligible) {
            _events.tryEmit(eval.reason)
            return
        }
        viewModelScope.launch {
            repository.initiateAutomaticPayout(user, eval)
                .onSuccess { txnId -> _events.tryEmit("Auto-payout initiated ($txnId) for ₹${eval.netPayoutInr}!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Payout failed") }
        }
    }

    fun retryFailedPayout(payoutId: String) {
        viewModelScope.launch {
            repository.retryFailedPayout(payoutId)
                .onSuccess { _events.tryEmit("Payout queued for retry.") }
        }
    }

    // --- AI TRANSLATION & RECOMMENDATIONS (FIREBASE AI LOGIC) ---

    fun translateVideo(video: Video, targetLangCode: String) {
        viewModelScope.launch {
            _translatedVideo.value = UiState.Loading
            val res = geminiAiService.translateVideoMetadata(
                title = video.title,
                description = video.description,
                subtitles = video.subtitlesText,
                targetLangCode = targetLangCode
            )
            _translatedVideo.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Translation failed") }
            )
        }
    }

    fun clearTranslation() {
        _translatedVideo.value = null
    }

    fun translateComment(commentId: String, text: String, targetLangCode: String) {
        viewModelScope.launch {
            geminiAiService.translateComment(text, targetLangCode)
                .onSuccess { translated ->
                    _translatedComments.value = _translatedComments.value + (commentId to translated)
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Comment translation failed") }
        }
    }

    fun refreshAiRecommendationInsight() {
        val user = userProfile.value
        val recent = watchHistory.value.map { it.title }
        viewModelScope.launch {
            _aiInsightLoading.value = true
            geminiAiService.generateRecommendationInsight(
                interests = user.interests,
                recentTitles = recent,
                languageCode = user.languageCode,
                countryCode = user.countryCode
            ).onSuccess { insight ->
                _aiInsight.value = insight
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "AI curator unavailable")
            }
            _aiInsightLoading.value = false
        }
    }

    // --- USER SETTINGS, CHANNELS, PLAYLISTS, SEARCH ---

    fun updateUserSettings(updates: Map<String, Any>, toast: String? = null) {
        viewModelScope.launch {
            repository.updateUserSettings(updates)
                .onSuccess { toast?.let { _events.tryEmit(it) } }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun updateChannel(name: String, handle: String, bio: String, country: String, lang: String) {
        viewModelScope.launch {
            repository.updateChannelProfile(name, handle, bio, country, lang)
                .onSuccess { _events.tryEmit("Channel customized!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update channel") }
        }
    }

    fun checkHandleAvailability(rawHandle: String) {
        val cleaned = rawHandle.trim()
        if (cleaned.length < 2) {
            _handleCheckState.value = cleaned to false
            return
        }
        viewModelScope.launch {
            val available = repository.isHandleAvailable(cleaned)
            _handleCheckState.value = cleaned to available
        }
    }

    fun resetHandleCheck() {
        _handleCheckState.value = "" to null
    }

    fun completeAccountOnboarding(
        displayName: String,
        photoUrl: String,
        countryCode: String,
        languageCode: String
    ) {
        viewModelScope.launch {
            repository.completeAccountOnboarding(displayName, photoUrl, countryCode, languageCode)
                .onSuccess { _events.tryEmit("BharatTube account profile saved!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save profile") }
        }
    }

    fun createChannel(
        name: String,
        rawHandle: String,
        description: String,
        category: String,
        avatarUrl: String,
        avatarKey: String,
        bannerUrl: String,
        bannerKey: String,
        socialLinks: List<String>,
        appearanceTheme: String,
        countryCode: String,
        languageCode: String,
        onCreated: (String) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _events.tryEmit("Please enter a channel name.")
            return
        }
        viewModelScope.launch {
            repository.createChannel(
                name = name,
                rawHandle = rawHandle,
                description = description,
                category = category,
                avatarUrl = avatarUrl,
                avatarKey = avatarKey,
                bannerUrl = bannerUrl,
                bannerKey = bannerKey,
                socialLinks = socialLinks,
                appearanceTheme = appearanceTheme,
                countryCode = countryCode,
                languageCode = languageCode
            ).onSuccess { newId ->
                _events.tryEmit("Channel \"$name\" created & set active!")
                onCreated(newId)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to create channel")
            }
        }
    }

    fun updateChannelFull(
        channelId: String,
        name: String,
        rawHandle: String,
        oldHandle: String,
        description: String,
        category: String,
        avatarUrl: String,
        bannerUrl: String,
        socialLinks: List<String>,
        appearanceTheme: String,
        countryCode: String,
        languageCode: String,
        onUpdated: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            _events.tryEmit("Channel name cannot be empty.")
            return
        }
        viewModelScope.launch {
            repository.updateChannelFull(
                channelId = channelId,
                name = name,
                rawHandle = rawHandle,
                oldHandle = oldHandle,
                description = description,
                category = category,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                socialLinks = socialLinks,
                appearanceTheme = appearanceTheme,
                countryCode = countryCode,
                languageCode = languageCode
            ).onSuccess {
                _events.tryEmit("Channel appearance & info updated!")
                onUpdated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update channel")
            }
        }
    }

    fun switchActiveChannel(channelId: String) {
        viewModelScope.launch {
            repository.switchActiveChannel(channelId)
                .onSuccess { _events.tryEmit("Switched active channel") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to switch channel") }
        }
    }

    fun verifyChannelOwnership(channelId: String) {
        viewModelScope.launch {
            repository.verifyChannelOwnership(channelId)
                .onSuccess { _events.tryEmit("Channel ownership verified via Google OAuth!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Verification failed") }
        }
    }

    fun moderateComment(videoId: String, commentId: String, newStatus: String) {
        viewModelScope.launch {
            repository.moderateComment(videoId, commentId, newStatus)
                .onSuccess { _events.tryEmit("Comment updated ($newStatus)") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Comment moderation failed") }
        }
    }

    fun toggleSubscription(channel: Channel, currentlySubscribed: Boolean, bellEnabled: Boolean) {
        if (!platformConfig.value.subscriptionsEnabled) {
            _events.tryEmit("Subscriptions are paused by platform admin.")
            return
        }
        viewModelScope.launch {
            repository.toggleSubscription(channel, currentlySubscribed, bellEnabled)
                .onSuccess {
                    _events.tryEmit(
                        if (!currentlySubscribed) "Subscribed & following ${channel.name}"
                        else "Unsubscribed from ${channel.name}"
                    )
                }
        }
    }

    fun createPlaylist(title: String, description: String, privacy: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(title, description, privacy)
                .onSuccess { _events.tryEmit("Playlist \"$title\" created") }
        }
    }

    fun addToWatchLater(videoId: String) {
        viewModelScope.launch {
            repository.addVideoToPlaylist("watch_later", videoId)
                .onSuccess { _events.tryEmit("Saved to Watch Later") }
        }
    }

    fun addVideoToPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, videoId)
                .onSuccess { _events.tryEmit("Added to playlist") }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
                .onSuccess { _events.tryEmit("Playlist deleted") }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
                .onSuccess { _events.tryEmit("Watch history cleared") }
        }
    }

    fun saveSearchQuery(query: String) {
        viewModelScope.launch { repository.saveSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
                .onSuccess { _events.tryEmit("Search history cleared") }
        }
    }

    // --- LIVE, CHAT, CAMPAIGNS, COPYRIGHT, SUPPORT ---

    fun startLiveStream(title: String, description: String) {
        val user = userProfile.value
        val cfg = platformConfig.value
        if (!cfg.liveStreamingEnabled || user.minorMode) {
            _events.tryEmit("Live streaming is disabled or restricted in Minor Safety Mode.")
            return
        }
        viewModelScope.launch {
            repository.startLiveStream(title, description, user.displayName, user.languageCode, user.countryCode)
                .onSuccess { id ->
                    _selectedStreamId.value = id
                    _events.tryEmit("You are now LIVE!")
                }
        }
    }

    fun sendLiveMessage(streamId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendLiveChatMessage(streamId, userProfile.value.displayName, text)
        }
    }

    fun blockLiveUser(streamId: String, userId: String) {
        viewModelScope.launch {
            repository.blockUserInLiveStream(streamId, userId)
                .onSuccess { _events.tryEmit("User blocked from live chat") }
        }
    }

    fun endLiveStream(streamId: String) {
        viewModelScope.launch {
            repository.endLiveStream(streamId)
                .onSuccess { _events.tryEmit("Live stream ended") }
        }
    }

    fun createChat(title: String, groupChat: Boolean) {
        if (!platformConfig.value.chatEnabled || userProfile.value.minorMode) {
            _events.tryEmit("Chat is disabled or restricted in Minor Safety Mode.")
            return
        }
        viewModelScope.launch {
            repository.createChatThread(title.ifBlank { "Creator Lounge" }, groupChat)
                .onSuccess { cid ->
                    _selectedChatId.value = cid
                    _events.tryEmit("Chat thread ready")
                }
        }
    }

    fun sendChatMessage(chat: ChatThread, text: String, type: String = "TEXT", attachment: String = "") {
        if (text.isBlank() && attachment.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(
                chat = chat,
                senderName = userProfile.value.displayName,
                text = text.ifBlank { attachment },
                messageType = type,
                attachmentLabel = attachment
            )
        }
    }

    fun createAdCampaign(
        advertiserName: String,
        title: String,
        ctaText: String,
        placement: String,
        countries: List<String>,
        languages: List<String>,
        budgetInr: Double
    ) {
        viewModelScope.launch {
            repository.createAdCampaign(
                advertiserName = advertiserName,
                title = title,
                ctaText = ctaText,
                targetUrl = "https://bharattube.example/campaign",
                placement = placement,
                targetCountries = countries,
                targetLanguages = languages,
                budgetInr = budgetInr
            ).onSuccess { _events.tryEmit("Ad campaign \"$title\" launched!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to create campaign") }
        }
    }

    fun toggleCampaignStatus(campaignId: String, status: String) {
        viewModelScope.launch {
            repository.toggleCampaignStatus(campaignId, status)
                .onSuccess { _events.tryEmit("Campaign status updated to $status") }
        }
    }

    fun appealCopyright(claimId: String, note: String) {
        viewModelScope.launch {
            repository.submitCopyrightAppeal(claimId, note)
                .onSuccess { _events.tryEmit("Copyright appeal submitted for Admin review.") }
        }
    }

    fun submitSupportTicket(category: String, subject: String, description: String, targetId: String = "") {
        viewModelScope.launch {
            repository.submitSupportTicket(category, subject, description, targetId)
                .onSuccess { _events.tryEmit("Support ticket submitted.") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to submit ticket") }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { repository.markNotificationRead(notifId) }
    }

    // --- SUPER ADMIN ACTIONS ---

    fun adminSaveConfig(config: PlatformConfig, reason: String) {
        viewModelScope.launch {
            repository.savePlatformConfig(config, reason)
                .onSuccess { _events.tryEmit("Platform config saved & logged in Audit Trail") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Admin update failed") }
        }
    }

    fun adminModerateUser(
        targetUserId: String,
        status: String,
        uploadRestricted: Boolean,
        commentRestricted: Boolean,
        monetizationEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.adminUpdateUserModeration(
                targetUserId, status, uploadRestricted, commentRestricted, monetizationEnabled
            ).onSuccess { _events.tryEmit("User moderation applied & audited") }
        }
    }

    fun adminResolveCopyright(claimId: String, decision: String) {
        viewModelScope.launch {
            repository.adminResolveCopyrightClaim(claimId, decision)
                .onSuccess { _events.tryEmit("Copyright claim marked $decision") }
        }
    }

    fun adminResolveTicket(ticketId: String, note: String) {
        viewModelScope.launch {
            repository.adminResolveTicket(ticketId, note)
                .onSuccess { _events.tryEmit("Ticket resolved") }
        }
    }

    fun updateVideoMetadata(videoId: String, title: String, desc: String, visibility: String, cat: String, monetized: Boolean) {
        viewModelScope.launch {
            repository.updateVideoMetadata(videoId, title, desc, visibility, cat, monetized)
                .onSuccess { _events.tryEmit("Video metadata updated") }
        }
    }

    fun emitMessage(msg: String) {
        _events.tryEmit(msg)
    }

    private companion object {
        const val TAG = "BharatTubeVM"
    }
}
