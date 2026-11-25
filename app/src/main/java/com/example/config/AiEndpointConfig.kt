package com.example.config

import com.example.BuildConfig

/**
 * Centralized configuration for the OpenAI Chat Completions REST endpoint.
 * Supports reading `VITE_OPENAI_API_KEY` from `.env` / AI Studio Secrets (`BuildConfig`)
 * as well as a temporary in-app API key entered by the user in Settings for testing and demos.
 */
object AiEndpointConfig {
    private const val PLACEHOLDER_KEY = "YOUR_OPENAI_API_KEY"
    private const val DEFAULT_OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"

    @Volatile
    private var runtimeApiKey: String = ""

    val baseUrl: String
        get() = BuildConfig.AI_BASE_URL.trim().ifBlank { DEFAULT_OPENAI_ENDPOINT }

    val modelName: String
        get() = BuildConfig.AI_MODEL_NAME.trim().ifBlank { DEFAULT_OPENAI_MODEL }

    val envApiKey: String
        get() = BuildConfig.VITE_OPENAI_API_KEY.trim()

    fun updateRuntimeApiKey(key: String) {
        runtimeApiKey = key.trim()
    }

    fun hasRuntimeKey(): Boolean {
        return runtimeApiKey.isNotBlank() && runtimeApiKey != PLACEHOLDER_KEY
    }

    fun hasEnvKey(): Boolean {
        return envApiKey.isNotBlank() && envApiKey != PLACEHOLDER_KEY
    }

    fun effectiveApiKey(): String {
        return if (hasRuntimeKey()) {
            runtimeApiKey
        } else if (hasEnvKey()) {
            envApiKey
        } else {
            ""
        }
    }

    fun chatCompletionsUrl(): String {
        val clean = baseUrl.trimEnd('/')
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else {
            "$clean/chat/completions"
        }
    }

    fun hasUsableApiKey(): Boolean {
        return effectiveApiKey().isNotBlank()
    }

    /**
     * Direct public endpoints like OpenAI or DeepSeek require a real API key;
     * custom proxy URLs may optionally handle authentication server-side.
     */
    fun isReadyForRequest(): Boolean {
        if (hasUsableApiKey()) return true
        val isKnownKeyRequiredHost =
            baseUrl.contains("api.openai.com", ignoreCase = true) ||
                baseUrl.contains("api.deepseek.com", ignoreCase = true)
        return !isKnownKeyRequiredHost && baseUrl.startsWith("http", ignoreCase = true)
    }

    /**
     * Heuristic check for text-only models (e.g., deepseek-chat / deepseek-reasoner)
     * that do not accept `image_url` payloads, though server responses are also handled dynamically.
     */
    fun supportsVision(): Boolean {
        val lower = modelName.lowercase()
        return !lower.startsWith("deepseek-")
    }
}

