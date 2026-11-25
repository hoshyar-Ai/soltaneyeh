package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.R
import com.example.model.Attraction
import com.example.model.AudienceLevel
import com.example.model.SoltaniyehAttractions
import org.json.JSONObject

private const val TAG = "LocalKnowledgeRepo"
private const val ASSET_FILE_NAME = "soltaniyeh_knowledge.json"

data class LocalFaqEntry(
    val id: String,
    val question: String,
    val keywords: List<String>,
    val answersByLevel: Map<AudienceLevel, String>
)

data class OfflineGuideData(
    val attractions: List<Attraction>,
    val attractionGuides: Map<String, Map<AudienceLevel, String>>,
    val faqs: List<LocalFaqEntry>,
    val visionGuideByLevel: Map<AudienceLevel, String>
)

class LocalKnowledgeRepository(private val context: Context) {

    @Volatile
    private var cachedData: OfflineGuideData? = null

    fun getData(): OfflineGuideData {
        cachedData?.let { return it }
        synchronized(this) {
            cachedData?.let { return it }
            val parsed = loadFromAssets()
            cachedData = parsed
            return parsed
        }
    }

    fun getAttractions(): List<Attraction> {
        return getData().attractions
    }

    fun getOfflineAttractionGuide(attraction: Attraction, level: AudienceLevel): String {
        val guides = getData().attractionGuides[attraction.id]
        val text = guides?.get(level) ?: guides?.get(AudienceLevel.GENERAL)
        if (!text.isNullOrBlank()) {
            return text
        }
        return buildString {
            appendLine("### معرفی و پیشینه تاریخی")
            appendLine(attraction.shortDescription)
            appendLine()
            appendLine("### نکات بازدید")
            appendLine("- **دوره تاریخی:** ${attraction.era}")
            appendLine("- **مدت بازدید پیشنهادی:** ${attraction.visitingDuration}")
            appendLine("- **موقعیت:** ${attraction.locationNote}")
        }
    }

    fun findBestOfflineChatReply(query: String, level: AudienceLevel): String {
        val data = getData()
        val normalizedQuery = normalizePersian(query)

        // 1. Score FAQs by exact question match + keyword overlap
        var bestFaq: LocalFaqEntry? = null
        var bestFaqScore = 0
        for (faq in data.faqs) {
            var score = 0
            val normQuestion = normalizePersian(faq.question)
            if (normQuestion == normalizedQuery) {
                score += 100
            } else if (normQuestion.contains(normalizedQuery) || normalizedQuery.contains(normQuestion)) {
                score += 40
            }
            for (kw in faq.keywords) {
                val normKw = normalizePersian(kw)
                if (normKw.isNotEmpty() && normalizedQuery.contains(normKw)) {
                    score += 12
                }
            }
            if (score > bestFaqScore) {
                bestFaqScore = score
                bestFaq = faq
            }
        }

        if (bestFaq != null && bestFaqScore >= 12) {
            val answer = bestFaq.answersByLevel[level]
                ?: bestFaq.answersByLevel[AudienceLevel.GENERAL]
                ?: ""
            if (answer.isNotBlank()) {
                return answer
            }
        }

        // 2. Check if query mentions one of the 4 attractions directly
        val matchedAttraction = data.attractions.firstOrNull { attraction ->
            val normTitle = normalizePersian(attraction.title)
            val titleTokens = normTitle.split(" ").filter { it.length >= 3 }
            titleTokens.any { token -> normalizedQuery.contains(token) }
        }
        if (matchedAttraction != null) {
            return getOfflineAttractionGuide(matchedAttraction, level)
        }

        // 3. Check if query looks related to Soltaniyeh/Iran heritage at all
        val domainTokens = listOf(
            "سلطانیه", "گنبد", "زنجان", "ایلخانی", "داش", "چلبی", "معماری",
            "تاریخ", "کاشی", "کتیبه", "بنا", "سفر", "بازدید", "آرامگاه", "موزه", "ایران"
        )
        val hasDomainTerm = domainTokens.any { normalizedQuery.contains(normalizePersian(it)) }

        return if (hasDomainTerm) {
            buildString {
                appendLine("اطلاعات دقیق مرتبط با این پرسش در حافظه آفلاین موجود نیست، اما خلاصه‌ای از مهم‌ترین آثار سلطانیه را در ادامه می‌بینید:")
                appendLine()
                data.attractions.forEach { item ->
                    appendLine("- **${item.title} (${item.era}):** ${item.shortDescription}")
                }
                appendLine()
                append("می‌توانید روی دکمه‌های «پیشنهاد سوال سریع» در بالای صفحه بزنید تا پاسخ‌های کامل آفلاین را ببینید.")
            }
        } else {
            buildString {
                appendLine("تخصص من تاریخ، فرهنگ، معماری و گردشگری سلطانیه و میراث فرهنگی ایران است.")
                appendLine()
                append("در حالت آفلاین می‌توانید از بخش «پیشنهاد سوال سریع» درباره **تاریخچه گنبد سلطانیه**، **ویژگی‌های معماری ایلخانی**، **معبد داش‌کسن**، **آرامگاه چلبی‌اوغلی** یا **بهترین مسیر بازدید** بپرسید.")
            }
        }
    }

    fun buildOfflineVisionGuide(userNote: String, level: AudienceLevel): String {
        val data = getData()
        val baseGuide = data.visionGuideByLevel[level]
            ?: data.visionGuideByLevel[AudienceLevel.GENERAL]
            ?: ""

        if (userNote.isBlank()) {
            return baseGuide
        }

        val matchedAnswer = findBestOfflineChatReply(userNote, level)
        return buildString {
            appendLine(matchedAnswer)
            appendLine()
            append(baseGuide)
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect network state", e)
            true
        }
    }

    private fun loadFromAssets(): OfflineGuideData {
        return try {
            val rawJson = context.assets.open(ASSET_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val root = JSONObject(rawJson)

            val attractionsList = mutableListOf<Attraction>()
            val guidesMap = mutableMapOf<String, Map<AudienceLevel, String>>()

            val attrArray = root.optJSONArray("attractions")
            if (attrArray != null) {
                for (i in 0 until attrArray.length()) {
                    val obj = attrArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val tagsArr = obj.optJSONArray("quickTags")
                    val tags = buildList {
                        if (tagsArr != null) {
                            for (j in 0 until tagsArr.length()) add(tagsArr.getString(j))
                        }
                    }
                    attractionsList.add(
                        Attraction(
                            id = id,
                            title = obj.getString("title"),
                            subtitle = obj.getString("subtitle"),
                            era = obj.getString("era"),
                            shortDescription = obj.getString("shortDescription"),
                            visitingDuration = obj.getString("visitingDuration"),
                            locationNote = obj.getString("locationNote"),
                            imageRes = resolveDrawableRes(obj.optString("imageKey")),
                            quickTags = tags,
                            suggestedQuestion = obj.getString("suggestedQuestion")
                        )
                    )
                    val guidesObj = obj.optJSONObject("guides")
                    if (guidesObj != null) {
                        val perLevel = mutableMapOf<AudienceLevel, String>()
                        AudienceLevel.entries.forEach { lvl ->
                            val text = guidesObj.optString(lvl.id)
                            if (text.isNotBlank()) perLevel[lvl] = text
                        }
                        guidesMap[id] = perLevel
                    }
                }
            }

            val faqList = mutableListOf<LocalFaqEntry>()
            val faqArray = root.optJSONArray("faqs")
            if (faqArray != null) {
                for (i in 0 until faqArray.length()) {
                    val obj = faqArray.getJSONObject(i)
                    val kwArr = obj.optJSONArray("keywords")
                    val kws = buildList {
                        if (kwArr != null) {
                            for (j in 0 until kwArr.length()) add(kwArr.getString(j))
                        }
                    }
                    val ansObj = obj.optJSONObject("answers")
                    val answersMap = mutableMapOf<AudienceLevel, String>()
                    if (ansObj != null) {
                        AudienceLevel.entries.forEach { lvl ->
                            val text = ansObj.optString(lvl.id)
                            if (text.isNotBlank()) answersMap[lvl] = text
                        }
                    }
                    faqList.add(
                        LocalFaqEntry(
                            id = obj.getString("id"),
                            question = obj.getString("question"),
                            keywords = kws,
                            answersByLevel = answersMap
                        )
                    )
                }
            }

            val visionObj = root.optJSONObject("visionOfflineGuide")
            val visionMap = mutableMapOf<AudienceLevel, String>()
            if (visionObj != null) {
                AudienceLevel.entries.forEach { lvl ->
                    val text = visionObj.optString(lvl.id)
                    if (text.isNotBlank()) visionMap[lvl] = text
                }
            }

            OfflineGuideData(
                attractions = if (attractionsList.isNotEmpty()) attractionsList else SoltaniyehAttractions.all,
                attractionGuides = guidesMap,
                faqs = faqList,
                visionGuideByLevel = visionMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse local JSON knowledge base; using built-in defaults", e)
            OfflineGuideData(
                attractions = SoltaniyehAttractions.all,
                attractionGuides = emptyMap(),
                faqs = emptyList(),
                visionGuideByLevel = emptyMap()
            )
        }
    }

    private fun resolveDrawableRes(imageKey: String): Int {
        return when (imageKey) {
            "img_soltaniyeh_dome" -> R.drawable.img_soltaniyeh_dome
            "img_dashkasan_temple" -> R.drawable.img_dashkasan_temple
            "img_chelebi_oghli" -> R.drawable.img_chelebi_oghli
            "img_soltaniyeh_complex" -> R.drawable.img_soltaniyeh_complex
            else -> R.drawable.img_soltaniyeh_dome
        }
    }

    private fun normalizePersian(input: String): String {
        return input
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace("\u200C", " ") // convert ZWNJ to space for forgiving keyword matching
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "") // strip Arabic diacritics
            .lowercase()
            .trim()
    }
}
