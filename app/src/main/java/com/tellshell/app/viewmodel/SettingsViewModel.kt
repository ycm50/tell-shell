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
    val systemPrompt: String = DeepSeekClient.SYSTEM_PROMPT,
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

            _uiState.update {
                it.copy(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    themeMode = themeMode
                )
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

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsStore.saveBaseUrl(state.baseUrl)
            settingsStore.saveApiKey(state.apiKey)
            settingsStore.saveThemeMode(state.themeMode)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
