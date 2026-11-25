package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AiEndpointConfig
import com.example.data.AiEndpointException
import com.example.data.AiNotConfiguredException
import com.example.data.AudiencePreferences
import com.example.data.LocalFaqEntry
import com.example.data.LocalKnowledgeRepository
import com.example.data.SoltaniyehAiRepository
import com.example.data.VisionNotSupportedException
import com.example.model.AnalysisUiState
import com.example.model.Attraction
import com.example.model.AudienceLevel
import com.example.model.ChatMessage
import com.example.model.DetailAiState
import com.example.model.MainTab
import com.example.model.SoltaniyehAttractions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "SoltaniyehViewModel"
private const val MAX_IMAGE_DIMENSION = 1280

class SoltaniyehViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AudiencePreferences(application.applicationContext)
    private val localKnowledge = LocalKnowledgeRepository(application.applicationContext)
    private val aiRepository = SoltaniyehAiRepository()

    val audienceLevel: StateFlow<AudienceLevel> = preferences.audienceLevelFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AudienceLevel.GENERAL
        )

    private val _runtimeApiKey = MutableStateFlow("")
    val runtimeApiKey: StateFlow<String> = _runtimeApiKey.asStateFlow()

    private val _attractions = MutableStateFlow(SoltaniyehAttractions.all)
    val attractions: StateFlow<List<Attraction>> = _attractions.asStateFlow()

    private val _localFaqs = MutableStateFlow<List<LocalFaqEntry>>(emptyList())
    val localFaqs: StateFlow<List<LocalFaqEntry>> = _localFaqs.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.EXPLORE)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Attraction Detail state & in-memory cache by (attractionId, audienceLevel)
    private val _selectedAttraction = MutableStateFlow<Attraction?>(null)
    val selectedAttraction: StateFlow<Attraction?> = _selectedAttraction.asStateFlow()

    private val _detailAiState = MutableStateFlow<DetailAiState>(DetailAiState.Idle)
    val detailAiState: StateFlow<DetailAiState> = _detailAiState.asStateFlow()

    private val aiDetailCache = mutableMapOf<Pair<String, AudienceLevel>, String>()
    private var detailJob: Job? = null

    // Smart Monument Identifier state
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _imageNote = MutableStateFlow("")
    val imageNote: StateFlow<String> = _imageNote.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private var analysisJob: Job? = null

    // Smart Historical Guide Chat state
    private val _chatMessages = MutableStateFlow(listOf(createWelcomeMessage()))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.runtimeOpenAiKeyFlow.collect { savedKey ->
                _runtimeApiKey.value = savedKey
                AiEndpointConfig.updateRuntimeApiKey(savedKey)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val data = localKnowledge.getData()
            _attractions.value = data.attractions
            _localFaqs.value = data.faqs
        }
    }

    fun saveTempOpenAiKey(apiKey: String) {
        val cleaned = apiKey.trim()
        AiEndpointConfig.updateRuntimeApiKey(cleaned)
        _runtimeApiKey.value = cleaned
        viewModelScope.launch {
            preferences.setRuntimeOpenAiKey(cleaned)
        }
    }

    fun clearTempOpenAiKey() {
        AiEndpointConfig.updateRuntimeApiKey("")
        _runtimeApiKey.value = ""
        viewModelScope.launch {
            preferences.clearRuntimeOpenAiKey()
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectAudienceLevel(level: AudienceLevel) {
        viewModelScope.launch {
            preferences.setAudienceLevel(level)
            val currentAttraction = _selectedAttraction.value
            if (currentAttraction != null) {
                showInstantOfflineAttractionGuide(currentAttraction, level)
            }
        }
    }

    fun openAttractionDetail(attraction: Attraction) {
        _selectedAttraction.value = attraction
        showInstantOfflineAttractionGuide(attraction, audienceLevel.value)
    }

    fun closeAttractionDetail() {
        detailJob?.cancel()
        _selectedAttraction.value = null
        _detailAiState.value = DetailAiState.Idle
    }

    private fun showInstantOfflineAttractionGuide(
        attraction: Attraction,
        level: AudienceLevel
    ) {
        detailJob?.cancel()
        val cachedAi = aiDetailCache[attraction.id to level]
        if (cachedAi != null) {
            _detailAiState.value = DetailAiState.Success(
                text = cachedAi,
                isFromOfflineDb = false
            )
            return
        }

        // Load rich pre-bundled JSON guide immediately with zero network dependency
        detailJob = viewModelScope.launch {
            val offlineText = withContext(Dispatchers.IO) {
                localKnowledge.getOfflineAttractionGuide(attraction, level)
            }
            _detailAiState.value = DetailAiState.Success(
                text = offlineText,
                isFromOfflineDb = true
            )
        }
    }

    fun enrichAttractionWithAi(attraction: Attraction) {
        val level = audienceLevel.value
        val cacheKey = attraction.id to level

        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val offlineText = withContext(Dispatchers.IO) {
                localKnowledge.getOfflineAttractionGuide(attraction, level)
            }

            if (!localKnowledge.isNetworkAvailable()) {
                _detailAiState.value = DetailAiState.Success(
                    text = offlineText,
                    isFromOfflineDb = true,
                    aiUnavailableNotice = "اتصال اینترنت برقرار نیست؛ معرفی کامل از دانشنامه آفلاین سلطانیه نمایش داده می‌شود."
                )
                return@launch
            }

            _detailAiState.value = DetailAiState.Loading
            var finalResult = ""
            try {
                aiRepository.generateAttractionGuide(attraction, level).collect { partial ->
                    finalResult = partial
                    _detailAiState.value = DetailAiState.Streaming(partial)
                }
                aiDetailCache[cacheKey] = finalResult
                _detailAiState.value = DetailAiState.Success(
                    text = finalResult,
                    isFromOfflineDb = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiNotConfiguredException) {
                Log.w(TAG, "OpenAI key not configured; using offline JSON guide", e)
                _detailAiState.value = DetailAiState.Success(
                    text = offlineText,
                    isFromOfflineDb = true,
                    aiUnavailableNotice = "کلید OpenAI API هنوز تنظیم نشده است؛ معرفی کامل از دانشنامه آفلاین سلطانیه نمایش داده می‌شود."
                )
            } catch (e: AiEndpointException) {
                Log.w(TAG, "OpenAI endpoint error (${e.statusCode}); falling back to local JSON", e)
                val notice = if (e.isAuthOrQuotaError()) {
                    "کلید OpenAI نامعتبر است یا اعتبار آن به پایان رسیده است؛ معرفی کامل از دانشنامه آفلاین سلطانیه نمایش داده می‌شود."
                } else {
                    "پاسخ‌گویی سرویس OpenAI با اختلال روبه‌رو شد؛ معرفی کامل از دانشنامه محلی نمایش داده می‌شود."
                }
                _detailAiState.value = DetailAiState.Success(
                    text = offlineText,
                    isFromOfflineDb = true,
                    aiUnavailableNotice = notice
                )
            } catch (e: Exception) {
                Log.e(TAG, "AI attraction enrichment unavailable; falling back to local JSON", e)
                _detailAiState.value = DetailAiState.Success(
                    text = offlineText,
                    isFromOfflineDb = true,
                    aiUnavailableNotice = "دسترسی به سرویس OpenAI در این شبکه محدود یا زمان‌بر شد؛ معرفی کامل از دانشنامه محلی نمایش داده می‌شود."
                )
            }
        }
    }

    fun askGuideAboutAttraction(attraction: Attraction) {
        closeAttractionDetail()
        _currentTab.value = MainTab.GUIDE
        sendChatMessage(attraction.suggestedQuestion)
    }

    fun onImageSelected(uri: Uri?) {
        analysisJob?.cancel()
        _selectedImageUri.value = uri
        _analysisState.value = AnalysisUiState.Idle
    }

    fun onImageNoteChanged(note: String) {
        _imageNote.value = note
    }

    fun clearSelectedImage() {
        analysisJob?.cancel()
        _selectedImageUri.value = null
        _imageNote.value = ""
        _analysisState.value = AnalysisUiState.Idle
    }

    fun analyzeSelectedImage() {
        val uri = _selectedImageUri.value ?: return
        val level = audienceLevel.value
        val note = _imageNote.value.trim()

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Loading
            try {
                if (!localKnowledge.isNetworkAvailable()) {
                    val offlineGuide = withContext(Dispatchers.IO) {
                        localKnowledge.buildOfflineVisionGuide(note, level)
                    }
                    _analysisState.value = AnalysisUiState.OfflineFallback(
                        notice = "در حال حاضر اتصال اینترنت برقرار نیست و تحلیلگر تصویری آنلاین در دسترس نمی‌باشد. می‌توانید از راهنمای آفلاین تشخیص آثار زیر استفاده کنید:",
                        localGuide = offlineGuide
                    )
                    return@launch
                }

                val bitmap = decodeAndScaleBitmap(getApplication(), uri)
                var finalText = ""
                aiRepository.analyzeMonumentImage(bitmap, level, note).collect { partial ->
                    finalText = partial
                    _analysisState.value = AnalysisUiState.Streaming(partial)
                }
                _analysisState.value = AnalysisUiState.Success(finalText)
            } catch (e: VisionNotSupportedException) {
                Log.w(TAG, "Selected model does not support image input; switching to offline guide", e)
                val offlineGuide = withContext(Dispatchers.IO) {
                    localKnowledge.buildOfflineVisionGuide(note, level)
                }
                _analysisState.value = AnalysisUiState.OfflineFallback(
                    notice = "مدل فعلی (${AiEndpointConfig.modelName}) از تحلیل مستقیم تصویر پشتیبانی نمی‌کند یا قابلیت بینایی برای این سرویس فعال نیست. از اطلاعات محلی و راهنمای تشخیص آثار زیر می‌توانید کمک بگیرید:",
                    localGuide = offlineGuide
                )
            } catch (e: AiNotConfiguredException) {
                Log.w(TAG, "OpenAI key not configured; switching to offline vision guide", e)
                val offlineGuide = withContext(Dispatchers.IO) {
                    localKnowledge.buildOfflineVisionGuide(note, level)
                }
                _analysisState.value = AnalysisUiState.OfflineFallback(
                    notice = "کلید OpenAI API هنوز در تنظیمات وارد نشده است. تا زمان وارد کردن کلید، می‌توانید از راهنمای محلی شناخت آثار سلطانیه زیر استفاده کنید:",
                    localGuide = offlineGuide
                )
            } catch (e: AiEndpointException) {
                Log.w(TAG, "OpenAI vision endpoint error (${e.statusCode}); switching to offline fallback", e)
                val offlineGuide = withContext(Dispatchers.IO) {
                    localKnowledge.buildOfflineVisionGuide(note, level)
                }
                val notice = if (e.isAuthOrQuotaError()) {
                    "کلید OpenAI نامعتبر است یا سهمیه مصرف آن به پایان رسیده است. راهنمای محلی شناخت آثار سلطانیه در ادامه آماده است:"
                } else {
                    "سرویس تحلیل تصویر OpenAI موقتاً در دسترس نیست. از راهنمای محلی شناخت آثار سلطانیه در ادامه استفاده کنید:"
                }
                _analysisState.value = AnalysisUiState.OfflineFallback(
                    notice = notice,
                    localGuide = offlineGuide
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image analysis timed out; switching to offline fallback", e)
                val offlineGuide = withContext(Dispatchers.IO) {
                    localKnowledge.buildOfflineVisionGuide(note, level)
                }
                _analysisState.value = AnalysisUiState.OfflineFallback(
                    notice = "دسترسی به سرویس تحلیل تصویر OpenAI در این شبکه محدود یا کند است. بر اساس دانشنامه محلی می‌توانید از راهنمای زیر برای شناخت اثر استفاده کنید:",
                    localGuide = offlineGuide
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image analysis failed; switching to offline fallback", e)
                val offlineGuide = withContext(Dispatchers.IO) {
                    localKnowledge.buildOfflineVisionGuide(note, level)
                }
                _analysisState.value = AnalysisUiState.OfflineFallback(
                    notice = "امکان دریافت تحلیل تصویری از OpenAI فراهم نشد. اطلاعات راهنمای محلی و بناهای شاخص سلطانیه در ادامه آماده است:",
                    localGuide = offlineGuide
                )
            }
        }
    }

    fun sendChatMessage(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank() || _isChatStreaming.value) return

        val historySnapshot = _chatMessages.value.drop(1)
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )
        val assistantId = UUID.randomUUID().toString()
        val placeholderReply = ChatMessage(
            id = assistantId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _chatMessages.value = _chatMessages.value + userMessage + placeholderReply
        _isChatStreaming.value = true

        val currentLevel = audienceLevel.value
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                if (!localKnowledge.isNetworkAvailable()) {
                    val offlineReply = withContext(Dispatchers.IO) {
                        localKnowledge.findBestOfflineChatReply(trimmed, currentLevel)
                    }
                    val fullOfflineMsg = buildOfflineChatNotice(
                        "اتصال اینترنت برقرار نیست؛ این پاسخ از دانشنامه آفلاین سلطانیه ارائه شده است:",
                        offlineReply
                    )
                    updateAssistantMessage(
                        id = assistantId,
                        newText = fullOfflineMsg,
                        isStreaming = false,
                        isOfflineFallback = true
                    )
                    return@launch
                }

                var latestText = ""
                aiRepository.sendChatMessageStream(
                    historyMessages = historySnapshot,
                    newUserMessage = trimmed,
                    audienceLevel = currentLevel
                ).collect { partial ->
                    latestText = partial
                    updateAssistantMessage(
                        id = assistantId,
                        newText = partial,
                        isStreaming = true,
                        isOfflineFallback = false
                    )
                }
                updateAssistantMessage(
                    id = assistantId,
                    newText = latestText,
                    isStreaming = false,
                    isOfflineFallback = false
                )
            } catch (e: AiNotConfiguredException) {
                Log.w(TAG, "OpenAI key not configured; falling back to local JSON", e)
                val offlineReply = withContext(Dispatchers.IO) {
                    localKnowledge.findBestOfflineChatReply(trimmed, currentLevel)
                }
                val fullOfflineMsg = buildOfflineChatNotice(
                    "کلید OpenAI API هنوز وارد نشده است (می‌توانید از دکمه تنظیمات بالای صفحه کلید موقت را ثبت کنید)؛ پاسخ مرتبط از دانشنامه محلی سلطانیه:",
                    offlineReply
                )
                updateAssistantMessage(
                    id = assistantId,
                    newText = fullOfflineMsg,
                    isStreaming = false,
                    isOfflineFallback = true
                )
            } catch (e: AiEndpointException) {
                Log.w(TAG, "OpenAI endpoint error (${e.statusCode}); falling back to local JSON", e)
                val offlineReply = withContext(Dispatchers.IO) {
                    localKnowledge.findBestOfflineChatReply(trimmed, currentLevel)
                }
                val notice = if (e.isAuthOrQuotaError()) {
                    "کلید OpenAI نامعتبر است یا سهمیه مصرف آن به پایان رسیده است؛ پاسخ مرتبط از دانشنامه محلی سلطانیه:"
                } else {
                    "سرویس OpenAI موقتاً با خطا پاسخ داد؛ پاسخ مرتبط از پایگاه داده محلی سلطانیه:"
                }
                val fullOfflineMsg = buildOfflineChatNotice(notice, offlineReply)
                updateAssistantMessage(
                    id = assistantId,
                    newText = fullOfflineMsg,
                    isStreaming = false,
                    isOfflineFallback = true
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat response timed out; falling back to local JSON", e)
                val offlineReply = withContext(Dispatchers.IO) {
                    localKnowledge.findBestOfflineChatReply(trimmed, currentLevel)
                }
                val fullOfflineMsg = buildOfflineChatNotice(
                    "دسترسی به سرویس OpenAI در این شبکه با کندی یا محدودیت روبه‌رو شد؛ پاسخ زیر از دانشنامه آفلاین سلطانیه آماده شده است:",
                    offlineReply
                )
                updateAssistantMessage(
                    id = assistantId,
                    newText = fullOfflineMsg,
                    isStreaming = false,
                    isOfflineFallback = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat streaming failed; falling back to local JSON", e)
                val offlineReply = withContext(Dispatchers.IO) {
                    localKnowledge.findBestOfflineChatReply(trimmed, currentLevel)
                }
                val fullOfflineMsg = buildOfflineChatNotice(
                    "دسترسی به OpenAI در این شبکه محدود است؛ پاسخ مرتبط از پایگاه داده محلی سلطانیه:",
                    offlineReply
                )
                updateAssistantMessage(
                    id = assistantId,
                    newText = fullOfflineMsg,
                    isStreaming = false,
                    isOfflineFallback = true
                )
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    fun resetChat() {
        chatJob?.cancel()
        _isChatStreaming.value = false
        _chatMessages.value = listOf(createWelcomeMessage())
    }

    private fun buildOfflineChatNotice(notice: String, body: String): String {
        return "$notice\n\n$body"
    }

    private fun updateAssistantMessage(
        id: String,
        newText: String,
        isStreaming: Boolean,
        isOfflineFallback: Boolean,
        isError: Boolean = false
    ) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    text = newText,
                    isStreaming = isStreaming,
                    isOfflineFallback = isOfflineFallback,
                    isError = isError
                )
            } else {
                msg
            }
        }
    }

    private fun createWelcomeMessage(): ChatMessage {
        return ChatMessage(
            id = "welcome",
            isUser = false,
            text = "سلام! من راهنمای هوشمند میراث فرهنگی سلطانیه هستم. می‌توانید درباره تاریخچه گنبد سلطانیه، معماری ایلخانی، معبد داش‌کسن یا بهترین مسیر بازدید از من بپرسید. حتی در صورت محدودیت شبکه، پاسخ‌های مستند دانشنامه محلی در دسترس شماست."
        )
    }

    private suspend fun decodeAndScaleBitmap(context: Context, uri: Uri): Bitmap =
        withContext(Dispatchers.IO) {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val maxDim = maxOf(info.size.width, info.size.height)
                    if (maxDim > MAX_IMAGE_DIMENSION) {
                        val scale = MAX_IMAGE_DIMENSION.toFloat() / maxDim.toFloat()
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw IllegalStateException("امکان خواندن تصویر وجود ندارد.")
            }

            val width = rawBitmap.width
            val height = rawBitmap.height
            val maxDim = maxOf(width, height)
            if (maxDim <= MAX_IMAGE_DIMENSION) {
                rawBitmap
            } else {
                val scale = MAX_IMAGE_DIMENSION.toFloat() / maxDim.toFloat()
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
        }
}
