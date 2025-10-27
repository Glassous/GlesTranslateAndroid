package com.glassous.glestranslate.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.client.request.accept
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object BuiltInTranslationService {
    private const val BASE_URL = "https://api.jkyai.top/API/depsek3.1.php"

    private val client = HttpClient(Android) {
        install(ContentNegotiation)
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 20000
        }
    }

    suspend fun translate(question: String, targetLanguageName: String): String {
        val systemPrompt = buildSystemPrompt(targetLanguageName)
        val response = client.get(BASE_URL) {
            accept(ContentType.Text.Plain)
            parameter("question", question)
            parameter("type", "text")
            parameter("system", systemPrompt)
        }
        val body = response.bodyAsText().trim()
        if (body.startsWith("{") && body.endsWith("}")) {
            return parseJsonResult(body)
        }
        return body
    }

    private fun buildSystemPrompt(languageName: String): String =
        """
        你是一个专业的翻译助手。请将用户输入的文本翻译成${languageName}。要求：
        1. 保持原文的语气和风格
        2. 确保翻译准确、自然、流畅
        3. 如果是专业术语，请提供准确的对应翻译
        4. 只返回翻译结果，不要添加任何解释或说明
        5. 如果原文已经是目标语言，请直接返回原文
        """.trimIndent()

    private fun parseJsonResult(jsonText: String): String {
        return try {
            val element = Json.parseToJsonElement(jsonText)
            val obj = element as? JsonObject
            val result = obj?.get("result")?.jsonPrimitive?.content
            result ?: obj?.get("text")?.jsonPrimitive?.content ?: jsonText
        } catch (e: Exception) {
            jsonText
        }
    }
}