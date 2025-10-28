package com.glassous.glestranslate.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Built-in OCR/ASR service using pearktrue.cn APIs.
 * Image OCR: https://api.pearktrue.cn/api/ocr/ (multipart: file)
 * Audio ASR: https://api.pearktrue.cn/api/audiocr/ (multipart: file)
 */
object OcrService {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(Android) {
        install(ContentNegotiation)
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 60000
        }
    }

    suspend fun recognizeImage(imageBytes: ByteArray, filename: String = "image.jpg"): String {
        val response = client.post("https://api.pearktrue.cn/api/ocr/") {
            setBody(
                MultiPartFormDataContent(formData {
                    append(
                        key = "file",
                        value = imageBytes,
                        headers = headersOf(
                            "Content-Disposition",
                            "form-data; name=\"file\"; filename=\"$filename\""
                        )
                    )
                })
            )
            contentType(ContentType.MultiPart.FormData)
        }
        val body = response.bodyAsText().trim()
        return parseResultOnly(body)
    }

    suspend fun recognizeAudio(audioBytes: ByteArray, filename: String = "audio.m4a"): String {
        val response = client.post("https://api.pearktrue.cn/api/audiocr/") {
            setBody(
                MultiPartFormDataContent(formData {
                    append(
                        key = "file",
                        value = audioBytes,
                        headers = headersOf(
                            "Content-Disposition",
                            "form-data; name=\"file\"; filename=\"$filename\""
                        )
                    )
                })
            )
            contentType(ContentType.MultiPart.FormData)
        }
        val body = response.bodyAsText().trim()
        return parseResultOnly(body)
    }

    // Parse pearktrue.cn OCR/ASR response and return only text result.
    private fun parseResultOnly(body: String): String {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            // Prefer `data` object if present
            val dataObj = root["data"]?.jsonObject
            val fromData = dataObj?.let { obj ->
                // 1) ParsedText
                obj["ParsedText"]?.jsonPrimitive?.contentOrNull?.let { normalizeNewlines(it).trim() } ?: run {
                    // 2) TextLine array
                    (obj["TextLine"] as? JsonArray)?.let { arr ->
                        arr.joinToString(separator = "\n") { el ->
                            normalizeNewlines(el.jsonPrimitive.contentOrNull ?: "")
                        }.trim()
                    }
                }
            }
            if (!fromData.isNullOrBlank()) return fromData

            // Fallbacks at root level
            root["ParsedText"]?.jsonPrimitive?.contentOrNull?.let { normalizeNewlines(it).trim() }?.takeIf { it.isNotBlank() }?.let { return it }
            (root["TextLine"] as? JsonArray)?.let { arr ->
                val joined = arr.joinToString("\n") { el -> normalizeNewlines(el.jsonPrimitive.contentOrNull ?: "") }.trim()
                if (joined.isNotBlank()) return joined
            }
            // Common alternatives
            root["text"]?.jsonPrimitive?.contentOrNull?.let { normalizeNewlines(it).trim() }?.takeIf { it.isNotBlank() }?.let { return it }
            root["result"]?.jsonPrimitive?.contentOrNull?.let { normalizeNewlines(it).trim() }?.takeIf { it.isNotBlank() }?.let { return it }

            // If code != 200, optionally return msg; else raw body
            val code = root["code"]?.jsonPrimitive?.contentOrNull
            val msg = root["msg"]?.jsonPrimitive?.contentOrNull
            if (code != null && code != "200" && !msg.isNullOrBlank()) return msg!!.trim()

            body
        } catch (_: Exception) {
            body
        }
    }

    private fun normalizeNewlines(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n')
}