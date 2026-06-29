package com.tellshell.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.data.ThemeMode
import com.tellshell.app.network.DeepSeekClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = SettingsStore.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.MATERIAL3,
    val selectedModel: String = SettingsStore.DEFAULT_MODEL,
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelError: String? = null,
    val systemPrompt: String = DeepSeekClient.SYSTEM_PROMPT,
    val analysisPrompt: String = SettingsStore.DEFAULT_ANALYSIS_PROMPT,
    val isSaved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val baseUrl = settingsStore.baseUrl.first()
            val apiKey = settingsStore.apiKey.first()
            val themeMode = settingsStore.themeMode.first()
            val model = settingsStore.model.first()
            val analysisPrompt = settingsStore.analysisPrompt.first()

            _uiState.update {
                it.copy(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    themeMode = themeMode,
                    selectedModel = model,
                    analysisPrompt = analysisPrompt
                )
            }

            // 如果有 API Key，自动加载模型列表
            if (apiKey.isNotBlank()) {
                loadModels(baseUrl, apiKey, model)
            }
        }
    }

    fun updateBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url, isSaved = false) }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, isSaved = false) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode, isSaved = false) }
    }

    fun updateSelectedModel(model: String) {
        _uiState.update { it.copy(selectedModel = model, isSaved = false) }
    }

    /** 加载模型列表 */
    fun loadModels() {
        val state = _uiState.value
        loadModels(state.baseUrl, state.apiKey, state.selectedModel)
    }

    private fun loadModels(baseUrl: String, apiKey: String, currentModel: String) {
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true, modelError = null) }

            val client = DeepSeekClient(baseUrl = baseUrl, apiKey = apiKey)
            val result = client.listModels()

            result.onSuccess { models ->
                val selected = if (currentModel in models) currentModel else models.first()
                _uiState.update {
                    it.copy(
                        availableModels = models,
                        selectedModel = selected,
                        isLoadingModels = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingModels = false,
                        modelError = "加载模型失败: ${error.message}"
                    )
                }
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsStore.saveBaseUrl(state.baseUrl)
            settingsStore.saveApiKey(state.apiKey)
            settingsStore.saveModel(state.selectedModel)
            settingsStore.saveThemeMode(state.themeMode)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
