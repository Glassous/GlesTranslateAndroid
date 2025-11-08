package com.glassous.glestranslate.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*

/**
 * OpenAI-compatible streaming client.
 * Uses Chat Completions with `stream=true` and parses SSE chunks.
 * Base URL, API key, and model are supplied by user settings.
 */
object CustomOpenAIService {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 60000
        }
    }

    /**
     * Stream translation using OpenAI Chat Completions.
     * Aggregates delta content and calls [onDelta] for each piece.
     * Returns the full translated result.
     */
    suspend fun streamTranslate(
        baseUrl: String,
        apiKey: String,
        model: String,
        targetLanguageName: String,
        sourceText: String,
        onDelta: (String) -> Unit
    ): String {
        val systemPrompt = buildSystemPrompt(targetLanguageName)
        val url = buildChatCompletionsUrl(baseUrl)

        val payload = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", sourceText)
                })
            })
        }

        val response = client.post(url) {
            header("Authorization", "Bearer $apiKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.parse("text/event-stream"))
            // Some providers require specific UA
            header("User-Agent", "GlesTranslate/1.0 (Android; Ktor)")
            setBody(payload)
        }

        val channel: ByteReadChannel = response.bodyAsChannel()
        val sb = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.isBlank()) continue
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            val deltaText = parseDeltaText(data)
            if (deltaText.isNotEmpty()) {
                sb.append(deltaText)
                onDelta(deltaText)
            }
        }

        return sb.toString()
    }

    /**
     * Stream audio transcription using OpenAI-compatible multimodal Chat Completions.
     * The audio is embedded as base64 in the user content. Delta chunks are parsed via SSE.
     */
    suspend fun streamRecognizeAudio(
        baseUrl: String,
        apiKey: String,
        model: String,
        audioBase64: String,
        audioFormat: String,
        onDelta: (String) -> Unit
    ): String {
        val url = buildChatCompletionsUrl(baseUrl)
        val prompt = "请识别这个音频文件中的文字内容，直接返回识别结果，不要添加任何解释。"

        val payload = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "input_text")
                            put("text", prompt)
                        })
                        add(buildJsonObject {
                            put("type", "input_audio")
                            put("audio", buildJsonObject {
                                put("data", audioBase64)
                                put("format", audioFormat)
                            })
                        })
                    })
                })
            })
        }

        val response = client.post(url) {
            header("Authorization", "Bearer $apiKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.parse("text/event-stream"))
            header("User-Agent", "GlesTranslate/1.0 (Android; Ktor)")
            setBody(payload)
        }

        val channel: ByteReadChannel = response.bodyAsChannel()
        val sb = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.isBlank()) continue
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            val deltaText = parseDeltaText(data)
            if (deltaText.isNotEmpty()) {
                sb.append(deltaText)
                onDelta(deltaText)
            }
        }
        return sb.toString()
    }

    /**
     * Stream image OCR using OpenAI-compatible multimodal Chat Completions.
     * The image is embedded as a data URL in the user content. Delta chunks are parsed via SSE.
     */
    suspend fun streamRecognizeImage(
        baseUrl: String,
        apiKey: String,
        model: String,
        imageBase64: String,
        imageMime: String,
        onDelta: (String) -> Unit
    ): String {
        val url = buildChatCompletionsUrl(baseUrl)
        val prompt = "请识别这张图片中的文字内容，直接返回识别结果，不要添加任何解释。"

        val dataUrl = "data:$imageMime;base64,$imageBase64"
        // 使用 OpenAI Chat Completions 标准的图像输入结构：image_url
        val payload = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", dataUrl)
                            })
                        })
                    })
                })
            })
        }

        val response = client.post(url) {
            header("Authorization", "Bearer $apiKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.parse("text/event-stream"))
            header("User-Agent", "GlesTranslate/1.0 (Android; Ktor)")
            setBody(payload)
        }

        val channel: ByteReadChannel = response.bodyAsChannel()
        val sb = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.isBlank()) continue
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            val deltaText = parseDeltaText(data)
            if (deltaText.isNotEmpty()) {
                sb.append(deltaText)
                onDelta(deltaText)
            }
        }
        return sb.toString()
    }

    private fun buildChatCompletionsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
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

    /**
     * Parse SSE "data:" line for OpenAI Chat Completions streaming.
     * Supports both Chat delta (choices[].delta.content) and
     * legacy Completions text (choices[].text).
     */
    private fun parseDeltaText(dataJson: String): String {
        return try {
            val root = json.parseToJsonElement(dataJson).jsonObject
            val choices = root["choices"] as? JsonArray ?: return ""
            val sb = StringBuilder()
            for (ch in choices) {
                val obj = ch.jsonObject
                val delta = obj["delta"]?.jsonObject
                val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                if (content != null) {
                    sb.append(content)
                } else {
                    val text = obj["text"]?.jsonPrimitive?.contentOrNull
                    if (text != null) sb.append(text)
                }
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }
}