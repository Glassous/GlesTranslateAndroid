package com.glassous.glestranslate.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object TranslationAppDataSerializer : Serializer<TranslationAppData> {
    override val defaultValue: TranslationAppData = TranslationAppData(
        translationHistory = emptyList(),
        customLanguages = emptyList(),
        selectedLanguage = SelectedLanguage("en", "英语"),
        aiConfigEnabled = false,
        aiConfig = AiConfig(
            baseUrl = "",
            model = "",
            apiKey = "",
            multiModalModel = ""
        ),
        multiModalEnabled = false
    )

    override suspend fun readFrom(input: InputStream): TranslationAppData {
        return try {
            val content = input.readBytes().decodeToString()
            if (content.isBlank()) defaultValue
            else Json { ignoreUnknownKeys = true }
                .decodeFromString(TranslationAppData.serializer(), content)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: TranslationAppData, output: OutputStream) {
        val json = Json { prettyPrint = true }
            .encodeToString(TranslationAppData.serializer(), t)
        output.write(json.encodeToByteArray())
    }
}