package com.tellshell.app.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tellshell.app.data.AppInfo
import com.tellshell.app.data.HistoryItem
import com.tellshell.app.data.HistoryStore
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.network.DeepSeekClient
import com.tellshell.app.shell.ShizukuExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val appList: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val naturalInput: String = "",
    val searchQuery: String = "",
    val generatedCommand: String = "",
    val commandOutput: String = "",
    val isTranslating: Boolean = false,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null,
    val permissionState: ShizukuExecutor.PermissionState = ShizukuExecutor.PermissionState.Idle,
    val permissionSource: String = ""
) {
    /** 根据搜索词过滤的应用列表 */
    val filteredAppList: List<AppInfo>
        get() = if (searchQuery.isBlank()) appList
        else appList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val settingsStore = SettingsStore(application)
    private val historyStore = HistoryStore(application)
    private val executor = ShizukuExecutor()
    private var deepSeekClient: DeepSeekClient? = null
    private var lastHistoryItemId: String? = null

    init {
        loadAppList()
        loadApiConfig()
        updatePermissionState()
    }

    private fun loadApiConfig() {
        viewModelScope.launch {
            val baseUrl = settingsStore.baseUrl.first()
            val apiKey = settingsStore.apiKey.first()
            val model = settingsStore.model.first()
            deepSeekClient = DeepSeekClient(baseUrl = baseUrl, apiKey = apiKey, model = model)
        }
    }

    /** 重新加载 API 配置（从设置页返回后调用） */
    fun refreshApiConfig() {
        loadApiConfig()
    }

    /** 加载桌面应用列表 */
    private fun loadAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = getApplication<Application>().packageManager
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                    .distinctBy { it.activityInfo.packageName }
                    .sortedBy { it.loadLabel(pm).toString() }

                val apps = resolveInfos.map { info ->
                    val pkg = info.activityInfo.packageName
                    AppInfo(
                        packageName = pkg,
                        appName = info.loadLabel(pm)?.toString() ?: pkg,
                        isSystemApp = isSystemPackage(info.activityInfo)
                    )
                }

                _uiState.update { it.copy(appList = apps) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "加载应用列表失败: ${e.message}") }
            }
        }
    }

    private fun isSystemPackage(activityInfo: android.content.pm.ActivityInfo): Boolean {
        return (activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /** 切换应用选中状态 */
    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val newSelected = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = newSelected)
        }
    }

    /** 更新搜索关键词 */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** 更新自然语言输入 */
    fun updateNaturalInput(input: String) {
        _uiState.update { it.copy(naturalInput = input, errorMessage = null) }
    }

    /** 调用 DeepSeek API 翻译为命令 */
    fun translateToCommand() {
        val input = _uiState.value.naturalInput.trim()
        if (input.isBlank()) return

        val client = deepSeekClient
        if (client == null) {
            _uiState.update { it.copy(errorMessage = "请先在设置中配置 API Key 和 BaseURL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, errorMessage = null, generatedCommand = "") }

            // 构建已选应用的上下文信息
            val appContext = _uiState.value.selectedPackages.joinToString("\n") { pkg ->
                val app = _uiState.value.appList.find { it.packageName == pkg }
                "- ${app?.appName ?: pkg} ($pkg)"
            }

            val result = client.translateToCommand(input, appContext)
            result.onSuccess { command ->
                _uiState.update { it.copy(generatedCommand = command, isTranslating = false) }
                // 保存到历史记录
                val item = HistoryItem(
                    naturalInput = input,
                    generatedCommand = command,
                    appContext = appContext
                )
                lastHistoryItemId = item.id
                viewModelScope.launch {
                    historyStore.addItem(item)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "翻译失败: ${error.message}", isTranslating = false) }
            }
        }
    }

    /** 执行生成的命令 */
    fun executeCommand() {
        val command = _uiState.value.generatedCommand
        if (command.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, commandOutput = "", errorMessage = null) }

            val result = executor.execute(command)

            val output = buildString {
                if (result.stdout.isNotBlank()) {
                    appendLine(result.stdout)
                }
                if (result.stderr.isNotBlank()) {
                    appendLine("stderr: ${result.stderr}")
                }
                append("exit code: ${result.exitCode}")
            }
            _uiState.update {
                it.copy(
                    commandOutput = output,
                    isExecuting = false,
                    errorMessage = if (result.exitCode != 0 && result.stderr.isNotBlank()) {
                        "命令执行异常 (exit=$result.exitCode)"
                    } else null
                )
            }
            // 更新历史记录中的输出
            lastHistoryItemId?.let { id ->
                historyStore.historyItems.first().find { it.id == id }?.let { item ->
                    viewModelScope.launch {
                        historyStore.updateItem(item.copy(commandOutput = output))
                    }
                }
            }
        }
    }

    /** 请求 Shizuku/Sui 权限 */
    fun requestPermission() {
        viewModelScope.launch {
            executor.requestPermission { state ->
                _uiState.update {
                    it.copy(
                        permissionState = state,
                        permissionSource = if (state is ShizukuExecutor.PermissionState.Granted) state.source else ""
                    )
                }
            }
        }
    }

    private fun updatePermissionState() {
        _uiState.update { it.copy(permissionState = executor.permissionState) }
    }

    /** 清除输出 */
    fun clearOutput() {
        _uiState.update { it.copy(commandOutput = "", errorMessage = null) }
    }

    /** 清除生成的命令 */
    fun clearCommand() {
        _uiState.update { it.copy(generatedCommand = "") }
    }
}
