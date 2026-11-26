package com.example.domain

import com.example.data.model.GlobalCatalog
import com.example.data.model.PlatformConfig
import kotlin.math.roundToLong

data class RevenueSplitResult(
    val grossRevenueInr: Double,
    val networkFeeInr: Double,
    val taxDeductionInr: Double,
    val netRevenueInr: Double,
    val creatorSharePercent: Double,
    val platformSharePercent: Double,
    val creatorEarnedInr: Double,
    val platformEarnedInr: Double
)

data class PayoutEvaluation(
    val eligible: Boolean,
    val thresholdInr: Double,
    val grossAmountInr: Double,
    val tdsTaxInr: Double,
    val netPayoutInr: Double,
    val reason: String
)

object MonetizationEngine {
    const val DEFAULT_CREATOR_SHARE = 50.0
    const val DEFAULT_PLATFORM_SHARE = 50.0
    const val INDIA_MIN_PAYOUT_INR = 1000.0
    const val DEFAULT_NETWORK_FEE_RATE = 0.12
    const val INDIA_TDS_RATE = 0.10

    fun roundCurrency(value: Double): Double =
        (value * 100.0).roundToLong() / 100.0

    fun isValidShareSplit(creatorPercent: Double, platformPercent: Double): Boolean {
        if (creatorPercent < 0.0 || platformPercent < 0.0) return false
        return kotlin.math.abs((creatorPercent + platformPercent) - 100.0) < 0.001
    }

    /**
     * Computes transparent Creator vs BharatTube revenue split from gross ad revenue.
     * Default split: 50% Creator / 50% BharatTube on net eligible ad revenue.
     */
    fun calculateAdRevenueSplit(
        grossAdRevenueInr: Double,
        networkFeeRate: Double = DEFAULT_NETWORK_FEE_RATE,
        creatorSharePercent: Double = DEFAULT_CREATOR_SHARE,
        platformSharePercent: Double = DEFAULT_PLATFORM_SHARE,
        isValidTraffic: Boolean = true
    ): RevenueSplitResult {
        require(isValidShareSplit(creatorSharePercent, platformSharePercent)) {
            "Revenue sharing percentages must equal 100% (got $creatorSharePercent + $platformSharePercent)"
        }
        if (!isValidTraffic || grossAdRevenueInr <= 0.0) {
            return RevenueSplitResult(
                grossRevenueInr = 0.0,
                networkFeeInr = 0.0,
                taxDeductionInr = 0.0,
                netRevenueInr = 0.0,
                creatorSharePercent = creatorSharePercent,
                platformSharePercent = platformSharePercent,
                creatorEarnedInr = 0.0,
                platformEarnedInr = 0.0
            )
        }
        val fee = roundCurrency(grossAdRevenueInr * networkFeeRate.coerceIn(0.0, 0.5))
        val net = roundCurrency((grossAdRevenueInr - fee).coerceAtLeast(0.0))
        val creatorShare = roundCurrency(net * (creatorSharePercent / 100.0))
        val platformShare = roundCurrency(net - creatorShare)
        return RevenueSplitResult(
            grossRevenueInr = roundCurrency(grossAdRevenueInr),
            networkFeeInr = fee,
            taxDeductionInr = 0.0,
            netRevenueInr = net,
            creatorSharePercent = creatorSharePercent,
            platformSharePercent = platformSharePercent,
            creatorEarnedInr = creatorShare,
            platformEarnedInr = platformShare
        )
    }

    /**
     * Evaluates whether a creator is eligible for automatic payout.
     * India minimum eligible payout balance is ₹1,000.
     */
    fun evaluateAutomaticPayout(
        availableBalanceInr: Double,
        countryCode: String,
        kycVerified: Boolean,
        hasPaymentDetails: Boolean,
        minorMode: Boolean,
        creatorMonetizationEnabled: Boolean,
        config: PlatformConfig
    ): PayoutEvaluation {
        val thresholdInr = if (countryCode.equals("IN", ignoreCase = true)) {
            config.indiaMinPayoutInr
        } else {
            val spec = GlobalCatalog.countryByCode(countryCode)
            (spec.minPayoutLocal / spec.inrMultiplier).coerceAtLeast(500.0)
        }

        if (!config.monetizationEnabled || !creatorMonetizationEnabled) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "Monetization is currently disabled.")
        }
        if (!config.autoPayoutEnabled) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "Automatic payouts are paused by Admin; balance preserved.")
        }
        if (countryCode in config.monetizationDisabledCountries) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "Monetization is paused in $countryCode.")
        }
        if (minorMode) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "Minor safety mode restricts financial payouts.")
        }
        if (!kycVerified) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "KYC verification is required before payout.")
        }
        if (!hasPaymentDetails) {
            return PayoutEvaluation(false, thresholdInr, 0.0, 0.0, 0.0, "Add a valid UPI ID or bank account first.")
        }
        if (availableBalanceInr < thresholdInr) {
            return PayoutEvaluation(
                eligible = false,
                thresholdInr = thresholdInr,
                grossAmountInr = availableBalanceInr,
                tdsTaxInr = 0.0,
                netPayoutInr = 0.0,
                reason = "Minimum eligible payout threshold is ${GlobalCatalog.formatCurrencyFromInr(thresholdInr, countryCode)}."
            )
        }

        val tdsRate = if (countryCode.equals("IN", ignoreCase = true)) INDIA_TDS_RATE else 0.0
        val tds = roundCurrency(availableBalanceInr * tdsRate)
        val net = roundCurrency(availableBalanceInr - tds)
        return PayoutEvaluation(
            eligible = true,
            thresholdInr = thresholdInr,
            grossAmountInr = roundCurrency(availableBalanceInr),
            tdsTaxInr = tds,
            netPayoutInr = net,
            reason = "Eligible for automatic payout."
        )
    }

    fun isAdEligibleForViewer(config: PlatformConfig, countryCode: String, placement: String): Boolean {
        if (!config.adsEnabled) return false
        if (countryCode in config.adDisabledCountries) return false
        return when (placement) {
            "PRE_ROLL" -> config.preRollAdsEnabled
            "MID_ROLL" -> config.midRollAdsEnabled
            "SHORTS_FEED" -> config.shortsAdsEnabled
            "HOME_SPONSORED" -> config.sponsoredHomeAdsEnabled
            else -> true
        }
    }
}
