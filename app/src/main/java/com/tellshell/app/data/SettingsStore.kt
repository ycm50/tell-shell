package com.tellshell.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val KEY_ANALYSIS_PROMPT = stringPreferencesKey("analysis_prompt")
        private val KEY_SHOW_ALL_APPS = stringPreferencesKey("show_all_apps")
        private val KEY_CHAT_MAX_TOKENS = stringPreferencesKey("chat_max_tokens")
        private val KEY_TEMPERATURE = stringPreferencesKey("temperature")
        private val KEY_TOP_P = stringPreferencesKey("top_p")
        private val KEY_REASONING_EFFORT = stringPreferencesKey("reasoning_effort")

        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-chat"
        const val DEFAULT_CHAT_MAX_TOKENS = 500
        const val DEFAULT_TEMPERATURE = 0.1
        const val DEFAULT_TOP_P = 1.0

        const val DEFAULT_ANALYSIS_PROMPT = """你是一个 Android 操作历史分析助手。用户会提供一段历史操作记录（包含自然语言描述和实际执行的 shell 命令），以及用户的分析要求。

请根据用户要求，对历史记录进行分析和总结。

分析要求：{requirement}

历史记录：
{history}

请直接给出分析结果，不要添加无关的解释。"""
    }

    /** BaseURL */
    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = url
        }
    }

    /** API Key */
    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key
        }
    }

    /** 主题模式 */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "miuix" -> ThemeMode.MIUIX
            else -> ThemeMode.MATERIAL3
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = when (mode) {
                ThemeMode.MATERIAL3 -> "material3"
                ThemeMode.MIUIX -> "miuix"
            }
        }
    }

    /** 模型 */
    val model: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: DEFAULT_MODEL
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODEL] = model
        }
    }

    /** 系统提示词 */
    val systemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SYSTEM_PROMPT] ?: ""
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SYSTEM_PROMPT] = prompt
        }
    }

    /** 分析提示词 */
    val analysisPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANALYSIS_PROMPT] ?: DEFAULT_ANALYSIS_PROMPT
    }

    suspend fun saveAnalysisPrompt(prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ANALYSIS_PROMPT] = prompt
        }
    }

    /** 是否显示所有应用（默认仅桌面可见应用） */
    val showAllApps: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_ALL_APPS] == "true"
    }

    suspend fun saveShowAllApps(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_ALL_APPS] = show.toString()
        }
    }

    /** 翻译请求的最大 token 数 */
    val chatMaxTokens: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHAT_MAX_TOKENS]?.toIntOrNull() ?: DEFAULT_CHAT_MAX_TOKENS
    }

    suspend fun saveChatMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHAT_MAX_TOKENS] = tokens.toString()
        }
    }

    /** Temperature（温度） */
    val temperature: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_TEMPERATURE]?.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
    }

    suspend fun saveTemperature(value: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEMPERATURE] = value.toString()
        }
    }

    /** Top P（核采样） */
    val topP: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOP_P]?.toDoubleOrNull() ?: DEFAULT_TOP_P
    }

    suspend fun saveTopP(value: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOP_P] = value.toString()
        }
    }

    /** Reasoning Effort（思考深度） */
    val reasoningEffort: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_REASONING_EFFORT] ?: ""
    }

    suspend fun saveReasoningEffort(value: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REASONING_EFFORT] = value
        }
    }
}
