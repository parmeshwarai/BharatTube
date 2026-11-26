package com.example.domain

import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.WatchHistoryItem
import kotlin.math.ln

object RecommendationEngine {

    fun rankPersonalizedFeed(
        videos: List<Video>,
        user: UserProfile,
        watchHistory: List<WatchHistoryItem>,
        subscribedChannelIds: Set<String>
    ): List<Video> {
        val watchedVideoIds = watchHistory.map { it.videoId }.toSet()
        val interestSet = user.interests.map { it.lowercase() }.toSet()

        return videos
            .filter { video ->
                video.visibility == "PUBLIC" &&
                    video.copyrightStatus != "BLOCKED" &&
                    (!user.restrictedMode || video.category != "Mature")
            }
            .sortedByDescending { video ->
                var score = 0.0
                if (video.category.lowercase() in interestSet) score += 40.0
                if (video.languageCode.equals(user.languageCode, ignoreCase = true)) score += 30.0
                if (video.countryCode.equals(user.countryCode, ignoreCase = true)) score += 20.0
                if (video.creatorId in subscribedChannelIds) score += 25.0
                if (video.id !in watchedVideoIds) score += 10.0
                val engagement = video.likeCount * 2.0 + video.commentCount * 3.0 + video.viewCount * 0.05
                score += ln(1.0 + engagement.coerceAtLeast(0.0)) * 5.0
                score
            }
    }

    fun filterSearch(
        videos: List<Video>,
        query: String,
        category: String,
        countryFilter: String,
        languageFilter: String,
        sortMode: String
    ): List<Video> {
        val q = query.trim().lowercase()
        val filtered = videos.filter { v ->
            v.visibility == "PUBLIC" &&
                (category == "All" || v.category.equals(category, ignoreCase = true)) &&
                (countryFilter == "ALL" || v.countryCode.equals(countryFilter, ignoreCase = true)) &&
                (languageFilter == "ALL" || v.languageCode.equals(languageFilter, ignoreCase = true)) &&
                (q.isEmpty() ||
                    v.title.lowercase().contains(q) ||
                    v.description.lowercase().contains(q) ||
                    v.creatorName.lowercase().contains(q) ||
                    v.category.lowercase().contains(q) ||
                    v.hashtags.any { it.lowercase().contains(q) })
        }
        return when (sortMode) {
            "VIEWS" -> filtered.sortedByDescending { it.viewCount }
            "POPULAR" -> filtered.sortedByDescending { it.likeCount * 2 + it.viewCount }
            else -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }
}
