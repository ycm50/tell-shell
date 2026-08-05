package com.tellshell.app.network

/**
 * 支持的 API 格式
 */
enum class ApiFormat(
    val shortName: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val description: String
) {
    OPENAI_COMPATIBLE(
        shortName = "OpenAI",
        displayName = "OpenAI 兼容",
        defaultBaseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        description = "Chat Completions 格式，兼容 DeepSeek、Moonshot、通义千问等"
    ),
    OPENAI_RESPONSES(
        shortName = "Responses",
        displayName = "OpenAI Responses",
        defaultBaseUrl = "https://api.openai.com",
        defaultModel = "gpt-4o-mini",
        description = "OpenAI 新版 Responses API"
    ),
    ANTHROPIC(
        shortName = "Anthropic",
        displayName = "Anthropic",
        defaultBaseUrl = "https://api.anthropic.com",
        defaultModel = "claude-3-7-sonnet-latest",
        description = "Claude Messages API"
    ),
    GEMINI(
        shortName = "Gemini",
        displayName = "Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        defaultModel = "gemini-2.0-flash",
        description = "Google Gemini 生成式 AI"
    );

    companion object {
        /** 反序列化：未知值回退到 OpenAI 兼容 */
        fun fromString(value: String?): ApiFormat =
            entries.firstOrNull { it.name == value } ?: OPENAI_COMPATIBLE
    }
}
