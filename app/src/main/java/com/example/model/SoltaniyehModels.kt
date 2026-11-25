package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AudienceLevel(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val promptDirective: String
) {
    GENERAL(
        id = "general",
        titleFa = "گردشگر عمومی",
        subtitleFa = "زبان ساده و داستانی",
        promptDirective = "مخاطب گردشگر عمومی است. با لحنی گرم، روان، داستانی و قابل فهم برای عموم بنویس. از اصطلاحات بیش از حد فنی پرهیز کن و اگر واژه معماری به کار بردی، خیلی کوتاه آن را توضیح بده."
    ),
    RESEARCHER(
        id = "researcher",
        titleFa = "دانشجو و پژوهشگر",
        subtitleFa = "تخصصی و معماری",
        promptDirective = "مخاطب دانشجو یا پژوهشگر معماری و تاریخ است. با لحن علمی، دقیق و تخصصی بنویس؛ به ساختارهای سازه‌ای (مانند گنبد دوپوسته پیوسته، طاق‌بندی، ترنبه‌ها، کتیبه‌های کوفی و ثلث، فنون کاشیکاری معرق و معقلی و گچ‌بری) و زمینه‌های تاریخی عصر ایلخانی بپرداز."
    ),
    KIDS_TEENS(
        id = "kids_teens",
        titleFa = "کودک و نوجوان",
        subtitleFa = "کوتاه و جذاب",
        promptDirective = "مخاطب کودک و نوجوان است. جملات را کوتاه، پرانرژی، کنجکاوی‌برانگیز و صمیمی نگه دار. با تصویرسازی ذهنی جذاب و نکات شگفت‌انگیز (بدون ایموجی زیاد و بدون پیچیدگی سنگین) توضیح بده."
    );

    companion object {
        fun fromId(id: String?): AudienceLevel {
            return entries.firstOrNull { it.id == id } ?: GENERAL
        }
    }
}

enum class MainTab {
    EXPLORE,
    IDENTIFIER,
    GUIDE
}

data class Attraction(
    val id: String,
    val title: String,
    val subtitle: String,
    val era: String,
    val shortDescription: String,
    val visitingDuration: String,
    val locationNote: String,
    @DrawableRes val imageRes: Int,
    val quickTags: List<String>,
    val suggestedQuestion: String
)

object SoltaniyehAttractions {
    val all: List<Attraction> = listOf(
        Attraction(
            id = "soltaniyeh_dome",
            title = "گنبد سلطانیه",
            subtitle = "آرامگاه سلطان محمد خدابنده (اولجایتو) • ثبت جهانی یونسکو",
            era = "دوره ایلخانی (۷۰۴ تا ۷۱۲ هـ.ق)",
            shortDescription = "بزرگ‌ترین گنبد آجری جهان و شاهکار معماری ایرانی-اسلامی با گنبد فیروزه‌ای دوپوسته و هشت مناره پیرامونی.",
            visitingDuration = "۶۰ تا ۹۰ دقیقه",
            locationNote = "مرکز شهر تاریخی سلطانیه، استان زنجان",
            imageRes = R.drawable.img_soltaniyeh_dome,
            quickTags = listOf("میراث یونسکو", "بزرگ‌ترین گنبد آجری", "معماری ایلخانی"),
            suggestedQuestion = "راز پایداری و شیوه ساخت گنبد دوپوسته سلطانیه و تزئینات داخل آن چیست؟"
        ),
        Attraction(
            id = "dashkasan_temple",
            title = "معبد داش‌کسن (اژدهای سنگی)",
            subtitle = "مجموعه صخره‌ای و نقوش برجسته اژدهای ایلخانی",
            era = "اوایل قرن هشتم هجری (دوره ایلخانی)",
            shortDescription = "معبدی تراشیده در دل صخره با دو نقش‌برجسته کم‌نظیر از اژدهای سبک چینی در کنار محراب‌ها و نقوش اسلیمی ایرانی.",
            visitingDuration = "۴۵ تا ۶۰ دقیقه",
            locationNote = "حدود ۱۰ کیلومتری جنوب‌شرقی سلطانیه (روستای ویر)",
            imageRes = R.drawable.img_dashkasan_temple,
            quickTags = listOf("معماری صخره‌ای", "نقش اژدها", "پیوند هنر ایران و شرق آسیا"),
            suggestedQuestion = "چرا در معبد داش‌کسن سلطانیه نقش اژدهای چینی در کنار مقرنس و اسلیمی ایرانی دیده می‌شود؟"
        ),
        Attraction(
            id = "chelebi_oghli",
            title = "آرامگاه و خانقاه چلبی‌اوغلی",
            subtitle = "برج آرامگاهی هشت‌ضلعی و مجموعه عرفانی قرن هشتم",
            era = "حدود ۷۱۰ تا ۷۳۳ هـ.ق (دوره ایلخانی)",
            shortDescription = "بنای آرامگاهی شیخ براق و چلبی‌اوغلی با گنبد رک هرمی، تزئینات آجری، کاشی‌های فیروزه‌ای و حجرات خانقاه.",
            visitingDuration = "۳۰ تا ۴۵ دقیقه",
            locationNote = "ضلع جنوب‌غربی گنبد سلطانیه",
            imageRes = R.drawable.img_chelebi_oghli,
            quickTags = listOf("برج آرامگاهی", "خانقاه تاریخی", "کاشیکاری معقلی"),
            suggestedQuestion = "چلبی‌اوغلی چه کسی بود و ویژگی‌های معماری خانقاه و برج آرامگاهی او چیست؟"
        ),
        Attraction(
            id = "soltaniyeh_complex",
            title = "مجموعه تاریخی و ارگ سلطانیه",
            subtitle = "محوطه باستان‌شناسی پایتخت ایلخانی و حصار شهر قدیم",
            era = "سده هشتم هجری به بعد",
            shortDescription = "گستره شهر باستانی سلطانیه شامل بقایای بارو، خندق، ایوان‌ها و ابنیه پیرامون گنبد که شکوه پایتخت اولجایتو را بازمی‌تاباند.",
            visitingDuration = "۴۵ تا ۶۰ دقیقه",
            locationNote = "محوطه پیرامونی گنبد و دشت سلطانیه",
            imageRes = R.drawable.img_soltaniyeh_complex,
            quickTags = listOf("محوطه باستان‌شناسی", "پایتخت ایلخانی", "بافت تاریخی"),
            suggestedQuestion = "شهر سلطانیه در دوره ایلخانی چه ساختار شهری و بناهای پیرامونی داشته است؟"
        )
    )
}

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Streaming(val text: String) : AnalysisUiState
    data class Success(val text: String) : AnalysisUiState
    data class OfflineFallback(
        val notice: String,
        val localGuide: String
    ) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface DetailAiState {
    data object Idle : DetailAiState
    data object Loading : DetailAiState
    data class Streaming(val text: String) : DetailAiState
    data class Success(
        val text: String,
        val isFromOfflineDb: Boolean = true,
        val aiUnavailableNotice: String? = null
    ) : DetailAiState
    data class Error(val message: String) : DetailAiState
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val isError: Boolean = false
)
