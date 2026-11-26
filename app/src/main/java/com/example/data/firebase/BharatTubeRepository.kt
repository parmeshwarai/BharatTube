package com.example.data.firebase

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.AdCampaign
import com.example.data.model.AdminAuditLog
import com.example.data.model.Channel
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.CopyrightClaim
import com.example.data.model.DeviceSession
import com.example.data.model.GlobalCatalog
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
import com.example.domain.MonetizationEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BharatTubeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing BharatTube.")

    fun currentUserEmail(): String = auth.currentUser?.email.orEmpty()

    fun isAdminUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    // --- STREAMS ---

    fun observePlatformConfig(): Flow<PlatformConfig> =
        db.collection("platform_config").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: PlatformConfig()
                } else {
                    PlatformConfig()
                }
            }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observePublicVideos(): Flow<List<Video>> =
        db.collection("videos")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snap ->
                snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeCreatorVideos(creatorId: String): Flow<List<Video>> =
        db.collection("videos")
            .whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeChannels(): Flow<List<Channel>> =
        db.collection("channels")
            .whereEqualTo("channelStatus", "ACTIVE")
            .snapshots()
            .map { snap ->
                snap.toObjects(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.subscriberCount }
            }

    fun observeComments(videoId: String): Flow<List<VideoComment>> =
        db.collection("videos").document(videoId).collection("comments")
            .whereEqualTo("status", "VISIBLE")
            .snapshots()
            .map { snap ->
                snap.toObjects(VideoComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeLiveStreams(): Flow<List<LiveStream>> =
        db.collection("live_streams")
            .whereEqualTo("status", "LIVE")
            .snapshots()
            .map { snap ->
                snap.toObjects(LiveStream::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.viewerCount }
            }

    fun observeLiveMessages(streamId: String): Flow<List<LiveChatMessage>> =
        db.collection("live_streams").document(streamId).collection("messages")
            .whereEqualTo("streamId", streamId)
            .snapshots()
            .map { snap ->
                snap.toObjects(LiveChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
            }

    fun observeActiveAdCampaigns(): Flow<List<AdCampaign>> =
        db.collection("ad_campaigns")
            .whereEqualTo("status", "ACTIVE")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdCampaign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeMyAdCampaigns(userId: String): Flow<List<AdCampaign>> =
        db.collection("ad_campaigns")
            .whereEqualTo("advertiserId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(AdCampaign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeRevenueLedger(creatorId: String): Flow<List<RevenueLedgerEntry>> =
        db.collection("revenue_ledger")
            .whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snap ->
                snap.toObjects(RevenueLedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observePayouts(creatorId: String): Flow<List<PayoutRecord>> =
        db.collection("payouts")
            .whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PayoutRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryItem>> =
        db.collection("users").document(userId).collection("watchHistory")
            .snapshots()
            .map { snap ->
                snap.toObjects(WatchHistoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.watchedAt?.seconds ?: 0L }
            }

    fun observePlaylists(userId: String): Flow<List<Playlist>> =
        db.collection("users").document(userId).collection("playlists")
            .snapshots()
            .map { snap ->
                snap.toObjects(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }

    fun observeSubscriptions(userId: String): Flow<List<SubscriptionItem>> =
        db.collection("users").document(userId).collection("subscriptions")
            .snapshots()
            .map { snap ->
                snap.toObjects(SubscriptionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> =
        db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntry>> =
        db.collection("users").document(userId).collection("searchHistory")
            .snapshots()
            .map { snap ->
                snap.toObjects(SearchHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeSessions(userId: String): Flow<List<DeviceSession>> =
        db.collection("users").document(userId).collection("sessions")
            .snapshots()
            .map { snap ->
                snap.toObjects(DeviceSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeChats(userId: String): Flow<List<ChatThread>> =
        db.collection("chats")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }

    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("memberUids", auth.currentUser?.uid ?: "")
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
            }

    fun observeCopyrightClaimsForCreator(creatorId: String): Flow<List<CopyrightClaim>> =
        db.collection("copyright_claims")
            .whereEqualTo("targetCreatorId", creatorId)
            .snapshots()
            .map { snap ->
                snap.toObjects(CopyrightClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> =
        db.collection("support_tickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    // --- ADMIN STREAMS (only collected when isAdminUser() is true) ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeAllAuditLogsAdmin(): Flow<List<AdminAuditLog>> =
        db.collection("admin_audit_logs")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllCopyrightClaimsAdmin(): Flow<List<CopyrightClaim>> =
        db.collection("copyright_claims")
            .snapshots()
            .map { snap ->
                snap.toObjects(CopyrightClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllSupportTicketsAdmin(): Flow<List<SupportTicket>> =
        db.collection("support_tickets")
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllRevenueLedgerAdmin(): Flow<List<RevenueLedgerEntry>> =
        db.collection("revenue_ledger")
            .snapshots()
            .map { snap ->
                snap.toObjects(RevenueLedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeAllPayoutsAdmin(): Flow<List<PayoutRecord>> =
        db.collection("payouts")
            .snapshots()
            .map { snap ->
                snap.toObjects(PayoutRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    // --- BOOTSTRAP & SEEDING ---

    suspend fun ensureUserAndCatalogInitialized(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        return try {
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "BharatTube Creator"
            val handleBase = displayName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14).ifBlank { "creator" }
            val handle = "@${handleBase}_${uid.take(4).lowercase()}"
            val bharatTubeId = "BT-${uid.take(6).uppercase()}-${((uid.hashCode().toLong() and 0xFFFFL).toString().padStart(4, '0'))}"

            if (!userSnap.exists()) {
                val userPayload = mapOf(
                    "userId" to uid,
                    "bharatTubeId" to bharatTubeId,
                    "onboardingCompleted" to false,
                    "activeChannelId" to uid,
                    "displayName" to displayName,
                    "handle" to handle,
                    "email" to (firebaseUser.email ?: ""),
                    "phone" to (firebaseUser.phoneNumber ?: ""),
                    "bio" to "Global BharatTube creator sharing stories, tech & music.",
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "countryCode" to "IN",
                    "languageCode" to "en",
                    "role" to "CREATOR",
                    "accountStatus" to "ACTIVE",
                    "restrictedMode" to false,
                    "minorMode" to false,
                    "twoFactorEnabled" to true,
                    "watchHistoryPaused" to false,
                    "uploadRestricted" to false,
                    "commentRestricted" to false,
                    "monetizationEnabled" to true,
                    "kycVerified" to false,
                    "upiId" to "",
                    "bankAccountMasked" to "",
                    "estimatedEarningsInr" to 0.0,
                    "confirmedEarningsInr" to 0.0,
                    "availableBalanceInr" to 0.0,
                    "pendingBalanceInr" to 0.0,
                    "totalPaidOutInr" to 0.0,
                    "fontScaleMultiplier" to 1.0,
                    "highContrastMode" to false,
                    "darkThemeMode" to "DARK",
                    "interests" to listOf("Technology", "Music", "Education"),
                    "notifSubscribers" to true,
                    "notifComments" to true,
                    "notifUploads" to true,
                    "notifEarnings" to true,
                    "notifPayouts" to true,
                    "notifSecurity" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload).await()

                // Create Watch Later default playlist & active session
                db.collection("users").document(uid).collection("playlists").document("watch_later")
                    .set(
                        mapOf(
                            "title" to "Watch Later",
                            "description" to "Saved videos to watch anytime",
                            "privacy" to "PRIVATE",
                            "videoIds" to emptyList<String>(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                addNotificationInternal(
                    uid,
                    "SECURITY",
                    "BharatTube Account Created ($bharatTubeId)",
                    "Linked securely to Google (${firebaseUser.email ?: uid}) on ${Build.MODEL ?: "Android"}."
                )
            } else if (userSnap.getString("bharatTubeId").isNullOrBlank()) {
                userRef.update(
                    mapOf(
                        "bharatTubeId" to bharatTubeId,
                        "activeChannelId" to (userSnap.getString("activeChannelId").takeUnless { it.isNullOrBlank() } ?: uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Record active device session
            db.collection("users").document(uid).collection("sessions").document("current_android")
                .set(
                    mapOf(
                        "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(75),
                        "platform" to "Android",
                        "locationLabel" to "Google OAuth Session",
                        "active" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            // Ensure creator's primary channel exists
            val chanRef = db.collection("channels").document(uid)
            if (!chanRef.get().await().exists()) {
                val proof = if (firebaseUser.isEmailVerified && !firebaseUser.email.isNullOrBlank()) {
                    "Google OAuth Verified (${firebaseUser.email})"
                } else {
                    ""
                }
                chanRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "name" to displayName,
                        "handle" to handle,
                        "description" to "Official BharatTube channel by $displayName",
                        "category" to "Technology",
                        "avatarUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                        "avatarKey" to "img_hero_bharattube",
                        "bannerUrl" to "",
                        "bannerKey" to "img_channel_banner",
                        "socialLinks" to emptyList<String>(),
                        "appearanceTheme" to "SAFFRON",
                        "countryCode" to "IN",
                        "languageCode" to "en",
                        "channelStatus" to "ACTIVE",
                        "subscriberCount" to 1L,
                        "followerCount" to 1L,
                        "totalViews" to 0L,
                        "verified" to false,
                        "ownershipVerified" to firebaseUser.isEmailVerified,
                        "ownershipProofLabel" to proof,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val handleKey = normalizeHandleKey(handle)
                if (handleKey.isNotBlank()) {
                    val hRef = db.collection("channel_handles").document(handleKey)
                    if (!hRef.get().await().exists()) {
                        hRef.set(
                            mapOf(
                                "ownerId" to uid,
                                "channelId" to uid,
                                "handle" to handle,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            // Seed starter playable videos/ads/streams once if catalog is empty
            seedInitialCatalogIfEmpty(uid, displayName, handle)

            if (isAdminUser()) {
                val cfgRef = db.collection("platform_config").document("global")
                if (!cfgRef.get().await().exists()) {
                    savePlatformConfig(PlatformConfig(updatedBy = uid), "Initial global config bootstrap")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    private suspend fun seedInitialCatalogIfEmpty(uid: String, creatorName: String, handle: String) {
        val existingVideos = db.collection("videos")
            .whereEqualTo("visibility", "PUBLIC")
            .limit(1)
            .get()
            .await()
        if (!existingVideos.isEmpty) return

        val starterVideos = listOf(
            Triple("vid_bt_space_4k", "ISRO & India's Next-Gen Orbital Economy (4K)", GlobalCatalog.STREAM_BUNNY) to
                mapOf(
                    "thumbnailKey" to "img_thumb_tech",
                    "format" to "LONG",
                    "category" to "Technology",
                    "languageCode" to "en",
                    "countryCode" to "IN",
                    "durationSec" to 596L,
                    "maxResolution" to "4K",
                    "hashtags" to listOf("#BharatTech", "#Space", "#4K"),
                    "chaptersSummary" to "00:00 Orbital Era | 01:30 Satellites | 04:10 Deep Space",
                    "subtitlesText" to "Welcome to BharatTube Tech. Exploring India's orbital innovations and global creator engineering.",
                    "viewCount" to 18420L,
                    "likeCount" to 1640L,
                    "commentCount" to 42L
                ),
            Triple("vid_bt_music_live", "Monsoon Classical Fusion Concert — Mumbai Live", GlobalCatalog.STREAM_BLAZES) to
                mapOf(
                    "thumbnailKey" to "img_thumb_music",
                    "format" to "LONG",
                    "category" to "Music",
                    "languageCode" to "hi",
                    "countryCode" to "IN",
                    "durationSec" to 312L,
                    "maxResolution" to "1080p",
                    "hashtags" to listOf("#Sangeet", "#Mumbai", "#LiveMusic"),
                    "chaptersSummary" to "00:00 Alap | 01:20 Sitar Drop | 03:40 Finale",
                    "subtitlesText" to "भारतट्यूब म्यूज़िक स्टेज में आपका स्वागत है। Live acoustic and electronic fusion from Mumbai.",
                    "viewCount" to 24910L,
                    "likeCount" to 2890L,
                    "commentCount" to 85L
                ),
            Triple("vid_bt_short_1", "60-Sec Street Chai & Monsoon Vibes #Shorts", GlobalCatalog.STREAM_FUN) to
                mapOf(
                    "thumbnailKey" to "img_hero_bharattube",
                    "format" to "SHORT",
                    "category" to "Travel",
                    "languageCode" to "hi",
                    "countryCode" to "IN",
                    "durationSec" to 58L,
                    "maxResolution" to "1080p",
                    "hashtags" to listOf("#Shorts", "#IndiaTravel", "#Chai"),
                    "chaptersSummary" to "",
                    "subtitlesText" to "Rainy evenings in Pune with hot masala chai!",
                    "viewCount" to 61200L,
                    "likeCount" to 5420L,
                    "commentCount" to 120L
                ),
            Triple("vid_bt_short_2", "Tokyo Shibuya Neon Synth Reel #Reels", GlobalCatalog.STREAM_JOY) to
                mapOf(
                    "thumbnailKey" to "img_channel_banner",
                    "format" to "SHORT",
                    "category" to "Music",
                    "languageCode" to "ja",
                    "countryCode" to "JP",
                    "durationSec" to 45L,
                    "maxResolution" to "1080p",
                    "hashtags" to listOf("#Reels", "#Tokyo", "#Synth"),
                    "chaptersSummary" to "",
                    "subtitlesText" to "渋谷の夜とエレクトロニックビート — Midnight Tokyo beats.",
                    "viewCount" to 43800L,
                    "likeCount" to 3910L,
                    "commentCount" to 64L
                ),
            Triple("vid_bt_edu_1", "System Design for 100M Viewers — Architecture", GlobalCatalog.STREAM_ESCAPES) to
                mapOf(
                    "thumbnailKey" to "img_thumb_tech",
                    "format" to "LONG",
                    "category" to "Education",
                    "languageCode" to "en",
                    "countryCode" to "US",
                    "durationSec" to 480L,
                    "maxResolution" to "4K",
                    "hashtags" to listOf("#Engineering", "#CDN", "#Scale"),
                    "chaptersSummary" to "00:00 Edge CDN | 03:00 Chunked Upload | 05:30 Transcoding",
                    "subtitlesText" to "How global video platforms distribute adaptive HLS streams to millions.",
                    "viewCount" to 12900L,
                    "likeCount" to 1120L,
                    "commentCount" to 31L
                )
        )

        starterVideos.forEach { (identity, meta) ->
            val (id, title, url) = identity
            val payload = mutableMapOf<String, Any>(
                "creatorId" to uid,
                "creatorName" to creatorName,
                "creatorHandle" to handle,
                "title" to title,
                "description" to "Featured global creator showcase on BharatTube. Supports 4K playback, multilingual AI translation, and 50:50 creator monetization.",
                "videoUrl" to url,
                "thumbnailUrl" to "",
                "visibility" to "PUBLIC",
                "musicTitle" to "Saffron Horizon (Lo-Fi Sitar)",
                "musicLicenseId" to "LIC-BT-101",
                "dislikeCount" to 12L,
                "copyrightHash" to "fp_seed_$id",
                "copyrightStatus" to "CLEAR",
                "monetized" to true,
                "adRevenueInr" to 120.0,
                "scheduledPublishAtMs" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            payload.putAll(meta)
            db.collection("videos").document(id).set(payload).await()
        }

        // Seed active pre-roll, shorts, and home sponsored campaigns
        val campaigns = listOf(
            mapOf(
                "advertiserId" to uid,
                "advertiserName" to "Swadeshi Cloud Studio",
                "title" to "Build Global 4K Streams with Swadeshi Cloud",
                "ctaText" to "Explore Plans",
                "targetUrl" to "https://bharattube.example/cloud",
                "placement" to "PRE_ROLL",
                "targetCountries" to listOf("IN", "US", "GB", "JP"),
                "targetLanguages" to listOf("en", "hi", "ta", "mr"),
                "budgetInr" to 25000.0,
                "spentInr" to 1200.0,
                "impressions" to 480L,
                "validViews" to 390L,
                "clicks" to 42L,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "advertiserId" to uid,
                "advertiserName" to "VandePay UPI",
                "title" to "Instant Creator UPI Payouts — Zero Hassle",
                "ctaText" to "Link UPI",
                "targetUrl" to "https://bharattube.example/upi",
                "placement" to "SHORTS_FEED",
                "targetCountries" to listOf("IN"),
                "targetLanguages" to listOf("hi", "en", "bho", "bn"),
                "budgetInr" to 15000.0,
                "spentInr" to 650.0,
                "impressions" to 310L,
                "validViews" to 240L,
                "clicks" to 29L,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "advertiserId" to uid,
                "advertiserName" to "BharatAudio Pro",
                "title" to "Studio Microphones for Creators — 20% Off",
                "ctaText" to "Shop Studio",
                "targetUrl" to "https://bharattube.example/audio",
                "placement" to "HOME_SPONSORED",
                "targetCountries" to listOf("IN", "US", "AE"),
                "targetLanguages" to listOf("en", "hi"),
                "budgetInr" to 18000.0,
                "spentInr" to 420.0,
                "impressions" to 210L,
                "validViews" to 180L,
                "clicks" to 19L,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        campaigns.forEachIndexed { idx, camp ->
            db.collection("ad_campaigns").document("seed_camp_${idx + 1}").set(camp).await()
        }

        // Seed 1 live stream
        db.collection("live_streams").document("live_bt_townhall")
            .set(
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to creatorName,
                    "title" to "BharatTube Global Creator Summit — Live Q&A",
                    "description" to "Discussing 50:50 creator revenue share and multilingual captions.",
                    "streamUrl" to GlobalCatalog.STREAM_MELTDOWNS,
                    "category" to "Live",
                    "languageCode" to "en",
                    "countryCode" to "IN",
                    "status" to "LIVE",
                    "viewerCount" to 342L,
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    // --- VIDEO ACTIONS ---

    suspend fun publishVideo(
        title: String,
        description: String,
        videoUrl: String,
        thumbnailKey: String,
        thumbnailUrl: String,
        format: String,
        visibility: String,
        category: String,
        languageCode: String,
        countryCode: String,
        hashtags: List<String>,
        durationSec: Long,
        maxResolution: String,
        musicTitle: String,
        musicLicenseId: String,
        chaptersSummary: String,
        subtitlesText: String,
        copyrightHash: String,
        copyrightStatus: String,
        similarityPercent: Int,
        matchedReference: String?,
        monetized: Boolean,
        scheduledPublishAtMs: Long,
        creatorName: String,
        creatorHandle: String,
        channelId: String = ""
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val videoId = "vid_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "videos/$videoId"
        return try {
            val payload = mapOf(
                "creatorId" to uid,
                "channelId" to channelId.ifBlank { uid }.take(128),
                "creatorName" to creatorName.take(60).ifBlank { "Creator" },
                "creatorHandle" to creatorHandle.take(40).ifBlank { "@creator" },
                "title" to title.trim().take(120),
                "description" to description.trim().take(2000),
                "videoUrl" to videoUrl.take(1000),
                "thumbnailKey" to thumbnailKey.take(100),
                "thumbnailUrl" to thumbnailUrl.take(500),
                "format" to format,
                "visibility" to visibility,
                "category" to category.take(40),
                "languageCode" to languageCode.take(10),
                "countryCode" to countryCode.take(5),
                "hashtags" to hashtags.take(10),
                "durationSec" to durationSec.coerceAtLeast(1L),
                "maxResolution" to maxResolution.take(20),
                "musicTitle" to musicTitle.take(100),
                "musicLicenseId" to musicLicenseId.take(60),
                "chaptersSummary" to chaptersSummary.take(1000),
                "subtitlesText" to subtitlesText.take(3000),
                "viewCount" to 0L,
                "likeCount" to 0L,
                "dislikeCount" to 0L,
                "commentCount" to 0L,
                "copyrightHash" to copyrightHash.take(128),
                "copyrightStatus" to copyrightStatus,
                "monetized" to monetized,
                "adRevenueInr" to 0.0,
                "scheduledPublishAtMs" to scheduledPublishAtMs.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("videos").document(videoId).set(payload).await()

            if (copyrightStatus == "WARNING") {
                val claimId = "claim_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                db.collection("copyright_claims").document(claimId).set(
                    mapOf(
                        "reporterId" to uid,
                        "targetCreatorId" to uid,
                        "videoId" to videoId,
                        "videoTitle" to title.trim().take(120),
                        "matchType" to "AUDIO_MATCH",
                        "similarityPercent" to similarityPercent.toLong(),
                        "status" to "WARNING",
                        "appealNote" to (matchedReference ?: "Similarity flagged during upload scan"),
                        "strikeCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                addNotificationInternal(
                    uid,
                    "COPYRIGHT",
                    "Similarity notice on \"$title\"",
                    "Fingerprint similarity ($similarityPercent%) detected. You can review or appeal in Creator Studio."
                )
            } else {
                addNotificationInternal(
                    uid,
                    "UPLOAD",
                    "Upload complete: $title",
                    "Your ${format.lowercase()} video is now $visibility."
                )
            }
            Result.success(videoId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        visibility: String,
        category: String,
        monetized: Boolean
    ): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            db.collection("videos").document(videoId).update(
                mapOf(
                    "title" to title.trim().take(120),
                    "description" to description.trim().take(2000),
                    "visibility" to visibility,
                    "category" to category.take(40),
                    "monetized" to monetized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordVideoEngagement(
        videoId: String,
        viewDelta: Long = 0L,
        likeDelta: Long = 0L,
        dislikeDelta: Long = 0L
    ): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (viewDelta != 0L) updates["viewCount"] = FieldValue.increment(viewDelta)
            if (likeDelta != 0L) updates["likeCount"] = FieldValue.increment(likeDelta)
            if (dislikeDelta != 0L) updates["dislikeCount"] = FieldValue.increment(dislikeDelta)
            db.collection("videos").document(videoId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        videoId: String,
        text: String,
        authorName: String,
        parentCommentId: String = ""
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val path = "videos/$videoId/comments/$commentId"
        return try {
            val payload = mapOf(
                "videoId" to videoId,
                "userId" to uid,
                "authorName" to authorName.take(60).ifBlank { "Viewer" },
                "text" to text.trim().take(500),
                "parentCommentId" to parentCommentId,
                "likeCount" to 0L,
                "status" to "VISIBLE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("videos").document(videoId).collection("comments").document(commentId)
                .set(payload).await()
            db.collection("videos").document(videoId)
                .update(
                    mapOf(
                        "commentCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- MONETIZATION, ADS & AUTOMATIC ₹1,000 PAYOUTS ---

    suspend fun recordAdImpressionAndSplitRevenue(
        campaign: AdCampaign,
        video: Video,
        config: PlatformConfig,
        viewerCountryCode: String,
        isValidView: Boolean,
        isClick: Boolean = false
    ): Result<RevenueLedgerEntry?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            // 1. Update Ad Campaign impression / validView / click metrics
            val grossRevenue = if (isValidView) {
                when (campaign.placement) {
                    "PRE_ROLL" -> 25.0
                    "MID_ROLL" -> 30.0
                    "SHORTS_FEED" -> 15.0
                    else -> 18.0
                }
            } else 0.0

            val campUpdates = mutableMapOf<String, Any>(
                "impressions" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isValidView) {
                campUpdates["validViews"] = FieldValue.increment(1L)
                campUpdates["spentInr"] = FieldValue.increment(grossRevenue)
            }
            if (isClick) campUpdates["clicks"] = FieldValue.increment(1L)
            db.collection("ad_campaigns").document(campaign.id).update(campUpdates).await()

            if (!isValidView || !config.monetizationEnabled || !video.monetized ||
                viewerCountryCode in config.monetizationDisabledCountries
            ) {
                return Result.success(null)
            }

            // 2. Compute transparent 50:50 split (or admin-configured split summing to 100%)
            val split = MonetizationEngine.calculateAdRevenueSplit(
                grossAdRevenueInr = grossRevenue,
                creatorSharePercent = config.creatorSharePercent,
                platformSharePercent = config.platformSharePercent,
                isValidTraffic = true
            )

            val entryId = "rev_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val ledgerPayload = mapOf(
                "creatorId" to video.creatorId,
                "videoId" to video.id,
                "campaignId" to campaign.id,
                "format" to video.format,
                "countryCode" to viewerCountryCode,
                "grossRevenueInr" to split.grossRevenueInr,
                "networkFeeInr" to split.networkFeeInr,
                "netRevenueInr" to split.netRevenueInr,
                "creatorSharePercent" to split.creatorSharePercent,
                "platformSharePercent" to split.platformSharePercent,
                "creatorEarnedInr" to split.creatorEarnedInr,
                "platformEarnedInr" to split.platformEarnedInr,
                "status" to "CONFIRMED",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("revenue_ledger").document(entryId).set(ledgerPayload).await()

            // Credit video revenue
            db.collection("videos").document(video.id).update(
                mapOf(
                    "adRevenueInr" to FieldValue.increment(split.netRevenueInr),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Credit creator wallet if current user is creator (owner-isolated rule)
            if (video.creatorId == uid) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "estimatedEarningsInr" to FieldValue.increment(split.creatorEarnedInr),
                        "confirmedEarningsInr" to FieldValue.increment(split.creatorEarnedInr),
                        "availableBalanceInr" to FieldValue.increment(split.creatorEarnedInr),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val entry = RevenueLedgerEntry(
                id = entryId,
                creatorId = video.creatorId,
                videoId = video.id,
                campaignId = campaign.id,
                format = video.format,
                countryCode = viewerCountryCode,
                grossRevenueInr = split.grossRevenueInr,
                networkFeeInr = split.networkFeeInr,
                netRevenueInr = split.netRevenueInr,
                creatorSharePercent = split.creatorSharePercent,
                platformSharePercent = split.platformSharePercent,
                creatorEarnedInr = split.creatorEarnedInr,
                platformEarnedInr = split.platformEarnedInr,
                status = "CONFIRMED"
            )
            Result.success(entry)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "revenue_ledger")
            Result.failure(e)
        }
    }

    /**
     * Records a transparent valid ad settlement (e.g. ₹1,000 net ad revenue -> ₹500 Creator / ₹500 BharatTube)
     * and automatically triggers payout if India ₹1,000 threshold & KYC are met.
     */
    suspend fun settleEligibleAdBatchAndEvaluateAutoPayout(
        grossRevenueInr: Double,
        video: Video,
        user: UserProfile,
        config: PlatformConfig
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val split = MonetizationEngine.calculateAdRevenueSplit(
                grossAdRevenueInr = grossRevenueInr,
                networkFeeRate = 0.0,
                creatorSharePercent = config.creatorSharePercent,
                platformSharePercent = config.platformSharePercent,
                isValidTraffic = true
            )
            val entryId = "rev_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("revenue_ledger").document(entryId).set(
                mapOf(
                    "creatorId" to uid,
                    "videoId" to video.id,
                    "campaignId" to "seed_camp_1",
                    "format" to video.format,
                    "countryCode" to user.countryCode,
                    "grossRevenueInr" to split.grossRevenueInr,
                    "networkFeeInr" to split.networkFeeInr,
                    "netRevenueInr" to split.netRevenueInr,
                    "creatorSharePercent" to split.creatorSharePercent,
                    "platformSharePercent" to split.platformSharePercent,
                    "creatorEarnedInr" to split.creatorEarnedInr,
                    "platformEarnedInr" to split.platformEarnedInr,
                    "status" to "CONFIRMED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val newAvailable = MonetizationEngine.roundCurrency(user.availableBalanceInr + split.creatorEarnedInr)
            db.collection("users").document(uid).update(
                mapOf(
                    "estimatedEarningsInr" to FieldValue.increment(split.creatorEarnedInr),
                    "confirmedEarningsInr" to FieldValue.increment(split.creatorEarnedInr),
                    "availableBalanceInr" to newAvailable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            addNotificationInternal(
                uid,
                "EARNINGS",
                "Creator Share Credited (+₹${split.creatorEarnedInr})",
                "From ₹${split.netRevenueInr} net ad revenue (${split.creatorSharePercent.toInt()}:${split.platformSharePercent.toInt()} split)."
            )

            // Check automatic payout eligibility (India ₹1,000 threshold)
            val eval = MonetizationEngine.evaluateAutomaticPayout(
                availableBalanceInr = newAvailable,
                countryCode = user.countryCode,
                kycVerified = user.kycVerified,
                hasPaymentDetails = user.upiId.isNotBlank() || user.bankAccountMasked.isNotBlank(),
                minorMode = user.minorMode,
                creatorMonetizationEnabled = user.monetizationEnabled,
                config = config
            )

            if (eval.eligible) {
                val payoutRes = initiateAutomaticPayout(user, eval)
                val msg = if (payoutRes.isSuccess) {
                    "Credited ₹${split.creatorEarnedInr} & auto-initiated ₹${eval.grossAmountInr} payout (threshold met)!"
                } else {
                    "Credited ₹${split.creatorEarnedInr}. ${payoutRes.exceptionOrNull()?.localizedMessage}"
                }
                Result.success(msg)
            } else {
                Result.success("Credited ₹${split.creatorEarnedInr} (${split.creatorSharePercent.toInt()}% Creator / ${split.platformSharePercent.toInt()}% BharatTube). ${eval.reason}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "revenue_ledger")
            Result.failure(e)
        }
    }

    suspend fun initiateAutomaticPayout(
        user: UserProfile,
        eval: com.example.domain.PayoutEvaluation
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val idempotencyKey = "IDEM_${uid.take(6)}_${System.currentTimeMillis() / 60000L}"
        val payoutId = "pay_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val txnId = "BT_TXN_${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
        val spec = GlobalCatalog.countryByCode(user.countryCode)
        val method = if (user.upiId.isNotBlank() && user.countryCode == "IN") "UPI"
        else if (user.countryCode == "IN") "BANK_TRANSFER"
        else "INTL_WIRE"
        val dest = user.upiId.ifBlank { user.bankAccountMasked }.ifBlank { "Verified Account" }

        return try {
            // Duplicate payment protection check
            val existing = db.collection("payouts")
                .whereEqualTo("creatorId", uid)
                .whereEqualTo("idempotencyKey", idempotencyKey)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.failure(IllegalStateException("Duplicate payout request blocked by idempotency protection."))
            }

            // Never mark payout COMPLETED until payment provider webhook/confirmation; initialize as PROCESSING
            db.collection("payouts").document(payoutId).set(
                mapOf(
                    "creatorId" to uid,
                    "countryCode" to user.countryCode,
                    "currencyCode" to spec.currencyCode,
                    "amountInr" to eval.grossAmountInr,
                    "taxDeductedInr" to eval.tdsTaxInr,
                    "netPayoutInr" to eval.netPayoutInr,
                    "method" to method,
                    "destinationMasked" to dest.take(80),
                    "transactionId" to txnId,
                    "idempotencyKey" to idempotencyKey,
                    "status" to "PROCESSING",
                    "failureReason" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "availableBalanceInr" to 0.0,
                    "pendingBalanceInr" to FieldValue.increment(eval.netPayoutInr),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            addNotificationInternal(
                uid,
                "PAYOUT",
                "Auto-Payout Initiated ($txnId)",
                "₹${eval.netPayoutInr} (after ₹${eval.tdsTaxInr} TDS) submitted via $method. Awaiting payment gateway confirmation."
            )
            Result.success(txnId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "payouts/$payoutId")
            Result.failure(e)
        }
    }

    suspend fun retryFailedPayout(payoutId: String): Result<Unit> {
        val path = "payouts/$payoutId"
        return try {
            requireUserId()
            db.collection("payouts").document(payoutId).update(
                mapOf(
                    "status" to "PROCESSING",
                    "failureReason" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createAdCampaign(
        advertiserName: String,
        title: String,
        ctaText: String,
        targetUrl: String,
        placement: String,
        targetCountries: List<String>,
        targetLanguages: List<String>,
        budgetInr: Double
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val campId = "camp_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("ad_campaigns").document(campId).set(
                mapOf(
                    "advertiserId" to uid,
                    "advertiserName" to advertiserName.take(60).ifBlank { "Advertiser" },
                    "title" to title.trim().take(120),
                    "ctaText" to ctaText.trim().take(40).ifBlank { "Learn More" },
                    "targetUrl" to targetUrl.trim().take(500),
                    "placement" to placement,
                    "targetCountries" to targetCountries.take(30),
                    "targetLanguages" to targetLanguages.take(30),
                    "budgetInr" to budgetInr.coerceAtLeast(100.0),
                    "spentInr" to 0.0,
                    "impressions" to 0L,
                    "validViews" to 0L,
                    "clicks" to 0L,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "ad_campaigns/$campId")
            Result.failure(e)
        }
    }

    suspend fun toggleCampaignStatus(campaignId: String, newStatus: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "ad_campaigns/$campaignId")
            Result.failure(e)
        }
    }

    // --- USER PROFILE, HISTORY, PLAYLISTS, SUBSCRIPTIONS ---

    suspend fun updateUserSettings(updates: Map<String, Any>): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val safeUpdates = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun normalizeHandle(raw: String): String {
        val cleaned = raw.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")
        return "@${cleaned.ifBlank { "creator" }.take(30)}"
    }

    private fun normalizeHandleKey(raw: String): String =
        raw.trim().removePrefix("@").lowercase().replace(Regex("[^a-z0-9_]"), "").take(32)

    suspend fun isHandleAvailable(rawHandle: String): Boolean {
        val key = normalizeHandleKey(rawHandle)
        if (key.length < 2) return false
        return try {
            !db.collection("channel_handles").document(key).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            true
        }
    }

    suspend fun completeAccountOnboarding(
        displayName: String,
        photoUrl: String,
        countryCode: String,
        languageCode: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.trim().take(60).ifBlank { "BharatTube Creator" },
                    "photoUrl" to photoUrl.trim().take(500),
                    "countryCode" to countryCode,
                    "languageCode" to languageCode,
                    "onboardingCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun createChannel(
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
        languageCode: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val owned = db.collection("channels").whereEqualTo("ownerId", uid).get().await()
            if (owned.size() >= MAX_CHANNELS_PER_ACCOUNT) {
                return Result.failure(
                    IllegalStateException("Platform policy allows up to $MAX_CHANNELS_PER_ACCOUNT channels per BharatTube account.")
                )
            }
            val handle = normalizeHandle(rawHandle)
            val handleKey = normalizeHandleKey(handle)
            if (handleKey.length < 2) {
                return Result.failure(IllegalArgumentException("Handle must be at least 2 letters or numbers."))
            }
            val handleRef = db.collection("channel_handles").document(handleKey)
            if (handleRef.get().await().exists()) {
                return Result.failure(IllegalStateException("Handle $handle is already taken. Choose another unique handle."))
            }

            val channelId = "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val firebaseUser = auth.currentUser
            val verifiedEmail = firebaseUser?.isEmailVerified == true
            val proofLabel = if (verifiedEmail && !firebaseUser?.email.isNullOrBlank()) {
                "Google OAuth Verified (${firebaseUser?.email})"
            } else {
                ""
            }
            val cleanLinks = socialLinks.map { it.trim().take(180) }.filter { it.isNotBlank() }.take(5)

            handleRef.set(
                mapOf(
                    "ownerId" to uid,
                    "channelId" to channelId,
                    "handle" to handle,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("channels").document(channelId).set(
                mapOf(
                    "ownerId" to uid,
                    "name" to name.trim().take(60),
                    "handle" to handle,
                    "description" to description.trim().take(500),
                    "category" to category.take(40).ifBlank { "Technology" },
                    "avatarUrl" to avatarUrl.take(500),
                    "avatarKey" to avatarKey.take(60).ifBlank { "img_hero_bharattube" },
                    "bannerUrl" to bannerUrl.take(500),
                    "bannerKey" to bannerKey.take(60).ifBlank { "img_channel_banner" },
                    "socialLinks" to cleanLinks,
                    "appearanceTheme" to appearanceTheme.ifBlank { "SAFFRON" },
                    "countryCode" to countryCode,
                    "languageCode" to languageCode,
                    "channelStatus" to "ACTIVE",
                    "subscriberCount" to 1L,
                    "followerCount" to 1L,
                    "totalViews" to 0L,
                    "verified" to false,
                    "ownershipVerified" to verifiedEmail,
                    "ownershipProofLabel" to proofLabel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "activeChannelId" to channelId,
                    "onboardingCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            addNotificationInternal(
                uid,
                "CHANNEL",
                "Channel Created: ${name.trim()}",
                "Your channel $handle is live under your BharatTube account."
            )
            Result.success(channelId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "channels")
            Result.failure(e)
        }
    }

    suspend fun switchActiveChannel(channelId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "activeChannelId" to channelId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun verifyChannelOwnership(channelId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!user.isEmailVerified) {
            return Result.failure(IllegalStateException("Verified Google OAuth email is required for ownership verification."))
        }
        return try {
            db.collection("channels").document(channelId).update(
                mapOf(
                    "ownershipVerified" to true,
                    "ownershipProofLabel" to "Google OAuth Verified (${user.email ?: uid})",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "channels/$channelId")
            Result.failure(e)
        }
    }

    suspend fun moderateComment(videoId: String, commentId: String, newStatus: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("videos").document(videoId).collection("comments").document(commentId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/$videoId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun updateChannelFull(
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
        languageCode: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val targetChannelId = channelId.ifBlank { uid }
        return try {
            val newHandle = normalizeHandle(rawHandle)
            val newKey = normalizeHandleKey(newHandle)
            val oldKey = normalizeHandleKey(oldHandle)
            if (newKey != oldKey) {
                if (newKey.length < 2) {
                    return Result.failure(IllegalArgumentException("Handle must have at least 2 characters."))
                }
                val newRef = db.collection("channel_handles").document(newKey)
                if (newRef.get().await().exists()) {
                    return Result.failure(IllegalStateException("Handle $newHandle is already claimed."))
                }
                newRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "channelId" to targetChannelId,
                        "handle" to newHandle,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (oldKey.isNotBlank()) {
                    runCatching { db.collection("channel_handles").document(oldKey).delete().await() }
                }
            }
            val cleanLinks = socialLinks.map { it.trim().take(180) }.filter { it.isNotBlank() }.take(5)
            db.collection("channels").document(targetChannelId).update(
                mapOf(
                    "name" to name.trim().take(60),
                    "handle" to newHandle,
                    "description" to description.trim().take(500),
                    "category" to category.take(40),
                    "avatarUrl" to avatarUrl.take(500),
                    "bannerUrl" to bannerUrl.take(500),
                    "socialLinks" to cleanLinks,
                    "appearanceTheme" to appearanceTheme,
                    "countryCode" to countryCode,
                    "languageCode" to languageCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "channels/$targetChannelId")
            Result.failure(e)
        }
    }

    suspend fun updateChannelProfile(
        name: String,
        handle: String,
        description: String,
        countryCode: String,
        languageCode: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return updateChannelFull(
            channelId = uid,
            name = name,
            rawHandle = handle,
            oldHandle = handle,
            description = description,
            category = "Technology",
            avatarUrl = "",
            bannerUrl = "",
            socialLinks = emptyList(),
            appearanceTheme = "SAFFRON",
            countryCode = countryCode,
            languageCode = languageCode
        )
    }

    suspend fun toggleSubscription(
        channel: Channel,
        currentlySubscribed: Boolean,
        bellEnabled: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val subRef = db.collection("users").document(uid).collection("subscriptions").document(channel.id)
        return try {
            val newSubscribed = !currentlySubscribed
            subRef.set(
                mapOf(
                    "channelId" to channel.id,
                    "channelName" to channel.name.take(60),
                    "channelHandle" to channel.handle.take(40),
                    "subscribed" to newSubscribed,
                    "following" to newSubscribed,
                    "bellEnabled" to bellEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("channels").document(channel.id).update(
                mapOf(
                    "subscriberCount" to FieldValue.increment(if (newSubscribed) 1L else -1L),
                    "followerCount" to FieldValue.increment(if (newSubscribed) 1L else -1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/subscriptions/${channel.id}")
            Result.failure(e)
        }
    }

    suspend fun saveWatchHistory(
        video: Video,
        positionMs: Long,
        durationMs: Long,
        watchHistoryPaused: Boolean
    ): Result<Unit> {
        if (watchHistoryPaused) return Result.success(Unit)
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("watchHistory").document(video.id).set(
                mapOf(
                    "videoId" to video.id,
                    "title" to video.title.take(120),
                    "creatorName" to video.creatorName.take(60),
                    "format" to video.format,
                    "positionMs" to positionMs.coerceAtLeast(0L),
                    "durationMs" to durationMs.coerceAtLeast(1L),
                    "thumbnailKey" to video.thumbnailKey.take(100),
                    "watchedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/watchHistory/${video.id}")
            Result.failure(e)
        }
    }

    suspend fun clearWatchHistory(): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val docs = db.collection("users").document(uid).collection("watchHistory").get().await()
            docs.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/watchHistory")
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(title: String, description: String, privacy: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val plId = "pl_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("users").document(uid).collection("playlists").document(plId).set(
                mapOf(
                    "title" to title.trim().take(80),
                    "description" to description.trim().take(300),
                    "privacy" to privacy,
                    "videoIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/playlists/$plId")
            Result.failure(e)
        }
    }

    suspend fun addVideoToPlaylist(playlistId: String, videoId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("playlists").document(playlistId).update(
                mapOf(
                    "videoIds" to FieldValue.arrayUnion(videoId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/playlists/$playlistId")
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("playlists").document(playlistId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/playlists/$playlistId")
            Result.failure(e)
        }
    }

    suspend fun saveSearchQuery(query: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val clean = query.trim().take(120)
        if (clean.isEmpty()) return Result.success(Unit)
        val docId = "q_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("users").document(uid).collection("searchHistory").document(docId).set(
                mapOf(
                    "query" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/searchHistory/$docId")
            Result.failure(e)
        }
    }

    suspend fun clearSearchHistory(): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val docs = db.collection("users").document(uid).collection("searchHistory").get().await()
            docs.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/searchHistory")
            Result.failure(e)
        }
    }

    // --- LIVE STREAMING & MESSAGING ---

    suspend fun startLiveStream(
        title: String,
        description: String,
        creatorName: String,
        languageCode: String,
        countryCode: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val streamId = "live_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("live_streams").document(streamId).set(
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to creatorName.take(60),
                    "title" to title.trim().take(120),
                    "description" to description.trim().take(500),
                    "streamUrl" to GlobalCatalog.STREAM_MELTDOWNS,
                    "category" to "Live",
                    "languageCode" to languageCode,
                    "countryCode" to countryCode,
                    "status" to "LIVE",
                    "viewerCount" to 1L,
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(streamId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "live_streams/$streamId")
            Result.failure(e)
        }
    }

    suspend fun sendLiveChatMessage(streamId: String, senderName: String, text: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgId = "lmsg_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("live_streams").document(streamId).collection("messages").document(msgId).set(
                mapOf(
                    "streamId" to streamId,
                    "userId" to uid,
                    "senderName" to senderName.take(60),
                    "text" to text.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "live_streams/$streamId/messages/$msgId")
            Result.failure(e)
        }
    }

    suspend fun blockUserInLiveStream(streamId: String, targetUserId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("live_streams").document(streamId).update(
                mapOf(
                    "blockedUserIds" to FieldValue.arrayUnion(targetUserId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "live_streams/$streamId")
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("live_streams").document(streamId).update(
                mapOf(
                    "status" to "ENDED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "live_streams/$streamId")
            Result.failure(e)
        }
    }

    suspend fun createChatThread(title: String, groupChat: Boolean, otherMemberUids: List<String> = emptyList()): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val chatId = "chat_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val members = (listOf(uid) + otherMemberUids).distinct().take(20)
        return try {
            db.collection("chats").document(chatId).set(
                mapOf(
                    "title" to title.trim().take(80),
                    "groupChat" to groupChat,
                    "memberUids" to members,
                    "lastMessage" to "Thread created",
                    "blockedByUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chat: ChatThread,
        senderName: String,
        text: String,
        messageType: String = "TEXT",
        attachmentLabel: String = ""
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("chats").document(chat.id).collection("messages").document(msgId).set(
                mapOf(
                    "chatId" to chat.id,
                    "senderId" to uid,
                    "senderName" to senderName.take(60),
                    "memberUids" to chat.memberUids,
                    "messageType" to messageType,
                    "text" to text.trim().take(1000),
                    "attachmentLabel" to attachmentLabel.take(120),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessage" to text.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}/messages/$msgId")
            Result.failure(e)
        }
    }

    // --- COPYRIGHT, SUPPORT, NOTIFICATIONS & ACCOUNT DELETION ---

    suspend fun submitCopyrightAppeal(claimId: String, appealNote: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("copyright_claims").document(claimId).update(
                mapOf(
                    "status" to "APPEALED",
                    "appealNote" to appealNote.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "copyright_claims/$claimId")
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        category: String,
        subject: String,
        description: String,
        targetId: String = ""
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val ticketId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            db.collection("support_tickets").document(ticketId).set(
                mapOf(
                    "userId" to uid,
                    "category" to category,
                    "subject" to subject.trim().take(120),
                    "description" to description.trim().take(1000),
                    "targetId" to targetId.take(128),
                    "status" to "OPEN",
                    "resolutionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "support_tickets/$ticketId")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("notifications").document(notifId)
                .update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notifId")
            Result.failure(e)
        }
    }

    private suspend fun addNotificationInternal(
        userId: String,
        category: String,
        title: String,
        body: String
    ) {
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        db.collection("users").document(userId).collection("notifications").document(notifId).set(
            mapOf(
                "category" to category,
                "title" to title.take(100),
                "body" to body.take(300),
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- SUPER ADMIN ACTIONS ---

    suspend fun savePlatformConfig(config: PlatformConfig, auditReason: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (!MonetizationEngine.isValidShareSplit(config.creatorSharePercent, config.platformSharePercent)) {
            return Result.failure(IllegalArgumentException("Creator and BharatTube revenue shares must sum to 100%."))
        }
        return try {
            val payload = mapOf(
                "creatorSharePercent" to config.creatorSharePercent,
                "platformSharePercent" to config.platformSharePercent,
                "indiaMinPayoutInr" to config.indiaMinPayoutInr,
                "adsEnabled" to config.adsEnabled,
                "preRollAdsEnabled" to config.preRollAdsEnabled,
                "midRollAdsEnabled" to config.midRollAdsEnabled,
                "shortsAdsEnabled" to config.shortsAdsEnabled,
                "sponsoredHomeAdsEnabled" to config.sponsoredHomeAdsEnabled,
                "adFrequency" to config.adFrequency,
                "monetizationEnabled" to config.monetizationEnabled,
                "autoPayoutEnabled" to config.autoPayoutEnabled,
                "registrationEnabled" to config.registrationEnabled,
                "uploadsEnabled" to config.uploadsEnabled,
                "longVideosEnabled" to config.longVideosEnabled,
                "shortsEnabled" to config.shortsEnabled,
                "liveStreamingEnabled" to config.liveStreamingEnabled,
                "commentsEnabled" to config.commentsEnabled,
                "likesEnabled" to config.likesEnabled,
                "subscriptionsEnabled" to config.subscriptionsEnabled,
                "followingEnabled" to config.followingEnabled,
                "chatEnabled" to config.chatEnabled,
                "searchEnabled" to config.searchEnabled,
                "notificationsEnabled" to config.notificationsEnabled,
                "downloadsEnabled" to config.downloadsEnabled,
                "communityPostsEnabled" to config.communityPostsEnabled,
                "maintenanceMode" to config.maintenanceMode,
                "announcementText" to config.announcementText.take(300),
                "disabledCountries" to config.disabledCountries.take(50),
                "disabledLanguages" to config.disabledLanguages.take(50),
                "adDisabledCountries" to config.adDisabledCountries.take(50),
                "monetizationDisabledCountries" to config.monetizationDisabledCountries.take(50),
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("platform_config").document("global").set(payload).await()
            writeAuditLog(uid, "UPDATE_PLATFORM_CONFIG", "platform_config", "global", auditReason)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "platform_config/global")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserModeration(
        targetUserId: String,
        accountStatus: String,
        uploadRestricted: Boolean,
        commentRestricted: Boolean,
        monetizationEnabled: Boolean
    ): Result<Unit> {
        val adminUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "accountStatus" to accountStatus,
                    "uploadRestricted" to uploadRestricted,
                    "commentRestricted" to commentRestricted,
                    "monetizationEnabled" to monetizationEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid,
                "MODERATE_USER",
                "user",
                targetUserId,
                "Status=$accountStatus, uploadRestricted=$uploadRestricted, commentRestricted=$commentRestricted, monetization=$monetizationEnabled"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun adminResolveCopyrightClaim(claimId: String, newStatus: String): Result<Unit> {
        val adminUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("copyright_claims").document(claimId).update(
                mapOf(
                    "status" to newStatus,
                    "strikeCount" to if (newStatus == "UPHELD") 1L else 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(adminUid, "REVIEW_COPYRIGHT", "copyright_claim", claimId, "Decision=$newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "copyright_claims/$claimId")
            Result.failure(e)
        }
    }

    suspend fun adminResolveTicket(ticketId: String, resolutionNote: String): Result<Unit> {
        val adminUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to "RESOLVED",
                    "resolutionNote" to resolutionNote.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(adminUid, "RESOLVE_TICKET", "support_ticket", ticketId, resolutionNote)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "support_tickets/$ticketId")
            Result.failure(e)
        }
    }

    private suspend fun writeAuditLog(
        adminUid: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        db.collection("admin_audit_logs").document(logId).set(
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to (auth.currentUser?.email ?: ADMIN_EMAIL),
                "action" to action.take(80),
                "targetType" to targetType.take(40),
                "targetId" to targetId.take(128),
                "details" to details.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    companion object {
        const val ADMIN_EMAIL = "parmeshwarparmeshwar38324@gmail.com"
        const val MAX_CHANNELS_PER_ACCOUNT = 5
    }
}
