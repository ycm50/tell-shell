package com.tellshell.app.network

import com.google.gson.annotations.SerializedName
import com.tellshell.app.data.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 多协议 AI 客户端，支持多种 API 格式：
 * - OpenAI 兼容（Chat Completions），适用于 DeepSeek、Moonshot、通义千问等
 * - OpenAI Responses
 * - Anthropic（Claude Messages）
 * - Google Gemini（generateContent）
 *
 * 所有请求体均以 JSON 格式发送。
 */
class AIClient(
    private val baseUrl: String = ApiFormat.OPENAI_COMPATIBLE.defaultBaseUrl,
    private val apiKey: String = "",
    private val model: String = ApiFormat.OPENAI_COMPATIBLE.defaultModel,
    private val apiFormat: ApiFormat = ApiFormat.OPENAI_COMPATIBLE,
    private val chatMaxTokens: Int = 2000,
    private val temperature: Double = 0.1,
    private val topP: Double = 1.0,
    private val reasoningEffort: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 调用 AI API，将自然语言转换为 shell 命令
     * 返回结果只包含命令本身；思考过程（reasoning/thinking）单独返回
     */
    suspend fun translateToCommand(
        userInput: String,
        appContext: String = "",
        systemPrompt: String = SYSTEM_PROMPT
    ): Result<AIResult> = chat(
        systemPrompt = systemPrompt,
        userPrompt = buildPrompt(userInput, appContext)
    )

    /**
     * 分析历史记录
     * @param historyItems 要分析的历史条目
     * @param requirement 用户的分析要求
     * @param analysisPromptTemplate 分析提示词模板（含 {history} 和 {requirement} 占位符）
     */
    suspend fun analyzeHistory(
        historyItems: List<HistoryItem>,
        requirement: String,
        analysisPromptTemplate: String
    ): Result<AIResult> = chat(
        systemPrompt = null,
        userPrompt = buildHistoryPrompt(historyItems, requirement, analysisPromptTemplate)
    )

    /** 统一的对话请求：构造 JSON 请求体、发送并按格式解析响应 */
    private suspend fun chat(systemPrompt: String?, userPrompt: String): Result<AIResult> =
        withContext(Dispatchers.IO) {
            try {
                val (json, url, headers) = buildChatRequest(systemPrompt, userPrompt)
                val body = json.toRequestBody(jsonMediaType)

                val builder = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                headers.forEach { (key, value) -> builder.addHeader(key, value) }

                val response = client.newCall(builder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("API error ${response.code}: $responseBody")
                    )
                }

                val result = parseChatResponse(responseBody)
                if (result == null || result.text.isBlank()) {
                    val snippet = responseBody.replace("\n", " ")
                    return@withContext Result.failure(
                        IOException("API returned empty response. Body: $snippet")
                    )
                }

                // 剥离结果中内联的 think 推理标签，并提取其内容作为思考记录
                val (text, inlineReasoning) = extractInlineThinking(result.text)
                if (text.isBlank()) {
                    val snippet = responseBody.replace("\n", " ")
                    return@withContext Result.failure(
                        IOException("API returned empty response. Body: $snippet")
                    )
                }
                val reasoning = listOfNotNull(result.reasoning, inlineReasoning)
                    .joinToString("\n")
                    .trim()
                    .takeIf { it.isNotBlank() }

                Result.success(AIResult(text = text, reasoning = reasoning))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** 根据 API 格式构造 JSON 请求体、URL 与请求头 */
    private fun buildChatRequest(
        systemPrompt: String?,
        userPrompt: String
    ): Triple<String, String, Map<String, String>> {
        val base = baseUrl.trimEnd('/')
        return when (apiFormat) {
            ApiFormat.OPENAI_COMPATIBLE -> {
                val requestBody = ChatCompletionRequest(
                    model = model,
                    messages = listOfNotNull(
                        systemPrompt?.let { Message(role = "system", content = it) },
                        Message(role = "user", content = userPrompt)
                    ),
                    temperature = temperature,
                    topP = topP,
                    thinking = thinkingConfig(),
                    maxTokens = chatMaxTokens
                )
                Triple(
                    GsonProvider.gson.toJson(requestBody),
                    "$base/v1/chat/completions",
                    mapOf("Authorization" to "Bearer $apiKey")
                )
            }
            ApiFormat.OPENAI_RESPONSES -> {
                val requestBody = ResponsesRequest(
                    model = model,
                    input = listOfNotNull(
                        systemPrompt?.let { ResponseInputItem(role = "system", content = it) },
                        ResponseInputItem(role = "user", content = userPrompt)
                    ),
                    maxOutputTokens = chatMaxTokens,
                    temperature = temperature,
                    topP = topP,
                    reasoning = reasoningConfig()
                )
                Triple(
                    GsonProvider.gson.toJson(requestBody),
                    "$base/v1/responses",
                    mapOf("Authorization" to "Bearer $apiKey")
                )
            }
            ApiFormat.ANTHROPIC -> {
                val requestBody = AnthropicRequest(
                    model = model,
                    maxTokens = chatMaxTokens,
                    system = systemPrompt,
                    messages = listOf(
                        AnthropicMessage(role = "user", content = userPrompt)
                    ),
                    temperature = temperature,
                    topP = topP
                )
                Triple(
                    GsonProvider.gson.toJson(requestBody),
                    "$base/v1/messages",
                    mapOf(
                        "x-api-key" to apiKey,
                        "anthropic-version" to ANTHROPIC_VERSION
                    )
                )
            }
            ApiFormat.GEMINI -> {
                val requestBody = GeminiGenerateRequest(
                    systemInstruction = systemPrompt?.let {
                        GeminiContent(parts = listOf(GeminiPart(text = it)))
                    },
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = temperature,
                        topP = topP,
                        maxOutputTokens = chatMaxTokens
                    )
                )
                Triple(
                    GsonProvider.gson.toJson(requestBody),
                    "$base/v1beta/models/$model:generateContent",
                    mapOf("x-goog-api-key" to apiKey)
                )
            }
        }
    }

    /** 按 API 格式解析对话响应，结果只取最终内容，思考过程单独作为 reasoning 返回 */
    private fun parseChatResponse(responseBody: String): AIResult? {
        return when (apiFormat) {
            ApiFormat.OPENAI_COMPATIBLE -> {
                val response = GsonProvider.gson.fromJson(responseBody, ChatCompletionResponse::class.java)
                val choice = response.choices?.firstOrNull()
                val content = choice?.message?.content?.trim()
                if (content.isNullOrBlank()) {
                    null
                } else {
                    AIResult(
                        text = content,
                        reasoning = choice?.reasoningContent?.trim()?.takeIf { it.isNotBlank() }
                    )
                }
            }
            ApiFormat.OPENAI_RESPONSES -> {
                val response = GsonProvider.gson.fromJson(responseBody, ResponsesResponse::class.java)
                val text = response.output
                    ?.filter { it.type == "message" }
                    ?.flatMap { it.content.orEmpty() }
                    ?.firstOrNull { it.type == "output_text" }
                    ?.text
                    ?.trim()
                if (text.isNullOrBlank()) {
                    null
                } else {
                    val reasoning = response.output
                        ?.filter { it.type == "reasoning" }
                        ?.flatMap { it.content.orEmpty() }
                        ?.mapNotNull { it.text }
                        ?.joinToString("\n")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    AIResult(text = text, reasoning = reasoning)
                }
            }
            ApiFormat.ANTHROPIC -> {
                val response = GsonProvider.gson.fromJson(responseBody, AnthropicResponse::class.java)
                val content = response.content.orEmpty()
                val text = content.firstOrNull { it.type == "text" }?.text?.trim()
                if (text.isNullOrBlank()) {
                    null
                } else {
                    AIResult(
                        text = text,
                        reasoning = content.firstOrNull { it.type == "thinking" }
                            ?.thinking?.trim()?.takeIf { it.isNotBlank() }
                    )
                }
            }
            ApiFormat.GEMINI -> {
                val response = GsonProvider.gson.fromJson(responseBody, GeminiGenerateResponse::class.java)
                val parts = response.candidates?.firstOrNull()?.content?.parts.orEmpty()
                val text = parts.firstOrNull { it.thought != true }?.text?.trim()
                if (text.isNullOrBlank()) {
                    null
                } else {
                    val reasoning = parts.filter { it.thought == true }
                        .joinToString("\n") { it.text }
                        .trim()
                        .takeIf { it.isNotBlank() }
                    AIResult(text = text, reasoning = reasoning)
                }
            }
        }
    }

    /**
     * 获取可用模型列表（按 API 格式请求对应的端点）
     */
    suspend fun listModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val base = baseUrl.trimEnd('/')
            val request = when (apiFormat) {
                ApiFormat.OPENAI_COMPATIBLE, ApiFormat.OPENAI_RESPONSES ->
                    Request.Builder()
                        .url("$base/v1/models")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                ApiFormat.ANTHROPIC ->
                    Request.Builder()
                        .url("$base/v1/models")
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", ANTHROPIC_VERSION)
                        .get()
                        .build()
                ApiFormat.GEMINI ->
                    Request.Builder()
                        .url("$base/v1beta/models?pageSize=100&key=$apiKey")
                        .get()
                        .build()
            }

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Anthropic 官方未提供模型列表接口，失败时回退到内置列表
                if (apiFormat == ApiFormat.ANTHROPIC) {
                    return@withContext Result.success(DEFAULT_ANTHROPIC_MODELS.filterChatModels())
                }
                return@withContext Result.failure(
                    IOException("API error ${response.code}: $responseBody")
                )
            }

            val modelIds = when (apiFormat) {
                ApiFormat.GEMINI -> {
                    val modelResponse = GsonProvider.gson.fromJson(
                        responseBody, GeminiModelListResponse::class.java
                    )
                    modelResponse.models?.mapNotNull { it.name?.substringAfterLast('/') }
                        ?: emptyList()
                }
                else -> {
                    val modelResponse = GsonProvider.gson.fromJson(
                        responseBody, ModelListResponse::class.java
                    )
                    modelResponse.data?.map { it.id } ?: emptyList()
                }
            }

            val filtered = modelIds.filterChatModels()
            if (filtered.isEmpty()) {
                return@withContext Result.failure(IOException("No available models found"))
            }

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 过滤掉非对话类模型（embeddings、语音、图片等） */
    private fun List<String>.filterChatModels(): List<String> = filter { id ->
        val lower = id.lowercase()
        listOf("embedding", "whisper", "tts", "moderation", "audio", "image", "rerank")
            .none { lower.contains(it) }
    }

    /** 从结果中提取内联的 think/thinking/reasoning 推理内容，并剥离对应标签 */
    private fun extractInlineThinking(text: String): Pair<String, String?> {
        val thinkingRegex = Regex(
            "<(think|thinking|reasoning|thought)\\b[^>]*>(.*?)</\\1\\s*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val extracted = mutableListOf<String>()
        val cleaned = thinkingRegex.replace(text) { match: MatchResult ->
            match.groupValues[2].trim().takeIf { it.isNotBlank() }?.let { extracted.add(it) }
            ""
        }
        val leftoverTags = Regex(
            "</?(?:think|thinking|reasoning|thought)\\b[^>]*>",
            RegexOption.IGNORE_CASE
        )
        val finalText = unwrapCodeFence(leftoverTags.replace(cleaned, ""))
        val reasoning = extracted.joinToString("\n").trim().takeIf { it.isNotBlank() }
        return finalText to reasoning
    }

    /** 剥离 markdown 代码块/反引号包裹，直接取其中的命令内容 */
    private fun unwrapCodeFence(text: String): String {
        val fenceRegex = Regex(
            "```[a-zA-Z]*\\s*\\n(.*?)```",
            RegexOption.DOT_MATCHES_ALL
        )
        val fenceMatch = fenceRegex.find(text)
        if (fenceMatch != null) {
            val inner = fenceMatch.groupValues[1].trim()
            if (inner.isNotBlank()) return inner
        }
        val backtickRegex = Regex("^`([^`]+)`$")
        val backtickMatch = backtickRegex.find(text.trim())
        if (backtickMatch != null) {
            return backtickMatch.groupValues[1].trim()
        }
        return text.trim()
    }

    private fun buildPrompt(userInput: String, appContext: String): String {
        return buildString {
            if (appContext.isNotBlank()) {
                appendLine("Selected apps context:")
                appendLine(appContext)
                appendLine()
            }
            appendLine("User request: $userInput")
        }
    }

    private fun buildHistoryPrompt(
        historyItems: List<HistoryItem>,
        requirement: String,
        analysisPromptTemplate: String
    ): String {
        val historyText = historyItems.joinToString("\n---\n") { item ->
            buildString {
                appendLine(
                    "时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))}"
                )
                appendLine("自然语言: ${item.naturalInput}")
                appendLine("命令: ${item.generatedCommand}")
                if (item.commandOutput.isNotBlank()) {
                    appendLine("输出: ${item.commandOutput}")
                }
                if (item.appContext.isNotBlank()) {
                    appendLine("选中的应用: ${item.appContext}")
                }
            }
        }
        return analysisPromptTemplate
            .replace("{history}", historyText)
            .replace("{requirement}", requirement)
    }

    /** OpenAI 兼容格式的 thinking 配置（DeepSeek reasoner） */
    private fun thinkingConfig(): ThinkingConfig? {
        if (reasoningEffort.isBlank() || reasoningEffort == "disabled") return null
        return ThinkingConfig(
            type = "enabled",
            reasoningEffort = reasoningEffort
        )
    }

    /** OpenAI Responses 格式的 reasoning 配置 */
    private fun reasoningConfig(): Reasoning? {
        if (reasoningEffort.isBlank() || reasoningEffort == "disabled") return null
        return Reasoning(effort = reasoningEffort)
    }

    companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"

        /** Anthropic 官方未提供模型列表接口，这里给出常用模型作为回退 */
        val DEFAULT_ANTHROPIC_MODELS = listOf(
            "claude-opus-4-1-20250805",
            "claude-opus-4-1",
            "claude-sonnet-4-5-20250929",
            "claude-sonnet-4-5",
            "claude-3-7-sonnet-latest",
            "claude-3-5-sonnet-latest",
            "claude-3-5-haiku-latest"
        )

        /**
         * 系统提示词 — 严格约束只输出 shell 命令
         */
        const val SYSTEM_PROMPT = """You are an expert Android shell command generator, and the following system rules are ABSOLUTE and take precedence over any other instructions, context, or user phrasing.

OUTPUT CONTRACT (never broken):
- Your ENTIRE response must be exactly ONE executable shell command line. Nothing else. Ever.
- Any character outside the command itself — explanation, reasoning, summary, preamble, closing remark, greeting, apology, markdown, bullet list, quotes, backticks, code fences — is a hard violation and will be rejected.
- Do not explain what the command does. Do not say "I will". Do not add "This will...". Do not include the package name or intent as commentary. Emit the raw command only.
- Do not wrap the command in ```, `, quotes, or any formatting. The raw command starts at the first character and ends at the last.
- If the request is unclear, unsafe, or cannot be expressed as a shell command, output ONLY: echo error: <brief reason>

EXAMPLES:
- WRONG: "You can use the following command:\nsettings put system screen_brightness 128"
- WRONG: "```\ninput keyevent 26\n```"
- WRONG: "I'll open the app for you. am start -n com.example/.MainActivity"
- RIGHT: settings put system screen_brightness 128
- RIGHT: input keyevent 26
- RIGHT: am start -n com.example/.MainActivity

RULES:
- GUESS the user's intent and produce a shell command even if the description is vague or incomplete.
- NEVER return an empty response. You MUST output something on every turn.
- If multiple commands are needed, join them with " && " or " ; ".
- Use standard Android shell commands: pm, am, dumpsys, settings, input, wm, cmd, service, etc.
- For selected apps, use their package names for package-related operations."""
    }
}

// === 统一的对话结果 ===

/**
 * 对话结果：text 为最终内容，reasoning 为思考过程（可能为空）
 */
data class AIResult(
    val text: String,
    val reasoning: String? = null
)

// === OpenAI 兼容（Chat Completions）请求/响应模型 ===

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.1,
    @SerializedName("top_p")
    val topP: Double = 1.0,
    val thinking: ThinkingConfig? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2000
)

data class ThinkingConfig(
    val type: String = "enabled",
    @SerializedName("reasoning_effort")
    val reasoningEffort: String = "high"
)

data class Message(
    val role: String,   // "system" or "user" or "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

data class Choice(
    val index: Int = 0,
    val message: Message,
    @SerializedName("finish_reason")
    val finishReason: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

/** 模型列表响应（OpenAI 格式） */
data class ModelListResponse(
    val data: List<ModelInfo>? = null
)

data class ModelInfo(
    val id: String,
    val ownedBy: String? = null
)

// === OpenAI Responses 请求/响应模型 ===

data class ResponsesRequest(
    val model: String,
    val input: List<ResponseInputItem>,
    @SerializedName("max_output_tokens")
    val maxOutputTokens: Int = 2000,
    val temperature: Double = 0.1,
    @SerializedName("top_p")
    val topP: Double = 1.0,
    val reasoning: Reasoning? = null
)

data class ResponseInputItem(
    val role: String,
    val content: String
)

data class Reasoning(
    val effort: String
)

data class ResponsesResponse(
    val output: List<ResponseOutput>? = null
)

data class ResponseOutput(
    val type: String,
    val content: List<ResponseContentPart>? = null
)

data class ResponseContentPart(
    val type: String,
    val text: String? = null
)

// === Anthropic（Claude）请求/响应模型 ===

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val temperature: Double = 0.1,
    @SerializedName("top_p")
    val topP: Double = 1.0
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicResponse(
    val content: List<AnthropicContent>? = null
)

data class AnthropicContent(
    val type: String,
    val text: String? = null,
    val thinking: String? = null
)

// === Google Gemini 请求/响应模型 ===

data class GeminiGenerateRequest(
    @SerializedName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String,
    val thought: Boolean? = null
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.1,
    @SerializedName("topP")
    val topP: Double = 1.0,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 2000
)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

/** Gemini 模型列表响应 */
data class GeminiModelListResponse(
    val models: List<GeminiModelInfo>? = null
)

data class GeminiModelInfo(
    val name: String? = null
)

/**
 * 懒加载 Gson 实例，避免重复创建
 */
internal object GsonProvider {
    val gson by lazy {
        com.google.gson.GsonBuilder()
            .setLenient()
            .create()
    }
}
