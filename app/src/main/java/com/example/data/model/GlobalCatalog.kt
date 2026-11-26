package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

data class LanguageSpec(
    val code: String,
    val englishName: String,
    val nativeName: String
)

data class CountrySpec(
    val code: String,
    val name: String,
    val currencyCode: String,
    val currencySymbol: String,
    val inrMultiplier: Double,
    val minPayoutLocal: Double,
    val paymentMethods: List<String>
)

data class LicensedTrack(
    val licenseId: String,
    val title: String,
    val artist: String,
    val mood: String
)

data class FaqEntry(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

object GlobalCatalog {
    // 20 Starter Languages with ISO-639 extensible registry supporting 100+ languages
    val starterLanguages: List<LanguageSpec> = listOf(
        LanguageSpec("hi", "Hindi", "हिन्दी"),
        LanguageSpec("en", "English", "English"),
        LanguageSpec("bho", "Bhojpuri", "भोजपुरी"),
        LanguageSpec("bn", "Bengali", "বাংলা"),
        LanguageSpec("ta", "Tamil", "தமிழ்"),
        LanguageSpec("te", "Telugu", "తెలుగు"),
        LanguageSpec("mr", "Marathi", "मराठी"),
        LanguageSpec("gu", "Gujarati", "ગુજરાતી"),
        LanguageSpec("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageSpec("zh", "Chinese", "中文"),
        LanguageSpec("ja", "Japanese", "日本語"),
        LanguageSpec("ko", "Korean", "한국어"),
        LanguageSpec("ar", "Arabic", "العربية"),
        LanguageSpec("fr", "French", "Français"),
        LanguageSpec("de", "German", "Deutsch"),
        LanguageSpec("es", "Spanish", "Español"),
        LanguageSpec("pt", "Portuguese", "Português"),
        LanguageSpec("ru", "Russian", "Русский"),
        LanguageSpec("id", "Indonesian", "Bahasa Indonesia"),
        LanguageSpec("th", "Thai", "ไทย")
    )

    // Extensible global country rules with accurate currency & payout methods
    val countries: List<CountrySpec> = listOf(
        CountrySpec("IN", "India", "INR", "₹", 1.0, 1000.0, listOf("UPI", "BANK_TRANSFER")),
        CountrySpec("US", "United States", "USD", "$", 0.012, 25.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("GB", "United Kingdom", "GBP", "£", 0.0095, 20.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("JP", "Japan", "JPY", "¥", 1.80, 3000.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("AE", "United Arab Emirates", "AED", "د.إ", 0.044, 100.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("DE", "Germany", "EUR", "€", 0.011, 25.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("FR", "France", "EUR", "€", 0.011, 25.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("BR", "Brazil", "BRL", "R$", 0.068, 100.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("ID", "Indonesia", "IDR", "Rp", 188.0, 250000.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("TH", "Thailand", "THB", "฿", 0.41, 800.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("KR", "South Korea", "KRW", "₩", 16.5, 30000.0, listOf("BANK_TRANSFER", "INTL_WIRE")),
        CountrySpec("RU", "Russia", "RUB", "₽", 1.15, 2000.0, listOf("INTL_WIRE"))
    )

    val categories: List<String> = listOf(
        "All",
        "Technology",
        "Music",
        "Gaming",
        "Education",
        "Cinema",
        "News",
        "Sports",
        "Cooking",
        "Travel"
    )

    val licensedSounds: List<LicensedTrack> = listOf(
        LicensedTrack("LIC-BT-101", "Saffron Horizon (Lo-Fi Sitar)", "Kiran Studio", "Chill"),
        LicensedTrack("LIC-BT-102", "Mumbai Midnight Pulse", "Rhythm Collective", "Electronic"),
        LicensedTrack("LIC-BT-103", "Monsoon Acoustic Breeze", "Ananya Strings", "Acoustic"),
        LicensedTrack("LIC-BT-104", "Deccan Beat Drop", "Vihaan Audio", "Upbeat")
    )

    val faqs: List<FaqEntry> = listOf(
        FaqEntry(
            "faq_split",
            "Monetization",
            "How does BharatTube's 50:50 revenue sharing work?",
            "For every eligible long video and Short/Reel, net advertising revenue (after ad network fees and disclosed taxes) is split 50% to the creator and 50% to BharatTube. Every revenue event is recorded transparently in your Creator Studio ledger."
        ),
        FaqEntry(
            "faq_payout",
            "Payouts",
            "When is the automatic ₹1,000 payout triggered in India?",
            "Once your confirmed available balance reaches ₹1,000 in India with verified KYC and a valid UPI ID or bank account, BharatTube automatically initiates your payout with duplicate-payment protection and TDS reporting."
        ),
        FaqEntry(
            "faq_copyright",
            "Copyright",
            "What happens if an audio or video fingerprint match is flagged?",
            "Fingerprint similarity triggers an early warning only—similarity alone is never treated as final proof of infringement. Creators can view claim details, submit a fair-use or license appeal, and have admins review the dispute."
        ),
        FaqEntry(
            "faq_safety",
            "Safety & Privacy",
            "How are minors protected on BharatTube?",
            "Enabling Minor Safety Mode restricts direct messaging with unknown accounts, disables live-broadcast hosting and payout onboarding, and applies strict age-appropriate filtering across feeds."
        )
    )

    // Verified playable sample media streams for reliable ExoPlayer rendering
    const val STREAM_BUNNY = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    const val STREAM_BLAZES = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    const val STREAM_ESCAPES = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
    const val STREAM_FUN = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
    const val STREAM_JOY = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4"
    const val STREAM_MELTDOWNS = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"

    fun countryByCode(code: String): CountrySpec =
        countries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: countries.first()

    fun languageByCode(code: String): LanguageSpec =
        starterLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: LanguageSpec(code, code.uppercase(Locale.US), code)

    fun formatCurrencyFromInr(amountInr: Double, countryCode: String): String {
        val spec = countryByCode(countryCode)
        val converted = amountInr * spec.inrMultiplier
        val formatter = NumberFormat.getNumberInstance(
            if (spec.code == "IN") Locale("en", "IN") else Locale.US
        ).apply {
            maximumFractionDigits = if (spec.currencyCode in listOf("JPY", "IDR", "KRW")) 0 else 2
            minimumFractionDigits = if (spec.currencyCode in listOf("JPY", "IDR", "KRW")) 0 else 2
        }
        return "${spec.currencySymbol}${formatter.format(converted)}"
    }

    fun formatInr(amountInr: Double): String = formatCurrencyFromInr(amountInr, "IN")
}
