package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class TranslatedQuestion(
    val id: Long,
    val question: String,
    val options: List<String>
)

class TranslationHelper private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_TRANSLATION_CACHE, Context.MODE_PRIVATE)

    // Ultra-fast in-memory cache for instant switching during active test
    private val memoryCache = ConcurrentHashMap<String, String>()

    companion object {
        private const val PREFS_TRANSLATION_CACHE = "quiz_translation_cache"
        private const val BASE_TRANSLATE_URL =
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=hi&dt=t&q="

        @Volatile
        private var instance: TranslationHelper? = null

        fun getInstance(context: Context): TranslationHelper {
            return instance ?: synchronized(this) {
                instance ?: TranslationHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Checks if a string contains Devanagari (Hindi) script characters.
     */
    fun containsHindi(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) {
                return true
            }
        }
        return false
    }

    /**
     * Translates a single text string from English to Hindi.
     * 1. Checks memory cache.
     * 2. Checks local disk cache (SharedPreferences).
     * 3. Checks if string already contains Hindi (e.g. bilingual 'Yak/ याक').
     * 4. Fetches live translation from Google GTX endpoint.
     */
    suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext text

        // 1. Check in-memory cache
        val cachedMem = memoryCache[trimmed]
        if (cachedMem != null) return@withContext cachedMem

        // 2. Check persistent disk cache
        val cacheKey = "tr_" + trimmed.hashCode()
        val cachedDisk = prefs.getString(cacheKey, null)
        if (!cachedDisk.isNullOrEmpty()) {
            memoryCache[trimmed] = cachedDisk
            return@withContext cachedDisk
        }

        // 3. Check for bilingual embedded Hindi (e.g. "Wild Yak / जंगली याक" or "Incus/ थूणक")
        val embeddedHindi = extractEmbeddedHindi(trimmed)
        if (embeddedHindi != null && containsHindi(embeddedHindi)) {
            memoryCache[trimmed] = embeddedHindi
            prefs.edit().putString(cacheKey, embeddedHindi).apply()
            return@withContext embeddedHindi
        }

        // 4. Fetch from Google Translate GTX free endpoint
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = URL(BASE_TRANSLATE_URL + encodedQuery)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val translated = parseGtxResponse(response)
                if (translated.isNotBlank()) {
                    memoryCache[trimmed] = translated
                    prefs.edit().putString(cacheKey, translated).apply()
                    return@withContext translated
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If offline or network error, return original text as fallback
        return@withContext trimmed
    }

    /**
     * Translates an entire Question (question text + all options) concurrently.
     */
    suspend fun translateQuestion(question: Question): TranslatedQuestion = coroutineScope {
        val qDeferred = async(Dispatchers.IO) { translateText(question.question) }
        val optDeferred = question.options.map { opt ->
            async(Dispatchers.IO) { translateText(opt) }
        }

        val translatedQuestionText = qDeferred.await()
        val translatedOptions = optDeferred.map { it.await() }

        TranslatedQuestion(
            id = question.id,
            question = translatedQuestionText,
            options = translatedOptions
        )
    }

    /**
     * Extracts Hindi portion if the string is already bilingual (e.g., slash separated).
     */
    private fun extractEmbeddedHindi(text: String): String? {
        if (!containsHindi(text)) return null

        // Format 1: "English / Hindi" or "English/Hindi"
        if (text.contains("/")) {
            val parts = text.split("/")
            for (p in parts) {
                val candidate = p.trim()
                if (containsHindi(candidate)) {
                    return candidate
                }
            }
        }

        // Format 2: "English: Hindi ?" or "English ? Hindi ?"
        if (text.contains(":") || text.contains("?")) {
            val delim = if (text.contains(":")) ":" else "?"
            val parts = text.split(delim)
            for (p in parts.reversed()) {
                val candidate = p.trim()
                if (containsHindi(candidate) && candidate.length > 5) {
                    return if (text.endsWith("?")) "$candidate ?" else candidate
                }
            }
        }

        return null
    }

    /**
     * Parses Google GTX endpoint nested JSON array:
     * [[["Translated segment 1","Source",null,null,...],["Translated segment 2",...]],null,"en",...]
     */
    private fun parseGtxResponse(jsonStr: String): String {
        return try {
            val rootArray = JSONArray(jsonStr)
            val segmentsArray = rootArray.optJSONArray(0) ?: return ""
            val sb = StringBuilder()
            for (i in 0 until segmentsArray.length()) {
                val seg = segmentsArray.optJSONArray(i)
                if (seg != null && seg.length() > 0) {
                    val part = seg.optString(0, "")
                    sb.append(part)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
