package com.glassous.glestranslate.data

import kotlinx.serialization.Serializable

@Serializable
data class TranslationBackup(
    val version: String,
    val timestamp: String,
    val data: TranslationAppData
)

@Serializable
data class TranslationAppData(
    val translationHistory: List<TranslationHistoryItem>,
    val customLanguages: List<CustomLanguage>,
    val selectedLanguage: SelectedLanguage,
    val aiConfigEnabled: Boolean,
    val aiConfig: AiConfig,
    val multiModalEnabled: Boolean
)

@Serializable
data class TranslationHistoryItem(
    val id: Long,
    val sourceText: String,
    val translatedText: String,
    val targetLanguage: String,
    val timestamp: String
)

@Serializable
data class CustomLanguage(
    val code: String,
    val name: String
)

@Serializable
data class SelectedLanguage(
    val code: String,
    val name: String
)

@Serializable
data class AiConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val multiModalModel: String
)

// 预定义语言列表
object PredefinedLanguages {
    val languages = listOf(
        SelectedLanguage("zh", "中文"),
        SelectedLanguage("en", "英语"),
        SelectedLanguage("ja", "日语"),
        SelectedLanguage("ko", "韩语"),
        SelectedLanguage("fr", "法语"),
        SelectedLanguage("de", "德语"),
        SelectedLanguage("es", "西班牙语"),
        SelectedLanguage("ru", "俄语"),
        SelectedLanguage("ar", "阿拉伯语"),
        SelectedLanguage("pt", "葡萄牙语")
    )
}