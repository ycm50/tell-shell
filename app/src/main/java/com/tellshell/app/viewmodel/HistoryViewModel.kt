package com.tellshell.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tellshell.app.data.HistoryItem
import com.tellshell.app.data.HistoryStore
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.network.DeepSeekClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val allItems: List<HistoryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet(),
    val analysisInput: String = "",
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null,
    val showDeleteConfirmDialog: Boolean = false
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyStore = HistoryStore(application)
    private val settingsStore = SettingsStore(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /** 加载历史记录 */
    private fun loadHistory() {
        viewModelScope.launch {
            historyStore.historyItems.collect { items ->
                _uiState.update { it.copy(allItems = items) }
            }
        }
    }

    /** 过滤后的历史记录（根据搜索关键词） */
    val filteredItems: List<HistoryItem>
        get() {
            val query = _uiState.value.searchQuery.trim().lowercase()
            if (query.isBlank()) return _uiState.value.allItems
            return _uiState.value.allItems.filter { item ->
                item.naturalInput.lowercase().contains(query) ||
                item.generatedCommand.lowercase().contains(query) ||
                item.appContext.lowercase().contains(query)
            }
        }

    /** 更新搜索关键词 */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** 切换选中状态 */
    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected)
        }
    }

    /** 全选/取消全选当前过滤后的条目 */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val visible = filteredItems
            val allSelected = visible.all { it.id in state.selectedIds }
            val newSelected = if (allSelected) {
                state.selectedIds - visible.map { it.id }.toSet()
            } else {
                state.selectedIds + visible.map { it.id }.toSet()
            }
            state.copy(selectedIds = newSelected)
        }
    }

    /** 显示/隐藏删除确认对话框 */
    fun showDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    /** 删除选中的条目 */
    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return

        viewModelScope.launch {
            historyStore.deleteItems(ids)
            _uiState.update {
                it.copy(
                    selectedIds = emptySet(),
                    showDeleteConfirmDialog = false
                )
            }
        }
    }

    /** 更新分析输入 */
    fun updateAnalysisInput(input: String) {
        _uiState.update { it.copy(analysisInput = input) }
    }

    /** 执行分析 */
    fun analyze(selectedItems: List<HistoryItem>) {
        val requirement = _uiState.value.analysisInput.trim()
        if (requirement.isBlank() || selectedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, analysisResult = null, errorMessage = null) }

            try {
                val baseUrl = settingsStore.baseUrl.first()
                val apiKey = settingsStore.apiKey.first()
                val model = settingsStore.model.first()
                val analysisPrompt = settingsStore.analysisPrompt.first()

                if (apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(isAnalyzing = false, errorMessage = "请先在设置中配置 API Key")
                    }
                    return@launch
                }

                val client = DeepSeekClient(baseUrl = baseUrl, apiKey = apiKey, model = model)
                val result = client.analyzeHistory(selectedItems, requirement, analysisPrompt)

                result.onSuccess { text ->
                    _uiState.update { it.copy(isAnalyzing = false, analysisResult = text) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(isAnalyzing = false, errorMessage = "分析失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAnalyzing = false, errorMessage = "分析失败: ${e.message}")
                }
            }
        }
    }

    /** 清除分析结果 */
    fun clearAnalysisResult() {
        _uiState.update { it.copy(analysisResult = null) }
    }
}
