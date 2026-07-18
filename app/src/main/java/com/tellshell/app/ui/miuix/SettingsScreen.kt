package com.tellshell.app.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.tellshell.app.data.ThemeMode
import com.tellshell.app.viewmodel.SettingsUiState
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixSettingsScreen(
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
    val colorScheme = MiuixTheme.colorScheme
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSaved = true
            delay(2000)
            showSaved = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = "设置",
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "API 配置",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChange,
                label = "BaseURL",
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                label = "API Key",
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(24.dp))

            // === 模型选择 ===
            Text(
                text = "模型",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.isLoadingModels) {
                LinearProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (uiState.availableModels.isNotEmpty()) {
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    TextField(
                        value = uiState.selectedModel,
                        onValueChange = {},
                        label = "选择模型",
                        colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        useLabelAsPlaceholder = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "展开"
                            )
                        },
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
                    text = uiState.modelError ?: "点击刷新获取模型列表"
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
            TextField(
                value = uiState.chatMaxTokens.toString(),
                onValueChange = { text ->
                    text.toIntOrNull()?.let { onChatMaxTokensChange(it) }
                },
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                label = "最大 Token 数",
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(12.dp))

            // === Temperature ===
            TextField(
                value = uiState.temperature.toString(),
                onValueChange = { text ->
                    text.toDoubleOrNull()?.let { onTemperatureChange(it) }
                },
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                label = "Temperature（温度）",
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(12.dp))

            // === Top P ===
            TextField(
                value = uiState.topP.toString(),
                onValueChange = { text ->
                    text.toDoubleOrNull()?.let { onTopPChange(it) }
                },
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                label = "Top P（核采样）",
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(16.dp))

            // === 思考深度 ===
            Text(
                text = "思考深度"
            )
            Spacer(Modifier.height(8.dp))

            val efforts = listOf("disabled" to "关闭", "high" to "高", "max" to "最高")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                efforts.forEach { (value, label) ->
                    Button(
                        onClick = { onReasoningEffortChange(value) },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.reasoningEffort == value) {
                            Text("● $label")
                        } else {
                            Text(label)
                        }
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
                fontSize = 12.sp,
                color = colorScheme.onSecondary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "主题",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = uiState.themeMode == mode
                        Button(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSelected) "✓ ${mode.displayName}" else mode.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "系统提示词",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = uiState.systemPrompt,
                onValueChange = onSystemPromptChange,
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "分析提示词",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = uiState.analysisPrompt,
                onValueChange = onAnalysisPromptChange,
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )

            Spacer(Modifier.height(24.dp))

            // === 显示所有应用 ===
            Text(
                text = "应用列表"
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "显示所有已安装应用"
                )
                top.yukonga.miuix.kmp.basic.Switch(
                    checked = uiState.showAllApps,
                    onCheckedChange = onShowAllAppsChange
                )
            }
            Text(
                text = if (uiState.showAllApps) "显示手机中所有已安装的应用（含系统应用）"
                       else "仅显示桌面可见的应用",
                fontSize = 12.sp,
                color = colorScheme.onSecondary
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSave
            ) {
                Text("保存设置")
            }

            if (showSaved) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "设置已保存", color = colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
