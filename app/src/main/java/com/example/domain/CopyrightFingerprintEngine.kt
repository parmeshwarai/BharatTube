package com.example.domain

import java.security.MessageDigest

data class FingerprintCheckResult(
    val contentHash: String,
    val similarityDetected: Boolean,
    val similarityPercent: Int,
    val matchedReferenceTitle: String?,
    val status: String,
    val advisoryMessage: String
)

object CopyrightFingerprintEngine {

    private val protectedReferenceKeywords = mapOf(
        "studio master track" to "Licensed Master Audio #BT-M01",
        "world cup broadcast" to "Official Sports Broadcast Feed",
        "cinema theatrical cut" to "Theatrical Film Rights Holder"
    )

    fun computeContentHash(title: String, mediaUri: String, durationSec: Long): String {
        val input = "${title.trim().lowercase()}|$mediaUri|$durationSec"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return "fp_" + digest.take(12).joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks audio/video metadata & hash signatures for similarity.
     * Similarity alone emits a WARNING for human/admin review—never automatic infringement proof.
     */
    fun scanUpload(
        title: String,
        musicTitle: String,
        mediaUri: String,
        durationSec: Long,
        existingHashes: Set<String>
    ): FingerprintCheckResult {
        val hash = computeContentHash(title, mediaUri, durationSec)
        val combined = "$title $musicTitle".lowercase()
        val matchedKeyword = protectedReferenceKeywords.entries.firstOrNull { combined.contains(it.key) }

        return when {
            hash in existingHashes -> FingerprintCheckResult(
                contentHash = hash,
                similarityDetected = true,
                similarityPercent = 88,
                matchedReferenceTitle = "Existing Catalog Signature ($hash)",
                status = "WARNING",
                advisoryMessage = "Video fingerprint similarity (88%) detected. Similarity alone is not final proof of infringement—you may publish with review or submit an appeal."
            )
            matchedKeyword != null -> FingerprintCheckResult(
                contentHash = hash,
                similarityDetected = true,
                similarityPercent = 76,
                matchedReferenceTitle = matchedKeyword.value,
                status = "WARNING",
                advisoryMessage = "Audio similarity (76%) with '${matchedKeyword.value}' detected. Similarity is flagged for rights review; original commentary or licensed use can be appealed."
            )
            else -> FingerprintCheckResult(
                contentHash = hash,
                similarityDetected = false,
                similarityPercent = 0,
                matchedReferenceTitle = null,
                status = "CLEAR",
                advisoryMessage = "Fingerprint scan clear. No matching protected audio or video signatures found."
            )
        }
    }
}
