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

        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-chat"

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
}
