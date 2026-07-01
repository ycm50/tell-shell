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
import java.util.concurrent.TimeUnit

/**
 * DeepSeek Chat Completion API 客户端
 * 兼容 OpenAI 格式
 */
class DeepSeekClient(
    private val baseUrl: String = "https://api.deepseek.com",
    private val apiKey: String = "",
    private val model: String = "deepseek-chat"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 调用 DeepSeek Chat Completion API，将自然语言转换为 shell 命令
     */
    suspend fun translateToCommand(
        userInput: String,
        appContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(userInput, appContext)
            val requestBody = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = SYSTEM_PROMPT),
                    Message(role = "user", content = prompt)
                ),
                temperature = 0.1,
                maxTokens = 500
            )

            val json = GsonProvider.gson.toJson(requestBody)
            val body = json.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("API error ${response.code}: $responseBody")
                )
            }

            val chatResponse = GsonProvider.gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            val command = chatResponse.choices?.firstOrNull()?.message?.content?.trim() ?: ""

            if (command.isBlank()) {
                return@withContext Result.failure(IOException("Empty response from API"))
            }

            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    /**
     * 获取可用模型列表
     */
    suspend fun listModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("API error ${response.code}: $responseBody")
                )
            }

            val modelResponse = GsonProvider.gson.fromJson(responseBody, ModelListResponse::class.java)
            val modelIds = modelResponse.data?.map { it.id }?.filter { id ->
                id.contains("deepseek") || id.contains("chat") || id.contains("reasoner")
            } ?: emptyList()

            if (modelIds.isEmpty()) {
                return@withContext Result.failure(IOException("No available models found"))
            }

            Result.success(modelIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 构建历史记录文本
            val historyText = historyItems.joinToString("\n---\n") { item ->
                buildString {
                    appendLine("时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))}")
                    appendLine("自然语言: ${item.naturalInput}")
                    appendLine("命令: ${item.generatedCommand}")
                    if (item.commandOutput.isNotBlank()) {
                        appendLine("输出: ${item.commandOutput.take(200)}")
                    }
                    if (item.appContext.isNotBlank()) {
                        appendLine("选中的应用: ${item.appContext}")
                    }
                }
            }

            // 替换模板中的占位符
            val prompt = analysisPromptTemplate
                .replace("{history}", historyText)
                .replace("{requirement}", requirement)

            val requestBody = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    Message(role = "user", content = prompt)
                ),
                temperature = 0.3,
                maxTokens = 1000
            )

            val json = GsonProvider.gson.toJson(requestBody)
            val body = json.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("API error ${response.code}: $responseBody")
                )
            }

            val chatResponse = GsonProvider.gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            val result = chatResponse.choices?.firstOrNull()?.message?.content?.trim() ?: ""

            if (result.isBlank()) {
                return@withContext Result.failure(IOException("Empty response from API"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * 系统提示词 — 严格约束只输出 shell 命令
         */
        const val SYSTEM_PROMPT = """You are a shell command generator for Android 16. 
The user describes what they want to do in natural language. Selected app context may be provided.

CRITICAL RULES:
- Output ONLY the raw shell command. No explanations, no markdown, no code blocks, no backticks.
- If multiple commands are needed, join them with " && " or " ; ".
- Use standard Android shell commands: pm, am, dumpsys, settings, input, wm, cmd, service, etc.
- For selected apps, use their package names for package-related operations.
- If the request is unclear, unsafe, or cannot be expressed as a shell command, output: echo error: <brief reason>
- Never include anything outside the command itself.
- Do NOT wrap the command in ``` or any other formatting."""
    }
}

// === API 请求/响应模型 ===

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.1,
    @SerializedName("max_tokens")
    val maxTokens: Int = 500
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
    val finishReason: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

/** 模型列表响应（兼容 OpenAI 格式） */
data class ModelListResponse(
    val data: List<ModelInfo>? = null
)

data class ModelInfo(
    val id: String,
    val ownedBy: String? = null
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
