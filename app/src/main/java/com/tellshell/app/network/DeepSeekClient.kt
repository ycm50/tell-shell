package com.tellshell.app.network

import com.google.gson.annotations.SerializedName
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
    private val apiKey: String = ""
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
                model = "deepseek-chat",
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
