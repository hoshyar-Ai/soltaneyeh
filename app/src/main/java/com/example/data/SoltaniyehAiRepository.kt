package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.config.AiEndpointConfig
import com.example.model.Attraction
import com.example.model.AudienceLevel
import com.example.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class AiNotConfiguredException(message: String) : IllegalStateException(message)
class VisionNotSupportedException(message: String) : UnsupportedOperationException(message)
class AiEndpointException(val statusCode: Int, message: String) : RuntimeException(message) {
    fun isAuthOrQuotaError(): Boolean = statusCode == 401 || statusCode == 403 || statusCode == 429
}

class SoltaniyehAiRepository {

    companion object {
        private const val TIMEOUT_MS = 30_000L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val GUIDE_SYSTEM_INSTRUCTION = """
            تو «راهنمای هوشمند سلطانیه»، متخصص برجسته تاریخ، فرهنگ، هنر، کاشیکاری، معماری و گردشگری مجموعه میراث جهانی سلطانیه (در استان زنجان) و میراث فرهنگی ایران هستی.

            قوانین علمی و محدودیت‌های اکید:
            ۱. فقط و فقط در حوزه تاریخ، فرهنگ، معماری، هنرهای سنتی و گردشگری سلطانیه و ایران پاسخ بده. اگر کاربر درباره موضوعات نامرتبط (مانند برنامه‌نویسی، ریاضی، اخبار روز، پزشکی، فناوری و...) پرسید، با احترام و ادب اعلام کن که تخصص تو تنها تاریخ، معماری و گردشگری سلطانیه و میراث فرهنگی ایران است و پیشنهاد بده سوالی در این حوزه بپرسد.
            ۲. از اطلاعات دقیق و مستند تاریخی و باستان‌شناسی استفاده کن. هرگز تاریخ‌ها، ابعاد، نام معماران، کتیبه‌ها یا رویدادهای تاریخی را حدس نزن یا جعل نکن. اگر در منابع تاریخی درباره نکته‌ای اختلاف نظر یا ابهام وجود دارد، صادقانه آن را بیان کن.
            ۳. پاسخ‌ها باید به زبان فارسی شیوا، دقیق و منطبق با سطح مخاطب درخواستی باشد.
            ۴. برای خوانایی در موبایل، از تیترهای کوتاه (با ### یا **تیتر**) و بولت‌پوینت‌های منظم استفاده کن.
        """.trimIndent()

        private val VISION_SYSTEM_INSTRUCTION = """
            تو «شناساگر هوشمند آثار تاریخی سلطانیه و ایران» هستی؛ یک پژوهشگر دقیق معماری اسلامی، کاشیکاری، کتیبه‌خوانی و هنرهای سنتی ایران.

            قوانین اکید تحلیل تصویر:
            ۱. ابتدا بررسی کن آیا تصویر مرتبط با بناهای تاریخی، معماری سنتی/اسلامی، کاشیکاری، گچ‌بری، سنگ‌نگاره، کتیبه، محوطه باستانی یا میراث فرهنگی و گردشگری سلطانیه و ایران است یا خیر.
            ۲. اگر تصویر کاملاً نامرتبط بود (مانند لوازم دیجیتال، خودرو، غذاهای عادی، اسناد متنی مدرن، پرتره شخصی، حیوانات خانگی یا محیط‌های مدرن بدون اثر تاریخی)، با احترام و صراحت اعلام کن که این تصویر مربوط به آثار تاریخی، معماری یا میراث فرهنگی نیست و از کاربر بخواه عکسی از یک بنا، کاشیکاری، کتیبه یا بخش تاریخی بارگذاری کند. هرگز درباره عکس نامرتبط تحلیل تاریخی نساز.
            ۳. اگر تصویر یک اثر تاریخی، کاشیکاری، کتیبه یا جزئیات معماری است:
               - اگر دقیقاً قابل تشخیص است (مثلاً گنبد سلطانیه، معبد داش‌کسن، آرامگاه چلبی‌اوغلی یا بخش مشخصی از آن‌ها)، نام دقیق و اطلاعات مستند آن را بیان کن.
               - اگر بنا یا جزئیات کاملاً قطعی نیست، حدس بی‌اساس نزن! با صداقت بگو که تشخیص دقیق محل بدون بافت بیشتر قطعی نیست، اما ویژگی‌های عینی قابل مشاهده در تصویر (نوع قوس، سبک کاشیکاری معرق/هفت‌رنگ/معقلی، خط کتیبه، آجرکاری یا دوره‌بندی احتمالی سبک معماری) را به دقت شرح بده.
            ۴. لحن و عمق توضیحات را دقیقاً مطابق سطح مخاطب درخواستی تنظیم کن.
            ۵. پاسخ را به فارسی و در بخش‌های شفاف (معرفی و تشخیص، دوره و تاریخچه، معماری و جزئیات هنری، نکته کاربردی) بنویس.
        """.trimIndent()
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun analyzeMonumentImage(
        bitmap: Bitmap,
        audienceLevel: AudienceLevel,
        userNote: String
    ): Flow<String> = flow {
        ensureConfigured()
        if (!AiEndpointConfig.supportsVision()) {
            throw VisionNotSupportedException(
                "مدل انتخاب‌شده (${AiEndpointConfig.modelName}) از تحلیل تصویر پشتیبانی نمی‌کند."
            )
        }

        val base64Image = encodeBitmapToBase64(bitmap)
        val systemPrompt = buildString {
            appendLine(VISION_SYSTEM_INSTRUCTION)
            appendLine()
            appendLine("سطح مخاطب انتخاب‌شده: ${audienceLevel.titleFa} (${audienceLevel.subtitleFa})")
            appendLine("دستورالعمل لحن: ${audienceLevel.promptDirective}")
        }

        val promptText = buildString {
            if (userNote.isNotBlank()) {
                appendLine("پرسش یا توضیح همراه کاربر: $userNote")
            }
            append("لطفاً این تصویر را از نظر ارتباط با آثار تاریخی/فرهنگی و در صورت ارتباط، تاریخچه، معماری و جزئیات هنری آن تحلیل کن.")
        }

        val userContentArray = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("type", "text")
                    put("text", promptText)
                }
            )
            put(
                JSONObject().apply {
                    put("type", "image_url")
                    put(
                        "image_url",
                        JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        }
                    )
                }
            )
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", userContentArray))
        }

        streamChatCompletion(
            messages = messages,
            temperature = 0.3,
            isVisionRequest = true,
            onChunk = { accumulated -> emit(accumulated) }
        )
    }.flowOn(Dispatchers.IO)

    fun generateAttractionGuide(
        attraction: Attraction,
        audienceLevel: AudienceLevel
    ): Flow<String> = flow {
        ensureConfigured()

        val systemPrompt = buildString {
            appendLine(GUIDE_SYSTEM_INSTRUCTION)
            appendLine()
            appendLine("سطح مخاطب فعلی: ${audienceLevel.titleFa} (${audienceLevel.subtitleFa})")
            appendLine("دستورالعمل لحن: ${audienceLevel.promptDirective}")
        }

        val promptText = """
            درباره جاذبه تاریخی «${attraction.title}» (${attraction.subtitle}، مربوط به ${attraction.era}) یک معرفی هوشمند و نکات کاربردی بازدید ارائه بده.

            لطفاً پاسخ را در ۳ بخش کوتاه و خوش‌خوان تنظیم کن:
            ### معرفی و پیشینه تاریخی
            ### شاخصه‌های معماری و هنری
            ### نکات طلایی بازدید و عکاسی
            فقط به واقعیت‌های مستند تاریخی و معماری بسنده کن.
        """.trimIndent()

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", promptText))
        }

        streamChatCompletion(
            messages = messages,
            temperature = 0.35,
            isVisionRequest = false,
            onChunk = { accumulated -> emit(accumulated) }
        )
    }.flowOn(Dispatchers.IO)

    fun sendChatMessageStream(
        historyMessages: List<ChatMessage>,
        newUserMessage: String,
        audienceLevel: AudienceLevel
    ): Flow<String> = flow {
        ensureConfigured()

        val systemPrompt = buildString {
            appendLine(GUIDE_SYSTEM_INSTRUCTION)
            appendLine()
            appendLine("سطح مخاطب فعلی: ${audienceLevel.titleFa} (${audienceLevel.subtitleFa})")
            appendLine("دستورالعمل لحن: ${audienceLevel.promptDirective}")
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))

            historyMessages
                .filter { !it.isError && !it.isStreaming && it.text.isNotBlank() }
                .takeLast(12)
                .forEach { msg ->
                    put(
                        JSONObject()
                            .put("role", if (msg.isUser) "user" else "assistant")
                            .put("content", msg.text)
                    )
                }

            put(JSONObject().put("role", "user").put("content", newUserMessage))
        }

        streamChatCompletion(
            messages = messages,
            temperature = 0.4,
            isVisionRequest = false,
            onChunk = { accumulated -> emit(accumulated) }
        )
    }.flowOn(Dispatchers.IO)

    private fun ensureConfigured() {
        if (!AiEndpointConfig.isReadyForRequest()) {
            throw AiNotConfiguredException(
                "کلید OpenAI API (VITE_OPENAI_API_KEY یا کلید موقت در تنظیمات برنامه) هنوز وارد نشده است."
            )
        }
    }

    private suspend fun streamChatCompletion(
        messages: JSONArray,
        temperature: Double,
        isVisionRequest: Boolean,
        onChunk: suspend (String) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("model", AiEndpointConfig.modelName)
            put("messages", messages)
            put("temperature", temperature)
            put("stream", true)
        }

        val requestBuilder = Request.Builder()
            .url(AiEndpointConfig.chatCompletionsUrl())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream, application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

        val activeKey = AiEndpointConfig.effectiveApiKey()
        if (activeKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $activeKey")
        }

        val call = client.newCall(requestBuilder.build())
        withTimeout(TIMEOUT_MS) {
            val cancelHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
            }
            try {
                call.execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    val errorMsg = extractErrorMessage(errorBody, response.code)
                    if (isVisionRequest && isVisionUnsupportedError(response.code, errorMsg)) {
                        throw VisionNotSupportedException(errorMsg)
                    }
                    throw AiEndpointException(response.code, errorMsg)
                }

                val body = response.body ?: throw IllegalStateException("پاسخ خالی از سرور دریافت شد.")
                val contentType = response.header("Content-Type").orEmpty().lowercase()

                if (contentType.contains("application/json") && !contentType.contains("text/event-stream")) {
                    val fullJson = body.string()
                    val text = extractNonStreamingContent(fullJson)
                    if (text.isBlank()) {
                        throw IllegalStateException("متن معتبری در پاسخ سرور وجود نداشت.")
                    }
                    onChunk(text)
                    return@use
                }

                val accumulated = StringBuilder()
                val rawBuffer = StringBuilder()
                val source = body.source()

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    rawBuffer.appendLine(line)

                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":")) continue

                    if (trimmed.startsWith("data:")) {
                        val dataPayload = trimmed.removePrefix("data:").trim()
                        if (dataPayload == "[DONE]") break

                        val delta = extractStreamingDelta(dataPayload)
                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            onChunk(accumulated.toString())
                        }
                    }
                }

                // Fallback if a proxy returned plain JSON without SSE `data:` lines despite missing header
                if (accumulated.isBlank()) {
                    val fallbackText = extractNonStreamingContent(rawBuffer.toString())
                    if (fallbackText.isNotBlank()) {
                        onChunk(fallbackText)
                    } else {
                        throw IllegalStateException("پاسخی از سرویس هوش مصنوعی دریافت نشد.")
                    }
                }
            }
            } finally {
                cancelHandle.dispose()
            }
        }
    }

    private fun extractStreamingDelta(jsonText: String): String {
        return try {
            val obj = JSONObject(jsonText)
            val choices = obj.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val first = choices.getJSONObject(0)
            val delta = first.optJSONObject("delta")
            if (delta != null && !delta.isNull("content")) {
                return delta.optString("content", "")
            }
            val message = first.optJSONObject("message")
            if (message != null && !message.isNull("content")) {
                return message.optString("content", "")
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractNonStreamingContent(jsonText: String): String {
        return try {
            val obj = JSONObject(jsonText)
            val choices = obj.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val first = choices.getJSONObject(0)
            val message = first.optJSONObject("message") ?: return ""
            if (message.isNull("content")) "" else message.optString("content", "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractErrorMessage(errorBody: String, statusCode: Int): String {
        return try {
            val obj = JSONObject(errorBody)
            val errObj = obj.optJSONObject("error")
            val msg = errObj?.optString("message").orEmpty().ifBlank {
                obj.optString("message").orEmpty()
            }
            msg.ifBlank { "خطای سرور ($statusCode)" }
        } catch (_: Exception) {
            errorBody.take(200).ifBlank { "خطای سرور ($statusCode)" }
        }
    }

    private fun isVisionUnsupportedError(statusCode: Int, message: String): Boolean {
        if (statusCode !in setOf(400, 404, 415, 422)) return false
        val lower = message.lowercase()
        return lower.contains("image") ||
            lower.contains("vision") ||
            lower.contains("multimodal") ||
            lower.contains("image_url") ||
            lower.contains("content must be a string") ||
            lower.contains("unknown variant") ||
            lower.contains("not support")
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
