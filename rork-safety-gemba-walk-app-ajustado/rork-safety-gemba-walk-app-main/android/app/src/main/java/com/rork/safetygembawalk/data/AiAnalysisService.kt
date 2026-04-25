package com.rork.safetygembawalk.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class AiAnalysisResult(
    val riskLevel: String = "Médio",
    val riskDescription: String,
    val confidence: String = "85%",
    val recommendations: List<String> = emptyList()
)

class AiAnalysisService(private val context: Context) {

    private val apiKey = "AIzaSyBFpYvLzmL0EArBq9NuRXHEblNypUKxm"

    suspend fun analyzeSafetyImage(imagePath: String): Result<AiAnalysisResult> {
        return try {
            val base64Image = resizeAndEncodeImage(imagePath)
                ?: return Result.failure(Exception("Não foi possível processar a imagem."))

            val prompt = """
                Você é um especialista em segurança do trabalho e auditoria industrial.

                Analise a imagem enviada e identifique:
                1. O risco ou condição insegura.
                2. A ação imediata recomendada.
                3. O nível de risco.
                4. A confiança da análise.

                Responda obrigatoriamente neste formato:

                RISCO: descreva claramente o risco identificado
                NIVEL: Baixo, Médio ou Alto
                ACAO: descreva a ação imediata recomendada
                CONFIANCA: percentual estimado
            """.trimIndent()

            val responseText = callGemini(base64Image, prompt)

            val risk = extractAfter(responseText, "RISCO:")
                .ifBlank { responseText }

            val level = extractAfter(responseText, "NIVEL:")
                .ifBlank { "Médio" }

            val action = extractAfter(responseText, "ACAO:")
                .ifBlank { "Avaliar a condição no local, corrigir o risco e registrar evidência da tratativa." }

            val confidence = extractAfter(responseText, "CONFIANCA:")
                .ifBlank { "85%" }

            Result.success(
                AiAnalysisResult(
                    riskLevel = level,
                    riskDescription = risk,
                    confidence = confidence,
                    recommendations = listOf(action)
                )
            )

        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Erro desconhecido na análise da IA."))
        }
    }

    private fun callGemini(base64Image: String, prompt: String): String {
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(
                                JSONObject().put("text", prompt)
                            )
                            .put(
                                JSONObject().put(
                                    "inline_data",
                                    JSONObject()
                                        .put("mime_type", "image/jpeg")
                                        .put("data", base64Image)
                                )
                            )
                    )
                )
            )
        }

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.doOutput = true

        connection.outputStream.use { output ->
            output.write(requestJson.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode

        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }
                ?: "Erro HTTP $responseCode sem resposta da API."
        }

        if (responseCode !in 200..299) {
            throw Exception("Erro Gemini HTTP $responseCode: $responseText")
        }

        val jsonResponse = JSONObject(responseText)

        val candidates = jsonResponse.optJSONArray("candidates")
            ?: throw Exception("A IA não retornou candidatos. Resposta: $responseText")

        if (candidates.length() == 0) {
            throw Exception("A IA retornou resposta vazia.")
        }

        val content = candidates
            .getJSONObject(0)
            .optJSONObject("content")
            ?: throw Exception("Resposta sem conteúdo: $responseText")

        val parts = content.optJSONArray("parts")
            ?: throw Exception("Resposta sem partes de texto: $responseText")

        if (parts.length() == 0) {
            throw Exception("Resposta da IA sem texto.")
        }

        return parts
            .getJSONObject(0)
            .optString("text")
            .ifBlank {
                throw Exception("Texto da IA veio vazio.")
            }
    }

    private fun resizeAndEncodeImage(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return null

            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                768,
                768,
                true
            )

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            Base64.encodeToString(
                outputStream.toByteArray(),
                Base64.NO_WRAP
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAfter(text: String, label: String): String {
        val line = text.lines().firstOrNull {
            it.trim().startsWith(label, ignoreCase = true)
        }

        return line
            ?.substringAfter(label)
            ?.trim()
            ?: ""
    }
}
