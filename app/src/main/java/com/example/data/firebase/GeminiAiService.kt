package com.example.data.firebase

import android.util.Log
import com.example.data.model.GlobalCatalog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class TranslatedVideoText(
    val title: String,
    val description: String,
    val subtitles: String,
    val targetLanguageName: String
)

class GeminiAiService {
    private val textModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Translated video title"),
            "description" to Schema.string(description = "Translated video description"),
            "subtitles" to Schema.string(description = "Translated or generated closed-caption text")
        )
    )

    private val translationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
            }
        )
    }

    suspend fun translateVideoMetadata(
        title: String,
        description: String,
        subtitles: String,
        targetLangCode: String
    ): Result<TranslatedVideoText> {
        val langSpec = GlobalCatalog.languageByCode(targetLangCode)
        val prompt = buildString {
            appendLine("Translate the following BharatTube video title, description, and captions into ${langSpec.englishName} (${langSpec.nativeName}).")
            appendLine("Preserve hashtags and proper nouns cleanly.")
            appendLine("Title: $title")
            appendLine("Description: $description")
            appendLine("Subtitles: ${subtitles.ifBlank { description }}")
        }
        return try {
            val response = withTimeout(60_000L) {
                translationModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty translation response.")
            val json = JSONObject(raw)
            Result.success(
                TranslatedVideoText(
                    title = json.optString("title", title),
                    description = json.optString("description", description),
                    subtitles = json.optString("subtitles", subtitles),
                    targetLanguageName = langSpec.nativeName
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            Result.failure(Exception("Translation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateComment(commentText: String, targetLangCode: String): Result<String> {
        val langSpec = GlobalCatalog.languageByCode(targetLangCode)
        val prompt = "Translate this video comment naturally into ${langSpec.englishName} (${langSpec.nativeName}). Return only the translated sentence:\n$commentText"
        return try {
            val response = withTimeout(45_000L) {
                textModel.generateContent(prompt)
            }
            val translated = response.text?.trim()
                ?: throw IllegalStateException("Empty translation response.")
            Result.success(translated)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Comment translation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Comment translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateRecommendationInsight(
        interests: List<String>,
        recentTitles: List<String>,
        languageCode: String,
        countryCode: String
    ): Result<String> {
        val lang = GlobalCatalog.languageByCode(languageCode)
        val prompt = buildString {
            appendLine("You are BharatTube's personalization engine.")
            appendLine("User country: $countryCode, preferred language: ${lang.englishName}.")
            appendLine("User interests: ${interests.joinToString(", ")}.")
            appendLine("Recently watched: ${recentTitles.take(5).joinToString(" | ").ifBlank { "New viewer" }}.")
            appendLine("Write a crisp 1-sentence recommendation summary explaining what content themes BharatTube is prioritizing for this viewer right now.")
        }
        return try {
            val response = withTimeout(45_000L) {
                textModel.generateContent(prompt)
            }
            val text = response.text?.trim()
                ?: throw IllegalStateException("Empty recommendation insight.")
            Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("AI insight timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI insight failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "GeminiAiService"
    }
}
