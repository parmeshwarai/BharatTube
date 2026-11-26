package com.example

import com.example.data.model.PlatformConfig
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.domain.CopyrightFingerprintEngine
import com.example.domain.MonetizationEngine
import com.example.domain.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BharatTubeDomainTest {

    @Test
    fun `50_50 revenue split distributes net ad revenue equally`() {
        val result = MonetizationEngine.calculateAdRevenueSplit(
            grossAdRevenueInr = 1000.0,
            networkFeeRate = 0.0,
            creatorSharePercent = 50.0,
            platformSharePercent = 50.0,
            isValidTraffic = true
        )
        assertEquals(1000.0, result.netRevenueInr, 0.01)
        assertEquals(500.0, result.creatorEarnedInr, 0.01)
        assertEquals(500.0, result.platformEarnedInr, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid revenue share split not summing to 100 throws`() {
        MonetizationEngine.calculateAdRevenueSplit(
            grossAdRevenueInr = 1000.0,
            creatorSharePercent = 60.0,
            platformSharePercent = 30.0
        )
    }

    @Test
    fun `india automatic payout triggers only at 1000 INR with kyc and payment details`() {
        val config = PlatformConfig(indiaMinPayoutInr = 1000.0, autoPayoutEnabled = true)

        val belowThreshold = MonetizationEngine.evaluateAutomaticPayout(
            availableBalanceInr = 999.0,
            countryCode = "IN",
            kycVerified = true,
            hasPaymentDetails = true,
            minorMode = false,
            creatorMonetizationEnabled = true,
            config = config
        )
        assertFalse(belowThreshold.eligible)

        val eligibleAt1000 = MonetizationEngine.evaluateAutomaticPayout(
            availableBalanceInr = 1000.0,
            countryCode = "IN",
            kycVerified = true,
            hasPaymentDetails = true,
            minorMode = false,
            creatorMonetizationEnabled = true,
            config = config
        )
        assertTrue(eligibleAt1000.eligible)
        assertEquals(100.0, eligibleAt1000.tdsTaxInr, 0.01)
        assertEquals(900.0, eligibleAt1000.netPayoutInr, 0.01)

        val blockedMinor = MonetizationEngine.evaluateAutomaticPayout(
            availableBalanceInr = 2500.0,
            countryCode = "IN",
            kycVerified = true,
            hasPaymentDetails = true,
            minorMode = true,
            creatorMonetizationEnabled = true,
            config = config
        )
        assertFalse(blockedMinor.eligible)
    }

    @Test
    fun `copyright fingerprint flags duplicate hash as warning without blocking`() {
        val hash = CopyrightFingerprintEngine.computeContentHash("My Video", "stream_1", 120L)
        val duplicateCheck = CopyrightFingerprintEngine.scanUpload(
            title = "My Video",
            musicTitle = "Original",
            mediaUri = "stream_1",
            durationSec = 120L,
            existingHashes = setOf(hash)
        )
        assertTrue(duplicateCheck.similarityDetected)
        assertEquals("WARNING", duplicateCheck.status)

        val cleanCheck = CopyrightFingerprintEngine.scanUpload(
            title = "Fresh Original Documentary",
            musicTitle = "Saffron Horizon",
            mediaUri = "stream_new",
            durationSec = 300L,
            existingHashes = setOf(hash)
        )
        assertFalse(cleanCheck.similarityDetected)
        assertEquals("CLEAR", cleanCheck.status)
    }

    @Test
    fun `recommendation engine prioritizes user language country and interests`() {
        val user = UserProfile(
            userId = "u1",
            countryCode = "IN",
            languageCode = "hi",
            interests = listOf("Technology")
        )
        val match = Video(
            id = "v_match",
            title = "Hindi Tech Review",
            category = "Technology",
            languageCode = "hi",
            countryCode = "IN",
            visibility = "PUBLIC"
        )
        val nonMatch = Video(
            id = "v_other",
            title = "Cooking Vlog",
            category = "Cooking",
            languageCode = "fr",
            countryCode = "FR",
            visibility = "PUBLIC"
        )
        val ranked = RecommendationEngine.rankPersonalizedFeed(
            videos = listOf(nonMatch, match),
            user = user,
            watchHistory = emptyList(),
            subscribedChannelIds = emptySet()
        )
        assertEquals("v_match", ranked.first().id)
    }
}
