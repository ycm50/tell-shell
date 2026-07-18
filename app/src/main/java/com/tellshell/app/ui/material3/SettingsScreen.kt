package com.tellshell.app.ui.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.data.ThemeMode
import com.tellshell.app.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3SettingsScreen(
    uiState: SettingsUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onModelChange: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onAnalysisPromptChange: (String) -> Unit,
    onShowAllAppsChange: (Boolean) -> Unit,
    onChatMaxTokensChange: (Int) -> Unit,
    onTemperatureChange: (Double) -> Unit,
    onTopPChange: (Double) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var modelExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("设置已保存")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // === API 配置 ===
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("BaseURL") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(SettingsUiState().baseUrl) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("sk-xxxxxxxxxxxxxxxx") }
            )

            Spacer(Modifier.height(12.dp))

            // === 模型选择 ===
            Text(
                text = "模型",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            if (uiState.isLoadingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            } else if (uiState.availableModels.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择模型") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        uiState.availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    onModelChange(model)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = uiState.modelError ?: "点击刷新获取模型列表",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.modelError != null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onRefreshModels,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("刷新模型列表")
                }
            }

            Spacer(Modifier.height(16.dp))

            // === 最大 Token 数 ===
            OutlinedTextField(
                value = uiState.chatMaxTokens.toString(),
                onValueChange = { text ->
                    text.toIntOrNull()?.let { onChatMaxTokensChange(it) }
                },
                label = { Text("最大 Token 数") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(SettingsStore.DEFAULT_CHAT_MAX_TOKENS.toString()) }
            )

            Spacer(Modifier.height(12.dp))

            // === Temperature ===
            OutlinedTextField(
                value = uiState.temperature.toString(),
                onValueChange = { text ->
                    text.toDoubleOrNull()?.let { onTemperatureChange(it) }
                },
                label = { Text("Temperature（温度）") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(SettingsStore.DEFAULT_TEMPERATURE.toString()) }
            )

            Spacer(Modifier.height(12.dp))

            // === Top P ===
            OutlinedTextField(
                value = uiState.topP.toString(),
                onValueChange = { text ->
                    text.toDoubleOrNull()?.let { onTopPChange(it) }
                },
                label = { Text("Top P（核采样）") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(SettingsStore.DEFAULT_TOP_P.toString()) }
            )

            Spacer(Modifier.height(16.dp))

            // === 思考深度 ===
            Text(
                text = "思考深度",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            val efforts = listOf("disabled" to "关闭", "high" to "高", "max" to "最高")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                efforts.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = uiState.reasoningEffort == value,
                        onClick = { onReasoningEffortChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = efforts.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (uiState.reasoningEffort) {
                    "disabled" -> "不使用思考模式，响应更快"
                    "high" -> "默认思考深度"
                    "max" -> "最深思考，适合复杂任务"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // === 主题切换 ===
            Text(
                text = "主题",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        )
                    ) {
                        Text(mode.displayName)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // === 系统提示词 ===
            Text(
                text = "系统提示词",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.systemPrompt,
                onValueChange = onSystemPromptChange,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = false
            )

            Spacer(Modifier.height(24.dp))

            // === 分析提示词 ===
            Text(
                text = "分析提示词",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.analysisPrompt,
                onValueChange = onAnalysisPromptChange,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = false
            )

            Spacer(Modifier.height(24.dp))

            // === 显示所有应用 ===
            Text(
                text = "应用列表",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "显示所有已安装应用",
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.Switch(
                    checked = uiState.showAllApps,
                    onCheckedChange = onShowAllAppsChange
                )
            }
            Text(
                text = if (uiState.showAllApps) "显示手机中所有已安装的应用（含系统应用）"
                       else "仅显示桌面可见的应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // === 保存按钮 ===
            Button(
                onClick = onSave
            ) {
                Text("保存设置")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
