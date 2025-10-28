package com.glassous.glestranslate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.glestranslate.data.*
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.glassous.glestranslate.data.appDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TranslationViewModel(private val context: Context) : ViewModel() {
    private val dataStore = context.appDataStore
    
    private val _translationHistory = MutableStateFlow<List<TranslationHistoryItem>>(emptyList())
    val translationHistory: StateFlow<List<TranslationHistoryItem>> = _translationHistory.asStateFlow()
    
    private val _customLanguages = MutableStateFlow<List<CustomLanguage>>(emptyList())
    val customLanguages: StateFlow<List<CustomLanguage>> = _customLanguages.asStateFlow()
    
    private val _selectedLanguage = MutableStateFlow(SelectedLanguage("en", "英语"))
    val selectedLanguage: StateFlow<SelectedLanguage> = _selectedLanguage.asStateFlow()
    
    private val _aiConfigEnabled = MutableStateFlow(false)
    val aiConfigEnabled: StateFlow<Boolean> = _aiConfigEnabled.asStateFlow()
    
    private val _aiConfig = MutableStateFlow(
        AiConfig(
            baseUrl = "",
            model = "",
            apiKey = "",
            multiModalModel = ""
        )
    )
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()
    
    private val _multiModalEnabled = MutableStateFlow(false)
    val multiModalEnabled: StateFlow<Boolean> = _multiModalEnabled.asStateFlow()
    
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()
    
    private val _translationResult = MutableStateFlow("")
    val translationResult: StateFlow<String> = _translationResult.asStateFlow()

    // 识别状态与结果
    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    private val _recognitionResult = MutableStateFlow("")
    val recognitionResult: StateFlow<String> = _recognitionResult.asStateFlow()
    
    init {
        // 从 DataStore 加载持久化数据
        viewModelScope.launch {
            dataStore.data.collect { data ->
                _translationHistory.value = data.translationHistory
                _customLanguages.value = data.customLanguages
                _selectedLanguage.value = data.selectedLanguage
                _aiConfigEnabled.value = data.aiConfigEnabled
                _aiConfig.value = data.aiConfig
                _multiModalEnabled.value = data.multiModalEnabled
            }
        }
    }
    
    fun translate(sourceText: String, targetLanguageCode: String) {
        viewModelScope.launch {
            _isTranslating.value = true
            
            try {
                // 模拟翻译API调用
                val translatedText = simulateTranslation(sourceText, targetLanguageCode)
                _translationResult.value = translatedText
                
                // 添加到历史记录
                addToHistory(sourceText, translatedText, _selectedLanguage.value.name)
                
            } catch (e: Exception) {
                _translationResult.value = "翻译失败: ${e.message}"
            } finally {
                _isTranslating.value = false
            }
        }
    }
    
    private suspend fun simulateTranslation(sourceText: String, targetLanguageCode: String): String {
        // 根据是否启用自定义AI API选择不同的翻译逻辑
        return if (_aiConfigEnabled.value) {
            customApiTranslateStreaming(sourceText)
        } else {
            projectApiTranslate(sourceText)
        }
    }
    
    private suspend fun projectApiTranslate(sourceText: String): String {
        return com.glassous.glestranslate.network.BuiltInTranslationService.translate(
            question = sourceText,
            targetLanguageName = _selectedLanguage.value.name
        )
    }
    
    private suspend fun customApiTranslateStreaming(sourceText: String): String {
        val cfg = _aiConfig.value
        require(cfg.baseUrl.isNotBlank()) { "自定义AI Base URL 未配置" }
        require(cfg.apiKey.isNotBlank()) { "自定义AI API Key 未配置" }
        require(cfg.model.isNotBlank()) { "自定义AI 模型未配置" }

        // 清空现有结果以便流式追加
        _translationResult.value = ""
        return com.glassous.glestranslate.network.CustomOpenAIService.streamTranslate(
            baseUrl = cfg.baseUrl,
            apiKey = cfg.apiKey,
            model = cfg.model,
            targetLanguageName = _selectedLanguage.value.name,
            sourceText = sourceText
        ) { delta ->
            _translationResult.value += delta
        }
    }
    
    private fun addToHistory(sourceText: String, translatedText: String, targetLanguage: String) {
        val newItem = TranslationHistoryItem(
            id = System.currentTimeMillis(),
            sourceText = sourceText,
            translatedText = translatedText,
            targetLanguage = targetLanguage,
            timestamp = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        
        // 立即更新内存状态
        _translationHistory.value = listOf(newItem) + _translationHistory.value
        // 持久化到 DataStore
        viewModelScope.launch {
            dataStore.updateData { app ->
                app.copy(translationHistory = listOf(newItem) + app.translationHistory)
            }
        }
    }
    
    fun selectLanguage(language: SelectedLanguage) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            dataStore.updateData { app -> app.copy(selectedLanguage = language) }
        }
    }
    
    fun addCustomLanguage(languageName: String) {
        val newLanguage = CustomLanguage(
            code = "custom_${System.currentTimeMillis()}",
            name = languageName
        )
        _customLanguages.value = _customLanguages.value + newLanguage
        viewModelScope.launch {
            dataStore.updateData { app ->
                app.copy(customLanguages = app.customLanguages + newLanguage)
            }
        }
    }
    
    fun deleteCustomLanguage(languageCode: String) {
        val updated = _customLanguages.value.filter { it.code != languageCode }
        _customLanguages.value = updated
        viewModelScope.launch {
            dataStore.updateData { app -> app.copy(customLanguages = app.customLanguages.filter { it.code != languageCode }) }
        }
    }
    
    fun editCustomLanguage(languageCode: String, newName: String) {
        val updated = _customLanguages.value.map { language ->
            if (language.code == languageCode) {
                language.copy(name = newName)
            } else {
                language
            }
        }
        _customLanguages.value = updated
        viewModelScope.launch {
            dataStore.updateData { app ->
                app.copy(customLanguages = app.customLanguages.map { language ->
                    if (language.code == languageCode) language.copy(name = newName) else language
                })
            }
        }
    }
    
    // 移除旧的实现（见下方持久化版本）
    
    fun updateMultiModalEnabled(enabled: Boolean) {
        _multiModalEnabled.value = enabled
        viewModelScope.launch {
            dataStore.updateData { app -> app.copy(multiModalEnabled = enabled) }
        }
    }
    
    fun clearTranslationResult() {
        _translationResult.value = ""
    }

    fun clearRecognitionResult() {
        _recognitionResult.value = ""
    }

    fun updateAiConfigEnabled(enabled: Boolean) {
        _aiConfigEnabled.value = enabled
        if (!enabled) {
            _multiModalEnabled.value = false
        }
        viewModelScope.launch {
            dataStore.updateData { app ->
                app.copy(
                    aiConfigEnabled = enabled,
                    multiModalEnabled = if (!enabled) false else app.multiModalEnabled
                )
            }
        }
    }

    fun updateAiConfig(config: AiConfig) {
        _aiConfig.value = config
        viewModelScope.launch {
            dataStore.updateData { app -> app.copy(aiConfig = config) }
        }
    }

    fun deleteHistoryItem(id: Long) {
        val updated = _translationHistory.value.filter { it.id != id }
        _translationHistory.value = updated
        viewModelScope.launch {
            dataStore.updateData { app ->
                app.copy(translationHistory = app.translationHistory.filter { it.id != id })
            }
        }
    }

    /**
     * 图片识别：根据设置选择内置 API 或多模态流式识别
     */
    fun recognizeImage(uri: Uri) {
        viewModelScope.launch {
            _recognitionResult.value = ""
            _isRecognizing.value = true
            try {
                val bytes = readBytesFromUri(uri)
                val mime = getMimeTypeFromUri(uri) ?: "image/jpeg"
                val filename = getDisplayNameFromUri(uri) ?: "image.jpg"

                if (_aiConfigEnabled.value && _multiModalEnabled.value) {
                    val cfg = _aiConfig.value
                    require(cfg.baseUrl.isNotBlank()) { "自定义AI Base URL 未配置" }
                    require(cfg.apiKey.isNotBlank()) { "自定义AI API Key 未配置" }
                    require(cfg.multiModalModel.isNotBlank()) { "自定义AI 多模态模型未配置" }

                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    // 流式输出：增量追加到识别结果
                    com.glassous.glestranslate.network.CustomOpenAIService.streamRecognizeImage(
                        baseUrl = cfg.baseUrl,
                        apiKey = cfg.apiKey,
                        model = cfg.multiModalModel,
                        imageBase64 = b64,
                        imageMime = mime
                    ) { delta ->
                        _recognitionResult.value += delta
                    }
                } else {
                    // 内置 API：一次性返回
                    val result = com.glassous.glestranslate.network.OcrService.recognizeImage(
                        imageBytes = bytes,
                        filename = filename
                    )
                    _recognitionResult.value = result
                }
            } catch (e: Exception) {
                _recognitionResult.value = "识别失败: ${e.message}"
            } finally {
                _isRecognizing.value = false
            }
        }
    }

    /**
     * 音频识别：根据设置选择内置 API 或多模态流式识别
     */
    fun recognizeAudio(uri: Uri) {
        viewModelScope.launch {
            _recognitionResult.value = ""
            _isRecognizing.value = true
            try {
                val bytes = readBytesFromUri(uri)
                val mime = getMimeTypeFromUri(uri) ?: "audio/mpeg"
                val filename = getDisplayNameFromUri(uri) ?: "audio.mp3"

                if (_aiConfigEnabled.value && _multiModalEnabled.value) {
                    val cfg = _aiConfig.value
                    require(cfg.baseUrl.isNotBlank()) { "自定义AI Base URL 未配置" }
                    require(cfg.apiKey.isNotBlank()) { "自定义AI API Key 未配置" }
                    require(cfg.multiModalModel.isNotBlank()) { "自定义AI 多模态模型未配置" }

                    // 解析格式，例如 audio/mpeg -> mpeg
                    val audioFormat = mime.substringAfter('/', missingDelimiterValue = "mpeg")
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    com.glassous.glestranslate.network.CustomOpenAIService.streamRecognizeAudio(
                        baseUrl = cfg.baseUrl,
                        apiKey = cfg.apiKey,
                        model = cfg.multiModalModel,
                        audioBase64 = b64,
                        audioFormat = audioFormat
                    ) { delta ->
                        _recognitionResult.value += delta
                    }
                } else {
                    val result = com.glassous.glestranslate.network.OcrService.recognizeAudio(
                        audioBytes = bytes,
                        filename = filename
                    )
                    _recognitionResult.value = result
                }
            } catch (e: Exception) {
                _recognitionResult.value = "识别失败: ${e.message}"
            } finally {
                _isRecognizing.value = false
            }
        }
    }

    // Helpers
    private fun readBytesFromUri(uri: Uri): ByteArray {
        val cr = context.contentResolver
        val input = cr.openInputStream(uri) ?: throw IllegalArgumentException("无法打开文件: $uri")
        return input.use { it.readBytes() }
    }

    private fun getMimeTypeFromUri(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    private fun getDisplayNameFromUri(uri: Uri): String? {
        val cr = context.contentResolver
        return try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}