package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "global",
    val creatorSharePercent: Double = 50.0,
    val platformSharePercent: Double = 50.0,
    val indiaMinPayoutInr: Double = 1000.0,
    val adsEnabled: Boolean = true,
    val preRollAdsEnabled: Boolean = true,
    val midRollAdsEnabled: Boolean = true,
    val shortsAdsEnabled: Boolean = true,
    val sponsoredHomeAdsEnabled: Boolean = true,
    val adFrequency: Long = 3L,
    val monetizationEnabled: Boolean = true,
    val autoPayoutEnabled: Boolean = true,
    val registrationEnabled: Boolean = true,
    val uploadsEnabled: Boolean = true,
    val longVideosEnabled: Boolean = true,
    val shortsEnabled: Boolean = true,
    val liveStreamingEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val likesEnabled: Boolean = true,
    val subscriptionsEnabled: Boolean = true,
    val followingEnabled: Boolean = true,
    val chatEnabled: Boolean = true,
    val searchEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val downloadsEnabled: Boolean = true,
    val communityPostsEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val announcementText: String = "BharatTube Global: 50:50 Creator Revenue Share & ₹1,000 Auto-Payouts Live!",
    val disabledCountries: List<String> = emptyList(),
    val disabledLanguages: List<String> = emptyList(),
    val adDisabledCountries: List<String> = emptyList(),
    val monetizationDisabledCountries: List<String> = emptyList(),
    val updatedBy: String = "system",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val countryCode: String = "IN",
    val languageCode: String = "en",
    val role: String = "CREATOR",
    val accountStatus: String = "ACTIVE",
    val restrictedMode: Boolean = false,
    val minorMode: Boolean = false,
    val twoFactorEnabled: Boolean = true,
    val watchHistoryPaused: Boolean = false,
    val uploadRestricted: Boolean = false,
    val commentRestricted: Boolean = false,
    val monetizationEnabled: Boolean = true,
    val kycVerified: Boolean = false,
    val upiId: String = "",
    val bankAccountMasked: String = "",
    val estimatedEarningsInr: Double = 0.0,
    val confirmedEarningsInr: Double = 0.0,
    val availableBalanceInr: Double = 0.0,
    val pendingBalanceInr: Double = 0.0,
    val totalPaidOutInr: Double = 0.0,
    val fontScaleMultiplier: Double = 1.0,
    val highContrastMode: Boolean = false,
    val darkThemeMode: String = "DARK",
    val interests: List<String> = listOf("Technology", "Music", "Education"),
    val notifSubscribers: Boolean = true,
    val notifComments: Boolean = true,
    val notifUploads: Boolean = true,
    val notifEarnings: Boolean = true,
    val notifPayouts: Boolean = true,
    val notifSecurity: Boolean = true,
    val bharatTubeId: String = "",
    val onboardingCompleted: Boolean = false,
    val activeChannelId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Channel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val handle: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val bannerKey: String = "img_channel_banner",
    val countryCode: String = "IN",
    val languageCode: String = "en",
    val channelStatus: String = "ACTIVE",
    val subscriberCount: Long = 0L,
    val followerCount: Long = 0L,
    val totalViews: Long = 0L,
    val verified: Boolean = false,
    val category: String = "Technology",
    val avatarKey: String = "img_hero_bharattube",
    val bannerUrl: String = "",
    val socialLinks: List<String> = emptyList(),
    val appearanceTheme: String = "SAFFRON",
    val ownershipVerified: Boolean = false,
    val ownershipProofLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Video(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorHandle: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailKey: String = "img_thumb_tech",
    val thumbnailUrl: String = "",
    val format: String = "LONG",
    val visibility: String = "PUBLIC",
    val category: String = "Technology",
    val languageCode: String = "en",
    val countryCode: String = "IN",
    val hashtags: List<String> = emptyList(),
    val durationSec: Long = 0L,
    val maxResolution: String = "1080p",
    val musicTitle: String = "",
    val musicLicenseId: String = "",
    val chaptersSummary: String = "",
    val subtitlesText: String = "",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val dislikeCount: Long = 0L,
    val commentCount: Long = 0L,
    val copyrightHash: String = "",
    val copyrightStatus: String = "CLEAR",
    val monetized: Boolean = true,
    val adRevenueInr: Double = 0.0,
    val scheduledPublishAtMs: Long = 0L,
    val channelId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoComment(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    val parentCommentId: String = "",
    val likeCount: Long = 0L,
    val status: String = "VISIBLE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val streamUrl: String = "",
    val category: String = "Live",
    val languageCode: String = "en",
    val countryCode: String = "IN",
    val status: String = "LIVE",
    val viewerCount: Long = 0L,
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveChatMessage(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val userId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdCampaign(
    @DocumentId val id: String = "",
    val advertiserId: String = "",
    val advertiserName: String = "",
    val title: String = "",
    val ctaText: String = "Learn More",
    val targetUrl: String = "",
    val placement: String = "PRE_ROLL",
    val targetCountries: List<String> = listOf("IN"),
    val targetLanguages: List<String> = listOf("en", "hi"),
    val budgetInr: Double = 5000.0,
    val spentInr: Double = 0.0,
    val impressions: Long = 0L,
    val validViews: Long = 0L,
    val clicks: Long = 0L,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RevenueLedgerEntry(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val videoId: String = "",
    val campaignId: String = "",
    val format: String = "LONG",
    val countryCode: String = "IN",
    val grossRevenueInr: Double = 0.0,
    val networkFeeInr: Double = 0.0,
    val netRevenueInr: Double = 0.0,
    val creatorSharePercent: Double = 50.0,
    val platformSharePercent: Double = 50.0,
    val creatorEarnedInr: Double = 0.0,
    val platformEarnedInr: Double = 0.0,
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRecord(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val countryCode: String = "IN",
    val currencyCode: String = "INR",
    val amountInr: Double = 0.0,
    val taxDeductedInr: Double = 0.0,
    val netPayoutInr: Double = 0.0,
    val method: String = "UPI",
    val destinationMasked: String = "",
    val transactionId: String = "",
    val idempotencyKey: String = "",
    val status: String = "PROCESSING",
    val failureReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val title: String = "",
    val groupChat: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val blockedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = "TEXT",
    val text: String = "",
    val attachmentLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CopyrightClaim(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetCreatorId: String = "",
    val videoId: String = "",
    val videoTitle: String = "",
    val matchType: String = "AUDIO_MATCH",
    val similarityPercent: Long = 0L,
    val status: String = "WARNING",
    val appealNote: String = "",
    val strikeCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "GENERAL",
    val subject: String = "",
    val description: String = "",
    val targetId: String = "",
    val status: String = "OPEN",
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WatchHistoryItem(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val title: String = "",
    val creatorName: String = "",
    val format: String = "LONG",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val thumbnailKey: String = "img_thumb_tech",
    @ServerTimestamp val watchedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Playlist(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val privacy: String = "PRIVATE",
    val videoIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionItem(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val subscribed: Boolean = true,
    val following: Boolean = true,
    val bellEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val category: String = "SYSTEM",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SearchHistoryEntry(
    @DocumentId val id: String = "",
    val query: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeviceSession(
    @DocumentId val id: String = "",
    val deviceName: String = "",
    val platform: String = "Android",
    val locationLabel: String = "",
    val active: Boolean = true,
    @ServerTimestamp val lastActiveAt: Timestamp? = null
)
